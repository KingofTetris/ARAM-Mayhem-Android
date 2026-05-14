package com.aram.mayhem.feature.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aram.mayhem.feature.community.databinding.FragmentReleaseSuccessBinding;
import com.aram.mayhem.network.dto.StrategyDetailResponse;

public class ReleaseSuccessFragment extends Fragment {

    private FragmentReleaseSuccessBinding binding;
    private StrategyDetailResponse strategy;

    public static ReleaseSuccessFragment newInstance(StrategyDetailResponse strategy) {
        ReleaseSuccessFragment fragment = new ReleaseSuccessFragment();
        Bundle args = new Bundle();
        args.putLong("strategyId", strategy.getId() != null ? strategy.getId() : -1);
        args.putString("title", strategy.getTitle());
        args.putString("heroName", strategy.getHeroName());
        args.putString("description", strategy.getDescription());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            strategy = new StrategyDetailResponse();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReleaseSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String title = getArguments().getString("title", "");
            String heroName = getArguments().getString("heroName", "");
            String description = getArguments().getString("description", "");

            binding.textPreviewTitle.setText(title);
            binding.textPreviewHero.setText(heroName);
            binding.textPreviewDescription.setText(description);
        }

        binding.btnShare.setOnClickListener(v -> shareStrategy());
        binding.btnBackCommunity.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void shareStrategy() {
        String title = getArguments() != null ? getArguments().getString("title", "") : "";
        String heroName = getArguments() != null ? getArguments().getString("heroName", "") : "";
        String description = getArguments() != null ? getArguments().getString("description", "") : "";

        String shareText = "🎮 ARAM Mayhem - 玩法分享\n" +
                "英雄: " + heroName + "\n" +
                "标题: " + title + "\n" +
                "描述: " + description;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_strategy)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
