package com.aram.mayhem.feature.bulletin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.feature.bulletin.adapter.BulletinAdapter;
import com.aram.mayhem.feature.bulletin.databinding.FragmentBulletinListBinding;
import com.aram.mayhem.feature.bulletin.viewmodel.BulletinListViewModel;
import com.aram.mayhem.ui.model.BulletinUiModel;
import com.google.android.material.tabs.TabLayout;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 公告列表页（公告模块）
 *
 * 功能：展示公告列表，支持类型筛选（全部/版本更新/活动/通知）、分页加载、轮播图展示
 * 导航：点击公告 → BulletinDetailFragment
 * 关联组件：BulletinListViewModel, BulletinAdapter, BulletinCarouselView
 *
 * @see BulletinListViewModel
 * @see BulletinAdapter
 */
@AndroidEntryPoint
public class BulletinListFragment extends Fragment {

    /** 视图绑定对象 */
    private FragmentBulletinListBinding binding;
    /** ViewModel */
    private BulletinListViewModel viewModel;
    /** 公告列表适配器 */
    private BulletinAdapter adapter;

    /** 公告详情点击监听器 */
    private OnBulletinDetailListener onBulletinDetailListener;

    /**
     * 公告详情点击回调接口
     */
    public interface OnBulletinDetailListener {
        /**
         * 点击公告跳转到详情页
         *
         * @param bulletinId 公告ID
         */
        void onBulletinDetail(long bulletinId);
    }

    /**
     * 设置公告详情点击监听器
     *
     * @param listener 点击监听器
     */
    public void setOnBulletinDetailListener(OnBulletinDetailListener listener) {
        this.onBulletinDetailListener = listener;
    }

    /**
     * 创建视图
     *
     * @param inflater 布局填充器
     * @param container 父容器
     * @param savedInstanceState 保存的状态
     * @return 视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBulletinListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化操作
     *
     * @param view 视图
     * @param savedInstanceState 保存的状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BulletinListViewModel.class);

        // 初始化各个组件
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupCarousel();
        observeViewModel();

        // 加载初始数据
        viewModel.loadCarouselBulletins();
        viewModel.loadBulletins(null, true);
    }

    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    /**
     * 设置标签页
     *
     * 添加四个标签：全部、版本更新、活动、通知
     * 监听标签切换事件，重新加载对应类型的公告列表
     */
    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("全部"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("版本更新"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("活动"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("通知"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String type = getTypeForTab(tab.getPosition());
                viewModel.loadBulletins(type, true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                String type = getTypeForTab(tab.getPosition());
                viewModel.loadBulletins(type, true);
            }
        });
    }

    /**
     * 根据标签位置获取对应的公告类型
     *
     * @param position 标签位置
     * @return 公告类型字符串（version/event/notice），全部则返回 null
     */
    private String getTypeForTab(int position) {
        switch (position) {
            case 1: return "version";
            case 2: return "event";
            case 3: return "notice";
            default: return null;
        }
    }

    /**
     * 设置 RecyclerView
     *
     * 初始化适配器，设置点击监听，配置布局管理器
     */
    private void setupRecyclerView() {
        adapter = new BulletinAdapter();
        adapter.setOnBulletinClickListener(this::showBulletinDetail);
        binding.recyclerBulletins.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBulletins.setAdapter(adapter);
    }

    /**
     * 设置轮播图
     *
     * 设置轮播图点击监听
     */
    private void setupCarousel() {
        binding.carouselView.setOnBulletinClickListener(this::showBulletinDetail);
    }

    /**
     * 跳转到公告详情页
     *
     * @param bulletin 公告数据
     */
    private void showBulletinDetail(BulletinUiModel bulletin) {
        if (onBulletinDetailListener != null) {
            onBulletinDetailListener.onBulletinDetail(bulletin.getId());
        }
    }

    /**
     * 观察 ViewModel 数据变化
     *
     * 监听公告列表、轮播数据、加载状态、错误信息的变化
     */
    private void observeViewModel() {
        // 监听公告列表变化
        viewModel.getBulletins().observe(getViewLifecycleOwner(), bulletins -> {
            if (bulletins != null) {
                adapter.submitList(bulletins);
            }
        });

        // 监听轮播公告变化
        viewModel.getCarouselBulletins().observe(getViewLifecycleOwner(), bulletins -> {
            if (bulletins != null && !bulletins.isEmpty()) {
                binding.carouselView.setBulletins(bulletins);
                binding.carouselView.startAutoScroll();
            } else {
                binding.carouselView.setVisibility(View.GONE);
            }
        });

        // 监听加载状态变化
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                binding.progressContainer.setVisibility(View.VISIBLE);
                binding.errorContainer.setVisibility(View.GONE);
            } else {
                binding.progressContainer.setVisibility(View.GONE);
            }
        });

        // 监听错误信息变化
        viewModel.getError().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                binding.progressContainer.setVisibility(View.GONE);
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.textError.setText(errorMessage);
                binding.buttonRetry.setOnClickListener(v -> viewModel.loadBulletins(null, true));
            }
        });
    }

    /**
     * 视图销毁时清理资源
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.carouselView.stopAutoScroll();
        }
        binding = null;
    }
}
