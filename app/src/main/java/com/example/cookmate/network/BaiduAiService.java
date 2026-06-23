package com.example.cookmate.network;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * 百度AI开放平台 API接口定义
 *
 * 你需要先去 https://ai.baidu.com/ 注册并创建应用，
 * 获取 API Key 和 Secret Key
 */
public interface BaiduAiService {

    /**
     * 获取Access Token（调用其他API的前提）
     * @param grantType 固定值 "client_credentials"
     * @param clientId  你的 API Key
     * @param clientSecret 你的 Secret Key
     */
    @GET("oauth/2.0/token")
    Call<ResponseBody> getAccessToken(
            @Query("grant_type") String grantType,
            @Query("client_id") String clientId,
            @Query("client_secret") String clientSecret
    );

    /**
     * 通用物体和场景识别（用于识别食材）
     * @param accessToken 通过 getAccessToken 获取
     * @param image 图片文件
     */
    @Multipart
    @POST("rest/2.0/image-classify/v2/advanced_general")
    Call<ResponseBody> imageRecognition(
            @Header("Content-Type") String contentType,
            @Query("access_token") String accessToken,
            @Part MultipartBody.Part image
    );

    /**
     * 自定义菜品识别
     * @param accessToken 访问令牌
     * @param image 图片Base64编码
     */
    @POST("rest/2.0/image-classify/v2/dish")
    Call<ResponseBody> dishRecognition(
            @Query("access_token") String accessToken,
            @Body RequestBody image
    );

    /**
     * 调用大语言模型生成菜谱（使用百度文心一言/ERNIE-Bot）
     * @param accessToken 访问令牌
     * @param requestBody 请求体，包含prompt等信息
     */
    @POST("rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions")
    Call<ResponseBody> generateRecipe(
            @Query("access_token") String accessToken,
            @Body Map<String, Object> requestBody
    );
}