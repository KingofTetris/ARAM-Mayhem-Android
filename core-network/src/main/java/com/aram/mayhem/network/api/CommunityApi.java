package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CommunityApi {

    @GET("api/strategies")
    Call<Result<Object>> getStrategies(
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/strategies/{id}")
    Call<Result<Object>> getStrategyDetail(@Path("id") long id);

    @POST("api/strategies")
    Call<Result<Object>> publishStrategy(@Body Object request);

    @POST("api/strategies/{id}/vote")
    Call<Result<Object>> vote(@Path("id") long strategyId, @Body VoteRequest request);

    class VoteRequest {
        public String voteType;

        public VoteRequest(String voteType) {
            this.voteType = voteType;
        }
    }
}
