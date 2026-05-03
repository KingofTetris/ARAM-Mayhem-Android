package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HeroApi {

    @GET("api/heroes")
    Call<Result<Object>> getHeroes(
            @Query("role") String role,
            @Query("tier") String tier,
            @Query("search") String search,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/heroes/{id}")
    Call<Result<Object>> getHeroDetail(@Path("id") long id);
}
