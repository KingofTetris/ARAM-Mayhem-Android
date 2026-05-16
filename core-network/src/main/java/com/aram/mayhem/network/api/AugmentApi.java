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
 * 强化符文 API 接口
 *
 * 端点：GET /api/augments（列表）、GET /api/augments/{id}（详情）
 *       GET /api/augments/synergy-progress（套装进度）、POST /api/augments/recommend（推荐）
 * 关联：AugmentResponse, AugmentDetailResponse, SynergyProgressResponse, AugmentRecommendResponse
 */
public interface AugmentApi {

    @GET("api/augments")
    Call<Result<PageResponse<AugmentResponse>>> getAugments(
            @Query("quality") String quality,
            @Query("synergySet") String synergySet,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/augments/{id}")
    Call<Result<AugmentDetailResponse>> getAugmentDetail(@Path("id") long id);

    @GET("api/augments/synergy-progress")
    Call<Result<List<SynergyProgressResponse>>> getSynergyProgress(@Query("augmentIds") String augmentIds);

    @POST("api/augments/recommend")
    Call<Result<List<AugmentRecommendResponse>>> getRecommendations(@Body AugmentRecommendRequest request);
}