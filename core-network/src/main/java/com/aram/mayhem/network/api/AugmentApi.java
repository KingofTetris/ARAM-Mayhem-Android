package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AugmentApi {

    @GET("api/augments")
    Call<Result<Object>> getAugments(
            @Query("quality") String quality,
            @Query("synergySet") String synergySet
    );

    @GET("api/augments/{id}")
    Call<Result<Object>> getAugmentDetail(@Path("id") long id);

    @GET("api/augments/synergy-progress")
    Call<Result<Object>> getSynergyProgress(@Query("augmentIds") String augmentIds);
}
