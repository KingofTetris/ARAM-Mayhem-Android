package com.aram.mayhem.feature.hero;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.common.Tier;
import com.aram.mayhem.feature.hero.databinding.FragmentHeroListBinding;
import com.aram.mayhem.feature.hero.viewmodel.HeroListViewModel;
import com.aram.mayhem.ui.adapter.HeroCardAdapter;
import com.aram.mayhem.ui.model.HeroUiModel;
import com.aram.mayhem.ui.widget.PaginationScrollListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 英雄列表页
 *
 * 功能：展示英雄列表，支持梯级/定位筛选、关键词搜索、分页加载
 * 导航：点击英雄卡片 → HeroDetailFragment
 * 关联：HeroListViewModel, HeroCardAdapter, SearchToolbar, StatefulLayout
 */
@AndroidEntryPoint
public class HeroListFragment extends Fragment {

    private FragmentHeroListBinding binding;
    private EditText searchEditText;
    private HeroListViewModel viewModel;
    private HeroCardAdapter adapter;
    private OnHeroSelectedListener heroSelectedListener;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean wasOffline = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnHeroSelectedListener) {
            heroSelectedListener = (OnHeroSelectedListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHeroListBinding.inflate(inflater, container, false);
        searchEditText = binding.getRoot().findViewById(com.aram.mayhem.ui.R.id.edit_search);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HeroListViewModel.class);
        setupRecyclerView();
        setupSearchToolbar();
        setupTierFilterChips();
        setupOfflineDetection();
        observeViewModel();
    }

    private void setupOfflineDetection() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                wasOffline = true;
                viewModel.setOffline(true);
                if (getView() != null) {
                    Snackbar.make(getView(), "网络已断开，正在显示缓存数据", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onAvailable(@NonNull Network network) {
                if (wasOffline && getView() != null) {
                    Snackbar.make(getView(), "网络已恢复，正在刷新数据", Snackbar.LENGTH_SHORT).show();
                    viewModel.retry();
                }
                wasOffline = false;
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, networkCallback);
    }

    private void setupRecyclerView() {
        adapter = new HeroCardAdapter();
        adapter.setOnHeroClickListener(this::navigateToHeroDetail);
        binding.recyclerHeroList.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerHeroList.setLayoutManager(layoutManager);

        binding.recyclerHeroList.addOnScrollListener(new PaginationScrollListener(layoutManager, 20) {
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

    private void setupSearchToolbar() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.searchHeroes(s.toString());
            }
        });
    }

    private void setupTierFilterChips() {
        binding.chipGroupTier.setOnCheckedChangeListener((group, checkedId) -> {
            Chip checkedChip = group.findViewById(checkedId);
            if (checkedChip != null) {
                String tierText = checkedChip.getText().toString();
                Tier selectedTier = mapChipTextToTier(tierText);
                viewModel.filterByTier(selectedTier);
            }
        });
    }

    private Tier mapChipTextToTier(String text) {
        if ("S+".equals(text)) return Tier.S_PLUS;
        if ("S".equals(text)) return Tier.S;
        if ("A".equals(text)) return Tier.A;
        if ("B".equals(text)) return Tier.B;
        if ("C".equals(text)) return Tier.C;
        return null;
    }

    private void observeViewModel() {
        viewModel.getHeroes().observe(getViewLifecycleOwner(), this::updateHeroList);
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoadingState);
        viewModel.getError().observe(getViewLifecycleOwner(), this::showError);
    }

    private void updateHeroList(List<HeroUiModel> heroes) {
        adapter.submitList(heroes);
        if (heroes != null && !heroes.isEmpty()) {
            binding.statefulLayout.showContent();
        } else if (heroes != null && heroes.isEmpty() && viewModel.getLoading().getValue() == false) {
            binding.statefulLayout.showEmpty();
        }
    }

    private void updateLoadingState(Boolean isLoading) {
        if (isLoading != null) {
            if (isLoading) {
                binding.statefulLayout.showLoading();
            }
        }
    }

    private void showError(String message) {
        binding.statefulLayout.showError(message);
        binding.statefulLayout.setOnRetryListener(() -> viewModel.retry());
        
        if (getView() != null) {
            Snackbar.make(getView(), message != null ? message : "加载失败", Snackbar.LENGTH_LONG)
                    .setAction("重试", v -> viewModel.retry())
                    .show();
        }
    }

    private void navigateToHeroDetail(HeroUiModel hero) {
        if (heroSelectedListener != null) {
            heroSelectedListener.onHeroSelected(hero.getId());
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
        searchEditText = null;
    }
}
