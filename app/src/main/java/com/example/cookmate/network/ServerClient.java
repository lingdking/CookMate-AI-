package com.example.cookmate.network;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * CookMate 服务器客户端类
 * 
 * 封装所有与后端服务器的 HTTP 通信，使用 OkHttp 库实现异步网络请求。
 * 提供菜谱、食材、用户认证、社区帖子等功能的 API 调用。
 * 
 * 技术架构：
 * - 使用 OkHttp 作为 HTTP 客户端
 * - 所有请求都是异步的（enqueue），避免阻塞主线程
 * - 使用 Handler 将回调结果切换到 UI 线程执行
 * - 统一的 OnResultListener 回调接口
 * 
 * 服务器地址：http://10.0.2.2:5000/api/
 * （10.0.2.2 是 Android 模拟器访问本地主机的特殊地址）
 * 
 * 使用示例：
 * <pre>
 * ServerClient.getRecipes(new ServerClient.OnResultListener() {
 *     @Override
 *     public void onSuccess(String json) {
 *         // 处理成功响应
 *     }
 *     
 *     @Override
 *     public void onError(String error) {
 *         // 处理错误
 *     }
 * });
 * </pre>
 */
public class ServerClient {

    /**
     * 后端服务器基础 URL
     * 
     * 注意：
     * - 10.0.2.2 是 Android 模拟器访问宿主机的特殊地址
     * - 生产环境应该使用域名和 HTTPS
     */
    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    /**
     * OkHttp 客户端实例（单例）
     * 
     * 配置说明：
     * - connectTimeout: 连接超时 10 秒
     * - readTimeout: 读取超时 10 秒
     * - 默认使用共享的连接池和线程池
     */
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)  // 连接超时时间
            .readTimeout(10, TimeUnit.SECONDS)     // 读取超时时间
            .build();

    /**
     * 主线程 Handler
     * 
     * 用于将网络请求的回调结果切换到 UI 线程执行，
     * 避免在子线程中更新 UI 导致崩溃。
     */
    private static final Handler handler = new Handler(Looper.getMainLooper());

    // ========== 获取所有菜谱（云端共享） ==========
    
    /**
     * 从服务器获取所有公开的菜谱列表
     * 
     * API: GET /api/recipes
     * 
     * 使用场景：RecipeFragment 加载云端菜谱
     * 
     * @param listener 回调监听器，成功时返回 JSON 格式数据
     */
    public static void getRecipes(OnResultListener listener) {
        Request request = new Request.Builder()
                .url(BASE_URL + "recipes")
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                // 网络请求失败（无网络连接、超时等）
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    // HTTP 状态码 2xx，请求成功
                    String json = response.body().string(); 
                    handler.post(() -> listener.onSuccess(json)); 
                } else { 
                    // HTTP 状态码非 2xx（4xx、5xx 等）
                    handler.post(() -> listener.onError("请求失败: " + response.code())); 
                }
            }
        });
    }

    // ========== 按用户获取菜谱 ==========
    
    /**
     * 根据用户 ID 获取该用户发布的所有菜谱
     * 
     * API: GET /api/recipes/user/{userId}
     * 
     * 使用场景：CreatePostActivity 加载用户的菜谱列表
     * 
     * @param userId 用户唯一标识
     * @param listener 回调监听器
     */
    public static void getRecipesByUser(long userId, OnResultListener listener) {
        Request request = new Request.Builder()
                .url(BASE_URL + "recipes/user/" + userId)
                .get()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    String json = response.body().string(); 
                    handler.post(() -> listener.onSuccess(json)); 
                } else { 
                    handler.post(() -> listener.onError("请求失败: " + response.code())); 
                }
            }
        });
    }

    // ========== 发布菜谱 ==========
    
    /**
     * 向服务器发布新菜谱
     * 
     * API: POST /api/recipes
     * Content-Type: application/json
     * 
     * 请求体示例：
     * {
     *   "name": "番茄炒蛋",
     *   "ingredients": "番茄、鸡蛋",
     *   "steps": "1. 切番茄...\n2. 打蛋...",
     *   "cookingTime": 15,
     *   "difficulty": "简单",
     *   "userId": 1
     * }
     * 
     * 使用场景：
     * - AI 生成菜谱后自动上传
     * - 用户手动添加菜谱
     * 
     * @param name 菜谱名称
     * @param ingredients 所需食材列表（逗号或顿号分隔）
     * @param steps 烹饪步骤详情
     * @param cookingTime 烹饪时长（分钟）
     * @param difficulty 难度等级（简单/中等/困难）
     * @param userId 发布者用户 ID
     * @param listener 回调监听器
     */
    public static void addRecipe(String name, String ingredients, String steps,
                                 int cookingTime, String difficulty, long userId,
                                 OnResultListener listener) {
        try {
            // 构建 JSON 请求体
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("ingredients", ingredients);
            body.put("steps", steps);
            body.put("cookingTime", cookingTime);
            body.put("difficulty", difficulty);
            body.put("userId", userId);
            
            RequestBody requestBody = RequestBody.create(
                body.toString(), 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(BASE_URL + "recipes")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build();
                
            client.newCall(request).enqueue(new Callback() {
                @Override 
                public void onFailure(Call call, IOException e) { 
                    handler.post(() -> listener.onError(e.getMessage())); 
                }
                @Override 
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) { 
                        String json = response.body().string(); 
                        handler.post(() -> listener.onSuccess(json)); 
                    } else { 
                        handler.post(() -> listener.onError("发布失败: " + response.code())); 
                    }
                }
            });
        } catch (Exception e) { 
            // JSON 构建失败
            listener.onError(e.getMessage()); 
        }
    }

    // ========== 删除菜谱 ==========
    
    /**
     * 从服务器删除指定菜谱
     * 
     * API: DELETE /api/recipes/{id}
     * 
     * 使用场景：用户删除不需要的菜谱
     * 
     * @param id 菜谱 ID
     * @param listener 回调监听器
     */
    public static void deleteRecipe(Long id, OnResultListener listener) {
        Request request = new Request.Builder()
            .url(BASE_URL + "recipes/" + id)
            .delete()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    handler.post(() -> listener.onSuccess("ok")); 
                } else { 
                    handler.post(() -> listener.onError("删除失败")); 
                }
            }
        });
    }

    // ========== 获取用户食材 ==========
    
    /**
     * 获取用户当前未消耗的所有食材
     * 
     * API: GET /api/food/{userId}
     * 
     * 使用场景：
     * - AIFragment 读取食材生成菜谱
     * - InventoryFragment 显示食材库存
     * 
     * @param userId 用户 ID
     * @param listener 回调监听器
     */
    public static void getFoodItems(long userId, OnResultListener listener) {
        Request request = new Request.Builder()
            .url(BASE_URL + "food/" + userId)
            .get()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    String json = response.body().string(); 
                    handler.post(() -> listener.onSuccess(json)); 
                } else { 
                    handler.post(() -> listener.onError("请求失败")); 
                }
            }
        });
    }

    // ========== 获取用户已消耗食材 ==========
    
    /**
     * 获取用户已标记为消耗的食材记录
     * 
     * API: GET /api/food/consumed/{userId}
     * 
     * 使用场景：InventoryFragment 统计浪费情况
     * 
     * @param userId 用户 ID
     * @param listener 回调监听器
     */
    public static void getConsumedFoodItems(long userId, OnResultListener listener) {
        Request request = new Request.Builder()
            .url(BASE_URL + "food/consumed/" + userId)
            .get()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    String json = response.body().string(); 
                    handler.post(() -> listener.onSuccess(json)); 
                } else { 
                    handler.post(() -> listener.onError("请求失败")); 
                }
            }
        });
    }

    // ========== 添加食材 ==========
    
    /**
     * 向用户的食材库添加新食材
     * 
     * API: POST /api/food
     * Content-Type: application/json
     * 
     * 请求体示例：
     * {
     *   "userId": 1,
     *   "name": "番茄",
     *   "category": "蔬菜",
     *   "quantity": "3个",
     *   "purchaseDate": "2024-06-04",
     *   "expiryDate": "2024-06-10",
     *   "status": "新鲜"
     * }
     * 
     * 使用场景：AddFoodActivity 添加新食材
     * 
     * @param userId 用户 ID
     * @param name 食材名称
     * @param category 食材类别（蔬菜/水果/肉类/乳制品/调料）
     * @param quantity 数量描述
     * @param purchaseDate 购买日期（yyyy-MM-dd 格式）
     * @param expiryDate 过期日期（yyyy-MM-dd 格式）
     * @param listener 回调监听器
     */
    public static void addFoodItem(long userId, String name, String category, String quantity,
                                   String purchaseDate, String expiryDate, OnResultListener listener) {
        try {
            JSONObject body = new JSONObject();
            body.put("userId", userId);
            body.put("name", name);
            body.put("category", category);
            body.put("quantity", quantity);
            body.put("purchaseDate", purchaseDate);
            body.put("expiryDate", expiryDate);
            body.put("status", "新鲜");  // 默认状态为新鲜
            
            RequestBody requestBody = RequestBody.create(
                body.toString(), 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(BASE_URL + "food")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build();
                
            client.newCall(request).enqueue(new Callback() {
                @Override 
                public void onFailure(Call call, IOException e) { 
                    handler.post(() -> listener.onError(e.getMessage())); 
                }
                @Override 
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) { 
                        String json = response.body().string(); 
                        handler.post(() -> listener.onSuccess(json)); 
                    } else { 
                        handler.post(() -> listener.onError("添加失败")); 
                    }
                }
            });
        } catch (Exception e) { 
            listener.onError(e.getMessage()); 
        }
    }

    // ========== 标记食材已消耗 ==========
    
    /**
     * 将指定食材标记为已消耗状态
     * 
     * API: PUT /api/food/{id}
     * Content-Type: application/json
     * 
     * 请求体：
     * {
     *   "status": "已消耗",
     *   "isConsumed": true
     * }
     * 
     * 使用场景：InventoryFragment 长按标记食材已使用
     * 
     * @param id 食材 ID
     * @param listener 回调监听器
     */
    public static void markFoodConsumed(long id, OnResultListener listener) {
        try {
            JSONObject body = new JSONObject();
            body.put("status", "已消耗");
            body.put("isConsumed", true);
            
            RequestBody requestBody = RequestBody.create(
                body.toString(), 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(BASE_URL + "food/" + id)
                .put(requestBody)
                .header("Content-Type", "application/json")
                .build();
                
            client.newCall(request).enqueue(new Callback() {
                @Override 
                public void onFailure(Call call, IOException e) { 
                    handler.post(() -> listener.onError(e.getMessage())); 
                }
                @Override 
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) { 
                        String json = response.body().string(); 
                        handler.post(() -> listener.onSuccess(json)); 
                    } else { 
                        handler.post(() -> listener.onError("操作失败")); 
                    }
                }
            });
        } catch (Exception e) { 
            listener.onError(e.getMessage()); 
        }
    }

    // ========== 删除食材 ==========
    
    /**
     * 从用户食材库中删除指定食材
     * 
     * API: DELETE /api/food/{id}
     * 
     * 使用场景：InventoryFragment 删除错误添加的食材
     * 
     * @param id 食材 ID
     * @param listener 回调监听器
     */
    public static void deleteFood(long id, OnResultListener listener) {
        Request request = new Request.Builder()
            .url(BASE_URL + "food/" + id)
            .delete()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    handler.post(() -> listener.onSuccess("ok")); 
                } else { 
                    handler.post(() -> listener.onError("删除失败")); 
                }
            }
        });
    }

    // ========== 清空已消耗 ==========
    
    /**
     * 清空用户所有已消耗的食材记录
     * 
     * API: DELETE /api/food/clear/{userId}
     * 
     * 使用场景：InventoryFragment 清理历史记录
     * 
     * @param userId 用户 ID
     * @param listener 回调监听器
     */
    public static void clearConsumed(long userId, OnResultListener listener) {
        Request request = new Request.Builder()
            .url(BASE_URL + "food/clear/" + userId)
            .delete()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    handler.post(() -> listener.onSuccess("ok")); 
                } else { 
                    handler.post(() -> listener.onError("清空失败")); 
                }
            }
        });
    }

    // ========== 清空所有 ==========
    
    /**
     * 清空用户所有食材（包括活跃和已消耗）
     * 
     * API: DELETE /api/food/clear/{userId}
     * 
     * 注意：此操作不可逆，会删除所有食材数据
     * 
     * 使用场景：InventoryFragment 重置食材库
     * 
     * @param userId 用户 ID
     * @param listener 回调监听器
     */
    public static void clearAllFood(long userId, OnResultListener listener) {
        Request request = new Request.Builder()
            .url(BASE_URL + "food/clear/" + userId)
            .delete()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    handler.post(() -> listener.onSuccess("ok")); 
                } else { 
                    handler.post(() -> listener.onError("清空失败")); 
                }
            }
        });
    }

    // ========== 登录 ==========
    
    /**
     * 用户登录验证
     * 
     * API: POST /api/user/login
     * Content-Type: application/json
     * 
     * 请求体：
     * {
     *   "username": "admin",
     *   "password": "123456"
     * }
     * 
     * 成功响应：
     * {
     *   "data": {
     *     "userId": 1,
     *     "username": "admin"
     *   }
     * }
     * 
     * 使用场景：LoginActivity 用户登录
     * 
     * @param username 用户名
     * @param password 密码
     * @param listener 回调监听器，成功时返回包含 userId 的 JSON 数据
     */
    public static void login(String username, String password, OnResultListener listener) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);
            
            RequestBody requestBody = RequestBody.create(
                body.toString(), 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(BASE_URL + "user/login")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build();
                
            client.newCall(request).enqueue(new Callback() {
                @Override 
                public void onFailure(Call call, IOException e) { 
                    handler.post(() -> listener.onError(e.getMessage())); 
                }
                @Override 
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) { 
                        String json = response.body().string(); 
                        handler.post(() -> listener.onSuccess(json)); 
                    } else { 
                        handler.post(() -> listener.onError("登录失败: " + response.code())); 
                    }
                }
            });
        } catch (Exception e) { 
            listener.onError(e.getMessage()); 
        }
    }

    // ========== 注册 ==========
    
    /**
     * 新用户注册
     * 
     * API: POST /api/user/register
     * Content-Type: application/json
     * 
     * 请求体：
     * {
     *   "username": "newuser",
     *   "password": "password123"
     * }
     * 
     * 使用场景：RegisterActivity 新用户注册
     * 
     * @param username 用户名
     * @param password 密码
     * @param listener 回调监听器
     */
    public static void register(String username, String password, OnResultListener listener) {
        try {
            JSONObject body = new JSONObject();
            body.put("username", username);
            body.put("password", password);
            
            RequestBody requestBody = RequestBody.create(
                body.toString(), 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(BASE_URL + "user/register")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build();
                
            client.newCall(request).enqueue(new Callback() {
                @Override 
                public void onFailure(Call call, IOException e) { 
                    handler.post(() -> listener.onError(e.getMessage())); 
                }
                @Override 
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) { 
                        String json = response.body().string(); 
                        handler.post(() -> listener.onSuccess(json)); 
                    } else { 
                        handler.post(() -> listener.onError("注册失败: " + response.code())); 
                    }
                }
            });
        } catch (Exception e) { 
            listener.onError(e.getMessage()); 
        }
    }

    // ========== 回调接口 ==========
    
    /**
     * 网络请求结果回调接口
     * 
     * 所有网络请求都通过此接口返回结果。
     * 回调会在 UI 线程执行，可以直接更新界面。
     * 
     * 使用示例：
     * <pre>
     * ServerClient.getRecipes(new OnResultListener() {
     *     @Override
     *     public void onSuccess(String json) {
     *         // 解析 JSON，更新 UI
     *     }
     *     
     *     @Override
     *     public void onError(String error) {
     *         // 显示错误提示
     *     }
     * });
     * </pre>
     */
    public interface OnResultListener {
        /**
         * 请求成功回调
         * @param json 服务器返回的 JSON 字符串
         */
        void onSuccess(String json);
        
        /**
         * 请求失败回调
         * @param error 错误信息
         */
        void onError(String error);
    }
    
    // ========== 社区帖子相关 API ==========
    
    /**
     * 获取所有社区帖子列表
     * 
     * API: GET /api/posts
     * 
     * 使用场景：CommunityFragment 加载帖子列表
     * 
     * @param listener 回调监听器
     */
    public static void getPosts(OnResultListener listener) {
        Request request = new Request.Builder()
            .url(BASE_URL + "posts")
            .get()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    String json = response.body().string(); 
                    handler.post(() -> listener.onSuccess(json)); 
                } else { 
                    handler.post(() -> listener.onError("请求失败")); 
                }
            }
        });
    }
    
    /**
     * 删除社区帖子
     * 
     * API: DELETE /api/posts/{id}
     * 
     * 使用场景：CommunityFragment 删除自己的帖子
     * 
     * @param id 帖子 ID
     * @param listener 回调监听器
     */
    public static void deletePost(long id, OnResultListener listener) {
        Request request = new Request.Builder()
            .url(BASE_URL + "posts/" + id)
            .delete()
            .build();
            
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    handler.post(() -> listener.onSuccess("ok")); 
                } else { 
                    handler.post(() -> listener.onError("删除失败")); 
                }
            }
        });
    }
    
    /**
     * 注销用户账号（删除用户及其所有数据）
     * 
     * API: DELETE /api/user/{userId}
     * 
     * 警告：此操作不可逆，会删除用户的所有数据！
     * 
     * 使用场景：ProfileFragment 用户注销账号
     * 
     * @param userId 用户 ID
     * @param listener 回调监听器
     */
    public static void deleteUser(long userId, OnResultListener listener) {
        Request request = new Request.Builder()
                .url(BASE_URL + "user/" + userId)
                .delete()
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override 
            public void onFailure(Call call, IOException e) { 
                handler.post(() -> listener.onError(e.getMessage())); 
            }
            @Override 
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) { 
                    handler.post(() -> listener.onSuccess("ok")); 
                } else { 
                    handler.post(() -> listener.onError("删除失败")); 
                }
            }
        });
    }
    
    /**
     * 发布社区帖子（可附带菜谱）
     * 
     * API: POST /api/posts
     * Content-Type: application/json
     * 
     * 请求体示例：
     * {
     *   "userId": 1,
     *   "username": "张三",
     *   "title": "今天做了番茄炒蛋",
     *   "content": "超级好吃！",
     *   "recipeSteps": "1. 切番茄...\n2. 打蛋..."
     * }
     * 
     * 使用场景：CreatePostActivity 发布新帖子
     * 
     * @param userId 发布者用户 ID
     * @param username 发布者用户名
     * @param title 帖子标题
     * @param content 帖子内容
     * @param recipeSteps 附带的菜谱步骤（可选，可为空字符串）
     * @param listener 回调监听器
     */
    public static void addPost(long userId, String username, String title, String content, String recipeSteps, OnResultListener listener) {
        try {
            JSONObject body = new JSONObject();
            body.put("userId", userId);
            body.put("username", username);
            body.put("title", title);
            body.put("content", content);
            body.put("recipeSteps", recipeSteps);
            
            RequestBody rb = RequestBody.create(
                body.toString(), 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(BASE_URL + "posts")
                .post(rb)
                .header("Content-Type", "application/json")
                .build();
                
            client.newCall(request).enqueue(new Callback() {
                @Override 
                public void onFailure(Call call, IOException e) { 
                    handler.post(() -> listener.onError(e.getMessage())); 
                }
                @Override 
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) { 
                        String json = response.body().string(); 
                        handler.post(() -> listener.onSuccess(json)); 
                    } else {
                        // 读取错误响应体，提供更详细的错误信息
                        String errorBody = response.body().string();
                        handler.post(() -> listener.onError("发布失败: " + response.code() + " " + errorBody));
                    }
                }
            });
        } catch (Exception e) { 
            listener.onError(e.getMessage()); 
        }
    }
}