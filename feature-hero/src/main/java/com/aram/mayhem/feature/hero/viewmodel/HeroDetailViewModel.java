package com.aram.mayhem.feature.hero.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.hero.repository.HeroRepository;
import com.aram.mayhem.ui.model.HeroDetailUiModel;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/**
 * 英雄详情 ViewModel ── 管理英雄详情页的数据状态
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroDetailViewModel 是英雄详情页的"数据管家"，负责：
 * 1. 根据 heroId 从 Repository 加载英雄详情数据
 * 2. 管理加载状态（加载中/加载完成/加载失败）
 * 3. 支持离线模式（网络断开时使用缓存数据）
 *
 * 与 HeroListViewModel 的区别：
 * - HeroListViewModel 管理列表数据（多个英雄的摘要信息）
 * - HeroDetailViewModel 管理单个英雄的完整数据（技能、出装、克制等）
 * - 列表页需要分页、搜索、筛选；详情页只需一次加载
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   HeroListFragment（用户点击英雄卡片）
 *        │
 *        │ heroId=42（通过 Bundle 传递）
 *        ↓
 *   HeroDetailFragment
 *        │
 *        │ viewModel.loadHeroDetail(42)
 *        ↓
 *   HeroDetailViewModel
 *        │
 *        │ heroRepository.getHeroDetail(42)
 *        ↓
 *   HeroRepository
 *        │
 *        │ 离线优先：Room缓存 → 网络请求 → 更新缓存
 *        ↓
 *   LiveData<HeroDetailUiModel>
 *        │
 *        │ Fragment observe() 回调
 *        ↓
 *   HeroDetailFragment.updateHeroDetail()
 *        │
 *        │ 更新 UI：名称、技能、出装、克制、版本陷阱等
 *        ↓
 *   用户看到英雄详情页
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、LiveData 状态管理
 * ═══════════════════════════════════════════════════════════════════
 *
 * ViewModel 暴露 4 个 LiveData：
 *
 *   heroDetail : HeroDetailUiModel  ── 英雄详情数据（包含所有展示信息）
 *   loading    : Boolean            ── 加载状态（true=加载中）
 *   error      : String             ── 错误信息（null=无错误）
 *   isOffline  : Boolean            ── 离线模式状态
 *
 * 状态互斥关系：
 * - loading=true 时，heroDetail 和 error 应为 null
 * - heroDetail 有值时，loading=false 且 error=null
 * - error 有值时，loading=false 且 heroDetail 可能为 null（无缓存）或有值（有缓存）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、生命周期
 * ═══════════════════════════════════════════════════════════════════
 *
 *   Fragment 创建          数据加载完成        用户返回列表
 *   ┌──────────┐          ┌──────────┐       ┌──────────┐
 *   │ onViewCreated()│    │ heroDetail│      │ onDestroyView()│
 *   │ loadHeroDetail()│ → │ 有值      │  →   │ binding=null│
 *   │ loading=true │        │ loading=false│     │ (ViewModel存活)│
 *   └──────────┘          └──────────┘       └──────────┘
 *
 * ViewModel 的存活范围：
 * - ViewModel 在 Fragment 因配置变更（如旋转屏幕）重建时不会被销毁
 * - ViewModel 在 Fragment 被永久销毁（用户导航离开）时才会被清除
 * - 这意味着旋转屏幕后不需要重新加载英雄详情
 *
 * @see HeroRepository
 * @see com.aram.mayhem.feature.hero.HeroDetailFragment
 */
@HiltViewModel
public class HeroDetailViewModel extends AndroidViewModel {

    private final HeroRepository heroRepository;

    /**
     * 英雄详情数据 ── Fragment 观察此 LiveData 更新整个详情页
     *
     * HeroDetailUiModel 包含英雄的所有展示信息：
     * - 基本信息（名称、称号、定位、梯级）
     * - 数据指标（胜率、选取率、KDA）
     * - 技能列表（被动/Q/W/E/R）
     * - 克制提示、协同推荐
     * - 推荐出装、推荐符文
     * - 版本陷阱标记
     */
    private final MutableLiveData<HeroDetailUiModel> heroDetail = new MutableLiveData<>();

    /**
     * 加载状态 ── 控制进度条显示
     *
     * true = 正在加载英雄详情（显示加载动画）
     * false = 加载完成（显示内容或错误）
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息 ── 加载失败时显示错误提示
     *
     * null 表示没有错误。
     * 有值时 Fragment 显示错误容器和重试按钮。
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 离线模式状态 ── 标记当前是否无网络连接
     */
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    /**
     * 构造函数 ── Hilt 自动调用，注入依赖
     *
     * 与 HeroListViewModel 不同，构造函数中不自动加载数据。
     * 因为需要先从 Bundle 中获取 heroId，才能调用 loadHeroDetail()。
     *
     * @param application    Android 应用上下文
     * @param heroRepository 英雄数据仓库（Hilt 自动注入 @Singleton 实例）
     */
    @Inject
    public HeroDetailViewModel(@NonNull Application application, HeroRepository heroRepository) {
        super(application);
        this.heroRepository = heroRepository;
    }

    /**
     * 获取英雄详情的可观察数据
     *
     * Fragment 通过 observe() 订阅此 LiveData。
     * 数据变化时 Fragment 调用 updateHeroDetail() 更新整个页面。
     *
     * @return LiveData<HeroDetailUiModel> 英雄详情
     */
    public LiveData<HeroDetailUiModel> getHeroDetail() {
        return heroDetail;
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
     * @return LiveData<String> 错误信息，null 表示无错误
     */
    public LiveData<String> getError() {
        return error;
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
     * 加载英雄详情 ── 核心方法，根据 heroId 获取完整英雄信息
     *
     * 由 HeroDetailFragment.onViewCreated() 调用。
     *
     * 执行流程：
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 1. 设置加载状态 loading=true                                 │
     * │ 2. 重置离线标记 isOffline=false                              │
     * │ 3. 调用 heroRepository.getHeroDetail(heroId)                │
     * │ 4. Repository 执行离线优先策略：                              │
     * │    a. 先从 Room 缓存读取（如果有，立即返回）                   │
     * │    b. 如果在线，发起网络请求                                  │
     * │    c. 网络成功：更新缓存 + 返回最新数据                       │
     * │    d. 网络失败：保持缓存数据                                  │
     * │ 5. 收到数据后更新 LiveData：                                  │
     * │    - 成功：heroDetail=数据, loading=false                    │
     * │    - 失败：error="获取英雄详情失败", loading=false            │
     * └─────────────────────────────────────────────────────────────┘
     *
     * observeForever 的使用说明：
     * - ViewModel 没有 LifecycleOwner，无法使用 observe()
     * - observeForever 不会自动取消订阅，需要手动管理
     * - 本项目中 Repository 返回的 LiveData 是一次性的，风险较低
     *
     * savedInstanceState 检查：
     * - Fragment.onViewCreated() 中会检查 savedInstanceState == null
     * - 如果是配置变更（旋转屏幕），savedInstanceState 不为 null，
     *   此时不再调用 loadHeroDetail()，因为 ViewModel 还活着，数据还在
     * - 如果是首次创建，savedInstanceState 为 null，正常加载数据
     *
     * @param heroId 英雄唯一标识符（数据库主键），从 Bundle 中获取
     */
    public void loadHeroDetail(long heroId) {
        loading.setValue(true);
        isOffline.setValue(false);

        heroRepository.getHeroDetail(heroId).observeForever(detail -> {
            loading.setValue(false);
            if (detail != null) {
                heroDetail.setValue(detail);
            } else {
                error.setValue("获取英雄详情失败");
            }
        });
    }
}
