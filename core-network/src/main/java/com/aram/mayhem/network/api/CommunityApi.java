package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.CreateStrategyRequest;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.network.dto.StrategyDetailResponse;
import com.aram.mayhem.network.dto.StrategyListResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 社区攻略 API 接口（社区模块）
 *
 * 功能：提供游戏攻略的列表查询、详情获取、发布、投票等功能
 * 基础路径：/api/strategies
 *
 * @see StrategyListResponse
 * @see StrategyDetailResponse
 * @see CreateStrategyRequest
 */
public interface CommunityApi {

    /**
     * 获取攻略列表（分页）
     *
     * @param sort 排序方式（hot-热门 / latest-最新）
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return Call<Result<PageResponse<StrategyListResponse>>> 分页攻略列表
     */
    @GET("api/strategies")
    Call<Result<PageResponse<StrategyListResponse>>> getStrategies(
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * 获取攻略详情
     *
     * @param id 攻略ID
     * @return Call<Result<StrategyDetailResponse>> 攻略详情
     */
    @GET("api/strategies/{id}")
    Call<Result<StrategyDetailResponse>> getStrategyDetail(@Path("id") long id);

    /**
     * 发布攻略
     *
     * @param request 发布攻略请求体
     * @return Call<Result<StrategyDetailResponse>> 发布后的攻略详情
     */
    @POST("api/strategies")
    Call<Result<StrategyDetailResponse>> publishStrategy(@Body CreateStrategyRequest request);

    /**
     * 获取当前用户发布的攻略列表
     *
     * @return Call<Result<List<StrategyListResponse>>> 用户攻略列表
     */
    @GET("api/strategies/my")
    Call<Result<java.util.List<StrategyListResponse>>> getMyStrategies();

    /**
     * 删除攻略
     *
     * @param id 攻略ID
     * @return Call<Result<Void>> 删除结果
     */
    @DELETE("api/strategies/{id}")
    Call<Result<Void>> deleteStrategy(@Path("id") long id);

    /**
     * 投票（点赞/点踩）
     *
     * @param strategyId 攻略ID
     * @param request 投票请求体（包含voteType）
     * @return Call<Result<Void>> 投票结果
     */
    @POST("api/strategies/{id}/vote")
    Call<Result<Void>> vote(@Path("id") long strategyId, @Body VoteRequest request);

    /**
     * 取消投票
     *
     * @param strategyId 攻略ID
     * @return Call<Result<Void>> 取消投票结果
     */
    @DELETE("api/strategies/{id}/vote")
    Call<Result<Void>> cancelVote(@Path("id") long strategyId);

    /**
     * 投票请求体内部类
     */
    class VoteRequest {
        /** 投票类型（UP-点赞 / DOWN-点踩） */
        public String voteType;

        /**
         * 构造函数
         *
         * @param voteType 投票类型
         */
        public VoteRequest(String voteType) {
            this.voteType = voteType;
        }
    }
}