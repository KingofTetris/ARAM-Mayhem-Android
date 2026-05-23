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
 * 社区攻略流 ViewModel ── 管理攻略列表的数据状态和业务逻辑
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * StrategyFeedViewModel 是社区攻略列表页的"数据管家"，负责：
 * 1. 从 Repository 获取攻略列表数据
 * 2. 管理加载状态（加载中/加载完成/加载失败）
 * 3. 支持分页加载（滑到底部自动加载下一页）
 * 4. 支持排序切换（热门/最新）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、与 AugmentViewModel 的对比
 * ═══════════════════════════════════════════════════════════════════
 *
 * ┌──────────────────────┬──────────────────────┬──────────────────────┐
 * │ 特性                 │ StrategyFeedViewModel│ AugmentViewModel     │
 * ├──────────────────────┼──────────────────────┼──────────────────────┤
 * │ 筛选方式             │ 排序（热门/最新）     │ 品质+套装筛选        │
 * │ 筛选 UI              │ RadioGroup           │ TabLayout            │
 * │ 每页数量             │ 10                   │ 20                   │
 * │ 数据类型             │ StrategyListResponse │ AugmentUiModel       │
 * │ 离线支持             │ Repository 缓存优先  │ Repository 网络优先  │
 * └──────────────────────┴──────────────────────┴──────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、分页加载机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   第1次加载          第2次加载（loadMore）     第3次加载（loadMore）
 *   ┌──────────┐      ┌──────────────────┐     ┌────────────────────┐
 *   │ page=1   │      │ page=1 + page=2  │     │ page=1+2+3        │
 *   │ 10条数据  │  →   │ 20条数据          │  →  │ 30条数据           │
 *   └──────────┘      └──────────────────┘     └────────────────────┘
 *
 *   reset=true：清空列表，从第1页重新加载
 *   reset=false：追加到现有列表末尾
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、排序方式
 * ═══════════════════════════════════════════════════════════════════
 *
 *   RadioGroup 选项    →    后端排序参数
 *   ─────────────────────────────────────
 *   热门（btn_hot）    →    "hot"（按分数排序）
 *   最新（btn_latest） →    "latest"（按创建时间排序）
 *
 * @see StrategyRepository
 * @see com.aram.mayhem.feature.community.CommunityFeedFragment
 */
@HiltViewModel
public class StrategyFeedViewModel extends AndroidViewModel {

    private final StrategyRepository strategyRepository;

    private final MutableLiveData<List<StrategyListResponse>> strategies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);

    private int currentPage = 1;
    private final int pageSize = 10;
    private String currentSort = "hot";

    /**
     * 构造方法 ── Hilt 自动注入依赖
     *
     * 初始化时自动加载第一页数据（热门排序）。
     *
     * @param application       Android 应用上下文
     * @param strategyRepository 攻略数据仓库（Hilt 注入）
     */
    @Inject
    public StrategyFeedViewModel(@NonNull Application application, StrategyRepository strategyRepository) {
        super(application);
        this.strategyRepository = strategyRepository;
        loadStrategies(true);
    }

    public LiveData<List<StrategyListResponse>> getStrategies() {
        return strategies;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoadingMore() {
        return loadingMore;
    }

    public LiveData<Boolean> getIsLastPage() {
        return isLastPage;
    }

    /**
     * 设置排序方式 ── 切换排序时重新加载第一页
     *
     * 只有排序方式真正改变时才触发重新加载，
     * 避免重复点击同一个排序按钮导致不必要的网络请求。
     *
     * @param sort 排序方式（"hot"=热门，"latest"=最新）
     */
    public void setSort(String sort) {
        if (!currentSort.equals(sort)) {
            currentSort = sort;
            loadStrategies(true);
        }
    }

    /**
     * 加载更多 ── 滑到底部时由 PaginationScrollListener 触发
     *
     * 防重复加载检查：
     * - loadingMore=true → 正在加载中，不触发
     * - isLastPage=true → 已是最后一页，不触发
     */
    public void loadMore() {
        if (loadingMore.getValue() == null || !loadingMore.getValue()) {
            if (isLastPage.getValue() == null || !isLastPage.getValue()) {
                loadStrategies(false);
            }
        }
    }

    /**
     * 重试加载 ── 加载失败或下拉刷新时调用
     *
     * 重置所有状态，从第一页重新加载。
     */
    public void retry() {
        loadStrategies(true);
    }

    /**
     * 加载攻略列表 ── 核心方法，处理分页和排序逻辑
     *
     * @param reset true=重新加载（清空列表从第1页开始）
     *              false=加载更多（追加到现有列表）
     *
     * 执行流程：
     * 1. reset 时：页码归1、清空列表、重置最后一页标记
     * 2. 更新加载状态（loading 或 loadingMore）
     * 3. 调用 Repository 获取攻略列表
     * 4. 成功：合并数据到列表，判断是否最后一页
     * 5. 失败/空数据：设置空列表，标记为最后一页
     *
     * 最后一页判断逻辑：
     * - 返回数据条数 < pageSize → 是最后一页
     * - 例如：请求 10 条，只返回 7 条 → 说明没有更多数据了
     */
    private void loadStrategies(boolean reset) {
        if (reset) {
            currentPage = 1;
            strategies.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        loading.setValue(reset);
        loadingMore.setValue(!reset);
        error.setValue(null);

        strategyRepository.getStrategies(currentSort, currentPage, pageSize).observeForever(newStrategies -> {
            loading.setValue(false);
            loadingMore.setValue(false);

            if (newStrategies != null && !newStrategies.isEmpty()) {
                List<StrategyListResponse> current = new ArrayList<>();
                if (!reset && strategies.getValue() != null) {
                    current.addAll(strategies.getValue());
                }
                current.addAll(newStrategies);
                strategies.setValue(current);

                isLastPage.setValue(newStrategies.size() < pageSize);
                currentPage++;
            } else {
                if (reset) {
                    strategies.setValue(new ArrayList<>());
                }
                isLastPage.setValue(true);
            }
        });
    }
}
