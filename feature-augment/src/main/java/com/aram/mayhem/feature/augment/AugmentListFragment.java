package com.aram.mayhem.feature.augment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.feature.augment.databinding.FragmentAugmentListBinding;
import com.aram.mayhem.feature.augment.viewmodel.AugmentViewModel;
import com.aram.mayhem.ui.adapter.AugmentCardAdapter;
import com.aram.mayhem.ui.model.AugmentUiModel;
import com.aram.mayhem.ui.widget.PaginationScrollListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AugmentListFragment extends Fragment {

    private FragmentAugmentListBinding binding;
    private AugmentViewModel viewModel;
    private AugmentCardAdapter adapter;
    private OnAugmentSelectedListener augmentSelectedListener;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean wasOffline = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAugmentSelectedListener) {
            augmentSelectedListener = (OnAugmentSelectedListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAugmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AugmentViewModel.class);
        setupRecyclerView();
        setupTabs();
        setupOfflineDetection();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new AugmentCardAdapter();
        adapter.setOnAugmentClickListener(this::showAugmentDetail);
        binding.recyclerAugmentList.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerAugmentList.setLayoutManager(layoutManager);

        binding.recyclerAugmentList.addOnScrollListener(new PaginationScrollListener(layoutManager, 20) {
            @Override
            public boolean isLoading() {
                return viewModel.getLoadingMore().getValue() != null && viewModel.getLoadingMore().getValue();
            }

            @Override
            public boolean isLastPage() {
                return viewModel.getIsLastPage().getValue() != null && viewModel.getIsLastPage().getValue();
            }

            @Override
            public void onLoadMore() {
                viewModel.loadMore();
            }
        });
    }

    private void setupTabs() {
        binding.tabQuality.addTab(binding.tabQuality.newTab().setText("全部"));
        binding.tabQuality.addTab(binding.tabQuality.newTab().setText("棱彩"));
        binding.tabQuality.addTab(binding.tabQuality.newTab().setText("金"));
        binding.tabQuality.addTab(binding.tabQuality.newTab().setText("银"));

        binding.tabQuality.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String quality = mapTabToQuality(tab.getPosition());
                viewModel.filterByQuality(quality);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private String mapTabToQuality(int position) {
        switch (position) {
            case 1:
                return "PRISMATIC";
            case 2:
                return "GOLD";
            case 3:
                return "SILVER";
            default:
                return "";
        }
    }

    private void setupOfflineDetection() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                wasOffline = true;
                if (getView() != null) {
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(getView(), "网络已断开，正在显示缓存数据", Snackbar.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onAvailable(@NonNull Network network) {
                if (wasOffline && getView() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(getView(), "网络已恢复", Snackbar.LENGTH_SHORT).show();
                        viewModel.retry();
                    });
                }
                wasOffline = false;
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, networkCallback);
    }

    private void observeViewModel() {
        viewModel.getAugments().observe(getViewLifecycleOwner(), this::updateAugmentList);
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoadingState);
        viewModel.getError().observe(getViewLifecycleOwner(), this::showError);
    }

    private void updateAugmentList(List<AugmentUiModel> augments) {
        adapter.submitList(augments);
        if (augments != null && !augments.isEmpty()) {
            binding.statefulLayout.setVisibility(View.GONE);
            binding.recyclerAugmentList.setVisibility(View.VISIBLE);
        } else if (augments != null && augments.isEmpty() && (viewModel.getLoading().getValue() == null || !viewModel.getLoading().getValue())) {
            binding.statefulLayout.setVisibility(View.VISIBLE);
            binding.recyclerAugmentList.setVisibility(View.GONE);
            binding.statefulLayout.showEmpty();
        }
    }

    private void updateLoadingState(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            if (adapter.getItemCount() == 0) {
                binding.statefulLayout.setVisibility(View.VISIBLE);
                binding.recyclerAugmentList.setVisibility(View.GONE);
                binding.statefulLayout.showLoading();
            }
        }
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message != null ? message : "加载失败", Snackbar.LENGTH_LONG)
                    .setAction("重试", v -> viewModel.retry())
                    .show();
        }
    }

    private void showAugmentDetail(AugmentUiModel augment) {
        if (augmentSelectedListener != null) {
            augmentSelectedListener.onAugmentSelected(augment.getId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                cm.unregisterNetworkCallback(networkCallback);
            }
        }
        binding = null;
    }
}