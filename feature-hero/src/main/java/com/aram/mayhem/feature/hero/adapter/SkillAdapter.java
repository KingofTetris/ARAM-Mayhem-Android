package com.aram.mayhem.feature.hero.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.hero.databinding.ItemSkillBinding;
import com.aram.mayhem.ui.model.HeroDetailUiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 英雄技能列表适配器 ── 在详情页展示英雄的 5 个技能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个适配器是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * SkillAdapter 负责把英雄的技能数据（SkillUiModel 列表）渲染成
 * RecyclerView 中的技能卡片。每个英雄有 5 个技能：
 * - 被动技能（P）：英雄的固有特性
 * - Q 技能：第一个主动技能
 * - W 技能：第二个主动技能
 * - E 技能：第三个主动技能
 * - R 技能（大招）：终极技能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、在界面上的样子
 * ═══════════════════════════════════════════════════════════════════
 *
 * 英雄详情页的技能区域：
 *
 *   ┌─────────────────────────────────────────┐
 *   │  [P]  被动技能名称                        │
 *   │       被动技能描述文字...                  │
 *   ├─────────────────────────────────────────┤
 *   │  [Q]  Q技能名称                          │
 *   │       Q技能描述文字...                    │
 *   ├─────────────────────────────────────────┤
 *   │  [W]  W技能名称                          │
 *   │       W技能描述文字...                    │
 *   ├─────────────────────────────────────────┤
 *   │  [E]  E技能名称                          │
 *   │       E技能描述文字...                    │
 *   ├─────────────────────────────────────────┤
 *   │  [R]  大招名称                           │
 *   │       大招描述文字...                     │
 *   └─────────────────────────────────────────┘
 *
 * 每个技能卡片包含 3 个元素：
 * - textSkillKey：技能按键标识（P/Q/W/E/R），大写显示
 * - textSkillName：技能名称（如"蘑菇陷阱"）
 * - textSkillDescription：技能描述（详细效果说明）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与 HeroCardAdapter 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * | 特性        | HeroCardAdapter          | SkillAdapter              |
 * |------------|--------------------------|--------------------------|
 * | 用途        | 英雄列表卡片              | 英雄详情页技能列表         |
 * | 数据类型    | HeroUiModel              | SkillUiModel             |
 * | 列表长度    | 可变（几十个英雄）         | 固定 5 个技能             |
 * | 更新策略    | DiffUtil（高效增量更新）   | notifyDataSetChanged（全量）|
 * | 点击事件    | 有（跳转详情页）           | 无（纯展示）              |
 * | 分页加载    | 有（PaginationScrollListener）| 无（一次加载完）       |
 *
 * 为什么 SkillAdapter 不用 DiffUtil？
 * - 技能列表固定 5 项，全量刷新的开销可以忽略
 * - DiffUtil 的计算成本（O(N) 比对）反而比全量刷新更高
 * - 简单场景用 notifyDataSetChanged 更直观
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   HeroDetailViewModel
 *        │
 *        │ heroDetail.skills（List<SkillUiModel>）
 *        ↓
 *   HeroDetailFragment
 *        │
 *        │ skillAdapter.setSkills(skills)
 *        ↓
 *   SkillAdapter
 *        │
 *        │ onBindViewHolder → holder.bind(skill)
 *        ↓
 *   ItemSkillBinding（item_skill.xml 布局）
 */
public class SkillAdapter extends RecyclerView.Adapter<SkillAdapter.SkillViewHolder> {

    /**
     * 技能数据列表
     *
     * 存储 HeroDetailUiModel.SkillUiModel 对象列表。
     * 每次调用 setSkills() 时替换整个列表。
     *
     * 初始值为空列表（不是 null），避免 getItemCount() 返回 null 导致崩溃。
     */
    private List<HeroDetailUiModel.SkillUiModel> skills = new ArrayList<>();

    /**
     * 设置技能数据并刷新列表 ── 外部调用入口
     *
     * 由 HeroDetailFragment 在观察到 heroDetail 数据变化时调用。
     *
     * 执行流程：
     * 1. 替换 skills 列表（如果传入 null 则设为空列表）
     * 2. 调用 notifyDataSetChanged() 通知 RecyclerView 重新绑定所有项
     *
     * 为什么不检查列表是否相同再决定是否刷新？
     * - 技能列表只在英雄详情加载时设置一次，不存在频繁调用的问题
     * - 简单场景不需要过度优化
     *
     * @param skills 技能 UI 模型列表，通常包含 5 个技能（P/Q/W/E/R）
     */
    public void setSkills(List<HeroDetailUiModel.SkillUiModel> skills) {
        this.skills = skills != null ? skills : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 创建 ViewHolder ── RecyclerView 调用，创建技能卡片视图持有者
     *
     * 当 RecyclerView 需要新的 ViewHolder 来显示一个技能卡片时调用。
     * 通常只在列表首次显示时调用，后续滚动时复用已有 ViewHolder。
     *
     * 执行步骤：
     * 1. 使用 ItemSkillBinding.inflate() 加载 item_skill.xml 布局
     * 2. 用 binding 对象创建 SkillViewHolder
     *
     * ViewBinding 的工作原理：
     * - inflate() 内部调用 LayoutInflater.inflate() 加载 XML
     * - 返回的 binding 对象包含对 XML 中所有带 id 视图的引用
     * - 如 binding.textSkillKey 对应 XML 中的 @+id/text_skill_key
     *
     * @param parent   父视图组（RecyclerView）
     * @param viewType 视图类型（本适配器只有一种类型，忽略此参数）
     * @return 新创建的 SkillViewHolder 实例
     */
    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSkillBinding binding = ItemSkillBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SkillViewHolder(binding);
    }

    /**
     * 绑定数据到 ViewHolder ── 将技能数据"贴"到视图上
     *
     * 当 RecyclerView 需要显示某个位置的技能时调用。
     * 从 skills 列表中取出对应位置的 SkillUiModel，传给 ViewHolder 的 bind 方法。
     *
     * @param holder   要绑定的 ViewHolder
     * @param position 列表中的位置索引（0=被动, 1=Q, 2=W, 3=E, 4=R）
     */
    @Override
    public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
        HeroDetailUiModel.SkillUiModel skill = skills.get(position);
        holder.bind(skill);
    }

    /**
     * 获取列表项数量
     *
     * RecyclerView 调用此方法确定需要显示多少个技能卡片。
     * 通常返回 5（P/Q/W/E/R），但如果没有技能数据则返回 0。
     *
     * @return 技能数量
     */
    @Override
    public int getItemCount() {
        return skills.size();
    }

    /**
     * 技能视图持有者 ── 持有单个技能卡片的所有视图引用
     *
     * ViewHolder 模式的作用：
     * - 避免每次绑定数据时都调用 findViewById（耗时操作）
     * - 在构造时一次性获取所有子视图的引用
     * - RecyclerView 滑动时复用 ViewHolder，只更新数据不重新创建视图
     *
     * 为什么用 static 内部类？
     * - static 内部类不持有外部类的隐式引用
     * - 避免内存泄漏（如果非 static，会持有 SkillAdapter 的引用，
     *   而 SkillAdapter 可能持有 Activity 的引用）
     */
    static class SkillViewHolder extends RecyclerView.ViewHolder {

        /**
         * ViewBinding 对象 ── 包含技能卡片中所有视图的引用
         *
         * 通过 binding.textSkillKey、binding.textSkillName 等方式
         * 直接访问 XML 布局中定义的视图，无需 findViewById。
         */
        private final ItemSkillBinding binding;

        /**
         * 构造 ViewHolder
         *
         * @param binding 技能卡片的 ViewBinding 对象
         */
        SkillViewHolder(ItemSkillBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定技能数据到视图 ── 核心方法，将数据"贴"到 UI 上
         *
         * 执行步骤：
         * 1. 设置技能按键标识（P/Q/W/E/R），转大写显示
         * 2. 设置技能名称（如"蘑菇陷阱"）
         * 3. 设置技能描述（详细效果说明）
         *
         * toUpperCase() 的作用：
         * - 后端返回的 key 可能是 "p"/"q"/"w"/"e"/"r"（小写）
         * - 转大写后显示为 "P"/"Q"/"W"/"E"/"R"，更醒目
         * - 与技能图标的视觉风格一致
         *
         * @param skill 技能 UI 模型，包含 key、name、description 三个字段
         */
        void bind(HeroDetailUiModel.SkillUiModel skill) {
            binding.textSkillKey.setText(skill.getKey().toUpperCase());
            binding.textSkillName.setText(skill.getName());
            binding.textSkillDescription.setText(skill.getDescription());
        }
    }
}
