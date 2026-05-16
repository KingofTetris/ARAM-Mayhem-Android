package com.aram.mayhem.feature.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aram.mayhem.network.api.CommunityApi;
import com.aram.mayhem.network.dto.StrategyListResponse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

/**
 * 我的攻略 ViewModel（个人中心模块）
 *
 * 功能：管理用户发布的攻略列表（加载、删除）
 * 数据流：CommunityApi → LiveData<List<StrategyListResponse>> → MyStrategiesFragment
 *
 * @see CommunityApi
 * @see com.aram.mayhem.feature.profile.MyStrategiesFragment
 */
@HiltViewModel
public class MyStrategiesViewModel extends ViewModel {

    private final CommunityApi communityApi;

    private final MutableLiveData<List<StrategyListResponse>> myStrategies = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();

    @Inject
    public MyStrategiesViewModel(CommunityApi communityApi) {
        this.communityApi = communityApi;
    }

    public LiveData<List<StrategyListResponse>> getMyStrategies() { return myStrategies; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getDeleteSuccess() { return deleteSuccess; }

    /**
     * 加载我的攻略列表
     */
    public void loadMyStrategies() {
        if (Boolean.TRUE.equals(loading.getValue())) return;

        loading.setValue(true);
        communityApi.getMyStrategies().enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<com.aram.mayhem.common.Result<List<StrategyListResponse>>> call,
                                   retrofit2.Response<com.aram.mayhem.common.Result<List<StrategyListResponse>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    myStrategies.setValue(response.body().getData());
                    Timber.d("Loaded %d my strategies", response.body().getData() != null ? response.body().getData().size() : 0);
                } else {
                    myStrategies.setValue(new ArrayList<>());
                    error.setValue("加载我的攻略失败");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.aram.mayhem.common.Result<List<StrategyListResponse>>> call, Throwable t) {
                loading.setValue(false);
                myStrategies.setValue(new ArrayList<>());
                error.setValue("网络错误：" + t.getMessage());
                Timber.e(t, "Failed to load my strategies");
            }
        });
    }

    /**
     * 删除攻略
     *
     * @param strategyId 攻略ID
     * @param position   列表中的位置（用于移除 UI 项）
     */
    public void deleteStrategy(long strategyId, int position) {
        communityApi.deleteStrategy(strategyId).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<com.aram.mayhem.common.Result<Void>> call,
                                   retrofit2.Response<com.aram.mayhem.common.Result<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    deleteSuccess.setValue(true);
                    Timber.d("Strategy deleted: id=%d", strategyId);
                } else {
                    deleteSuccess.setValue(false);
                    error.setValue("删除失败");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.aram.mayhem.common.Result<Void>> call, Throwable t) {
                deleteSuccess.setValue(false);
                error.setValue("网络错误：" + t.getMessage());
                Timber.e(t, "Failed to delete strategy: id=%d", strategyId);
            }
        });
    }
}
