package com.aram.mayhem.feature.hero.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Tier;
import com.aram.mayhem.feature.hero.repository.HeroRepository;
import com.aram.mayhem.ui.model.HeroUiModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/**
 * 英雄列表 ViewModel（英雄模块）
 *
 * 功能：管理英雄列表数据、支持分页加载、关键词搜索、梯级筛选、排序
 * 数据流：HeroRepository → LiveData<List<HeroUiModel>> → HeroListFragment
 *
 * @see HeroRepository
 * @see com.aram.mayhem.feature.hero.HeroListFragment
 */
@HiltViewModel
public class HeroListViewModel extends AndroidViewModel {

    /** 英雄数据仓库 */
    private final HeroRepository heroRepository;

    /** 英雄列表数据 */
    private final MutableLiveData<List<HeroUiModel>> heroes = new MutableLiveData<>();
    /** 是否正在加载 */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 错误信息 */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    /** 是否正在加载更多 */
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    /** 是否为最后一页 */
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);
    /** 是否处于离线模式 */
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    /** 当前页码（从1开始） */
    private int currentPage = 1;
    /** 每页数据条数 */
    private final int pageSize = 20;
    /** 当前搜索关键词 */
    private String currentKeyword = "";
    /** 当前梯级筛选条件 */
    private String currentTier = "";
    /** 当前排序字段（winRate-胜率 / pickRate-选取率 / name-名称） */
    private String currentSortBy = "winRate";

    /**
     * 构造函数
     *
     * @param application Android 应用上下文
     * @param heroRepository 英雄数据仓库（通过 Hilt 依赖注入）
     */
    @Inject
    public HeroListViewModel(@NonNull Application application, HeroRepository heroRepository) {
        super(application);
        this.heroRepository = heroRepository;
        // 初始化时加载第一页数据
        loadHeroes(true);
    }

    /**
     * 获取英雄列表的可观察数据
     *
     * @return LiveData<List<HeroUiModel>> 英雄列表
     */
    public LiveData<List<HeroUiModel>> getHeroes() {
        return heroes;
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
     * 获取离线状态的可观察数据
     *
     * @return LiveData<Boolean> 是否处于离线模式
     */
    public LiveData<Boolean> getIsOffline() {
        return isOffline;
    }

    /**
     * 搜索英雄
     *
     * @param keyword 搜索关键词
     */
    public void searchHeroes(String keyword) {
        currentKeyword = keyword;
        loadHeroes(true);
    }

    /**
     * 按梯级筛选英雄
     *
     * @param tier 梯级枚举（S_PLUS/S/A/B/C）
     */
    public void filterByTier(Tier tier) {
        currentTier = tier != null ? tier.getLabel() : "";
        loadHeroes(true);
    }

    /**
     * 加载更多数据（分页）
     */
    public void loadMore() {
        // 防止重复加载
        if (!loadingMore.getValue() && !isLastPage.getValue()) {
            loadHeroes(false);
        }
    }

    /**
     * 设置离线模式状态
     *
     * 作用：由 Fragment 的 ConnectivityManager 回调触发，同步更新 Repository 的离线标记
     * 影响：离线时 Repository 跳过网络请求，直接返回 Room 缓存数据
     *
     * @param offline true 表示离线，false 表示在线
     */
    public void setOffline(boolean offline) {
        isOffline.setValue(offline);
        heroRepository.setOffline(offline);
    }

    /**
     * 重试加载
     *
     * 用于加载失败后重新尝试，或网络恢复后刷新数据
     */
    public void retry() {
        setOffline(false);
        loadHeroes(true);
    }

    /**
     * 加载英雄列表
     *
     * @param reset 是否重置（重新加载第一页）
     */
    private void loadHeroes(boolean reset) {
        // 重置时：页码归零、清空列表、重置最后一页标记
        if (reset) {
            currentPage = 1;
            heroes.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        // 更新加载状态
        loading.setValue(reset);
        loadingMore.setValue(!reset);
        isOffline.setValue(false);

        // 调用 Repository 获取英雄列表
        heroRepository.getHeroes(currentPage, pageSize, currentKeyword, currentTier, currentSortBy)
                .observeForever(uiModels -> {
                    // 加载完成，更新状态
                    loading.setValue(false);
                    loadingMore.setValue(false);

                    if (uiModels != null && !uiModels.isEmpty()) {
                        // 数据有效，合并到现有列表
                        List<HeroUiModel> current = new ArrayList<>();
                        if (!reset && heroes.getValue() != null) {
                            current.addAll(heroes.getValue());
                        }
                        current.addAll(uiModels);
                        heroes.setValue(current);

                        // 判断是否为最后一页
                        isLastPage.setValue(uiModels.size() < pageSize);
                        currentPage++;
                    } else {
                        // 数据为空或失败
                        if (reset) {
                            heroes.setValue(new ArrayList<>());
                        }
                        isLastPage.setValue(true);
                    }
                });
    }

    /**
     * 解析梯级字符串为枚举
     *
     * @param tierStr 梯级字符串（S+/S/A/B/C）
     * @return Tier 枚举值
     */
    private Tier parseTier(String tierStr) {
        if (tierStr == null) return Tier.C;
        switch (tierStr) {
            case "S+": return Tier.S_PLUS;
            case "S": return Tier.S;
            case "A": return Tier.A;
            case "B": return Tier.B;
            case "C": return Tier.C;
            default: return Tier.C;
        }
    }
}