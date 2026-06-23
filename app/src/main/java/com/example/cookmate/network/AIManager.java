package com.example.cookmate.network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI 管理器 - 单例模式
 * 
 * 负责与阿里云通义千问 API（DashScope）进行交互，提供智能菜谱生成和聊天功能。
 * 使用 deepseek-v4-pro 模型进行自然语言处理。
 * 
 * 核心功能：
 * 1. generateRecipe: 根据现有食材智能生成创意菜谱
 * 2. chat: 通用聊天对话（不带上下文）
 * 3. chat(systemPrompt): 带系统提示词的对话（如食材上下文）
 * 
 * 技术架构：
 * - 单例模式确保全局只有一个实例
 * - 异步调用 API，在新线程中执行网络请求
 * - 使用 OkHttp 作为 HTTP 客户端
 * - 回调接口 AICallback 返回结果
 * 
 * API 端点：阿里云 DashScope（兼容 OpenAI 格式）
 * 模型：deepseek-v4-pro
 * 
 * 使用示例：
 * <pre>
 * // 初始化 API Key（在 MainActivity.onCreate 中）
 * AIManager.init("sk-your-api-key");
 * 
 * // 生成菜谱
 * AIManager.getInstance().generateRecipe(ingredients, new AICallback() {
 *     @Override
 *     public void onSuccess(String result) {
 *         // 处理 AI 生成的菜谱
 *     }
 *     
 *     @Override
 *     public void onError(String error) {
 *         // 处理错误
 *     }
 * });
 * </pre>
 */
public class AIManager {

    /**
     * 阿里云 DashScope API 端点
     * 
     * 使用兼容 OpenAI 的格式，可以直接使用 OpenAI SDK 的调用方式。
     * 完整 URL: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
     */
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
    /**
     * 使用的 AI 模型名称
     * 
     * deepseek-v4-pro: 深度求索公司的高性能语言模型
     * 特点：中文理解能力强，适合菜谱生成等创意任务
     */
    private static final String MODEL = "deepseek-v4-pro";
    
    /**
     * API 密钥
     * 
     * 需要在应用启动时通过 init() 方法设置。
     * ⚠️ 注意：生产环境不应该将 API Key 硬编码在代码中！
     * 应该使用环境变量或安全的配置管理方案。
     */
    private static String apiKey = "";
    
    /**
     * 单例实例（懒汉式）
     */
    private static AIManager instance;
    
    /**
     * OkHttp 客户端实例
     * 
     * 配置较长的超时时间以适应 AI API 的响应延迟。
     */
    private final OkHttpClient client;

    /**
     * 私有构造函数 - 初始化 OkHttp 客户端
     * 
     * 超时配置说明：
     * - connectTimeout: 连接超时 30 秒（建立 TCP 连接）
     * - readTimeout: 读取超时 60 秒（等待 AI 响应，较长因为 AI 生成需要时间）
     * - writeTimeout: 写入超时 30 秒（发送请求数据）
     */
    private AIManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)  // 连接超时
                .readTimeout(60, TimeUnit.SECONDS)     // 读取超时（AI 响应较慢）
                .writeTimeout(30, TimeUnit.SECONDS)    // 写入超时
                .build();
    }

    /**
     * 获取单例实例（懒汉式，非线程安全）
     * 
     * 注意：当前实现不是线程安全的，如果多线程同时首次调用可能创建多个实例。
     * 改进方案：使用双重检查锁定或静态内部类方式。
     * 
     * @return AIManager 单例实例
     */
    public static AIManager getInstance() {
        if (instance == null) {
            instance = new AIManager();
        }
        return instance;
    }

    /**
     * 初始化 API 密钥
     * 
     * 必须在调用任何 AI 功能之前调用此方法。
     * 建议在 MainActivity.onCreate 中初始化。
     * 
     * @param key 阿里云 DashScope API 密钥
     */
    public static void init(String key) {
        apiKey = key;
    }

    /**
     * 根据现有食材智能生成菜谱
     * 
     * 工作流程：
     * 1. 将食材列表转换为顿号分隔的字符串
     * 2. 构建详细的 AI 提示词，包含严格的生成规则
     * 3. 调用 AI API 生成菜谱
     * 4. 通过回调返回结果
     * 
     * 提示词设计要点：
     * - 角色设定：资深厨师
     * - 任务目标：从现有食材中挑选 1-3 种设计家常菜
     * - 多样性保证：8 条规则确保每次生成的菜谱不重复
     *   1. 不同的食材组合
     *   2. 菜名要有区分度
     *   3. 可单独成菜或搭配
     *   4. 优先简单快手
     *   5. 烹饪方式轮换
     *   6. 利用食材不同部位
     *   7. 同食材不同手法
     *   8. 菜类型轮换（炒/蒸/煮/炖/凉拌/煎/焖）
     * - 输出格式：严格规定返回格式便于解析
     * 
     * 使用场景：RecipeFragment AI 生成菜谱功能
     * 
     * @param ingredients 可用食材列表（如 ["番茄", "鸡蛋", "青菜"]）
     * @param callback AI 响应回调
     */
    public void generateRecipe(List<String> ingredients, AICallback callback) {
        // 将食材列表转换为顿号分隔的字符串
        StringBuilder sb = new StringBuilder();
        for (String s : ingredients) {
            sb.append(s).append("、");
        }
        // 移除最后一个顿号
        String ingStr = sb.toString().replaceAll("、$", "");

        // 构建详细的 AI 提示词
        String prompt = "你是一位资深厨师。用户冰箱里有这些食材：" + ingStr + "。\n\n" +
                "请从中随机挑选1-3种食材，设计一道简单家常菜。\n" +
                "规则：\n" +
                "1. 每次必须选不同的食材组合，禁止重复之前的搭配\n" +
                "2. 菜名要有区分度，禁止用同义词替换凑数,也不要高度相似的菜名\n" +
                "3. 可以只用1种食材单独成菜，也可以2-3种搭配\n" +
                "4. 优先考虑简单快手的做法\n" +
                "5. 烹饪方式每次必须轮换，不能连续两次相同\n" +
                "6. 充分利用食材的不同部位和特性来变化做法\n" +
                "7. 同一种食材要尝试不同的烹饪手法，不要每次做法雷同\n" +
                "8. 尽量每次推荐不同类型的菜（炒、蒸、煮、炖、凉拌、煎、焖等轮换）\n\n" +
                "严格按以下格式回复：\n" +
                "菜名：xxx\n" +
                "所用食材：xxx\n" +
                "需额外购买：xxx（没有就写\"无\"）\n" +
                "烹饪时间：xx分钟\n" +
                "难度：简单/中等/困难\n" +
                "步骤：\n1. xxx\n2. xxx\n3. xxx\n" +
                "小贴士：xxx";
        
        // 调用 AI API
        callAPI(prompt, callback);
    }

    /**
     * 通用聊天功能 - 不带上下文
     * 
     * 用于简单的问答对话，不包含额外的系统提示词。
     * 
     * 系统角色设定：CookMate 智能厨房助手
     * 职责：管理食材、推荐菜谱、减少食物浪费
     * 风格：友好简洁
     * 
     * 使用场景：AIFragment 普通对话
     * 
     * @param userMessage 用户消息
     * @param callback AI 响应回调
     */
    public void chat(String userMessage, AICallback callback) {
        String prompt = "你是 CookMate 智能厨房助手，帮用户管理食材、推荐菜谱、减少食物浪费。请友好简洁地回答。\n\n用户：" + userMessage;
        callAPI(prompt, callback);
    }

    /**
     * 核心 API 调用方法 - 在新线程中异步执行
     * 
     * 实现细节：
     * 1. 创建新线程（避免阻塞主线程）
     * 2. 构建符合 OpenAI 格式的 JSON 请求体
     * 3. 设置模型参数（temperature、max_tokens、seed）
     * 4. 发送 POST 请求到阿里云 API
     * 5. 解析响应，提取 AI 生成的内容
     * 6. 通过回调返回结果（成功或失败）
     * 
     * 请求体结构：
     * {
     *   "model": "deepseek-v4-pro",
     *   "temperature": 1.5,
     *   "max_tokens": 1000,
     *   "seed": 随机数,
     *   "messages": [
     *     {
     *       "role": "user",
     *       "content": "用户提示词"
     *     }
     *   ]
     * }
     * 
     * 响应结构：
     * {
     *   "choices": [
     *     {
     *       "message": {
     *         "content": "AI 生成的内容"
     *       }
     *     }
     *   ]
     * }
     * 
     * @param prompt 用户提示词
     * @param callback 响应回调
     */
    private void callAPI(String prompt, AICallback callback) {
        // 在新线程中执行网络请求（避免 ANR）
        new Thread(() -> {
            try {
                // 构建请求 JSON
                JSONObject json = new JSONObject();
                json.put("model", MODEL);                      // 模型名称
                json.put("temperature", 1.5);                  // 温度参数（1.5 较高，更富创意）
                json.put("max_tokens", 1000);                  // 最大 token 数（限制回复长度）
                json.put("seed", System.currentTimeMillis() % 100000);  // 随机种子（增加多样性）
                
                // 构建消息数组
                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");      // 角色：用户
                msg.put("content", prompt);   // 内容：提示词
                messages.put(msg);
                json.put("messages", messages);

                // 创建请求体
                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.parse("application/json")
                );
                // 构建 HTTP 请求
                Request request = new Request.Builder()
                        .url(API_URL)                                    // API 端点
                        .header("Authorization", "Bearer " + apiKey)    // 身份认证
                        .header("Content-Type", "application/json")     // 内容类型
                        .post(body)                                      // POST 请求
                        .build();

                // 执行请求并获取响应
                Response response = client.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                // 处理响应
                if (response.isSuccessful()) {
                    // HTTP 状态码 2xx，请求成功
                    JSONObject result = new JSONObject(responseBody);
                    JSONArray choices = result.getJSONArray("choices");
                    
                    // 提取 AI 生成的内容
                    String content = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    // 通过回调返回结果（去除首尾空白）
                    if (callback != null) {
                        callback.onSuccess(content.trim());
                    }
                } else {
                    // HTTP 状态码非 2xx，请求失败
                    if (callback != null) {
                        callback.onError("API错误: " + response.code() + " " + responseBody);
                    }
                }
            } catch (Exception e) {
                // 网络异常或 JSON 解析错误
                if (callback != null) {
                    callback.onError("网络错误: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * 带系统提示词的聊天功能
     * 
     * 允许传入自定义的系统提示词，用于特定场景的对话。
     * 
     * 使用场景：AIFragment 中需要根据用户食材回答问题
     * 示例：systemPrompt = "用户当前食材库：番茄、鸡蛋。请根据这些食材回答用户问题。"
     * 
     * @param userMessage 用户消息
     * @param systemPrompt 系统级提示词（提供上下文信息）
     * @param callback AI 响应回调
     */
    public void chat(String userMessage, String systemPrompt, AICallback callback) {
        // 将系统提示词和用户消息拼接
        String prompt = systemPrompt + "\n\n用户：" + userMessage;
        callAPI(prompt, callback);
    }
    
    /**
     * AI 回调接口
     * 
     * 用于异步接收 AI 响应结果。
     * 回调在新线程中执行，如果需要更新 UI，请使用 runOnUiThread。
     * 
     * 使用示例：
     * <pre>
     * AIManager.getInstance().chat("你好", new AICallback() {
     *     @Override
     *     public void onSuccess(String result) {
     *         // 在新线程中执行
     *         runOnUiThread(() -> {
     *             // 更新 UI
     *             textView.setText(result);
     *         });
     *     }
     *     
     *     @Override
     *     public void onError(String error) {
     *         runOnUiThread(() -> {
     *             Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
     *         });
     *     }
     * });
     * </pre>
     */
    public interface AICallback {
        /**
         * AI 响应成功回调
         * @param result AI 生成的文本内容
         */
        void onSuccess(String result);
        
        /**
         * AI 响应失败回调
         * @param error 错误信息（网络错误或 API 错误）
         */
        void onError(String error);
    }
}