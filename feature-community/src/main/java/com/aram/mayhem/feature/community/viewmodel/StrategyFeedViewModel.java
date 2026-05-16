package com.aram.mayhem.feature.community.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.community.repository.StrategyRepository;
import com.aram.mayhem.network.dto.StrategyListResponse;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/**
 * 社区攻略流 ViewModel（社区模块）
 *
 * 功能：管理攻略列表数据、分页加载、排序切换
 * 数据流：StrategyRepository → LiveData<List<StrategyListResponse>> → CommunityFeedFragment
 *
 * @see StrategyRepository
 * @see com.aram.mayhem.feature.community.CommunityFeedFragment
 */
@HiltViewModel
public class StrategyFeedViewModel extends AndroidViewModel {

    /** 攻略数据仓库 */
    private final StrategyRepository strategyRepository;

    /** 攻略列表数据 */
    private final MutableLiveData<List<StrategyListResponse>> strategies = new MutableLiveData<>(new ArrayList<>());
    /** 是否正在加载 */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 错误信息 */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    /** 是否正在加载更多 */
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    /** 是否为最后一页 */
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);

    /** 当前页码（从1开始） */
    private int currentPage = 1;
    /** 每页数据条数 */
    private final int pageSize = 10;
    /** 当前排序方式（hot-热门 / latest-最新） */
    private String currentSort = "hot";

    /**
     * 构造函数
     *
     * @param application Android 应用上下文
     * @param strategyRepository 攻略数据仓库（通过 Hilt 依赖注入）
     */
    @Inject
    public StrategyFeedViewModel(@NonNull Application application, StrategyRepository strategyRepository) {
        super(application);
        this.strategyRepository = strategyRepository;
        // 初始化时加载第一页数据
        loadStrategies(true);
    }

    /**
     * 获取攻略列表的可观察数据
     *
     * @return LiveData<List<StrategyListResponse>> 攻略列表
     */
    public LiveData<List<StrategyListResponse>> getStrategies() {
        return strategies;
    }

    /**
     * 获取加载状态的可观察数据
     *
     * @return LiveData<Boolean> 是否正在加载
     */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * 获取错误信息的可观察数据
     *
     * @return LiveData<String> 错误信息
     */
    public LiveData<String> getError() {
        return error;
    }

    /**
     * 获取加载更多状态的可观察数据
     *
     * @return LiveData<Boolean> 是否正在加载更多
     */
    public LiveData<Boolean> getLoadingMore() {
        return loadingMore;
    }

    /**
     * 获取是否为最后一页的可观察数据
     *
     * @return LiveData<Boolean> 是否为最后一页
     */
    public LiveData<Boolean> getIsLastPage() {
        return isLastPage;
    }

    /**
     * 设置排序方式
     *
     * @param sort 排序方式（hot-热门 / latest-最新）
     */
    public void setSort(String sort) {
        if (!currentSort.equals(sort)) {
            currentSort = sort;
            // 切换排序时重新加载第一页
            loadStrategies(true);
        }
    }

    /**
     * 加载更多数据（分页）
     */
    public void loadMore() {
        // 防止重复加载：不在加载中且不是最后一页
        if (loadingMore.getValue() == null || !loadingMore.getValue()) {
            if (isLastPage.getValue() == null || !isLastPage.getValue()) {
                loadStrategies(false);
            }
        }
    }

    /**
     * 重试加载
     *
     * 用于加载失败后重新尝试
     */
    public void retry() {
        loadStrategies(true);
    }

    /**
     * 加载攻略列表
     *
     * @param reset 是否重置（重新加载第一页）
     */
    private void loadStrategies(boolean reset) {
        // 重置时：页码归零、清空列表、重置最后一页标记
        if (reset) {
            currentPage = 1;
            strategies.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        // 更新加载状态
        loading.setValue(reset);
        loadingMore.setValue(!reset);
        error.setValue(null);

        // 调用 Repository 获取攻略列表
        strategyRepository.getStrategies(currentSort, currentPage, pageSize).observeForever(newStrategies -> {
            // 加载完成，更新状态
            loading.setValue(false);
            loadingMore.setValue(false);

            if (newStrategies != null && !newStrategies.isEmpty()) {
                // 数据有效，合并到现有列表
                List<StrategyListResponse> current = new ArrayList<>();
                if (!reset && strategies.getValue() != null) {
                    current.addAll(strategies.getValue());
                }
                current.addAll(newStrategies);
                strategies.setValue(current);

                // 判断是否为最后一页：返回的数据少于每页条数则为最后一页
                isLastPage.setValue(newStrategies.size() < pageSize);
                currentPage++;
            } else {
                // 数据为空或失败
                if (reset) {
                    strategies.setValue(new ArrayList<>());
                }
                isLastPage.setValue(true);
            }
        });
    }
}