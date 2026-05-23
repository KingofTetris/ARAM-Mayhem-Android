package com.aram.mayhem.feature.augment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.augment.databinding.FragmentAugmentRecommendBinding;
import com.aram.mayhem.feature.augment.viewmodel.AugmentRecommendViewModel;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.aram.mayhem.network.dto.SynergyProgressResponse;
import com.aram.mayhem.ui.model.HeroUiModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 符文推荐页 ── 基于英雄和已选符文的智能推荐
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentRecommendFragment 是符文推荐功能的页面，负责：
 * 1. 让用户选择英雄（Spinner 下拉框）
 * 2. 展示已选符文的套装进度（水平滚动列表）
 * 3. 展示智能推荐结果（垂直滚动列表）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构
 * ═══════════════════════════════════════════════════════════════════
 *
 *   fragment_augment_recommend.xml
 *   ┌──────────────────────────────────────────┐
 *   │  Spinner（英雄选择下拉框）                 │
 *   │  [亚索 ▼]                                │
 *   ├──────────────────────────────────────────┤
 *   │  套装进度区域（水平滚动）                   │
 *   │  ┌────────┐ ┌────────┐ ┌────────┐       │
 *   │  │刺客 2/3│ │法师 1/2│ │战士 0/2│       │
 *   │  │ ████░░ │ │ ██░░░░ │ │ ░░░░░░ │       │
 *   │  └────────┘ └────────┘ └────────┘       │
 *   ├──────────────────────────────────────────┤
 *   │  推荐结果区域（垂直滚动）                   │
 *   │  ┌────────────────────────────────────┐  │
 *   │  │ 🗡️ 刺客符文A   评分: 85            │  │
 *   │  │    推荐理由: 完成刺客套装            │  │
 *   │  │    胜率: 53.2%  选取率: 12.1%      │  │
 *   │  ├────────────────────────────────────┤  │
 *   │  │ 🗡️ 刺客符文B   评分: 72            │  │
 *   │  │    推荐理由: 高胜率符文              │  │
 *   │  │    胜率: 55.1%  选取率: 8.3%       │  │
 *   │  └────────────────────────────────────┘  │
 *   ├──────────────────────────────────────────┤
 *   │  ProgressBar（加载中）                    │
 *   └──────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户选择英雄（Spinner）
 *     → viewModel.setSelectedHero(hero)
 *     → refreshData()
 *     → 同时查询：
 *       ├─ 套装进度 → SynergyProgressAdapter
 *       └─ 推荐结果 → AugmentRecommendAdapter
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、英雄列表的硬编码问题
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当前英雄列表是硬编码的3个英雄（亚索、劫、VN），仅用于演示。
 * 生产环境应从 HeroRepository 动态获取英雄列表。
 *
 * @see AugmentRecommendViewModel
 * @see SynergyProgressAdapter
 * @see AugmentRecommendAdapter
 */
@AndroidEntryPoint
public class AugmentRecommendFragment extends Fragment {

    /**
     * ViewBinding ── 类型安全地访问布局中的 View
     */
    private FragmentAugmentRecommendBinding binding;

    /**
     * 推荐页 ViewModel ── 管理英雄选择、套装进度和推荐数据
     */
    private AugmentRecommendViewModel viewModel;

    /**
     * 推荐结果适配器 ── 展示智能推荐的符文列表
     */
    private AugmentRecommendAdapter adapter;

    /**
     * 套装进度适配器 ── 展示已选符文的套装收集进度
     */
    private SynergyProgressAdapter progressAdapter;

    /**
     * 英雄列表 ── Spinner 下拉框的数据源
     *
     * 注意：当前为硬编码数据，仅用于演示。
     * 生产环境应从 HeroRepository 获取完整英雄列表。
     */
    private List<HeroUiModel> heroList = new ArrayList<>();

    /**
     * 创建视图 ── 初始化 ViewBinding
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAugmentRecommendBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成 ── 初始化所有组件
     *
     * 初始化顺序：
     * 1. ViewModel（数据源）
     * 2. RecyclerView（套装进度 + 推荐列表）
     * 3. Spinner（英雄选择）
     * 4. 观察数据（LiveData 订阅）
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AugmentRecommendViewModel.class);

        setupRecyclerViews();
        setupHeroSpinner();
        observeViewModel();
    }

    /**
     * 初始化两个 RecyclerView ── 套装进度和推荐结果
     *
     * 布局结构：
     * - 推荐结果：垂直滚动（纵向列表）
     * - 套装进度：水平滚动（横向列表）
     *
     * 套装进度的 RecyclerView 嵌套在 section_synergy_progress 内，
     * 需要通过 findViewById 逐层查找。
     */
    private void setupRecyclerViews() {
        adapter = new AugmentRecommendAdapter();
        binding.recyclerRecommendations.setAdapter(adapter);
        binding.recyclerRecommendations.setLayoutManager(new LinearLayoutManager(requireContext()));

        progressAdapter = new SynergyProgressAdapter();
        View progressSection = binding.getRoot().findViewById(R.id.section_synergy_progress);
        if (progressSection != null) {
            RecyclerView progressRecycler = progressSection.findViewById(R.id.recycler_synergy_progress);
            if (progressRecycler != null) {
                progressRecycler.setAdapter(progressAdapter);
                progressRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            }
        }
    }

    /**
     * 初始化英雄选择 Spinner ── 配置下拉框数据和选中事件
     *
     * Spinner 工作原理：
     * 1. ArrayAdapter 将 List<String> 映射到下拉列表项
     * 2. 用户选中某项 → onItemSelected 回调
     * 3. 根据选中位置从 heroList 获取 HeroUiModel
     * 4. 通知 ViewModel 更新选中英雄
     *
     * 注意：onNothingSelected 在 Spinner 中几乎不会触发，
     * 因为 Spinner 必须有一个选中项。
     */
    private void setupHeroSpinner() {
        heroList.add(new HeroUiModel(1, "亚索", "Yasuo", "快乐风男", "战士", null, 0.52, 0.15, null));
        heroList.add(new HeroUiModel(2, "劫", "Zed", "影流之主", "刺客", null, 0.51, 0.12, null));
        heroList.add(new HeroUiModel(3, "VN", "Vayne", "暗夜猎手", "ADC", null, 0.50, 0.18, null));

        List<String> heroNames = new ArrayList<>();
        for (HeroUiModel hero : heroList) {
            heroNames.add(hero.getNameZh());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                heroNames
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerHero.setAdapter(spinnerAdapter);

        binding.spinnerHero.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < heroList.size()) {
                    viewModel.setSelectedHero(heroList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * 观察 ViewModel 数据 ── 订阅三个 LiveData
     *
     * 1. recommendations：推荐结果 → 更新推荐列表
     * 2. synergyProgress：套装进度 → 更新进度条
     * 3. loading：加载状态 → 显示/隐藏进度条
     */
    private void observeViewModel() {
        viewModel.getRecommendations().observe(getViewLifecycleOwner(), this::updateRecommendations);
        viewModel.getSynergyProgress().observe(getViewLifecycleOwner(), this::updateSynergyProgress);
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoading);
    }

    /**
     * 更新推荐结果列表 ── 将新数据提交给适配器
     *
     * @param recommendations 推荐结果列表，null 时清空
     */
    private void updateRecommendations(List<AugmentRecommendResponse> recommendations) {
        if (recommendations != null) {
            adapter.submitList(recommendations);
        } else {
            adapter.submitList(new ArrayList<>());
        }
    }

    /**
     * 更新套装进度列表 ── 将新数据提交给进度适配器
     *
     * @param progress 套装进度列表
     */
    private void updateSynergyProgress(List<SynergyProgressResponse> progress) {
        if (progress != null) {
            progressAdapter.submitList(progress);
        }
    }

    /**
     * 更新加载状态 ── 显示/隐藏进度条
     *
     * @param isLoading true=正在加载推荐结果
     */
    private void updateLoading(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            binding.progressLoading.setVisibility(View.VISIBLE);
        } else {
            binding.progressLoading.setVisibility(View.GONE);
        }
    }

    /**
     * 视图销毁 ── 释放 ViewBinding
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
