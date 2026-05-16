package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.HeroDetailResponse;
import com.aram.mayhem.network.dto.HeroResponse;
import com.aram.mayhem.network.dto.PageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 英雄 API 接口（英雄模块）
 *
 * 功能：提供英雄列表查询、英雄详情获取功能
 * 基础路径：/api/heroes
 *
 * @see HeroResponse
 * @see HeroDetailResponse
 */
public interface HeroApi {

    /**
     * 获取英雄列表（分页）
     *
     * @param keyword 搜索关键词（可选）
     * @param tier 梯级筛选（S+/S/A/B/C，可选）
     * @param sortBy 排序字段（winRate-胜率 / pickRate-选取率 / name-名称）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return Call<Result<PageResponse<HeroResponse>>> 分页英雄列表
     */
    @GET("api/heroes")
    Call<Result<PageResponse<HeroResponse>>> getHeroes(
            @Query("keyword") String keyword,
            @Query("tier") String tier,
            @Query("sortBy") String sortBy,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * 获取英雄详情
     *
     * @param id 英雄ID
     * @return Call<Result<HeroDetailResponse>> 英雄详情
     */
    @GET("api/heroes/{id}")
    Call<Result<HeroDetailResponse>> getHeroDetail(@Path("id") long id);
}