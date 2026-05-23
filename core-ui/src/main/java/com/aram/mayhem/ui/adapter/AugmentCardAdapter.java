package com.aram.mayhem.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.ui.model.AugmentUiModel;
import com.aram.mayhem.ui.databinding.ItemAugmentCardBinding;
import com.bumptech.glide.Glide;

/**
 * 符文卡片列表适配器 ── 将 AugmentUiModel 数据绑定到 RecyclerView 卡片视图
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentCardAdapter 与 HeroCardAdapter 结构完全相同，只是数据类型不同：
 * - HeroCardAdapter：处理 HeroUiModel → 英雄卡片
 * - AugmentCardAdapter：处理 AugmentUiModel → 符文卡片
 *
 * 两者的核心逻辑一致：创建视图 → 绑定数据 → DiffUtil 刷新
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、符文卡片 vs 英雄卡片的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * | 特性       | 英雄卡片                    | 符文卡片                        |
 * |-----------|-----------------------------|--------------------------------|
 * | 图片       | 圆形头像（circleCrop）        | 方形图标（无裁剪）               |
 * | 品质边框   | 无品质概念                   | 根据品质着色（紫/金/灰）         |
 * | 陷阱标记   | 红色卡片边框                  | 红色卡片边框（覆盖品质色）       |
 * | 副标题     | 英雄定位（战士/法师）          | 套装名称（套装：暴击）           |
 * | 梯级标签   | TierBadgeView（S+/S/A/B/C）  | 无梯级标签                      |
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、品质边框优先级
 * ═══════════════════════════════════════════════════════════════════
 *
 * 符文卡片有两种边框颜色逻辑，优先级从高到低：
 * 1. 版本陷阱（isTrap = true）→ 红色警告边框（最高优先级）
 * 2. 品质颜色（quality）→ 紫色/金色/灰色边框
 *
 * 代码中的处理顺序：
 * - 先设置品质边框颜色
 * - 再检查 isTrap，如果是陷阱则覆盖为红色
 * - 这样确保陷阱警告始终可见
 */
public class AugmentCardAdapter extends ListAdapter<AugmentUiModel, AugmentCardAdapter.ViewHolder> {

    /**
     * 符文点击事件监听器 ── Fragment 实现此接口处理点击跳转
     *
     * 使用方式：
     * <pre>
     * adapter.setOnAugmentClickListener(augment -> {
     *     // 跳转到符文详情页
     *     NavHostFragment.findNavController(this)
     *         .navigate(AugmentListFragmentDirections.actionToDetail(augment.getId()));
     * });
     * </pre>
     */
    private OnAugmentClickListener listener;

    /**
     * 符文点击事件监听器接口
     *
     * @param augment 被点击的符文数据模型
     */
    public interface OnAugmentClickListener {
        void onAugmentClick(@NonNull AugmentUiModel augment);
    }

    /**
     * 构造函数 ── 初始化 DiffUtil 回调
     *
     * super(new AugmentDiffCallback()) 传入符文专用的差异计算规则
     */
    public AugmentCardAdapter() {
        super(new AugmentDiffCallback());
    }

    /**
     * 设置符文点击事件监听器
     *
     * @param listener 点击回调实现（通常是 Fragment）
     */
    public void setOnAugmentClickListener(OnAugmentClickListener listener) {
        this.listener = listener;
    }

    /**
     * 创建 ViewHolder 实例 ── 当 RecyclerView 需要新的符文卡片视图时调用
     *
     * @param parent   RecyclerView 父容器
     * @param viewType 视图类型（本适配器仅支持一种视图类型）
     * @return ViewHolder 实例
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用 ViewBinding 方式加载布局，避免 findViewById
        // ItemAugmentCardBinding 对应 item_augment_card.xml 布局文件
        ItemAugmentCardBinding binding = ItemAugmentCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    /**
     * 绑定数据到 ViewHolder ── 将 AugmentUiModel 的数据显示到符文卡片上
     *
     * @param holder   视图持有者
     * @param position 当前数据项位置
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AugmentUiModel augment = getItem(position);
        holder.bind(augment);
    }

    /**
     * 视图持有者类 ── 持有单个符文卡片的所有视图引用
     *
     * 与 HeroCardAdapter.ViewHolder 结构类似，但绑定的数据字段不同：
     * - 英雄卡片：名称 + 定位 + 胜率 + 梯级标签 + 圆形头像
     * - 符文卡片：名称 + 套装 + 品质边框 + 方形图标
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        /** 布局绑定对象 ── 包含符文卡片中所有视图的引用 */
        private final ItemAugmentCardBinding binding;

        /**
         * ViewHolder 构造函数
         *
         * @param binding 布局绑定对象
         */
        ViewHolder(@NonNull ItemAugmentCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定符文数据到视图 ── 核心方法，将数据"贴"到 UI 上
         *
         * 执行步骤：
         * 1. 设置符文名称和套装信息
         * 2. 设置品质边框颜色
         * 3. 检查版本陷阱标记（覆盖品质色）
         * 4. 加载符文图标
         * 5. 设置点击事件
         *
         * @param augment 符文 UI 模型
         */
        void bind(@NonNull AugmentUiModel augment) {
            // 步骤 1：设置符文名称（中文）
            // getName() 返回中文名称（getNameZh() 的别名）
            binding.textAugmentName.setText(augment.getName());

            // 设置套装显示文本（如"套装：暴击" 或 空字符串）
            binding.textAugmentSynergy.setText(augment.getSynergyDisplay());

            // 步骤 2：根据品质设置卡片边框颜色
            // getQualityColorRes() 返回颜色资源 ID（如 R.color.quality_prismatic）
            // context.getColor() 将资源 ID 转为实际颜色值
            int qualityColor = binding.getRoot().getContext().getColor(augment.getQualityColorRes());
            // cardAugment 是 MaterialCardView，setStrokeColor 设置边框颜色
            binding.cardAugment.setStrokeColor(qualityColor);

            // 步骤 3：如果是版本陷阱符文，强制设置红色警告边框
            // 这一步在品质颜色之后执行，确保陷阱警告覆盖品质色
            if (augment.isTrap()) {
                binding.cardAugment.setStrokeColor(
                        binding.cardAugment.getContext().getColor(
                                com.aram.mayhem.ui.R.color.trap_warning));
            }

            // 步骤 4：使用 Glide 加载符文图标
            // 与英雄头像的区别：
            // - 英雄头像用 circleCrop()（圆形裁剪）
            // - 符文图标不用 circleCrop()（保持方形）
            // - 占位图和错误图使用 ic_augments（符文专用默认图标）
            Glide.with(binding.imageAugmentIcon.getContext())
                    .load(augment.getIconUrl())
                    .placeholder(com.aram.mayhem.ui.R.drawable.ic_augments)
                    .error(com.aram.mayhem.ui.R.drawable.ic_augments)
                    .into(binding.imageAugmentIcon);

            // 步骤 5：设置点击事件
            // 点击整个卡片时，回调给上层（Fragment）处理跳转
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAugmentClick(augment);
                }
            });
        }
    }

    /**
     * DiffUtil 回调类 ── 定义符文数据的差异计算规则
     *
     * 与 HeroDiffCallback 的区别：
     * - HeroDiffCallback 比较 id + winRate + tier + isTrap
     * - AugmentDiffCallback 只比较 id + isTrap
     *
     * 为什么符文只比较两个字段？
     * - 符文的 winRate/pickRate 变化不影响列表卡片显示
     *   （卡片上不显示胜率数值，只在详情页显示）
     * - 符文的 quality/synergySet 不会变化
     * - isTrap 是唯一可能变化的字段（版本更新时标记陷阱）
     */
    static class AugmentDiffCallback extends DiffUtil.ItemCallback<AugmentUiModel> {

        /**
         * 判断是否为同一数据项 ── 通过 ID 比较
         *
         * @param oldItem 旧列表中的数据项
         * @param newItem 新列表中的数据项
         * @return true 表示是同一个符文（ID 相同）
         */
        @Override
        public boolean areItemsTheSame(@NonNull AugmentUiModel oldItem, @NonNull AugmentUiModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /**
         * 判断数据项内容是否发生变化 ── 只比较可能变化的字段
         *
         * 比较字段：
         * - id：标识（保险起见也比较）
         * - isTrap：陷阱标记可能随版本更新变化
         *
         * 不比较的字段（不会变化或不影响卡片显示）：
         * - nameZh/nameEn/description：不会变
         * - quality/synergySet：不会变
         * - winRate/pickRate/avgPlacement：卡片上不显示，不需要刷新
         * - tier：卡片上不显示梯级标签
         * - iconUrl：不会变
         *
         * @param oldItem 旧列表中的数据项
         * @param newItem 新列表中的数据项
         * @return true 表示内容相同，不需要刷新视图
         */
        @Override
        public boolean areContentsTheSame(@NonNull AugmentUiModel oldItem, @NonNull AugmentUiModel newItem) {
            return oldItem.getId() == newItem.getId()
                    && oldItem.isTrap() == newItem.isTrap();
        }
    }
}
