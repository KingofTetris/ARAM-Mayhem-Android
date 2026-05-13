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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AugmentDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_AUGMENT_ID = "augment_id";

    private BottomSheetAugmentDetailBinding binding;
    private AugmentDetailViewModel viewModel;

    public static AugmentDetailBottomSheet newInstance(long augmentId) {
        AugmentDetailBottomSheet fragment = new AugmentDetailBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_AUGMENT_ID, augmentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AugmentDetailViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAugmentDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            long augmentId = getArguments().getLong(ARG_AUGMENT_ID);
            viewModel.loadAugmentDetail(augmentId);
        }

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getAugmentDetail().observe(getViewLifecycleOwner(), this::displayAugment);
    }

    private void displayAugment(AugmentUiModel augment) {
        if (augment == null) return;

        binding.textAugmentName.setText(augment.getNameZh());
        binding.textAugmentQuality.setText(augment.getQualityDisplay());
        binding.textDescription.setText(augment.getDescription());
        binding.textWinRate.setText(augment.getWinRateDisplay());
        binding.textPickRate.setText(augment.getPickRateDisplay());
        binding.textAvgPlacement.setText(augment.getAvgPlacementDisplay());

        if (augment.isTrap()) {
            binding.textTrapWarning.setVisibility(View.VISIBLE);
        } else {
            binding.textTrapWarning.setVisibility(View.GONE);
        }

        binding.chipGroupSynergy.removeAllViews();
        addSynergyChip(augment.getSynergySet());
        addSynergyChip(augment.getSynergySet2());
        addSynergyChip(augment.getSynergySet3());
    }

    private void addSynergyChip(String synergy) {
        if (synergy == null || synergy.isEmpty()) return;

        Chip chip = new Chip(requireContext());
        chip.setText(synergy);
        chip.setClickable(false);
        binding.chipGroupSynergy.addView(chip);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}