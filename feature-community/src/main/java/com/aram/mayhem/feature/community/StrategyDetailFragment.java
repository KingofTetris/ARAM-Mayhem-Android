package com.aram.mayhem.feature.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.feature.community.databinding.FragmentStrategyDetailBinding;
import com.aram.mayhem.feature.community.ui.AugmentListAdapter;
import com.aram.mayhem.feature.community.ui.ItemListAdapter;
import com.aram.mayhem.ui.widget.VoteButton;
import com.aram.mayhem.feature.community.viewmodel.StrategyDetailViewModel;
import com.aram.mayhem.network.dto.StrategyDetailResponse;
import com.bumptech.glide.Glide;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 攻略详情页
 *
 * 功能：展示攻略详情（标题、内容、英雄/符文搭配、投票）
 * 导航：从 CommunityFeedFragment 点击攻略进入
 * 关联：StrategyDetailViewModel, VoteButton, ItemListAdapter, AugmentListAdapter
 */
@AndroidEntryPoint
public class StrategyDetailFragment extends Fragment {

    private FragmentStrategyDetailBinding binding;
    private StrategyDetailViewModel viewModel;
    private AugmentListAdapter augmentAdapter;
    private ItemListAdapter itemAdapter;
    private long strategyId = -1;

    public static StrategyDetailFragment newInstance(long strategyId) {
        StrategyDetailFragment fragment = new StrategyDetailFragment();
        Bundle args = new Bundle();
        args.putLong("strategyId", strategyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            strategyId = getArguments().getLong("strategyId", -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStrategyDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StrategyDetailViewModel.class);

        setupRecyclerViews();
        setupVoteButton();
        observeViewModel();

        if (strategyId != -1) {
            viewModel.loadStrategy(strategyId);
        }
    }

    private void setupRecyclerViews() {
        augmentAdapter = new AugmentListAdapter();
        binding.recyclerAugments.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerAugments.setAdapter(augmentAdapter);

        itemAdapter = new ItemListAdapter();
        binding.recyclerItems.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerItems.setAdapter(itemAdapter);
    }

    private void setupVoteButton() {
        binding.voteButton.setOnVoteChangeListener(new VoteButton.OnVoteChangeListener() {
            @Override
            public void onUpvote() {
                viewModel.vote("UP");
            }

            @Override
            public void onDownvote() {
                viewModel.vote("DOWN");
            }

            @Override
            public void onCancelVote() {
                viewModel.cancelVote();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getStrategy().observe(getViewLifecycleOwner(), this::displayStrategy);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getVoteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                Toast.makeText(requireContext(),
                        success ? R.string.vote_success : R.string.vote_failed,
                        Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getCurrentVoteType().observe(getViewLifecycleOwner(), voteType -> {
            StrategyDetailResponse current = viewModel.getStrategy().getValue();
            if (current != null) {
                binding.voteButton.setCurrentVoteType(voteType);
            }
        });
    }

    private void displayStrategy(StrategyDetailResponse strategy) {
        if (strategy == null) return;

        binding.textTitle.setText(strategy.getTitle());
        binding.textDescription.setText(strategy.getDescription());
        binding.textAuthor.setText(strategy.getAuthorNickname());
        binding.textHero.setText(strategy.getHeroName());

        Integer upvotes = strategy.getUpvotes();
        Integer downvotes = strategy.getDownvotes();
        binding.voteButton.setVoteCounts(
                upvotes != null ? upvotes : 0,
                downvotes != null ? downvotes : 0
        );

        String voteType = strategy.getUserVoteType();
        binding.voteButton.setCurrentVoteType(voteType);

        if (strategy.getHeroIcon() != null && !strategy.getHeroIcon().isEmpty()) {
            Glide.with(this)
                    .load(strategy.getHeroIcon())
                    .circleCrop()
                    .into(binding.imageHero);
        }

        if (strategy.getAugments() != null) {
            binding.layoutAugments.setVisibility(strategy.getAugments().isEmpty() ? View.GONE : View.VISIBLE);
            augmentAdapter.setAugments(strategy.getAugments());
        } else {
            binding.layoutAugments.setVisibility(View.GONE);
        }

        if (strategy.getItems() != null) {
            binding.layoutItems.setVisibility(strategy.getItems().isEmpty() ? View.GONE : View.VISIBLE);
            itemAdapter.setItems(strategy.getItems());
        } else {
            binding.layoutItems.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
