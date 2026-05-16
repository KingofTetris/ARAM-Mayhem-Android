package com.aram.mayhem.feature.hero;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aram.mayhem.feature.hero.databinding.FragmentStrongLevelExplainBinding;
import com.aram.mayhem.feature.hero.databinding.ItemTierExplainBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
/**
 * 强度等级说明弹窗
 *
 * 功能：弹窗展示梯级评级（S+/S/A/B/C）的含义说明
 * 触发：英雄列表页筛选栏的"?"图标点击
 */
public class StrongLevelExplainFragment extends Fragment {

    private FragmentStrongLevelExplainBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStrongLevelExplainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        setupTierItems();
    }

    private void setupTierItems() {
        setupTierItem(binding.tierSPlus, "S+", "超模级别", "胜率 ≥ 55%，版本之子", "55%+", 0xFFE91E63);
        setupTierItem(binding.tierS, "S", "强势英雄", "胜率 52%~55%，稳定上分", "52%~55%", 0xFFFF5722);
        setupTierItem(binding.tierA, "A", "均衡英雄", "胜率 49%~52%，表现平稳", "49%~52%", 0xFFFFC107);
        setupTierItem(binding.tierB, "B", "偏弱英雄", "胜率 46%~49%，需要技巧", "46%~49%", 0xFF4CAF50);
        setupTierItem(binding.tierC, "C", "弱势英雄", "胜率 < 46%，谨慎选择", "<46%", 0xFF9E9E9E);
    }

    private void setupTierItem(ItemTierExplainBinding item, String badge, String title, String description, String range, int color) {
        item.textTierBadge.setText(badge);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(8);
        bg.setColor(color);
        item.textTierBadge.setBackground(bg);

        item.textTierTitle.setText(title);
        item.textTierDescription.setText(description);
        item.textTierRange.setText(range);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
