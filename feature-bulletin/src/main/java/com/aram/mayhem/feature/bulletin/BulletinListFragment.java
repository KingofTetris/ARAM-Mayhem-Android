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

@AndroidEntryPoint
/**
 * 公告列表页
 *
 * 功能：展示公告列表，支持类型筛选、分页加载、轮播图
 * 导航：点击公告 → BulletinDetailFragment
 * 关联：BulletinListViewModel, BulletinAdapter, BulletinCarouselView
 */
public class BulletinListFragment extends Fragment {

    private FragmentBulletinListBinding binding;
    private BulletinListViewModel viewModel;
    private BulletinAdapter adapter;

    private OnBulletinDetailListener onBulletinDetailListener;

    public interface OnBulletinDetailListener {
        void onBulletinDetail(long bulletinId);
    }

    public void setOnBulletinDetailListener(OnBulletinDetailListener listener) {
        this.onBulletinDetailListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBulletinListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BulletinListViewModel.class);

        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupCarousel();
        observeViewModel();

        viewModel.loadCarouselBulletins();
        viewModel.loadBulletins(null, true);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

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

    private String getTypeForTab(int position) {
        switch (position) {
            case 1: return "version";
            case 2: return "event";
            case 3: return "notice";
            default: return null;
        }
    }

    private void setupRecyclerView() {
        adapter = new BulletinAdapter();
        adapter.setOnBulletinClickListener(bulletin -> {
            showBulletinDetail(bulletin);
        });
        binding.recyclerBulletins.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBulletins.setAdapter(adapter);
    }

    private void setupCarousel() {
        binding.carouselView.setOnBulletinClickListener(bulletin -> {
            showBulletinDetail(bulletin);
        });
    }

    private void showBulletinDetail(BulletinUiModel bulletin) {
        if (onBulletinDetailListener != null) {
            onBulletinDetailListener.onBulletinDetail(bulletin.getId());
        }
    }

    private void observeViewModel() {
        viewModel.getBulletins().observe(getViewLifecycleOwner(), bulletins -> {
            if (bulletins != null) {
                adapter.submitList(bulletins);
            }
        });

        viewModel.getCarouselBulletins().observe(getViewLifecycleOwner(), bulletins -> {
            if (bulletins != null && !bulletins.isEmpty()) {
                binding.carouselView.setBulletins(bulletins);
                binding.carouselView.startAutoScroll();
            } else {
                binding.carouselView.setVisibility(View.GONE);
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                binding.progressContainer.setVisibility(View.VISIBLE);
                binding.errorContainer.setVisibility(View.GONE);
            } else {
                binding.progressContainer.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                binding.progressContainer.setVisibility(View.GONE);
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.textError.setText(errorMessage);
                binding.buttonRetry.setOnClickListener(v -> viewModel.loadBulletins(null, true));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.carouselView.stopAutoScroll();
        }
        binding = null;
    }
}
