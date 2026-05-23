package com.aram.mayhem.feature.hero;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.feature.hero.adapter.SkillAdapter;
import com.aram.mayhem.feature.hero.databinding.FragmentHeroDetailBinding;
import com.aram.mayhem.feature.hero.viewmodel.HeroDetailViewModel;
import com.aram.mayhem.ui.model.HeroDetailUiModel;
import com.aram.mayhem.ui.widget.BalanceBar;
import com.bumptech.glide.Glide;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * 英雄详情页 ── 展示单个英雄的完整信息
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroDetailFragment 是用户从英雄列表点击某个英雄后进入的详情页，负责展示：
 * 1. 英雄基本信息（中文名、英文名、称号、定位、梯级）
 * 2. 数据指标（胜率、选取率、KDA）
 * 3. 英雄描述（背景故事/玩法概述）
 * 4. 技能列表（被动/Q/W/E/R 5个技能）
 * 5. 克制提示（对抗该英雄的建议）
 * 6. 协同推荐（与该英雄配合好的英雄）
 * 7. 推荐出装（装备建议）
 * 8. 推荐符文（符文搭配建议，带品质色标识）
 * 9. 版本陷阱标记（是否为当前版本陷阱英雄）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构
 * ═══════════════════════════════════════════════════════════════════
 *
 *   fragment_hero_detail.xml（CoordinatorLayout + AppBarLayout）
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  CollapsingToolbarLayout（可折叠标题栏）                  │
 *   │  ┌─────────────────────────────────────────────────┐   │
 *   │  │  imageHeroBanner（英雄横幅图片）                  │   │
 *   │  │  toolbar（返回按钮 + 标题）                       │   │
 *   │  └─────────────────────────────────────────────────┘   │
 *   │                                                         │
 *   │  NestedScrollView（可滚动内容区域）                      │
 *   │  ┌─────────────────────────────────────────────────┐   │
 *   │  │  基本信息区：中文名 / 英文名 / 称号 / 定位 / 梯级 │   │
 *   │  ├─────────────────────────────────────────────────┤   │
 *   │  │  数据指标区：胜率 / 选取率 / KDA / BalanceBar    │   │
 *   │  ├─────────────────────────────────────────────────┤   │
 *   │  │  VersionTrapBanner（版本陷阱警告横幅）            │   │
 *   │  ├─────────────────────────────────────────────────┤   │
 *   │  │  英雄描述卡片                                    │   │
 *   │  ├─────────────────────────────────────────────────┤   │
 *   │  │  技能列表卡片（RecyclerView）                     │   │
 *   │  ├─────────────────────────────────────────────────┤   │
 *   │  │  克制提示卡片（ChipGroup）                        │   │
 *   │  ├─────────────────────────────────────────────────┤   │
 *   │  │  协同推荐卡片（ChipGroup）                        │   │
 *   │  ├─────────────────────────────────────────────────┤   │
 *   │  │  推荐出装卡片                                    │   │
 *   │  ├─────────────────────────────────────────────────┤   │
 *   │  │  推荐符文卡片（品质色 Chip）                      │   │
 *   │  └─────────────────────────────────────────────────┘   │
 *   └─────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、导航参数传递
 * ═══════════════════════════════════════════════════════════════════
 *
 *   HeroListFragment                     HeroDetailFragment
 *   ┌──────────────┐                    ┌──────────────────┐
 *   │ 点击英雄卡片  │                    │ onViewCreated()  │
 *   │ heroId=42    │ ──Bundle──→        │ getArguments()   │
 *   │              │   "heroId"=42      │ .getLong("heroId")│
 *   └──────────────┘                    │ → loadHeroDetail(42)│
 *                                       └──────────────────┘
 *
 *   参数传递链：
 *   HeroCardAdapter.onHeroClick()
 *     → OnHeroSelectedListener.onHeroSelected(42)
 *     → MainActivity 将 heroId 放入 Bundle
 *     → 创建 HeroDetailFragment 并设置 Arguments
 *     → Fragment.onViewCreated() 读取 heroId
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、条件渲染策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 详情页的每个卡片都是条件显示的：
 * - 有数据 → 显示卡片（View.VISIBLE）
 * - 无数据 → 隐藏卡片（View.GONE）
 *
 * 这样做的好处：
 * - 不同英雄的数据完整度不同（有些英雄可能没有克制提示）
 * - 隐藏空卡片避免页面出现空白区域
 * - 用户只看到有实际内容的部分
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、推荐符文降级显示
 * ═══════════════════════════════════════════════════════════════════
 *
 * 推荐符文有两种数据来源：
 * 1. recommendedAugments（完整数据：名称 + 品质 + 图标）→ 品质色 Chip
 * 2. recommendedAugmentIds（仅 ID 列表）→ 灰色 Chip "符文 #ID"
 *
 * 优先使用完整数据，降级使用 ID 列表。降级场景：
 * - 缓存数据中只有 ID（列表页缓存不包含符文详情）
 * - 后端接口未返回符文详情
 *
 * @see HeroDetailViewModel
 * @see SkillAdapter
 * @see com.aram.mayhem.ui.widget.VersionTrapBanner
 * @see com.aram.mayhem.ui.widget.BalanceBar
 */
@AndroidEntryPoint
public class HeroDetailFragment extends Fragment {

    /**
     * ViewBinding 对象 ── 持有详情页布局中所有视图的引用
     *
     * 生命周期：onCreateView() 创建 → onDestroyView() 置 null
     */
    private FragmentHeroDetailBinding binding;

    /**
     * 英雄详情 ViewModel ── 管理详情数据和加载状态
     *
     * 通过 ViewModelProvider 获取，Hilt 自动注入依赖。
     * 与 HeroListViewModel 不同，构造函数中不自动加载数据，
     * 需要手动调用 loadHeroDetail(heroId)。
     */
    private HeroDetailViewModel viewModel;

    /**
     * 技能列表适配器 ── 展示英雄的 5 个技能（P/Q/W/E/R）
     */
    private SkillAdapter skillAdapter;

    /**
     * 创建 Fragment 视图 ── 加载详情页布局
     *
     * @param inflater           布局填充器
     * @param container          父视图容器
     * @param savedInstanceState 保存的实例状态
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHeroDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成 ── 初始化组件并加载数据
     *
     * 执行步骤：
     * 1. 获取 ViewModel
     * 2. 设置 Toolbar 返回按钮
     * 3. 配置技能列表 RecyclerView
     * 4. 订阅 ViewModel 的 LiveData
     * 5. 从 Bundle 获取 heroId 并加载详情
     *
     * savedInstanceState 检查：
     * - savedInstanceState == null：首次创建，需要加载数据
     * - savedInstanceState != null：配置变更（旋转屏幕），ViewModel 还活着，数据还在
     * - 不做检查会导致旋转屏幕时重复加载
     *
     * heroId 获取：
     * - getArguments() 获取 Fragment 的 Arguments Bundle
     * - getLong("heroId", -1) 获取 heroId，默认值 -1 表示无效
     * - heroId > 0 才发起加载，避免无效请求
     *
     * @param view               Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HeroDetailViewModel.class);

        setupToolbar();
        setupRecyclerView();
        observeViewModel();

        if (savedInstanceState == null) {
            long heroId = getArguments() != null ? getArguments().getLong("heroId", -1) : -1;
            if (heroId > 0) {
                viewModel.loadHeroDetail(heroId);
            }
        }
    }

    /**
     * 设置 Toolbar ── 配置返回按钮
     *
     * 点击返回按钮调用 requireActivity().onBackPressed()，
     * 返回到英雄列表页。
     *
     * 为什么用 onBackPressed() 而不是 popBackStack()？
     * - onBackPressed() 会触发 Activity 的回退逻辑
     * - 包括 Fragment 事务的回退栈弹出
     * - 效果等价，但 onBackPressed() 更通用
     */
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    /**
     * 设置技能列表 RecyclerView ── 配置技能卡片的列表展示
     *
     * 使用 LinearLayoutManager 垂直排列 5 个技能卡片。
     * 技能顺序：被动(P) → Q → W → E → R（与后端返回顺序一致）
     */
    private void setupRecyclerView() {
        skillAdapter = new SkillAdapter();
        binding.recyclerSkills.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSkills.setAdapter(skillAdapter);
    }

    /**
     * 订阅 ViewModel 的 LiveData ── 建立数据观察关系
     *
     * 三个观察关系：
     * 1. loading → updateLoading()：加载状态变化时显示/隐藏进度条
     * 2. error → updateError()：错误信息变化时显示错误页面
     * 3. heroDetail → updateHeroDetail()：详情数据变化时更新整个页面
     */
    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoading);
        viewModel.getError().observe(getViewLifecycleOwner(), this::updateError);
        viewModel.getHeroDetail().observe(getViewLifecycleOwner(), this::updateHeroDetail);
    }

    /**
     * 更新加载状态 ── 显示/隐藏进度条
     *
     * 加载中：显示进度条，隐藏错误容器
     * 加载完成：隐藏进度条（内容显示由 updateHeroDetail 处理）
     *
     * @param isLoading 是否正在加载
     */
    private void updateLoading(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            binding.progressContainer.setVisibility(View.VISIBLE);
            binding.errorContainer.setVisibility(View.GONE);
        } else {
            binding.progressContainer.setVisibility(View.GONE);
        }
    }

    /**
     * 更新错误状态 ── 显示错误信息和重试按钮
     *
     * 错误发生时：
     * 1. 隐藏进度条
     * 2. 显示错误容器
     * 3. 设置错误文本
     * 4. 配置重试按钮（重新加载英雄详情）
     *
     * 重试时需要重新获取 heroId（从 Arguments 中读取）。
     *
     * @param errorMessage 错误信息
     */
    private void updateError(String errorMessage) {
        if (errorMessage != null) {
            binding.progressContainer.setVisibility(View.GONE);
            binding.errorContainer.setVisibility(View.VISIBLE);
            binding.textError.setText(errorMessage);
            binding.buttonRetry.setOnClickListener(v -> {
                long heroId = getArguments() != null ? getArguments().getLong("heroId", -1) : -1;
                if (heroId > 0) {
                    viewModel.loadHeroDetail(heroId);
                }
            });
        }
    }

    /**
     * 更新英雄详情 ── 核心方法，数据到达后更新整个页面
     *
     * 执行流程：
     * 1. 隐藏进度条和错误容器
     * 2. 依次调用各区域更新方法
     *
     * 8 个更新方法（按页面从上到下的顺序）：
     * - updateBasicInfo()：基本信息（名称、称号、定位、梯级）
     * - updateBalanceData()：数据指标（胜率、选取率、KDA、BalanceBar）
     * - updateVersionTrapBanner()：版本陷阱横幅
     * - updateDescription()：英雄描述
     * - updateSkills()：技能列表
     * - updateCounterTips()：克制提示
     * - updateSynergies()：协同推荐
     * - updateRecommendedBuild()：推荐出装 + 推荐符文
     * - loadHeroImage()：英雄横幅图片
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateHeroDetail(HeroDetailUiModel hero) {
        if (hero == null) return;

        binding.progressContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);

        updateBasicInfo(hero);
        updateBalanceData(hero);
        updateVersionTrapBanner(hero);
        updateDescription(hero);
        updateSkills(hero);
        updateCounterTips(hero);
        updateSynergies(hero);
        updateRecommendedBuild(hero);
        loadHeroImage(hero);
    }

    /**
     * 更新基本信息 ── 英雄名称、称号、定位、梯级
     *
     * 设置内容：
     * - textHeroNameZh：中文名（如"提莫"）
     * - textHeroNameEn：英文名（如"Teemo"）
     * - textHeroTitle：称号（如"迅捷斥候"）
     * - textHeroRole：定位（如"射手"）
     * - collapsingToolbar.title：折叠工具栏标题（中文名）
     * - chipTier：梯级标签（带对应颜色背景）
     *
     * 梯级颜色映射：
     * - S+ → tier_s_plus（红色）
     * - S  → tier_s（橙色）
     * - A  → tier_a（黄色）
     * - B  → tier_b（绿色）
     * - C  → tier_c（灰色）
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateBasicInfo(HeroDetailUiModel hero) {
        binding.textHeroNameZh.setText(hero.getNameZh());
        binding.textHeroNameEn.setText(hero.getNameEn());
        binding.textHeroTitle.setText(hero.getTitle());
        binding.textHeroRole.setText(hero.getRole());

        binding.collapsingToolbar.setTitle(hero.getNameZh());

        if (hero.getTier() != null) {
            binding.chipTier.setText(hero.getTier().getLabel());
            binding.chipTier.setChipBackgroundColorResource(getTierColor(hero.getTier()));
        }
    }

    /**
     * 更新数据指标 ── 胜率、选取率、KDA 及可视化进度条
     *
     * 展示内容：
     * - textWinRate：胜率百分比（如"52.3%"）
     * - textPickRate：选取率百分比（如"15.7%"）
     * - textKDA：KDA 比值（如"3.2:1"）
     * - balanceBarWinRate：胜率可视化进度条
     * - balanceBarPickRate：选取率可视化进度条
     *
     * BalanceBar 的工作原理：
     * - setData(label, value, maxValue) 设置进度条数据
     * - 自动计算百分比并绘制进度条
     * - 胜率 maxValue=100（因为胜率是百分比）
     * - 选取率 maxValue=100（同理）
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateBalanceData(HeroDetailUiModel hero) {
        binding.textWinRate.setText(hero.getWinRateDisplay());
        binding.textPickRate.setText(hero.getPickRateDisplay());
        binding.textKDA.setText(hero.getKdaDisplay());
        binding.textWinRateBar.setText(hero.getWinRateDisplay());
        binding.textPickRateBar.setText(hero.getPickRateDisplay());

        binding.balanceBarWinRate.setData("胜率", (float) hero.getWinRate(), 100f);
        binding.balanceBarPickRate.setData("登场率", (float) hero.getPickRate(), 100f);
    }

    /**
     * 更新英雄描述 ── 条件显示描述卡片
     *
     * 有描述内容 → 显示卡片
     * 无描述内容 → 隐藏卡片（View.GONE 不占空间）
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateDescription(HeroDetailUiModel hero) {
        if (hero.getDescription() != null && !hero.getDescription().isEmpty()) {
            binding.cardDescription.setVisibility(View.VISIBLE);
            binding.textDescription.setText(hero.getDescription());
        } else {
            binding.cardDescription.setVisibility(View.GONE);
        }
    }

    /**
     * 更新技能列表 ── 将技能数据传递给 SkillAdapter
     *
     * 有技能数据 → 显示技能卡片，设置适配器数据
     * 无技能数据 → 隐藏技能卡片
     *
     * skillAdapter.setSkills() 会触发 notifyDataSetChanged()，
     * RecyclerView 重新绑定所有技能卡片。
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateSkills(HeroDetailUiModel hero) {
        if (hero.getSkills() != null && !hero.getSkills().isEmpty()) {
            binding.cardSkills.setVisibility(View.VISIBLE);
            skillAdapter.setSkills(hero.getSkills());
        } else {
            binding.cardSkills.setVisibility(View.GONE);
        }
    }

    /**
     * 更新克制提示 ── 以 Chip 标签形式展示对抗建议
     *
     * 每条克制提示渲染为一个不可点击的 Chip。
     * Chip 设置：
     * - setClickable(false)：不可点击（纯展示）
     * - setCheckable(false)：不可选中
     *
     * 为什么要先 removeAllViews()？
     * - 如果网络恢复后重新加载数据，ChipGroup 中可能有旧的 Chip
     * - 先清空再添加，避免重复显示
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateCounterTips(HeroDetailUiModel hero) {
        if (hero.getCounterTips() != null && !hero.getCounterTips().isEmpty()) {
            binding.cardCounterTips.setVisibility(View.VISIBLE);
            binding.chipGroupCounters.removeAllViews();
            for (String tip : hero.getCounterTips()) {
                Chip chip = new Chip(requireContext());
                chip.setText(tip);
                chip.setClickable(false);
                chip.setCheckable(false);
                binding.chipGroupCounters.addView(chip);
            }
        } else {
            binding.cardCounterTips.setVisibility(View.GONE);
        }
    }

    /**
     * 更新协同推荐 ── 以 Chip 标签形式展示配合英雄
     *
     * 与 updateCounterTips() 实现完全一致，只是数据源不同：
     * - counterTips：克制提示（对抗该英雄的建议）
     * - synergies：协同推荐（与该英雄配合好的英雄）
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateSynergies(HeroDetailUiModel hero) {
        if (hero.getSynergies() != null && !hero.getSynergies().isEmpty()) {
            binding.cardSynergies.setVisibility(View.VISIBLE);
            binding.chipGroupSynergies.removeAllViews();
            for (String synergy : hero.getSynergies()) {
                Chip chip = new Chip(requireContext());
                chip.setText(synergy);
                chip.setClickable(false);
                chip.setCheckable(false);
                binding.chipGroupSynergies.addView(chip);
            }
        } else {
            binding.cardSynergies.setVisibility(View.GONE);
        }
    }

    /**
     * 更新推荐出装和推荐符文
     *
     * 推荐出装：纯文本展示（如"卢登的伙伴 → 影焰 → 帽子"）
     * 推荐符文：调用 updateRecommendedAugments() 单独处理
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateRecommendedBuild(HeroDetailUiModel hero) {
        if (hero.getRecommendedBuild() != null && !hero.getRecommendedBuild().isEmpty()) {
            binding.cardBuild.setVisibility(View.VISIBLE);
            binding.textRecommendedBuild.setText(hero.getRecommendedBuild());
        } else {
            binding.cardBuild.setVisibility(View.GONE);
        }

        updateRecommendedAugments(hero);
    }

    /**
     * 更新推荐强化符文展示 ── 以品质色 Chip 形式展示
     *
     * ══════════════════════════════════════════════════════════════
     * 降级策略：
     *   优先使用 recommendedAugments（含名称+品质）→ 品质色 Chip
     *   降级使用 recommendedAugmentIds（仅 ID）→ 灰色 Chip "符文 #ID"
     * ══════════════════════════════════════════════════════════════
     *
     * 品质色规则：
     * - 棱彩 → 紫金色背景 + 白色文字（最稀有）
     * - 金色 → 金黄色背景 + 白色文字
     * - 银色 → 银灰色背景 + 黑色文字
     *
     * Timber 日志：
     * - 使用 Timber 记录符文展示方式（按名称/按ID降级/无符文）
     * - 用于排查符文显示问题（如"为什么显示符文 #ID 而不是名称"）
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateRecommendedAugments(HeroDetailUiModel hero) {
        List<HeroDetailUiModel.AugmentBriefUiModel> augments = hero.getRecommendedAugments();
        List<Long> augmentIds = hero.getRecommendedAugmentIds();
        boolean hasAugments = (augments != null && !augments.isEmpty()) || (augmentIds != null && !augmentIds.isEmpty());

        if (hasAugments) {
            binding.cardAugments.setVisibility(View.VISIBLE);
            binding.chipGroupAugments.removeAllViews();

            if (augments != null && !augments.isEmpty()) {
                // 有完整符文数据：按名称+品质展示
                Timber.i("AugmentDisplay: load_by_name | heroId=%d | count=%d | source=recommendedAugments | timestamp=%d",
                        hero.getId(), augments.size(), System.currentTimeMillis());
                for (HeroDetailUiModel.AugmentBriefUiModel augment : augments) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(augment.getNameZh());
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    applyQualityChipStyle(chip, augment.getQuality());
                    binding.chipGroupAugments.addView(chip);
                }
            } else {
                // 只有 ID 列表：降级展示
                Timber.w("AugmentDisplay: load_by_id_fallback | heroId=%d | count=%d | source=recommendedAugmentIds | timestamp=%d",
                        hero.getId(), augmentIds.size(), System.currentTimeMillis());
                for (Long augmentId : augmentIds) {
                    Chip chip = new Chip(requireContext());
                    chip.setText("符文 #" + augmentId);
                    chip.setClickable(false);
                    chip.setCheckable(false);
                    binding.chipGroupAugments.addView(chip);
                }
            }
        } else {
            // 无符文数据
            Timber.d("AugmentDisplay: no_augments | heroId=%d | timestamp=%d", hero.getId(), System.currentTimeMillis());
            binding.cardAugments.setVisibility(View.GONE);
        }
    }

    /**
     * 根据符文品质设置 Chip 样式 ── 为符文标签着色
     *
     * 品质色规则（与游戏内颜色一致）：
     * - "棱彩" → quality_prismatic（紫金色）+ 白色文字
     * - "金色" → quality_gold（金黄色）+ 白色文字
     * - "银色" → quality_silver（银灰色）+ 黑色文字
     * - null/其他 → 不设置样式（使用默认样式）
     *
     * 为什么银色用黑色文字？
     * - 银灰色背景较浅，白色文字对比度不够
     * - 黑色文字在银灰色背景上更清晰
     *
     * @param chip    要设置样式的 Chip
     * @param quality 符文品质（棱彩/金色/银色）
     */
    private void applyQualityChipStyle(Chip chip, String quality) {
        if (quality == null) return;
        switch (quality) {
            case "棱彩":
                chip.setChipBackgroundColorResource(com.aram.mayhem.ui.R.color.quality_prismatic);
                chip.setTextColor(android.graphics.Color.WHITE);
                break;
            case "金色":
                chip.setChipBackgroundColorResource(com.aram.mayhem.ui.R.color.quality_gold);
                chip.setTextColor(android.graphics.Color.WHITE);
                break;
            case "银色":
                chip.setChipBackgroundColorResource(com.aram.mayhem.ui.R.color.quality_silver);
                chip.setTextColor(android.graphics.Color.BLACK);
                break;
            default:
                break;
        }
    }

    /**
     * 更新版本陷阱横幅 ── 条件显示警告横幅
     *
     * 版本陷阱英雄定义：
     * - 外观/人气很高但实际胜率很低的英雄
     * - 提醒玩家"这个英雄在当前版本表现不佳"
     *
     * 显示逻辑：
     * - isVersionTrap=true → 显示横幅，设置梯级信息
     * - isVersionTrap=false → 隐藏横幅（调用 hide() 带动画效果）
     *
     * @param hero 英雄详情 UI 模型
     */
    private void updateVersionTrapBanner(HeroDetailUiModel hero) {
        if (hero.isVersionTrap()) {
            binding.versionTrapBanner.setTrapInfo(hero.getTier() != null ? hero.getTier().getLabel() : "当前");
            binding.versionTrapBanner.setVisibility(View.VISIBLE);
        } else {
            binding.versionTrapBanner.hide();
        }
    }

    /**
     * 加载英雄横幅图片 ── 使用 Glide 加载网络图片
     *
     * Glide 的工作原理：
     * 1. 检查内存缓存 → 有则直接使用
     * 2. 检查磁盘缓存 → 有则解码后使用
     * 3. 发起网络请求 → 下载后缓存并使用
     *
     * centerCrop() 的作用：
     * - 图片等比缩放填满整个 ImageView
     * - 超出部分被裁剪，不留空白
     * - 适合横幅类图片展示
     *
     * 为什么用 Glide 而不是原生 Bitmap？
     * - Glide 自动处理图片缓存（内存+磁盘）
     * - Glide 自动处理图片解码和缩放
     * - Glide 自动处理生命周期（Fragment 销毁时取消加载）
     * - Glide 自动处理占位图和错误图
     *
     * @param hero 英雄详情 UI 模型
     */
    private void loadHeroImage(HeroDetailUiModel hero) {
        if (hero.getImageUrl() != null && !hero.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(hero.getImageUrl())
                    .centerCrop()
                    .into(binding.imageHeroBanner);
        }
    }

    /**
     * 梯级 → 颜色资源映射
     *
     * 将 Tier 枚举映射为 core-ui 模块中定义的颜色资源 ID。
     * 每个梯级对应不同的背景色，用于 Chip 标签着色。
     *
     * @param tier 梯级枚举
     * @return 颜色资源 ID
     */
    private int getTierColor(com.aram.mayhem.common.Tier tier) {
        switch (tier) {
            case S_PLUS: return com.aram.mayhem.ui.R.color.tier_s_plus;
            case S: return com.aram.mayhem.ui.R.color.tier_s;
            case A: return com.aram.mayhem.ui.R.color.tier_a;
            case B: return com.aram.mayhem.ui.R.color.tier_b;
            case C: return com.aram.mayhem.ui.R.color.tier_c;
            default: return com.aram.mayhem.ui.R.color.tier_c;
        }
    }

    /**
     * Fragment 视图销毁时调用 ── 释放 binding 引用
     *
     * 只需将 binding 置 null，防止视图泄漏。
     * 不需要注销 NetworkCallback（详情页没有注册网络监听）。
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
