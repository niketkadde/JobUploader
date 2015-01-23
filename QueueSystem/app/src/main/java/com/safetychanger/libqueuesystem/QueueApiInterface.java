package com.safetychanger.libqueuesystem;

import com.google.gson.JsonElement;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;

/**
 * Created by niket
 */
interface QueueApiInterface {

    @POST("/")
    void postLog(@Body JsonElement data, Callback<Response> callback);

    //@POST(QueueApiClient.API_COLLECTOR_LOG)
    @POST("/")
    Response postLog(@Body JsonElement type);
}
