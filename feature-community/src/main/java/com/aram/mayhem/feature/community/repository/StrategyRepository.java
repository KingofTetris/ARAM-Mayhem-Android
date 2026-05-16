package com.aram.mayhem.feature.community.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.api.CommunityApi;
import com.aram.mayhem.network.dto.CreateStrategyRequest;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.network.dto.StrategyDetailResponse;
import com.aram.mayhem.network.dto.StrategyListResponse;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
/**
 * 攻略数据仓库
 *
 * 功能：攻略列表查询、攻略详情查询、攻略发布、投票/取消投票、我的攻略查询
 * 关联：CommunityApi, StrategyListResponse, StrategyDetailResponse
 */
public class StrategyRepository {

    private final CommunityApi communityApi;

    @Inject
    public StrategyRepository(CommunityApi communityApi) {
        this.communityApi = communityApi;
    }

    public LiveData<List<StrategyListResponse>> getStrategies(String sort, int page, int size) {
        MutableLiveData<List<StrategyListResponse>> result = new MutableLiveData<>();

        communityApi.getStrategies(sort, page, size).enqueue(new Callback<Result<PageResponse<StrategyListResponse>>>() {
            @Override
            public void onResponse(Call<Result<PageResponse<StrategyListResponse>>> call, Response<Result<PageResponse<StrategyListResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<StrategyListResponse> data = response.body().getData();
                    if (data != null && data.getRecords() != null) {
                        result.setValue(data.getRecords());
                    } else {
                        result.setValue(Collections.emptyList());
                    }
                } else {
                    result.setValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<Result<PageResponse<StrategyListResponse>>> call, Throwable t) {
                result.setValue(Collections.emptyList());
            }
        });

        return result;
    }

    public LiveData<StrategyDetailResponse> getStrategyDetail(long strategyId) {
        MutableLiveData<StrategyDetailResponse> result = new MutableLiveData<>();

        communityApi.getStrategyDetail(strategyId).enqueue(new Callback<Result<StrategyDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<StrategyDetailResponse>> call, Response<Result<StrategyDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(response.body().getData());
                } else {
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Result<StrategyDetailResponse>> call, Throwable t) {
                result.setValue(null);
            }
        });

        return result;
    }

    public LiveData<StrategyDetailResponse> publishStrategy(Long heroId, String title, String description,
                                                            List<Long> augmentIds, List<Long> itemIds) {
        MutableLiveData<StrategyDetailResponse> result = new MutableLiveData<>();

        CreateStrategyRequest request = new CreateStrategyRequest(heroId, title, description, augmentIds, itemIds);

        communityApi.publishStrategy(request).enqueue(new Callback<Result<StrategyDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<StrategyDetailResponse>> call, Response<Result<StrategyDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(response.body().getData());
                } else {
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Result<StrategyDetailResponse>> call, Throwable t) {
                result.setValue(null);
            }
        });

        return result;
    }

    public LiveData<Boolean> vote(long strategyId, String voteType) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        communityApi.vote(strategyId, new CommunityApi.VoteRequest(voteType)).enqueue(new Callback<Result<Void>>() {
            @Override
            public void onResponse(Call<Result<Void>> call, Response<Result<Void>> response) {
                result.setValue(response.isSuccessful() && response.body() != null && response.body().isSuccess());
            }

            @Override
            public void onFailure(Call<Result<Void>> call, Throwable t) {
                result.setValue(false);
            }
        });

        return result;
    }

    public LiveData<Boolean> cancelVote(long strategyId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        communityApi.cancelVote(strategyId).enqueue(new Callback<Result<Void>>() {
            @Override
            public void onResponse(Call<Result<Void>> call, Response<Result<Void>> response) {
                result.setValue(response.isSuccessful() && response.body() != null && response.body().isSuccess());
            }

            @Override
            public void onFailure(Call<Result<Void>> call, Throwable t) {
                result.setValue(false);
            }
        });

        return result;
    }

    public LiveData<List<StrategyListResponse>> getMyStrategies() {
        MutableLiveData<List<StrategyListResponse>> result = new MutableLiveData<>();

        communityApi.getMyStrategies().enqueue(new Callback<Result<List<StrategyListResponse>>>() {
            @Override
            public void onResponse(Call<Result<List<StrategyListResponse>>> call, Response<Result<List<StrategyListResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<StrategyListResponse> data = response.body().getData();
                    result.setValue(data != null ? data : Collections.emptyList());
                } else {
                    result.setValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<Result<List<StrategyListResponse>>> call, Throwable t) {
                result.setValue(Collections.emptyList());
            }
        });

        return result;
    }
}