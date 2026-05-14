package com.aram.mayhem.feature.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.community.databinding.FragmentCommunityFeedBinding;
import com.aram.mayhem.feature.community.ui.StrategyCardAdapter;
import com.aram.mayhem.feature.community.viewmodel.StrategyFeedViewModel;
import com.aram.mayhem.network.dto.StrategyListResponse;
import com.aram.mayhem.ui.widget.PaginationScrollListener;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CommunityFeedFragment extends Fragment implements StrategyCardAdapter.OnStrategyClickListener {

    private FragmentCommunityFeedBinding binding;
    private StrategyFeedViewModel viewModel;
    private StrategyCardAdapter adapter;

    public interface OnStrategyClickListener {
        void onStrategyClick(long strategyId);
    }

    @Nullable
    private OnStrategyClickListener strategyClickListener;

    public void setOnStrategyClickListener(@Nullable OnStrategyClickListener listener) {
        this.strategyClickListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable android.os.Bundle savedInstanceState) {
        binding = FragmentCommunityFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StrategyFeedViewModel.class);

        setupRecyclerView();
        setupSortToggle();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new StrategyCardAdapter();
        adapter.setOnStrategyClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerStrategies.setLayoutManager(layoutManager);
        binding.recyclerStrategies.setAdapter(adapter);

        binding.recyclerStrategies.addOnScrollListener(new PaginationScrollListener(layoutManager, 10) {
            @Override
            public boolean isLoading() {
                Boolean loading = viewModel.getLoadingMore().getValue();
                return loading != null && loading;
            }

            @Override
            public boolean isLastPage() {
                Boolean lastPage = viewModel.getIsLastPage().getValue();
                return lastPage != null && lastPage;
            }

            @Override
            public void onLoadMore() {
                viewModel.loadMore();
            }
        });

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.retry());
    }

    private void setupSortToggle() {
        binding.toggleSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == com.aram.mayhem.feature.community.R.id.btn_hot) {
                viewModel.setSort("hot");
            } else if (checkedId == com.aram.mayhem.feature.community.R.id.btn_latest) {
                viewModel.setSort("latest");
            }
        });
    }

    private void observeViewModel() {
        viewModel.getStrategies().observe(getViewLifecycleOwner(), strategies -> {
            adapter.setStrategies(strategies);
            binding.layoutEmpty.setVisibility(strategies.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.swipeRefresh.setRefreshing(loading);
        });

        viewModel.getLoadingMore().observe(getViewLifecycleOwner(), loadingMore -> {
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStrategyClick(StrategyListResponse strategy) {
        if (strategyClickListener != null) {
            strategyClickListener.onStrategyClick(strategy.getId() != null ? strategy.getId() : -1);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}