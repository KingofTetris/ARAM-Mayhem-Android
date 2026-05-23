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

/**
 * 强度等级说明页 ── 向用户解释 S+/S/A/B/C 梯级评级的含义
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当用户在英雄列表页看到 S+/S/A/B/C 的梯级标签时，
 * 可能不理解这些字母代表什么。本页面就是用来解释这些评级的含义。
 *
 * 用户点击英雄列表页筛选栏中的"?"图标后，跳转到此页面。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_strong_level_explain.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────────────┐
 *   │  ← 返回    强度等级说明                      │  ← toolbar
 *   ├──────────────────────────────────────────────┤
 *   │  ┌──────┐  S+  超模级别                      │  ← tierSPlus
 *   │  │  S+  │  胜率 ≥ 55%，版本之子   55%+      │
 *   │  └──────┘                                    │
 *   ├──────────────────────────────────────────────┤
 *   │  ┌──────┐  S   强势英雄                      │  ← tierS
 *   │  │  S   │  胜率 52%~55%，稳定上分  52%~55%   │
 *   │  └──────┘                                    │
 *   ├──────────────────────────────────────────────┤
 *   │  ┌──────┐  A   均衡英雄                      │  ← tierA
 *   │  │  A   │  胜率 49%~52%，表现平稳  49%~52%   │
 *   │  └──────┘                                    │
 *   ├──────────────────────────────────────────────┤
 *   │  ┌──────┐  B   偏弱英雄                      │  ← tierB
 *   │  │  B   │  胜率 46%~49%，需要技巧  46%~49%   │
 *   │  └──────┘                                    │
 *   ├──────────────────────────────────────────────┤
 *   │  ┌──────┐  C   弱势英雄                      │  ← tierC
 *   │  │  C   │  胜率 < 46%，谨慎选择    <46%      │
 *   │  └──────┘                                    │
 *   └──────────────────────────────────────────────┘
 *
 * 每行包含 4 个信息：
 * 1. 梯级徽章（S+/S/A/B/C）── 带颜色背景的圆角标签
 * 2. 梯级标题（超模级别/强势英雄/...）── 简短描述
 * 3. 梯级说明（胜率范围 + 特征）── 详细解释
 * 4. 胜率范围（55%+/52%~55%/...）── 数值区间
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、梯级颜色方案
 * ═══════════════════════════════════════════════════════════════════
 *
 *   S+ → 粉红色（0xFFE91E63）── 最强，版本之子
 *   S  → 深橙色（0xFFFF5722）── 强势英雄
 *   A  → 琥珀色（0xFFFFC107）── 均衡英雄
 *   B  → 绿色  （0xFF4CAF50）── 偏弱英雄
 *   C  → 灰色  （0xFF9E9E9E）── 弱势英雄
 *
 * 颜色从暖色（红/橙）到冷色（绿/灰）渐变，
 * 直观传达"从强到弱"的视觉感受。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、导航关系
 * ═══════════════════════════════════════════════════════════════════
 *
 *   HeroListFragment（点击"?"图标）
 *       ↓ Navigation
 *   StrongLevelExplainFragment（本页面）
 *       ↓ 点击返回
 *   HeroListFragment
 *
 * 关联组件：
 * - Tier 枚举：定义梯级标识（S_PLUS/S/A/B/C）
 * - HeroListFragment：触发本页面的入口
 * - TierBadgeView：英雄卡片上显示梯级标签的组件
 */
@AndroidEntryPoint
public class StrongLevelExplainFragment extends Fragment {

    /**
     * ViewBinding 实例 ── 用于访问布局中的所有视图控件
     *
     * 为什么用 ViewBinding 而不是 findViewById？
     * ── ViewBinding 在编译期生成绑定类，类型安全，不会因 ID 拼写导致运行时崩溃
     *
     * 为什么在 onDestroyView 中置 null？
     * ── Fragment 的 View 生命周期比 Fragment 本身短，
     *    Fragment 可能被保留在回退栈中而 View 已被销毁，
     *    置 null 避免访问已销毁的 View 导致内存泄漏
     */
    private FragmentStrongLevelExplainBinding binding;

    /**
     * 创建 Fragment 的视图 ── 加载布局文件并返回根视图
     *
     * Fragment 生命周期第一步：创建视图
     * onCreateView → onViewCreated → onStart → onResume
     *
     * @param inflater  布局填充器，用于将 XML 布局转换为 View 对象
     * @param container 父容器，Fragment 的视图将被添加到此容器中
     * @param savedInstanceState 保存的实例状态（旋转屏幕等场景恢复数据用）
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStrongLevelExplainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化 ── 设置 Toolbar 返回按钮和梯级说明项
     *
     * 为什么在 onViewCreated 而不是 onCreateView 中初始化？
     * ── onCreateView 只负责创建视图，此时视图层次结构可能还未完全构建
     * ── onViewCreated 确保所有视图都已创建完毕，可以安全地操作它们
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        setupTierItems();
    }

    /**
     * 初始化所有梯级说明项 ── 依次设置 S+/S/A/B/C 五个梯级的信息
     *
     * 每个梯级项包含：
     * - 梯级徽章文本（如 "S+"）
     * - 梯级标题（如 "超模级别"）
     * - 梯级说明（如 "胜率 ≥ 55%，版本之子"）
     * - 胜率范围（如 "55%+"）
     * - 徽章背景颜色（如 0xFFE91E63 粉红色）
     */
    private void setupTierItems() {
        setupTierItem(binding.tierSPlus, "S+", "超模级别", "胜率 ≥ 55%，版本之子", "55%+", 0xFFE91E63);
        setupTierItem(binding.tierS, "S", "强势英雄", "胜率 52%~55%，稳定上分", "52%~55%", 0xFFFF5722);
        setupTierItem(binding.tierA, "A", "均衡英雄", "胜率 49%~52%，表现平稳", "49%~52%", 0xFFFFC107);
        setupTierItem(binding.tierB, "B", "偏弱英雄", "胜率 46%~49%，需要技巧", "46%~49%", 0xFF4CAF50);
        setupTierItem(binding.tierC, "C", "弱势英雄", "胜率 < 46%，谨慎选择", "<46%", 0xFF9E9E9E);
    }

    /**
     * 设置单个梯级说明项 ── 将数据绑定到一个梯级行的各个 TextView 上
     *
     * ═══════════════════════════════════════════════════════════
     * GradientDrawable 动态创建圆角背景
     * ═══════════════════════════════════════════════════════════
     *
     * 为什么用 GradientDrawable 而不是 XML drawable？
     * ── XML drawable 需要为每种颜色创建单独的文件（5 个梯级 = 5 个 XML）
     * ── GradientDrawable 可以在代码中动态设置颜色，只需一个方法
     * ── 代码更简洁，颜色值集中管理，方便修改
     *
     * setCornerRadius(8) ── 设置 8dp 的圆角半径
     * setColor(color)     ── 设置纯色填充背景
     *
     * @param item        单个梯级行的 ViewBinding（item_tier_explain.xml）
     * @param badge       梯级徽章文本（如 "S+"、"A"、"C"）
     * @param title       梯级标题（如 "超模级别"、"均衡英雄"）
     * @param description 梯级说明（如 "胜率 ≥ 55%，版本之子"）
     * @param range       胜率范围文本（如 "55%+"、"<46%"）
     * @param color       徽章背景颜色（ARGB 格式的 int 值）
     */
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

    /**
     * Fragment 视图销毁时清理 ── 释放 ViewBinding 引用防止内存泄漏
     *
     * 内存泄漏原理：
     * ── binding 持有 View 的引用 → View 持有 Activity 的 Context
     * ── 如果 Fragment 被销毁但 binding 不置 null，
     *    binding → View → Activity Context → 整个 Activity 无法被 GC 回收
     * ── 置 null 后，GC 可以正常回收 View 和 Context
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
