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
 * 符文推荐页
 *
 * 功能：基于已选符文列表，展示套装进度和智能推荐
 * 导航：从 AugmentDetailBottomSheet 或底部导航进入
 * 关联：AugmentRecommendViewModel, AugmentRecommendAdapter, SynergyProgressAdapter
 */
@AndroidEntryPoint
public class AugmentRecommendFragment extends Fragment {

    private FragmentAugmentRecommendBinding binding;
    private AugmentRecommendViewModel viewModel;
    private AugmentRecommendAdapter adapter;
    private SynergyProgressAdapter progressAdapter;
    private List<HeroUiModel> heroList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAugmentRecommendBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AugmentRecommendViewModel.class);

        setupRecyclerViews();
        setupHeroSpinner();
        observeViewModel();
    }

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

    private void observeViewModel() {
        viewModel.getRecommendations().observe(getViewLifecycleOwner(), this::updateRecommendations);
        viewModel.getSynergyProgress().observe(getViewLifecycleOwner(), this::updateSynergyProgress);
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoading);
    }

    private void updateRecommendations(List<AugmentRecommendResponse> recommendations) {
        if (recommendations != null) {
            adapter.submitList(recommendations);
        } else {
            adapter.submitList(new ArrayList<>());
        }
    }

    private void updateSynergyProgress(List<SynergyProgressResponse> progress) {
        if (progress != null) {
            progressAdapter.submitList(progress);
        }
    }

    private void updateLoading(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            binding.progressLoading.setVisibility(View.VISIBLE);
        } else {
            binding.progressLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}