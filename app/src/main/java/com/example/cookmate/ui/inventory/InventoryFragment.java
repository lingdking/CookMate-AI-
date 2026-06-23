package com.example.cookmate.ui.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cookmate.R;
import com.example.cookmate.data.model.FoodItem;
import com.example.cookmate.network.ServerClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 库存管理Fragment
 * 显示用户的食材清单，包括：
 * - 当前活跃的食材列表
 * - 已消耗/浪费的食材统计
 * - 即将过期的食材提醒
 * 支持添加、删除、标记消耗等操作
 */
public class InventoryFragment extends Fragment {

    // 食材列表适配器
    private FoodItemAdapter adapter;
    // UI组件：显示总数、即将过期数、浪费数的文本视图
    private TextView tvCount, tvExpiring, tvWasted;
    // 活跃食材列表（未消耗的食材）
    private List<FoodItem> activeItems = new ArrayList<>();
    // 已消耗食材列表（被标记为已吃掉的食材）
    private List<FoodItem> consumedItems = new ArrayList<>();

    /**
     * 创建Fragment的视图布局
     * @param inflater 布局填充器
     * @param container 父容器
     * @param savedInstanceState 保存的状态
     * @return 填充后的视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    /**
     * 视图创建完成后的初始化操作
     * - 初始化RecyclerView和适配器
     * - 设置按钮点击事件监听器
     * - 加载食材数据
     * @param view 根视图
     * @param savedInstanceState 保存的状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 获取RecyclerView并设置布局管理器
        RecyclerView rvFoodItems = view.findViewById(R.id.rv_food_items);
        tvCount = view.findViewById(R.id.tv_count);
        tvExpiring = view.findViewById(R.id.tv_expiring);
        tvWasted = view.findViewById(R.id.tv_wasted);
        TextView btnAdd = view.findViewById(R.id.btn_add_food);
        TextView btnClearConsumed = view.findViewById(R.id.btn_clear_consumed);
        TextView btnClearAll = view.findViewById(R.id.btn_clear_all);

        // 设置线性布局管理器和适配器
        rvFoodItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FoodItemAdapter();
        rvFoodItems.setAdapter(adapter);

        // 设置按钮点击事件
        btnAdd.setOnClickListener(v -> startActivity(new Intent(getActivity(), AddFoodActivity.class)));
        btnClearConsumed.setOnClickListener(v -> clearConsumed());
        btnClearAll.setOnClickListener(v -> clearAll());

        // 首次加载食材数据
        loadFoodItems();
    }

    /**
     * Fragment恢复时重新加载数据
     * 确保从其他页面返回时显示最新数据
     */
    @Override
    public void onResume() { super.onResume(); loadFoodItems(); }

    /**
     * 获取当前登录用户的ID
     * 从SharedPreferences中读取用户信息
     * @return 用户ID，默认为1
     */
    private long getUserId() {
        return requireContext().getSharedPreferences("user", 0).getLong("userId", 1);
    }

    /**
     * 从服务器加载食材数据
     * 包括两个异步请求：
     * 1. 获取活跃食材列表
     * 2. 获取已消耗食材列表
     */
    private void loadFoodItems() {
        // 异步请求：获取活跃食材列表
        ServerClient.getFoodItems(getUserId(), new ServerClient.OnResultListener() {
            @Override
            public void onSuccess(String json) {
                activeItems.clear();
                try {
                    // 解析JSON响应数据
                    JSONArray data = new JSONObject(json).getJSONArray("data");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        FoodItem item = new FoodItem();
                        item.setId(obj.getLong("id"));
                        item.setName(obj.getString("name"));
                        item.setCategory(obj.optString("category", "其他"));
                        item.setQuantity(obj.optString("quantity", ""));
                        item.setStatus(obj.optString("status", "新鲜"));
                        try { item.setExpiryDate(sdf.parse(obj.optString("expiryDate", ""))); } catch (Exception e) {}
                        activeItems.add(item);
                    }
                } catch (Exception e) { e.printStackTrace(); }
                // 更新UI显示
                updateUI();
            }
            @Override public void onError(String e) { safeToast("加载失败: " + e); }
        });

        // 异步请求：获取已消耗食材列表（用于统计浪费数量）
        ServerClient.getConsumedFoodItems(getUserId(), new ServerClient.OnResultListener() {
            @Override
            public void onSuccess(String json) {
                consumedItems.clear();
                try {
                    JSONArray data = new JSONObject(json).getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        FoodItem item = new FoodItem();
                        item.setId(data.getJSONObject(i).getLong("id"));
                        consumedItems.add(item);
                    }
                } catch (Exception e) {}
                // 更新浪费数量显示
                tvWasted.setText(String.valueOf(consumedItems.size()));
            }
            @Override public void onError(String e) {}
        });
    }

    /**
     * 更新UI界面显示
     * - 刷新食材列表
     * - 更新总数量
     * - 统计并显示即将过期的食材数量
     */
    private void updateUI() {
        // 更新RecyclerView显示的食材列表
        adapter.submitList(activeItems);
        // 更新总数量显示
        tvCount.setText(String.valueOf(activeItems.size()));
        // 统计即将过期的食材数量
        int expiring = 0;
        for (FoodItem item : activeItems) if ("临近过期".equals(item.getStatus())) expiring++;
        tvExpiring.setText(String.valueOf(expiring));
    }

    /**
     * 清空已消耗的食材记录
     * 调用服务器API清除所有已标记为消耗的食材
     */
    private void clearConsumed() { ServerClient.clearConsumed(getUserId(), new ServerClient.OnResultListener() { @Override public void onSuccess(String j) { loadFoodItems(); safeToast("已清空"); } @Override public void onError(String e) { safeToast("清空失败"); } }); }
    
    /**
     * 清空所有食材（包括活跃和已消耗的）
     * 危险操作，会删除用户的所有食材数据
     */
    private void clearAll() { ServerClient.clearAllFood(getUserId(), new ServerClient.OnResultListener() { @Override public void onSuccess(String j) { loadFoodItems(); safeToast("已清空"); } @Override public void onError(String e) { safeToast("清空失败"); } }); }
    
    /**
     * 安全地显示Toast消息
     * 检查Fragment是否仍然附加到Activity，避免崩溃
     * @param msg 要显示的消息
     */
    private void safeToast(String msg) { if (isAdded() && getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show(); }
    
    /**
     * 将食材标记为已消耗
     * 用户长按食材项时触发此操作
     * @param id 食材ID
     */
    private void markAsConsumed(long id) { ServerClient.markFoodConsumed(id, new ServerClient.OnResultListener() { @Override public void onSuccess(String j) { loadFoodItems(); } @Override public void onError(String e) {} }); }
    
    /**
     * 删除食材
     * 从服务器和列表中彻底删除该食材
     * @param id 食材ID
     */
    private void deleteFood(long id) { ServerClient.deleteFood(id, new ServerClient.OnResultListener() { @Override public void onSuccess(String j) { loadFoodItems(); } @Override public void onError(String e) {} }); }

    /**
     * 食材列表适配器
     * 负责在RecyclerView中显示每个食材项的信息
     */
    private class FoodItemAdapter extends RecyclerView.Adapter<FoodItemAdapter.ViewHolder> {
        private List<FoodItem> items = new ArrayList<>();
        private SimpleDateFormat sdf = new SimpleDateFormat("M月d日", Locale.getDefault());
        
        /**
         * 提交新的食材列表并刷新显示
         * @param newItems 新的食材列表
         */
        public void submitList(List<FoodItem> newItems) { this.items = newItems; notifyDataSetChanged(); }
        
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) { return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_food, p, false)); }
        
        /**
         * 绑定数据到ViewHolder
         * 设置食材的名称、分类、数量、过期日期、状态等信息
         * 根据分类显示对应的图标emoji
         * 根据状态设置不同的颜色样式
         * @param h ViewHolder
         * @param pos 位置索引
         */
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            FoodItem item = items.get(pos);
            h.tvName.setText(item.getName()); h.tvCategory.setText(item.getCategory()); h.tvQuantity.setText("· " + item.getQuantity());
            h.tvExpiry.setText(item.getExpiryDate() != null ? sdf.format(item.getExpiryDate()) : ""); h.tvStatus.setText(item.getStatus());
            // 根据食材分类设置对应的图标
            String icon = "📦";
            switch (item.getCategory()) { case "蔬菜": icon = "🥬"; break; case "水果": icon = "🍎"; break; case "肉类": icon = "🥩"; break; case "乳制品": icon = "🥛"; break; case "调料": icon = "🧂"; break; }
            h.tvIcon.setText(icon);
            // 根据食材状态设置不同的颜色样式
            if ("临近过期".equals(item.getStatus())) { h.tvStatus.setTextColor(0xFFE74C3C); h.tvStatus.setBackgroundColor(0xFFFFEBEE); }
            else if ("已过期".equals(item.getStatus())) { h.tvStatus.setTextColor(0xFF999999); h.tvStatus.setBackgroundColor(0xFFEEEEEE); }
            else { h.tvStatus.setTextColor(0xFF27AE60); h.tvStatus.setBackgroundColor(0xFFE8F5E9); }
            // 长按标记为已消耗
            h.itemView.setOnLongClickListener(v -> { markAsConsumed(item.getId()); return true; });
            // 点击删除按钮删除食材
            h.tvDelete.setOnClickListener(v -> deleteFood(item.getId()));
        }
        @Override public int getItemCount() { return items.size(); }
        
        /**
         * ViewHolder类
         * 持有每个食材项的视图引用，避免重复查找
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvCategory, tvQuantity, tvExpiry, tvStatus, tvDelete;
            ViewHolder(@NonNull View v) { super(v); tvIcon = v.findViewById(R.id.tv_icon); tvName = v.findViewById(R.id.tv_food_name); tvCategory = v.findViewById(R.id.tv_food_category); tvQuantity = v.findViewById(R.id.tv_food_quantity); tvExpiry = v.findViewById(R.id.tv_expiry); tvStatus = v.findViewById(R.id.tv_status); tvDelete = v.findViewById(R.id.tv_delete); }
        }
    }
}