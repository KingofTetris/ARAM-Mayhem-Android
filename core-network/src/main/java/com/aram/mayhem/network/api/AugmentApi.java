package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.AugmentDetailResponse;
import com.aram.mayhem.network.dto.AugmentRecommendRequest;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.aram.mayhem.network.dto.AugmentResponse;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.network.dto.SynergyProgressResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 强化符文 API 接口（符文模块）
 *
 * 功能：提供符文列表查询、符文详情获取、套装进度查询、智能推荐功能
 * 基础路径：/api/augments
 *
 * @see AugmentResponse
 * @see AugmentDetailResponse
 * @see SynergyProgressResponse
 * @see AugmentRecommendResponse
 */
public interface AugmentApi {

    /**
     * 获取符文列表（分页）
     *
     * @param quality 品质筛选（MYTHIC-神话 / LEGENDARY-传说 / EPIC-史诗 / RARE-稀有，可选）
     * @param synergySet 套装筛选（可选）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return Call<Result<PageResponse<AugmentResponse>>> 分页符文列表
     */
    @GET("api/augments")
    Call<Result<PageResponse<AugmentResponse>>> getAugments(
            @Query("quality") String quality,
            @Query("synergySet") String synergySet,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * 获取符文详情
     *
     * @param id 符文ID
     * @return Call<Result<AugmentDetailResponse>> 符文详情
     */
    @GET("api/augments/{id}")
    Call<Result<AugmentDetailResponse>> getAugmentDetail(@Path("id") long id);

    /**
     * 获取套装进度
     *
     * @param augmentIds 已拥有的符文ID列表（逗号分隔）
     * @return Call<Result<List<SynergyProgressResponse>>> 套装进度列表
     */
    @GET("api/augments/synergy-progress")
    Call<Result<List<SynergyProgressResponse>>> getSynergyProgress(@Query("augmentIds") String augmentIds);

    /**
     * 获取符文推荐
     *
     * @param request 推荐请求体（包含英雄ID和已选符文列表）
     * @return Call<Result<List<AugmentRecommendResponse>>> 推荐方案列表
     */
    @POST("api/augments/recommend")
    Call<Result<List<AugmentRecommendResponse>>> getRecommendations(@Body AugmentRecommendRequest request);
}