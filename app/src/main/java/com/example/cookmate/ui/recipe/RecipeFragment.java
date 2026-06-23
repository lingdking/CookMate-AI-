package com.example.cookmate.ui.recipe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cookmate.R;
import com.example.cookmate.data.model.Recipe;
import com.example.cookmate.data.repository.RecipeRepository;
import com.example.cookmate.network.AIManager;
import com.example.cookmate.network.ServerClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜谱浏览 Fragment
 * 
 * 这是菜谱页面的核心组件，提供以下功能：
 * 1. AI 智能生成菜谱：根据用户现有食材自动生成创意菜谱
 * 2. 云端菜谱同步：从服务器加载所有用户分享的菜谱
 * 3. 菜谱列表展示：动态显示菜谱名称，支持点击查看详情
 * 4. 菜谱管理：删除不需要的菜谱
 * 5. 手动添加菜谱：跳转到自定义菜谱添加页面
 * 
 * 工作流程：
 * - 用户点击"AI生成菜谱" → 获取用户食材 → 调用AI API → 解析结果 → 保存到本地和云端
 * - 用户点击"加载云端菜谱" → 从服务器获取所有菜谱 → 动态构建列表
 * 
 * 技术要点：
 * - 使用 isLoading 标志防止重复请求
 * - safeRun/safeToast 确保线程安全
 * - 动态创建 UI 元素（LinearLayout + TextView + Button）
 * - AI 返回文本的解析（菜名、烹饪时间、难度等）
 */
public class RecipeFragment extends Fragment {

    // 菜谱列表容器，动态添加菜谱项
    private LinearLayout recipeContainer;
    
    // 功能按钮
    private Button btnGenerate, btnLoadCloud;  // AI生成按钮、加载云端按钮
    
    // 空状态提示文本
    private TextView tvEmpty;
    
    // 菜谱数据仓库，用于本地数据库操作
    private RecipeRepository recipeRepository;
    
    // 云端菜谱数据列表
    private List<String> recipeNames = new ArrayList<>();   // 菜谱名称列表
    private List<Long> recipeIds = new ArrayList<>();       // 菜谱ID列表
    private List<String> recipeSteps = new ArrayList<>();   // 菜谱步骤列表
    
    // 加载状态标志，防止并发请求
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 填充布局文件
        View view = inflater.inflate(R.layout.fragment_recipe, container, false);
        
        // 初始化菜谱数据仓库
        recipeRepository = new RecipeRepository(requireActivity().getApplication());
        
        // 绑定视图组件
        recipeContainer = view.findViewById(R.id.recipe_container);
        tvEmpty = view.findViewById(R.id.tv_empty);
        btnGenerate = view.findViewById(R.id.btn_generate);
        btnLoadCloud = view.findViewById(R.id.btn_load_cloud);
        
        // 手动添加菜谱按钮
        Button btnAddCustom = view.findViewById(R.id.btn_add_custom);
        btnAddCustom.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddRecipeActivity.class)));
        
        // AI 生成菜谱按钮点击事件
        btnGenerate.setOnClickListener(v -> {
            // 防止重复点击
            if (isLoading) return;
            
            // 设置加载状态
            isLoading = true;
            btnGenerate.setEnabled(false);      // 禁用生成按钮
            btnLoadCloud.setEnabled(false);     // 禁用加载按钮
            btnGenerate.setText("AI思考中...");  // 显示加载提示
            
            // 第一步：从服务器获取用户的食材列表
            ServerClient.getFoodItems(getUserId(), new ServerClient.OnResultListener() {
                @Override
                public void onSuccess(String json) {
                    // 解析食材名称列表
                    List<String> names = new ArrayList<>();
                    try {
                        JSONArray data = new JSONObject(json).getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            names.add(data.getJSONObject(i).getString("name"));
                        }
                    } catch (Exception e) { 
                        e.printStackTrace(); 
                    }

                    // 检查是否有食材
                    if (names.isEmpty()) {
                        safeRun(() -> {
                            isLoading = false;
                            btnGenerate.setEnabled(true);
                            btnLoadCloud.setEnabled(true);
                            btnGenerate.setText("🤖 AI生成菜谱");
                            safeToast("请先添加食材！");
                        });
                        return;
                    }

                    // 第二步：调用 AI 生成菜谱
                    AIManager.getInstance().generateRecipe(names, new AIManager.AICallback() {
                        @Override
                        public void onSuccess(String result) { 
                            // AI 成功返回菜谱，处理结果
                            safeRun(() -> handleRecipeResult(result, names)); 
                        }
                        @Override
                        public void onError(String error) { 
                            // AI 调用失败
                            safeRun(() -> handleRecipeError(error)); 
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    // 获取食材失败
                    safeRun(() -> {
                        isLoading = false;
                        btnGenerate.setEnabled(true);
                        btnLoadCloud.setEnabled(true);
                        btnGenerate.setText("🤖 AI生成菜谱");
                        safeToast("获取食材失败: " + error);
                    });
                }
            });
        });

        // 加载云端菜谱按钮
        btnLoadCloud.setOnClickListener(v -> loadCloudRecipes());

        return view;
    }

    /**
     * 处理 AI 生成的菜谱结果
     * 
     * 工作流程：
     * 1. 解析 AI 返回的文本为 Recipe 对象
     * 2. 保存到本地数据库
     * 3. 上传到云端服务器
     * 4. 刷新菜谱列表
     * 
     * @param result AI 返回的菜谱文本
     * @param names 使用的食材名称列表
     */
    private void handleRecipeResult(String result, List<String> names) {
        // 解析 AI 返回的文本
        Recipe recipe = parseRecipe(result);
        
        // 保存到本地数据库
        recipeRepository.insertRecipe(recipe);
        
        // 准备食材字符串（优先使用 AI 提取的食材，否则使用传入的列表）
        String ingredientsStr = recipe.getUsedFoodItemIds() != null && !recipe.getUsedFoodItemIds().isEmpty()
                ? recipe.getUsedFoodItemIds() 
                : String.join("、", names);
        
        // 上传到云端服务器
        ServerClient.addRecipe(
            recipe.getName(),           // 菜谱名称
            ingredientsStr,             // 食材列表
            result,                     // 完整描述
            recipe.getCookingTime(),    // 烹饪时间
            recipe.getDifficulty(),     // 难度
            getUserId(),                // 用户ID
            new ServerClient.OnResultListener() {
                @Override 
                public void onSuccess(String json) { 
                    safeToast("AI菜谱已生成并分享到云端！"); 
                    loadCloudRecipes();  // 刷新列表
                }
                @Override 
                public void onError(String error) { 
                    safeRun(() -> { 
                        isLoading = false; 
                        btnGenerate.setEnabled(true); 
                        btnLoadCloud.setEnabled(true); 
                        btnGenerate.setText("🤖 AI生成菜谱"); 
                    }); 
                }
            }
        );
    }

    /**
     * 处理 AI 生成菜谱失败的情况
     * 
     * 恢复按钮状态并显示错误提示
     * 
     * @param error 错误信息
     */
    private void handleRecipeError(String error) {
        isLoading = false;
        btnGenerate.setEnabled(true);
        btnLoadCloud.setEnabled(true);
        btnGenerate.setText("🤖 AI生成菜谱");
        safeToast("AI失败: " + error);
    }

    /**
     * 安全地在 UI 线程执行操作
     * 
     * 检查 Fragment 是否仍然附加到 Activity，避免崩溃
     * 
     * @param action 要执行的 Runnable
     */
    private void safeRun(Runnable action) {
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    /**
     * 安全地显示 Toast 提示
     * 
     * 检查 Fragment 是否仍然附加到 Activity，避免崩溃
     * 
     * @param msg 提示消息
     */
    private void safeToast(String msg) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 获取当前登录用户的 ID
     * 
     * 从 SharedPreferences 读取 userId，默认为 1
     * 
     * @return 用户 ID
     */
    private long getUserId() {
        return requireContext().getSharedPreferences("user", 0).getLong("userId", 1);
    }

    /**
     * 从云端服务器加载所有菜谱
     * 
     * 工作流程：
     * 1. 设置加载状态，禁用按钮
     * 2. 调用服务器 API 获取菜谱列表
     * 3. 解析 JSON 数据，填充三个列表（名称、ID、步骤）
     * 4. 更新 UI 显示菜谱列表
     * 5. 恢复按钮状态
     */
    private void loadCloudRecipes() {
        // 设置加载状态
        isLoading = true;
        btnGenerate.setEnabled(false);
        btnLoadCloud.setEnabled(false);
        
        // 调用服务器 API
        ServerClient.getRecipes(new ServerClient.OnResultListener() {
            @Override
            public void onSuccess(String json) {
                // 清空旧数据
                recipeNames.clear(); 
                recipeIds.clear(); 
                recipeSteps.clear();
                
                // 解析 JSON 数据
                try {
                    JSONArray data = new JSONObject(json).getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        recipeNames.add(obj.getString("name"));              // 菜谱名称
                        recipeIds.add(obj.getLong("id"));                    // 菜谱ID
                        recipeSteps.add(obj.optString("steps", ""));         // 烹饪步骤（可选）
                    }
                } catch (Exception e) { 
                    e.printStackTrace(); 
                }
                
                // 切换到 UI 线程更新界面
                safeRun(() -> {
                    updateRecipeList();  // 更新列表显示
                    isLoading = false;
                    btnGenerate.setEnabled(true);
                    btnLoadCloud.setEnabled(true);
                    btnGenerate.setText("🤖 AI生成菜谱");
                    safeToast("加载了 " + recipeNames.size() + " 个菜谱");
                });
            }
            
            @Override 
            public void onError(String error) {
                safeRun(() -> { 
                    isLoading = false; 
                    btnGenerate.setEnabled(true); 
                    btnLoadCloud.setEnabled(true); 
                    btnGenerate.setText("🤖 AI生成菜谱"); 
                    safeToast("加载失败: " + error); 
                });
            }
        });
    }

    /**
     * 删除指定菜谱
     * 
     * 调用服务器 API 删除菜谱，成功后重新加载列表
     * 
     * @param id 要删除的菜谱 ID
     */
    private void removeRecipe(Long id) {
        ServerClient.deleteRecipe(id, new ServerClient.OnResultListener() {
            @Override 
            public void onSuccess(String json) { 
                safeRun(() -> { 
                    safeToast("已删除"); 
                    loadCloudRecipes();  // 重新加载列表
                }); 
            }
            @Override 
            public void onError(String error) { 
                safeRun(() -> safeToast("删除失败")); 
            }
        });
    }

    /**
     * 更新菜谱列表 UI
     * 
     * 动态创建线性布局，为每个菜谱添加一行（名称 + 删除按钮）
     * 
     * 实现细节：
     * 1. 清空容器中的所有视图
     * 2. 如果列表为空，显示空状态提示
     * 3. 遍历菜谱列表，为每个菜谱创建：
     *    - TextView：显示菜谱名称，点击跳转到详情页
     *    - Button：删除按钮，红色背景
     * 4. 添加到容器中
     */
    private void updateRecipeList() {
        // 检查 Fragment 是否仍然附加
        if (!isAdded()) return;
        
        // 清空容器
        recipeContainer.removeAllViews();
        
        // 如果列表为空，显示空状态
        if (recipeNames.isEmpty()) { 
            tvEmpty.setVisibility(View.VISIBLE); 
            recipeContainer.setVisibility(View.GONE); 
            return; 
        }
        
        // 隐藏空状态，显示列表
        tvEmpty.setVisibility(View.GONE); 
        recipeContainer.setVisibility(View.VISIBLE);
        
        // 遍历菜谱列表，动态创建 UI
        for (int i = 0; i < recipeNames.size(); i++) {
            String name = recipeNames.get(i); 
            Long id = recipeIds.get(i); 
            String steps = recipeSteps.get(i);
            
            // 创建行容器（水平布局）
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL); 
            row.setPadding(20, 16, 20, 16); 
            row.setBackgroundColor(0xFFFFFFFF);
            
            // 创建菜谱名称文本
            TextView tv = new TextView(requireContext());
            tv.setText(name); 
            tv.setTextSize(16); 
            tv.setTextColor(0xFF1A1A1A);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            
            // 点击跳转到详情页
            tv.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), RecipeDetailActivity.class);
                intent.putExtra("recipe_name", name); 
                intent.putExtra("recipe_steps", steps); 
                startActivity(intent);
            });
            
            // 创建删除按钮
            Button btnDel = new Button(requireContext());
            btnDel.setText("删除"); 
            btnDel.setTextSize(12); 
            btnDel.setBackgroundColor(0xFFFF4444);  // 红色背景
            btnDel.setTextColor(0xFFFFFFFF);         // 白色文字
            btnDel.setOnClickListener(v -> removeRecipe(id));
            
            // 添加到行容器
            row.addView(tv); 
            row.addView(btnDel);
            
            // 设置行间距
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 4, 0, 4); 
            row.setLayoutParams(rowParams);
            
            // 添加到主容器
            recipeContainer.addView(row);
        }
    }

    /**
     * 解析 AI 返回的菜谱文本
     * 
     * 从 AI 生成的自然语言文本中提取结构化数据：
     * - 菜名：查找以"菜名："或"菜名:"开头的行
     * - 烹饪时间：查找包含"分钟"的行，提取数字
     * - 难度：查找以"难度："或"难度:"开头的行
     * - 所用食材：查找以"所用食材："或"所用食材:"开头的行
     * 
     * 容错处理：
     * - 如果未找到菜名，使用默认值"AI推荐菜谱"
     * - 如果解析烹饪时间失败，使用默认值 30 分钟
     * - 统一设置菜谱类型为"AI智能生成"
     * 
     * @param aiResult AI 返回的完整文本
     * @return 解析后的 Recipe 对象
     */
    private Recipe parseRecipe(String aiResult) {
        Recipe recipe = new Recipe(); 
        recipe.setDescription(aiResult);  // 保存完整描述
        
        // 逐行解析
        for (String line : aiResult.split("\n")) {
            line = line.trim();
            
            // 提取菜名
            if (line.startsWith("菜名：") || line.startsWith("菜名:")) {
                recipe.setName(line.replace("菜名：", "").replace("菜名:", "").trim());
            }
            // 提取烹饪时间（查找包含"分钟"的行）
            else if (line.contains("分钟")) { 
                try { 
                    recipe.setCookingTime(Integer.parseInt(line.replaceAll("[^0-9]", ""))); 
                } catch (Exception e) { 
                    recipe.setCookingTime(30);  // 默认 30 分钟
                } 
            }
            // 提取难度
            else if (line.startsWith("难度：") || line.startsWith("难度:")) {
                recipe.setDifficulty(line.replace("难度：", "").replace("难度:", "").trim());
            }
            // 提取所用食材
            else if (line.startsWith("所用食材：") || line.startsWith("所用食材:")) {
                recipe.setUsedFoodItemIds(line.replace("所用食材：", "").replace("所用食材:", "").trim());
            }
        }
        
        // 如果未找到菜名，使用默认值
        if (recipe.getName() == null || recipe.getName().isEmpty()) {
            recipe.setName("AI推荐菜谱");
        }
        
        // 设置菜谱类型
        recipe.setRecipeType("AI智能生成");
        
        return recipe;
    }
}