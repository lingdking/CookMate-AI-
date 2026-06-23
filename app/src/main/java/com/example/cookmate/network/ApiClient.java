package com.example.cookmate.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    // 百度AI平台接口地址
    private static final String BAIDU_AI_BASE_URL = "https://aip.baidubce.com/";
    // 高德地图API地址
    private static final String AMAP_BASE_URL = "https://restapi.amap.com/";

    private static Retrofit baiduRetrofit;
    private static Retrofit amapRetrofit;

    private static OkHttpClient createOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // 获取百度AI的Retrofit实例
    public static Retrofit getBaiduRetrofit() {
        if (baiduRetrofit == null) {
            baiduRetrofit = new Retrofit.Builder()
                    .baseUrl(BAIDU_AI_BASE_URL)
                    .client(createOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return baiduRetrofit;
    }

    // 获取高德的Retrofit实例
    public static Retrofit getAmapRetrofit() {
        if (amapRetrofit == null) {
            amapRetrofit = new Retrofit.Builder()
                    .baseUrl(AMAP_BASE_URL)
                    .client(createOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return amapRetrofit;
    }

    // ========== 便捷方法：获取Service接口实例 ==========

    public static BaiduAiService getBaiduAiService() {
        return getBaiduRetrofit().create(BaiduAiService.class);
    }

    public static AmapService getAmapService() {
        return getAmapRetrofit().create(AmapService.class);
    }
}