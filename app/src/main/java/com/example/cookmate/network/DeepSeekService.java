package com.example.cookmate.network;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface DeepSeekService {

    @POST("v1/chat/completions")
    Call<Map<String, Object>> chatCompletion(
            @Header("Authorization") String authorization,
            @Body Map<String, Object> body
    );
}