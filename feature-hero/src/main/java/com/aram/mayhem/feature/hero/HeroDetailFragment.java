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
 * 英雄详情页
 *
 * 功能：展示英雄详情（技能、克制、协同、推荐出装、版本陷阱标记）
 * 导航：从 HeroListFragment 点击英雄卡片进入
 * 关联：HeroDetailViewModel, VersionTrapBanner, SkillAdapter
 */
@AndroidEntryPoint
public class HeroDetailFragment extends Fragment {

    private FragmentHeroDetailBinding binding;
    private HeroDetailViewModel viewModel;
    private SkillAdapter skillAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHeroDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupRecyclerView() {
        skillAdapter = new SkillAdapter();
        binding.recyclerSkills.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSkills.setAdapter(skillAdapter);
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoading);
        viewModel.getError().observe(getViewLifecycleOwner(), this::updateError);
        viewModel.getHeroDetail().observe(getViewLifecycleOwner(), this::updateHeroDetail);
    }

    private void updateLoading(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            binding.progressContainer.setVisibility(View.VISIBLE);
            binding.errorContainer.setVisibility(View.GONE);
        } else {
            binding.progressContainer.setVisibility(View.GONE);
        }
    }

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

    private void updateBalanceData(HeroDetailUiModel hero) {
        binding.textWinRate.setText(hero.getWinRateDisplay());
        binding.textPickRate.setText(hero.getPickRateDisplay());
        binding.textKDA.setText(hero.getKdaDisplay());
        binding.textWinRateBar.setText(hero.getWinRateDisplay());
        binding.textPickRateBar.setText(hero.getPickRateDisplay());
        
        binding.balanceBarWinRate.setData("胜率", (float) hero.getWinRate(), 100f);
        binding.balanceBarPickRate.setData("登场率", (float) hero.getPickRate(), 100f);
    }

    private void updateDescription(HeroDetailUiModel hero) {
        if (hero.getDescription() != null && !hero.getDescription().isEmpty()) {
            binding.cardDescription.setVisibility(View.VISIBLE);
            binding.textDescription.setText(hero.getDescription());
        } else {
            binding.cardDescription.setVisibility(View.GONE);
        }
    }

    private void updateSkills(HeroDetailUiModel hero) {
        if (hero.getSkills() != null && !hero.getSkills().isEmpty()) {
            binding.cardSkills.setVisibility(View.VISIBLE);
            skillAdapter.setSkills(hero.getSkills());
        } else {
            binding.cardSkills.setVisibility(View.GONE);
        }
    }

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
     * 更新推荐强化符文展示
     *
     * 作用：将英雄详情中的推荐符文以品质色 Chip 形式展示
     * 实现：优先使用 recommendedAugments（含名称+品质），降级使用 recommendedAugmentIds（仅ID）
     * 品质色：棱彩=紫金渐变、金=金黄、银=银灰
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
            Timber.d("AugmentDisplay: no_augments | heroId=%d | timestamp=%d", hero.getId(), System.currentTimeMillis());
            binding.cardAugments.setVisibility(View.GONE);
        }
    }

    /**
     * 根据符文品质设置 Chip 样式
     *
     * 品质色规则：棱彩=紫金渐变、金=金黄色、银=银灰色
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

    private void updateVersionTrapBanner(HeroDetailUiModel hero) {
        if (hero.isVersionTrap()) {
            binding.versionTrapBanner.setTrapInfo(hero.getTier() != null ? hero.getTier().getLabel() : "当前");
            binding.versionTrapBanner.setVisibility(View.VISIBLE);
        } else {
            binding.versionTrapBanner.hide();
        }
    }

    private void loadHeroImage(HeroDetailUiModel hero) {
        if (hero.getImageUrl() != null && !hero.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(hero.getImageUrl())
                    .centerCrop()
                    .into(binding.imageHeroBanner);
        }
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
