package com.aram.mayhem.feature.augment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.aram.mayhem.feature.augment.databinding.BottomSheetAugmentDetailBinding;
import com.aram.mayhem.feature.augment.viewmodel.AugmentDetailViewModel;
import com.aram.mayhem.ui.model.AugmentUiModel;
import com.aram.mayhem.ui.widget.VersionTrapBanner;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 符文详情底部弹窗 ── 展示符文的完整信息和套装进度
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 BottomSheet 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当用户在符文列表中点击某个符文时，从屏幕底部弹出此弹窗，
 * 展示该符文的完整信息，包括：
 * 1. 符文名称和品质
 * 2. 符文描述（效果说明）
 * 3. 胜率、选取率、平均排名
 * 4. 所属套装（Chip 标签展示）
 * 5. 版本陷阱标记（如果该符文是陷阱）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么用 BottomSheet 而不是全屏 Fragment？
 * ═══════════════════════════════════════════════════════════════════
 *
 * - 符文详情信息量不大，不需要全屏展示
 * - BottomSheet 可以快速查看后关闭，不影响浏览列表的体验
 * - 英雄详情信息量大（技能、克制、出装），需要全屏展示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、页面布局结构
 * ═══════════════════════════════════════════════════════════════════
 *
 *   bottom_sheet_augment_detail.xml
 *   ┌──────────────────────────────────────────┐
 *   │  符文名称         品质标签               │
 *   │  ─────────────────────────────────────── │
 *   │  符文描述                                 │
 *   │  ─────────────────────────────────────── │
 *   │  胜率: 52.3%   选取率: 15.2%   平均排名: 4.2 │
 *   │  ─────────────────────────────────────── │
 *   │  套装标签                                 │
 *   │  [刺客] [法师]                            │
 *   │  ─────────────────────────────────────── │
 *   │  ⚠️ 版本陷阱横幅（仅陷阱符文显示）         │
 *   └──────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、创建和显示流程
 * ═══════════════════════════════════════════════════════════════════
 *
 *   AugmentListFragment           MainActivity              本 BottomSheet
 *   ┌──────────────┐            ┌──────────────┐          ┌──────────────┐
 *   │ 点击符文卡片  │            │ 实现接口      │          │              │
 *   │ augmentId=5  │ ─────────→ │ 创建实例      │ ───────→ │ 接收参数 5   │
 *   │              │  回调       │ 显示弹窗      │          │ 加载详情数据 │
 *   └──────────────┘            └──────────────┘          └──────────────┘
 *
 * @see AugmentDetailViewModel
 * @see OnAugmentSelectedListener
 * @see VersionTrapBanner
 */
@AndroidEntryPoint
public class AugmentDetailBottomSheet extends BottomSheetDialogFragment {

    /**
     * Arguments 键名 ── 用于从 Bundle 中获取符文 ID
     *
     * 传参流程：
     * newInstance(augmentId) → Bundle.putLong("augment_id", id)
     * onViewCreated() → getArguments().getLong("augment_id")
     */
    private static final String ARG_AUGMENT_ID = "augment_id";

    /**
     * ViewBinding ── 类型安全地访问布局中的 View
     */
    private BottomSheetAugmentDetailBinding binding;

    /**
     * 符文详情 ViewModel ── 管理符文详情数据
     */
    private AugmentDetailViewModel viewModel;

    /**
     * 工厂方法 ── 创建 BottomSheet 实例并传入符文 ID
     *
     * 为什么用工厂方法而不是直接 new + setArguments？
     * - 封装参数传递逻辑，调用方只需传 augmentId
     * - Android 推荐的 Fragment 参数传递模式
     * - 系统重建 Fragment 时能自动恢复 Arguments
     *
     * @param augmentId 符文 ID
     * @return 配置好参数的 BottomSheet 实例
     */
    public static AugmentDetailBottomSheet newInstance(long augmentId) {
        AugmentDetailBottomSheet fragment = new AugmentDetailBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_AUGMENT_ID, augmentId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Fragment 创建时调用 ── 初始化 ViewModel
     *
     * 为什么在 onCreate 而不是 onViewCreated 中初始化 ViewModel？
     * - ViewModel 应该在 Fragment 生命周期的早期初始化
     * - onViewCreated 可能在配置变更后被多次调用
     * - onCreate 只调用一次，确保 ViewModel 不会重复创建
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AugmentDetailViewModel.class);
    }

    /**
     * 创建视图 ── 初始化 ViewBinding
     *
     * @param inflater           布局填充器
     * @param container          父视图容器
     * @param savedInstanceState 保存的实例状态
     * @return BottomSheet 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAugmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成 ── 加载符文详情数据
     *
     * 执行流程：
     * 1. 从 Arguments 中取出 augmentId
     * 2. 调用 ViewModel 加载详情
     * 3. 订阅 LiveData 观察数据变化
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            long augmentId = getArguments().getLong(ARG_AUGMENT_ID);
            viewModel.loadAugmentDetail(augmentId);
        }

        observeViewModel();
    }

    /**
     * 观察 ViewModel 数据 ── 订阅符文详情数据变化
     *
     * 当 ViewModel 的 augmentDetail 数据更新时，自动调用 displayAugment() 更新 UI。
     */
    private void observeViewModel() {
        viewModel.getAugmentDetail().observe(getViewLifecycleOwner(), this::displayAugment);
    }

    /**
     * 展示符文详情 ── 将数据绑定到 UI 控件
     *
     * 绑定内容：
     * 1. 名称和品质
     * 2. 描述文本
     * 3. 胜率、选取率、平均排名
     * 4. 版本陷阱横幅（仅陷阱符文显示）
     * 5. 套装标签（Chip，最多3个）
     *
     * @param augment 符文详情 UI 模型
     */
    private void displayAugment(AugmentUiModel augment) {
        if (augment == null) return;

        binding.textAugmentName.setText(augment.getNameZh());
        binding.textAugmentQuality.setText(augment.getQualityDisplay());
        binding.textDescription.setText(augment.getDescription());
        binding.textWinRate.setText(augment.getWinRateDisplay());
        binding.textPickRate.setText(augment.getPickRateDisplay());
        binding.textAvgPlacement.setText(augment.getAvgPlacementDisplay());

        if (augment.isTrap()) {
            binding.trapBanner.setTrapInfo("该符文", "当前");
        } else {
            binding.trapBanner.hide();
        }

        binding.chipGroupSynergy.removeAllViews();
        addSynergyChip(augment.getSynergySet());
        addSynergyChip(augment.getSynergySet2());
        addSynergyChip(augment.getSynergySet3());
    }

    /**
     * 添加套装标签 ── 动态创建 Chip 并添加到 ChipGroup
     *
     * 每个符文最多属于 3 个套装，每个套装用一个 Chip 展示。
     * Chip 设为不可点击（仅展示用途）。
     *
     * 为什么动态创建而不是在 XML 中预定义？
     * - 不同符文属于不同数量的套装（1~3个）
     * - 动态创建可以灵活适配不同数量
     *
     * @param synergy 套装名称，null 或空字符串时不添加
     */
    private void addSynergyChip(String synergy) {
        if (synergy == null || synergy.isEmpty()) return;

        Chip chip = new Chip(requireContext());
        chip.setText(synergy);
        chip.setClickable(false);
        binding.chipGroupSynergy.addView(chip);
    }

    /**
     * 视图销毁 ── 释放 ViewBinding
     *
     * 置 null 防止内存泄漏：Fragment 视图销毁后，binding 持有的 View 引用无效。
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
