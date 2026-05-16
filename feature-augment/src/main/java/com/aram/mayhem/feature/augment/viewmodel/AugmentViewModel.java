package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.ui.model.AugmentUiModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/**
 * 符文列表 ViewModel（符文模块）
 *
 * 功能：管理符文列表数据、支持分页加载、品质筛选、套装筛选
 * 数据流：AugmentRepository → LiveData<List<AugmentUiModel>> → AugmentListFragment
 *
 * @see AugmentRepository
 * @see com.aram.mayhem.feature.augment.AugmentListFragment
 */
@HiltViewModel
public class AugmentViewModel extends AndroidViewModel {

    /** 符文数据仓库 */
    private final AugmentRepository augmentRepository;

    /** 符文列表数据 */
    private final MutableLiveData<List<AugmentUiModel>> augments = new MutableLiveData<>();
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
    /** 当前品质筛选条件 */
    private String currentQuality = "";
    /** 当前套装筛选条件 */
    private String currentSynergySet = "";

    /**
     * 构造函数
     *
     * @param application Android 应用上下文
     * @param augmentRepository 符文数据仓库（通过 Hilt 依赖注入）
     */
    @Inject
    public AugmentViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
        // 初始化时加载第一页数据
        loadAugments(true);
    }

    /**
     * 获取符文列表的可观察数据
     *
     * @return LiveData<List<AugmentUiModel>> 符文列表
     */
    public LiveData<List<AugmentUiModel>> getAugments() {
        return augments;
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
     * 按品质筛选符文
     *
     * @param quality 品质（MYTHIC-神话 / LEGENDARY-传说 / EPIC-史诗 / RARE-稀有）
     */
    public void filterByQuality(String quality) {
        currentQuality = quality != null ? quality : "";
        loadAugments(true);
    }

    /**
     * 按套装筛选符文
     *
     * @param synergySet 套装名称
     */
    public void filterBySynergy(String synergySet) {
        currentSynergySet = synergySet != null ? synergySet : "";
        loadAugments(true);
    }

    /**
     * 加载更多数据（分页）
     */
    public void loadMore() {
        // 防止重复加载
        if (loadingMore.getValue() == null || !loadingMore.getValue()) {
            if (isLastPage.getValue() == null || !isLastPage.getValue()) {
                loadAugments(false);
            }
        }
    }

    /**
     * 重试加载
     *
     * 用于加载失败后重新尝试
     */
    public void retry() {
        loadAugments(true);
    }

    /**
     * 加载符文列表
     *
     * @param reset 是否重置（重新加载第一页）
     */
    private void loadAugments(boolean reset) {
        // 重置时：页码归零、清空列表、重置最后一页标记
        if (reset) {
            currentPage = 1;
            augments.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        // 更新加载状态
        loading.setValue(reset);
        loadingMore.setValue(!reset);
        isOffline.setValue(false);

        // 调用 Repository 获取符文列表
        augmentRepository.getAugments(currentPage, pageSize, currentQuality, currentSynergySet)
                .observeForever(uiModels -> {
                    // 加载完成，更新状态
                    loading.setValue(false);
                    loadingMore.setValue(false);

                    if (uiModels != null && !uiModels.isEmpty()) {
                        // 数据有效，合并到现有列表
                        List<AugmentUiModel> current = new ArrayList<>();
                        if (!reset && augments.getValue() != null) {
                            current.addAll(augments.getValue());
                        }
                        current.addAll(uiModels);
                        augments.setValue(current);

                        // 判断是否为最后一页
                        isLastPage.setValue(uiModels.size() < pageSize);
                        currentPage++;
                    } else {
                        // 数据为空或失败
                        if (reset) {
                            augments.setValue(new ArrayList<>());
                        }
                        isLastPage.setValue(true);
                    }
                });
    }
}