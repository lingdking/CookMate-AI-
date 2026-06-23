package com.example.cookmate.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * 高德地图 API接口定义
 *
 * 你需要先去 https://lbs.amap.com/ 注册并创建应用，
 * 获取 Web服务 API Key
 */
public interface AmapService {

    /**
     * 逆地理编码：根据经纬度获取位置描述
     */
    @GET("v3/geocode/regeo")
    Call<ResponseBody> reverseGeocode(
            @Query("key") String apiKey,
            @Query("location") String location,  // 格式："经度,纬度"
            @Query("output") String output        // JSON
    );

    /**
     * 周边搜索：搜索附近的POI
     */
    @GET("v3/place/around")
    Call<ResponseBody> searchAround(
            @Query("key") String apiKey,
            @Query("location") String location,
            @Query("radius") int radius,          // 搜索半径，单位米
            @Query("keywords") String keywords,   // 关键词
            @Query("output") String output
    );

    /**
     * IP定位
     */
    @GET("v3/ip")
    Call<ResponseBody> ipLocation(
            @Query("key") String apiKey,
            @Query("ip") String ip
    );
}