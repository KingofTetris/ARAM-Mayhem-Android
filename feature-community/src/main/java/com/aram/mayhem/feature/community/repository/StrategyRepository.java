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

/**
 * 攻略数据仓库
 *
 * 功能：攻略列表查询、攻略详情查询、攻略发布、投票/取消投票、我的攻略查询
 * 关联：CommunityApi, StrategyListResponse, StrategyDetailResponse
 */
@Singleton
public class StrategyRepository {

    private final CommunityApi communityApi;

    @Inject
    public StrategyRepository(CommunityApi communityApi) {
        this.communityApi = communityApi;
    }

    /**
     * 获取攻略列表（社区模块）
     *
     * 作用：从服务器分页获取社区攻略列表，支持排序
     * 实现：采用 Retrofit 网络请求 + LiveData 数据回调模式
     *
     * @param sort 排序规则（latest=最新，hot=最热）
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return LiveData<List<StrategyListResponse>> 可观察的攻略列表，请求成功返回列表，失败返回空列表
     */
    public LiveData<List<StrategyListResponse>> getStrategies(String sort, int page, int size) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<List<StrategyListResponse>> result = new MutableLiveData<>();

        // 调用 Retrofit 接口发起异步网络请求：获取攻略列表
        communityApi.getStrategies(sort, page, size).enqueue(new Callback<Result<PageResponse<StrategyListResponse>>>() {
            @Override
            public void onResponse(Call<Result<PageResponse<StrategyListResponse>>> call, Response<Result<PageResponse<StrategyListResponse>>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<StrategyListResponse> data = response.body().getData();
                    // 判断分页数据有效：数据不为空且记录列表不为空
                    if (data != null && data.getRecords() != null) {
                        // 业务成功：设置攻略列表到 LiveData
                        result.setValue(data.getRecords());
                    } else {
                        // 数据为空：设置空列表
                        result.setValue(Collections.emptyList());
                    }
                } else {
                    // 业务失败：设置空列表
                    result.setValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<Result<PageResponse<StrategyListResponse>>> call, Throwable t) {
                // 网络异常：设置空列表
                result.setValue(Collections.emptyList());
            }
        });

        return result;
    }

    /**
     * 获取攻略详情（社区模块）
     *
     * 作用：根据攻略ID获取完整攻略内容，包括作者信息、正文、关联装备符文等
     * 实现：采用 Retrofit 网络请求 + LiveData 数据回调模式
     *
     * @param strategyId 攻略ID
     * @return LiveData<StrategyDetailResponse> 可观察的攻略详情数据，请求成功返回详情，失败返回null
     */
    public LiveData<StrategyDetailResponse> getStrategyDetail(long strategyId) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<StrategyDetailResponse> result = new MutableLiveData<>();

        // 调用 Retrofit 接口发起异步网络请求：获取攻略详情
        communityApi.getStrategyDetail(strategyId).enqueue(new Callback<Result<StrategyDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<StrategyDetailResponse>> call, Response<Result<StrategyDetailResponse>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // 业务成功：设置攻略详情到 LiveData
                    result.setValue(response.body().getData());
                } else {
                    // 业务失败：设置null表示获取失败
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Result<StrategyDetailResponse>> call, Throwable t) {
                // 网络异常：设置null表示获取失败
                result.setValue(null);
            }
        });

        return result;
    }

    /**
     * 发布游戏攻略（社区模块）
     *
     * 作用：将用户编辑的攻略信息通过网络接口提交到服务器，并返回发布后的攻略详情数据
     * 实现：采用 Retrofit 网络请求 + LiveData 数据回调模式
     *
     * @param heroId 关联英雄ID
     * @param title 攻略标题
     * @param description 攻略描述/内容
     * @param augmentIds 攻略关联的强化符文ID列表
     * @param itemIds 攻略关联的装备ID列表
     * @return LiveData<StrategyDetailResponse> 可观察的攻略详情数据，请求成功返回数据，失败/错误返回null
     */
    public LiveData<StrategyDetailResponse> publishStrategy(Long heroId, String title, String description,
                                                            List<Long> augmentIds, List<Long> itemIds) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<StrategyDetailResponse> result = new MutableLiveData<>();

        // 构建网络请求所需的实体类，封装所有传入的攻略参数
        CreateStrategyRequest request = new CreateStrategyRequest(heroId, title, description, augmentIds, itemIds);

        // 调用 Retrofit 接口发起异步网络请求：发布攻略
        communityApi.publishStrategy(request).enqueue(new Callback<Result<StrategyDetailResponse>>() {
            /**
             * 网络请求成功响应回调
             */
            @Override
            public void onResponse(Call<Result<StrategyDetailResponse>> call, Response<Result<StrategyDetailResponse>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // 业务成功：将服务器返回的攻略详情数据设置到 LiveData 中
                    result.setValue(response.body().getData());
                } else {
                    // 业务失败/数据异常：设置null表示发布失败
                    result.setValue(null);
                }
            }

            /**
             * 网络请求失败回调（网络异常、连接超时、解析错误等）
             */
            @Override
            public void onFailure(Call<Result<StrategyDetailResponse>> call, Throwable t) {
                // 网络异常：设置null表示发布失败
                result.setValue(null);
            }
        });

        // 返回 LiveData，UI层可通过观察此数据获取发布结果
        return result;
    }

    /**
     * 为攻略投票（社区模块）
     *
     * 作用：用户对攻略进行投票（点赞/点踩），同一用户对同一攻略只能投一次
     * 实现：采用 Retrofit 网络请求 + LiveData 数据回调模式
     *
     * @param strategyId 攻略ID
     * @param voteType 投票类型（up=点赞，down=点踩）
     * @return LiveData<Boolean> 投票是否成功，true=成功，false=失败
     */
    public LiveData<Boolean> vote(long strategyId, String voteType) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        // 构建投票请求体，封装攻略ID和投票类型
        CommunityApi.VoteRequest voteRequest = new CommunityApi.VoteRequest(voteType);

        // 调用 Retrofit 接口发起异步网络请求：为攻略投票
        communityApi.vote(strategyId, voteRequest).enqueue(new Callback<Result<Void>>() {
            @Override
            public void onResponse(Call<Result<Void>> call, Response<Result<Void>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                result.setValue(response.isSuccessful() && response.body() != null && response.body().isSuccess());
            }

            @Override
            public void onFailure(Call<Result<Void>> call, Throwable t) {
                // 网络异常：投票失败
                result.setValue(false);
            }
        });

        return result;
    }

    /**
     * 取消攻略投票（社区模块）
     *
     * 作用：用户取消对攻略的投票（仅能取消自己投过的票）
     * 实现：采用 Retrofit 网络请求 + LiveData 数据回调模式
     *
     * @param strategyId 攻略ID
     * @return LiveData<Boolean> 取消投票是否成功，true=成功，false=失败
     */
    public LiveData<Boolean> cancelVote(long strategyId) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        // 调用 Retrofit 接口发起异步网络请求：取消攻略投票
        communityApi.cancelVote(strategyId).enqueue(new Callback<Result<Void>>() {
            @Override
            public void onResponse(Call<Result<Void>> call, Response<Result<Void>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                result.setValue(response.isSuccessful() && response.body() != null && response.body().isSuccess());
            }

            @Override
            public void onFailure(Call<Result<Void>> call, Throwable t) {
                // 网络异常：取消失败
                result.setValue(false);
            }
        });

        return result;
    }

    /**
     * 获取我的攻略列表（社区模块）
     *
     * 作用：获取当前登录用户发布的所有攻略
     * 实现：采用 Retrofit 网络请求 + LiveData 数据回调模式
     *
     * @return LiveData<List<StrategyListResponse>> 可观察的攻略列表，请求成功返回列表，失败返回空列表
     */
    public LiveData<List<StrategyListResponse>> getMyStrategies() {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<List<StrategyListResponse>> result = new MutableLiveData<>();

        // 调用 Retrofit 接口发起异步网络请求：获取我的攻略列表
        communityApi.getMyStrategies().enqueue(new Callback<Result<List<StrategyListResponse>>>() {
            @Override
            public void onResponse(Call<Result<List<StrategyListResponse>>> call, Response<Result<List<StrategyListResponse>>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<StrategyListResponse> data = response.body().getData();
                    // 数据保护：确保列表不为null
                    result.setValue(data != null ? data : Collections.emptyList());
                } else {
                    // 业务失败：设置空列表
                    result.setValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<Result<List<StrategyListResponse>>> call, Throwable t) {
                // 网络异常：设置空列表
                result.setValue(Collections.emptyList());
            }
        });

        return result;
    }
}