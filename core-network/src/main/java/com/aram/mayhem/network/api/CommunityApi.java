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

public interface CommunityApi {

    @GET("api/strategies")
    Call<Result<PageResponse<StrategyListResponse>>> getStrategies(
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/strategies/{id}")
    Call<Result<StrategyDetailResponse>> getStrategyDetail(@Path("id") long id);

    @POST("api/strategies")
    Call<Result<StrategyDetailResponse>> publishStrategy(@Body CreateStrategyRequest request);

    @GET("api/strategies/my")
    Call<Result<java.util.List<StrategyListResponse>>> getMyStrategies();

    @POST("api/strategies/{id}/vote")
    Call<Result<Void>> vote(@Path("id") long strategyId, @Body VoteRequest request);

    @DELETE("api/strategies/{id}/vote")
    Call<Result<Void>> cancelVote(@Path("id") long strategyId);

    class VoteRequest {
        public String voteType;

        public VoteRequest(String voteType) {
            this.voteType = voteType;
        }
    }
}