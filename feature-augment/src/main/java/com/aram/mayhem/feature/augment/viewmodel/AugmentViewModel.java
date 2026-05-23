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
 * 符文列表 ViewModel ── 管理符文列表的数据状态和业务逻辑
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentViewModel 是符文列表页的"数据管家"，负责：
 * 1. 从 Repository 获取符文列表数据
 * 2. 管理加载状态（加载中/加载完成/加载失败）
 * 3. 支持分页加载（滑到底部自动加载下一页）
 * 4. 支持品质筛选（棱彩/金/银）
 * 5. 支持套装筛选
 * 6. 支持离线模式（网络断开时使用缓存数据）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、与 HeroListViewModel 的对比
 * ═══════════════════════════════════════════════════════════════════
 *
 * 两者结构几乎相同，差异点：
 * - HeroListViewModel：搜索 + 梯级筛选（ChipGroup）
 * - AugmentViewModel：品质筛选（TabLayout）+ 套装筛选
 * - 英雄列表有 SearchToolbar，符文列表没有
 * - 英雄列表用 ChipGroup 筛选，符文列表用 TabLayout 筛选
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、分页加载机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   第1次加载          第2次加载（loadMore）     第3次加载（loadMore）
 *   ┌──────────┐      ┌──────────────────┐     ┌────────────────────┐
 *   │ page=1   │      │ page=1 + page=2  │     │ page=1+2+3        │
 *   │ 20条数据  │  →   │ 40条数据          │  →  │ 60条数据           │
 *   └──────────┘      └──────────────────┘     └────────────────────┘
 *
 *   reset=true：清空列表，从第1页重新加载
 *   reset=false：追加到现有列表末尾
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、品质筛选映射
 * ═══════════════════════════════════════════════════════════════════
 *
 *   TabLayout 位置    →    后端品质参数
 *   ─────────────────────────────────────
 *   0（全部）         →    ""（不筛选）
 *   1（棱彩）         →    "PRISMATIC"
 *   2（金）           →    "GOLD"
 *   3（银）           →    "SILVER"
 *
 * @see AugmentRepository
 * @see com.aram.mayhem.feature.augment.AugmentListFragment
 */
@HiltViewModel
public class AugmentViewModel extends AndroidViewModel {

    /**
     * 符文数据仓库 ── 提供网络请求和本地缓存的数据访问
     */
    private final AugmentRepository augmentRepository;

    /**
     * 符文列表数据 ── 当前已加载的所有符文
     *
     * 分页加载时，新数据会追加到现有列表末尾。
     * 筛选条件变化时，列表会清空并重新加载。
     */
    private final MutableLiveData<List<AugmentUiModel>> augments = new MutableLiveData<>();

    /**
     * 是否正在加载 ── 首次加载或刷新时为 true
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息 ── 加载失败时的错误描述
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 是否正在加载更多 ── 分页加载下一页时为 true
     */
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);

    /**
     * 是否为最后一页 ── true 时不再触发 loadMore
     */
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);

    /**
     * 是否处于离线模式 ── 网络断开时为 true
     */
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    /**
     * 当前页码 ── 从 1 开始，每次 loadMore 后递增
     */
    private int currentPage = 1;

    /**
     * 每页数据条数 ── 固定 20 条
     */
    private final int pageSize = 20;

    /**
     * 当前品质筛选条件 ── 空字符串表示不筛选
     */
    private String currentQuality = "";

    /**
     * 当前套装筛选条件 ── 空字符串表示不筛选
     */
    private String currentSynergySet = "";

    /**
     * 构造函数 ── Hilt 自动注入依赖
     *
     * 构造时自动加载第一页数据（与 HeroListViewModel 不同，
     * HeroListViewModel 的构造函数中不自动加载）。
     *
     * @param application       Android 应用上下文
     * @param augmentRepository 符文数据仓库（Hilt 自动注入单例）
     */
    @Inject
    public AugmentViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
        loadAugments(true);
    }

    public LiveData<List<AugmentUiModel>> getAugments() {
        return augments;
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

    public LiveData<Boolean> getIsOffline() {
        return isOffline;
    }

    /**
     * 按品质筛选符文 ── 切换品质标签时调用
     *
     * @param quality 品质参数（"PRISMATIC"/"GOLD"/"SILVER"/""表示全部）
     */
    public void filterByQuality(String quality) {
        currentQuality = quality != null ? quality : "";
        loadAugments(true);
    }

    /**
     * 按套装筛选符文 ── 选择套装时调用
     *
     * @param synergySet 套装名称，空字符串表示不筛选
     */
    public void filterBySynergy(String synergySet) {
        currentSynergySet = synergySet != null ? synergySet : "";
        loadAugments(true);
    }

    /**
     * 加载更多数据 ── 分页加载下一页
     *
     * 由 PaginationScrollListener 在用户滑到底部时触发。
     * 内部有防重复加载检查：isLoading 或 isLastPage 时不执行。
     */
    public void loadMore() {
        if (loadingMore.getValue() == null || !loadingMore.getValue()) {
            if (isLastPage.getValue() == null || !isLastPage.getValue()) {
                loadAugments(false);
            }
        }
    }

    /**
     * 重试加载 ── 加载失败后重新尝试
     *
     * 重置页码，从第一页重新加载。
     */
    public void retry() {
        loadAugments(true);
    }

    /**
     * 加载符文列表 ── 核心方法，处理分页和筛选逻辑
     *
     * @param reset true=重新加载（清空列表从第1页开始）
     *              false=加载更多（追加到现有列表）
     *
     * 执行流程：
     * 1. reset 时：页码归1、清空列表、重置最后一页标记
     * 2. 更新加载状态（loading 或 loadingMore）
     * 3. 调用 Repository 获取符文列表
     * 4. 成功：合并数据到列表，判断是否最后一页
     * 5. 失败/空数据：设置空列表，标记为最后一页
     *
     * 最后一页判断逻辑：
     * - 返回数据条数 < pageSize → 是最后一页
     * - 例如：请求 20 条，只返回 15 条 → 说明没有更多数据了
     */
    private void loadAugments(boolean reset) {
        if (reset) {
            currentPage = 1;
            augments.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        loading.setValue(reset);
        loadingMore.setValue(!reset);
        isOffline.setValue(false);

        augmentRepository.getAugments(currentPage, pageSize, currentQuality, currentSynergySet)
                .observeForever(uiModels -> {
                    loading.setValue(false);
                    loadingMore.setValue(false);

                    if (uiModels != null && !uiModels.isEmpty()) {
                        List<AugmentUiModel> current = new ArrayList<>();
                        if (!reset && augments.getValue() != null) {
                            current.addAll(augments.getValue());
                        }
                        current.addAll(uiModels);
                        augments.setValue(current);

                        isLastPage.setValue(uiModels.size() < pageSize);
                        currentPage++;
                    } else {
                        if (reset) {
                            augments.setValue(new ArrayList<>());
                        }
                        isLastPage.setValue(true);
                    }
                });
    }
}
