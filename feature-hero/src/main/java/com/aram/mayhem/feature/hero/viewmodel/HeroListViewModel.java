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
 * 英雄列表 ViewModel ── 管理英雄列表的数据状态和业务逻辑
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroListViewModel 是英雄列表页的"数据管家"，负责：
 * 1. 从 Repository 获取英雄列表数据
 * 2. 管理加载状态（加载中/加载完成/加载失败）
 * 3. 支持分页加载（滑到底部自动加载下一页）
 * 4. 支持关键词搜索（输入框实时搜索）
 * 5. 支持梯级筛选（S+/S/A/B/C 筛选）
 * 6. 支持离线模式（网络断开时使用缓存数据）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、MVVM 架构中的角色
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
 *   │  View (Fragment) │     │  ViewModel      │     │  Repository     │
 *   │                 │     │                 │     │                 │
 *   │  显示 UI        │ ←── │  管理 LiveData  │ ←── │  获取数据       │
 *   │  处理用户交互    │ 观察 │  业务逻辑       │ 调用 │  缓存策略       │
 *   │                 │     │                 │     │  网络请求       │
 *   └─────────────────┘     └─────────────────┘     └─────────────────┘
 *
 * ViewModel 的核心原则：
 * - 不持有 View 的引用（避免内存泄漏）
 * - 不关心 UI 如何展示数据（只管数据本身）
 * - 配置变更（如旋转屏幕）时不会被销毁
 * - 通过 LiveData 让 View 自动观察数据变化
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、LiveData 数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 * ViewModel 暴露 6 个 LiveData 给 Fragment 观察：
 *
 *   heroes      : List<HeroUiModel>  ── 英雄列表数据
 *   loading     : Boolean            ── 首次加载状态
 *   error       : String             ── 错误信息
 *   loadingMore : Boolean            ── 加载更多状态
 *   isLastPage  : Boolean            ── 是否最后一页
 *   isOffline   : Boolean            ── 离线模式状态
 *
 * Fragment 通过 observe() 订阅这些 LiveData，数据变化时自动更新 UI：
 *
 *   viewModel.getHeroes().observe(getViewLifecycleOwner(), heroes -> {
 *       adapter.submitList(heroes);  // 列表数据变了，更新 RecyclerView
 *   });
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、分页加载机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   第1次加载          第2次加载（loadMore）     第3次加载（loadMore）
 *   ┌──────────┐      ┌──────────────────┐     ┌────────────────────┐
 *   │ page=1   │      │ page=1 + page=2  │     │ page=1+2+3        │
 *   │ 20条数据  │  →   │ 40条数据          │  →  │ 60条数据           │
 *   └──────────┘      └──────────────────┘     └────────────────────┘
 *
 * 分页加载流程：
 * 1. 用户滑到列表底部 → PaginationScrollListener 触发
 * 2. Fragment 调用 viewModel.loadMore()
 * 3. ViewModel 检查：是否正在加载？是否最后一页？
 * 4. 如果都不是，调用 loadHeroes(false) 加载下一页
 * 5. 新数据追加到现有列表末尾（不是替换）
 *
 * 判断"最后一页"的逻辑：
 * - 如果返回的数据条数 < pageSize（20），说明没有更多数据了
 * - 例如：总共 45 个英雄，第 3 页只返回 5 个（< 20），标记为最后一页
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、搜索和筛选
 * ═══════════════════════════════════════════════════════════════════
 *
 * 搜索和筛选都会触发 loadHeroes(true)（重置加载）：
 *
 *   用户输入"提莫" → searchHeroes("提莫") → currentKeyword="提莫" → loadHeroes(true)
 *   用户选择 S+ 筛选 → filterByTier(Tier.S_PLUS) → currentTier="S+" → loadHeroes(true)
 *
 * reset=true 时的操作：
 * 1. 页码重置为 1
 * 2. 清空现有列表
 * 3. 重置 isLastPage 标记
 * 4. 重新从第一页开始加载
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、依赖注入
 * ═══════════════════════════════════════════════════════════════════
 *
 * @HiltViewModel 注解让 Hilt 自动管理 ViewModel 的创建和依赖注入。
 * @Inject 标注的构造函数参数由 Hilt 自动提供：
 * - Application：Android 应用上下文
 * - HeroRepository：英雄数据仓库（@Singleton 单例）
 *
 * 为什么用 Hilt 而不手动创建 Repository？
 * - Hilt 保证 Repository 是单例，所有 ViewModel 共享同一实例
 * - Hilt 自动处理依赖关系（Repository 需要 HeroApi 和 HeroDao）
 * - 方便单元测试时替换为 Mock Repository
 *
 * @see HeroRepository
 * @see com.aram.mayhem.feature.hero.HeroListFragment
 */
@HiltViewModel
public class HeroListViewModel extends AndroidViewModel {

    private final HeroRepository heroRepository;

    /**
     * 英雄列表数据 ── Fragment 观察此 LiveData 更新 RecyclerView
     *
     * 数据变化场景：
     * - 首次加载：从空列表变为第 1 页数据
     * - 加载更多：列表末尾追加新数据
     * - 搜索/筛选：列表被替换为新的筛选结果
     * - 离线模式：显示 Room 缓存数据
     */
    private final MutableLiveData<List<HeroUiModel>> heroes = new MutableLiveData<>();

    /**
     * 首次加载状态 ── 控制 StatefulLayout 显示 Loading/Content/Error
     *
     * true = 正在加载第一页（显示加载动画）
     * false = 加载完成（显示内容或错误）
     * 注意：加载更多时不改变此值，只改变 loadingMore
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息 ── 加载失败时设置错误消息
     *
     * Fragment 观察此 LiveData，有错误时显示错误页面和重试按钮。
     * null 表示没有错误。
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 加载更多状态 ── 控制列表底部的"加载中"提示
     *
     * true = 正在加载下一页（列表底部显示加载动画）
     * false = 加载完成或空闲
     * PaginationScrollListener 检查此值防止重复触发
     */
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);

    /**
     * 是否最后一页 ── 控制是否还能继续加载更多
     *
     * true = 已加载所有数据，不再触发 loadMore
     * false = 还有更多数据可以加载
     * 判断依据：返回数据条数 < pageSize
     */
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);

    /**
     * 离线模式状态 ── 标记当前是否无网络连接
     *
     * true = 无网络，Repository 使用缓存数据
     * false = 有网络，Repository 优先请求服务器
     * 由 Fragment 的 ConnectivityManager 回调触发更新
     */
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    /** 当前页码（从1开始，每次 loadMore 后 +1） */
    private int currentPage = 1;

    /** 每页数据条数（与后端 API 约定的分页大小） */
    private final int pageSize = 20;

    /** 当前搜索关键词（空字符串表示不筛选） */
    private String currentKeyword = "";

    /** 当前梯级筛选条件（空字符串表示不筛选） */
    private String currentTier = "";

    /** 当前排序字段（winRate=胜率 / pickRate=选取率 / name=名称） */
    private String currentSortBy = "winRate";

    /**
     * 构造函数 ── Hilt 自动调用，注入依赖
     *
     * @param application    Android 应用上下文（AndroidViewModel 需要）
     * @param heroRepository 英雄数据仓库（Hilt 自动注入 @Singleton 实例）
     */
    @Inject
    public HeroListViewModel(@NonNull Application application, HeroRepository heroRepository) {
        super(application);
        this.heroRepository = heroRepository;
        loadHeroes(true);
    }

    /**
     * 获取英雄列表的可观察数据
     *
     * Fragment 通过 observe() 订阅此 LiveData，数据变化时自动更新 UI。
     *
     * @return LiveData<List<HeroUiModel>> 英雄列表
     */
    public LiveData<List<HeroUiModel>> getHeroes() {
        return heroes;
    }

    /**
     * 获取加载状态的可观察数据
     *
     * @return LiveData<Boolean> 是否正在首次加载
     */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * 获取错误信息的可观察数据
     *
     * @return LiveData<String> 错误信息，null 表示无错误
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
     * @return LiveData<Boolean> 是否已加载全部数据
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
     * 搜索英雄 ── 更新搜索关键词并重新加载
     *
     * 由 Fragment 的搜索框 TextWatcher 触发。
     * 每次输入变化都会调用此方法，触发 loadHeroes(true) 重新加载。
     *
     * 注意：Fragment 侧应该做防抖（debounce），避免每次按键都发请求。
     * 本项目中 SearchToolbar 组件内置了 300ms 防抖。
     *
     * @param keyword 搜索关键词，空字符串表示不筛选
     */
    public void searchHeroes(String keyword) {
        currentKeyword = keyword;
        loadHeroes(true);
    }

    /**
     * 按梯级筛选英雄 ── 更新筛选条件并重新加载
     *
     * 由 Fragment 的 ChipGroup 选择变化触发。
     * 用户点击 S+/S/A/B/C 筛选标签时调用。
     *
     * @param tier 梯级枚举（S_PLUS/S/A/B/C），null 表示取消筛选
     */
    public void filterByTier(Tier tier) {
        currentTier = tier != null ? tier.getLabel() : "";
        loadHeroes(true);
    }

    /**
     * 加载更多数据 ── 分页加载下一页
     *
     * 由 PaginationScrollListener 在用户滑到底部时触发。
     * 内部有防重复加载检查：isLoading 或 isLastPage 时不执行。
     */
    public void loadMore() {
        if (!loadingMore.getValue() && !isLastPage.getValue()) {
            loadHeroes(false);
        }
    }

    /**
     * 设置离线模式状态
     *
     * 由 Fragment 的 ConnectivityManager.NetworkCallback 触发：
     * - onLost()（网络断开）→ setOffline(true)
     * - onAvailable()（网络恢复）→ retry() → setOffline(false)
     *
     * 同时同步更新 Repository 的离线标记，影响数据获取策略：
     * - 离线时 Repository 跳过网络请求，直接返回 Room 缓存
     * - 在线时 Repository 先返回缓存，再发起网络请求更新
     *
     * @param offline true 表示离线，false 表示在线
     */
    public void setOffline(boolean offline) {
        isOffline.setValue(offline);
        heroRepository.setOffline(offline);
    }

    /**
     * 重试加载 ── 网络恢复或加载失败后重新尝试
     *
     * 执行步骤：
     * 1. 设置离线模式为 false（标记为在线）
     * 2. 重新从第一页加载数据
     *
     * 触发场景：
     * - 用户点击错误页面的"重试"按钮
     * - 网络从断开恢复（ConnectivityManager.onAvailable）
     */
    public void retry() {
        setOffline(false);
        loadHeroes(true);
    }

    /**
     * 加载英雄列表 ── 核心数据加载方法
     *
     * 这是 ViewModel 中最复杂的方法，处理首次加载和加载更多两种场景。
     *
     * ┌─────────────────────────────────────────────────────────────┐
     * │ reset=true（首次加载/搜索/筛选）                              │
     * │   1. 页码重置为 1                                            │
     * │   2. 清空现有列表                                            │
     * │   3. 重置 isLastPage 标记                                    │
     * │   4. loading=true（显示全屏加载动画）                         │
     * │   5. 请求第 1 页数据                                         │
     * │   6. 用新数据替换现有列表                                     │
     * ├─────────────────────────────────────────────────────────────┤
     * │ reset=false（加载更多）                                      │
     * │   1. 页码保持当前值                                          │
     * │   2. 保留现有列表                                            │
     * │   3. loadingMore=true（显示底部加载动画）                     │
     * │   4. 请求下一页数据                                          │
     * │   5. 新数据追加到现有列表末尾                                 │
     * └─────────────────────────────────────────────────────────────┘
     *
     * observeForever 的使用说明：
     * - 正常应该用 observe() 绑定 LifecycleOwner，自动取消订阅
     * - 这里用 observeForever 是因为 ViewModel 没有 LifecycleOwner
     * - 风险：如果忘记移除观察者，会导致内存泄漏
     * - 本项目中 Repository 返回的 LiveData 是一次性的（只发一次值），
     *   所以泄漏风险较低，但这是一个已知的待改进点
     *
     * @param reset true=重新加载第一页，false=加载下一页
     */
    private void loadHeroes(boolean reset) {
        if (reset) {
            currentPage = 1;
            heroes.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        loading.setValue(reset);
        loadingMore.setValue(!reset);
        isOffline.setValue(false);

        heroRepository.getHeroes(currentPage, pageSize, currentKeyword, currentTier, currentSortBy)
                .observeForever(uiModels -> {
                    loading.setValue(false);
                    loadingMore.setValue(false);

                    if (uiModels != null && !uiModels.isEmpty()) {
                        List<HeroUiModel> current = new ArrayList<>();
                        if (!reset && heroes.getValue() != null) {
                            current.addAll(heroes.getValue());
                        }
                        current.addAll(uiModels);
                        heroes.setValue(current);

                        isLastPage.setValue(uiModels.size() < pageSize);
                        currentPage++;
                    } else {
                        if (reset) {
                            heroes.setValue(new ArrayList<>());
                        }
                        isLastPage.setValue(true);
                    }
                });
    }

    /**
     * 解析梯级字符串为枚举 ── 将后端返回的字符串转为 Tier 枚举
     *
     * 后端返回的梯级是字符串格式（如 "S+"），前端使用 Tier 枚举。
     * 此方法做格式转换，null 或无法识别的值默认返回 Tier.C。
     *
     * @param tierStr 梯级字符串（S+/S/A/B/C）
     * @return Tier 枚举值，默认 Tier.C
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
