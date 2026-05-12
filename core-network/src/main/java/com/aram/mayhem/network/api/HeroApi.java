package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.HeroDetailResponse;
import com.aram.mayhem.network.dto.HeroResponse;
import com.aram.mayhem.network.dto.PageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HeroApi {

    @GET("api/heroes")
    Call<Result<PageResponse<HeroResponse>>> getHeroes(
            @Query("keyword") String keyword,
            @Query("tier") String tier,
            @Query("sortBy") String sortBy,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/heroes/{id}")
    Call<Result<HeroDetailResponse>> getHeroDetail(@Path("id") long id);
}
