package com.aram.mayhem.feature.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.aram.mayhem.data.local.TokenStore;
import com.aram.mayhem.feature.community.databinding.FragmentPublishStrategyBinding;
import com.aram.mayhem.feature.community.viewmodel.PublishStrategyViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 发布攻略页
 *
 * 功能：攻略发布表单（标题、内容、关联英雄/符文选择）
 * 导航：发布成功 → ReleaseSuccessFragment
 * 关联：PublishStrategyViewModel, ItemListAdapter, AugmentListAdapter
 */
@AndroidEntryPoint
public class PublishStrategyFragment extends Fragment {

    private FragmentPublishStrategyBinding binding;
    private PublishStrategyViewModel viewModel;
    private List<HeroOption> heroOptions = new ArrayList<>();
    private List<AugmentOption> augmentOptions = new ArrayList<>();
    private List<ItemOption> itemOptions = new ArrayList<>();

    @Inject
    TokenStore tokenStore;

    public static PublishStrategyFragment newInstance() {
        return new PublishStrategyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPublishStrategyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!isLoggedIn()) {
            Toast.makeText(requireContext(), R.string.please_login_first, Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        viewModel = new ViewModelProvider(this).get(PublishStrategyViewModel.class);

        setupViews();
        observeViewModel();
    }

    private boolean isLoggedIn() {
        return tokenStore.hasToken() && !tokenStore.isTokenExpired();
    }

    private void navigateToLogin() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigateUp();
    }

    private void setupViews() {
        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        setupHeroSearch();
        setupAugmentSelection();
        setupItemSelection();

        binding.btnPublish.setOnClickListener(v -> {
            String title = binding.editTitle.getText().toString();
            String description = binding.editDescription.getText().toString();

            viewModel.setTitle(title);
            viewModel.setDescription(description);
            viewModel.publish();
        });
    }

    private void setupHeroSearch() {
        ArrayAdapter<String> heroAdapter = new ArrayAdapter<>(requireContext(),
                com.google.android.material.R.layout.support_simple_spinner_dropdown_item);
        binding.autoHero.setAdapter(heroAdapter);
        binding.autoHero.setOnItemClickListener((parent, view, position, id) -> {
            if (position < heroOptions.size()) {
                HeroOption selected = heroOptions.get(position);
                viewModel.setSelectedHeroId(selected.id);
            }
        });
    }

    private void setupAugmentSelection() {
        binding.btnSelectAugments.setOnClickListener(v -> showAugmentPicker());
    }

    private void setupItemSelection() {
        binding.btnSelectItems.setOnClickListener(v -> showItemPicker());
    }

    private void showAugmentPicker() {
        String[] names = new String[augmentOptions.size()];
        boolean[] checked = new boolean[augmentOptions.size()];
        List<Long> selectedIds = viewModel.getSelectedAugmentIds().getValue();

        for (int i = 0; i < augmentOptions.size(); i++) {
            names[i] = augmentOptions.get(i).name;
            if (selectedIds != null) {
                checked[i] = selectedIds.contains(augmentOptions.get(i).id);
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_augments)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        viewModel.addAugment(augmentOptions.get(which).id);
                    } else {
                        viewModel.removeAugment(augmentOptions.get(which).id);
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showItemPicker() {
        String[] names = new String[itemOptions.size()];
        boolean[] checked = new boolean[itemOptions.size()];
        List<Long> selectedIds = viewModel.getSelectedItemIds().getValue();

        for (int i = 0; i < itemOptions.size(); i++) {
            names[i] = itemOptions.get(i).name;
            if (selectedIds != null) {
                checked[i] = selectedIds.contains(itemOptions.get(i).id);
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_items)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        viewModel.addItem(itemOptions.get(which).id);
                    } else {
                        viewModel.removeItem(itemOptions.get(which).id);
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void observeViewModel() {
        viewModel.getPublishing().observe(getViewLifecycleOwner(), publishing -> {
            binding.btnPublish.setEnabled(!publishing);
            binding.progressBar.setVisibility(publishing ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getPublishedStrategy().observe(getViewLifecycleOwner(), strategy -> {
            if (strategy != null) {
                Toast.makeText(requireContext(), R.string.publish_success, Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });

        viewModel.getIsFormValid().observe(getViewLifecycleOwner(), valid -> {
            binding.btnPublish.setEnabled(valid != null && valid);
        });

        viewModel.getSelectedHeroId().observe(getViewLifecycleOwner(), heroId -> {
            if (heroId != null) {
                binding.autoHero.setText(getHeroNameById(heroId), false);
            }
        });

        viewModel.getSelectedAugmentIds().observe(getViewLifecycleOwner(), augmentIds -> {
            updateAugmentChips(augmentIds);
        });

        viewModel.getSelectedItemIds().observe(getViewLifecycleOwner(), itemIds -> {
            updateItemChips(itemIds);
        });
    }

    private void updateAugmentChips(List<Long> augmentIds) {
        binding.chipGroupAugments.removeAllViews();
        if (augmentIds == null || augmentIds.isEmpty()) {
            binding.chipGroupAugments.setVisibility(View.GONE);
            return;
        }
        binding.chipGroupAugments.setVisibility(View.VISIBLE);
        for (Long id : augmentIds) {
            Chip chip = new Chip(requireContext());
            chip.setText(getAugmentNameById(id));
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> viewModel.removeAugment(id));
            binding.chipGroupAugments.addView(chip);
        }
    }

    private void updateItemChips(List<Long> itemIds) {
        binding.chipGroupItems.removeAllViews();
        if (itemIds == null || itemIds.isEmpty()) {
            binding.chipGroupItems.setVisibility(View.GONE);
            return;
        }
        binding.chipGroupItems.setVisibility(View.VISIBLE);
        for (Long id : itemIds) {
            Chip chip = new Chip(requireContext());
            chip.setText(getItemNameById(id));
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> viewModel.removeItem(id));
            binding.chipGroupItems.addView(chip);
        }
    }

    private String getHeroNameById(Long id) {
        for (HeroOption h : heroOptions) {
            if (h.id.equals(id)) return h.name;
        }
        return "英雄 #" + id;
    }

    private String getAugmentNameById(Long id) {
        for (AugmentOption a : augmentOptions) {
            if (a.id.equals(id)) return a.name;
        }
        return "符文 #" + id;
    }

    private String getItemNameById(Long id) {
        for (ItemOption i : itemOptions) {
            if (i.id.equals(id)) return i.name;
        }
        return "装备 #" + id;
    }

    public void setHeroOptions(List<HeroOption> options) {
        this.heroOptions = options != null ? options : new ArrayList<>();
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.autoHero.getAdapter();
        adapter.clear();
        for (HeroOption h : heroOptions) {
            adapter.add(h.name);
        }
        adapter.notifyDataSetChanged();
    }

    public void setAugmentOptions(List<AugmentOption> options) {
        this.augmentOptions = options != null ? options : new ArrayList<>();
    }

    public void setItemOptions(List<ItemOption> options) {
        this.itemOptions = options != null ? options : new ArrayList<>();
    }

    public void setSelectedHeroId(Long heroId) {
        viewModel.setSelectedHeroId(heroId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class HeroOption {
        public final Long id;
        public final String name;

        public HeroOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class AugmentOption {
        public final Long id;
        public final String name;

        public AugmentOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class ItemOption {
        public final Long id;
        public final String name;

        public ItemOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
