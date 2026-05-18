package com.example.smarthome;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GroqService {
    @POST("chat/completions")
    Call<GroqResponse> getChatCompletion(
            @Header("Authorization") String apiKey,
            @Body GroqRequest request
    );
}
