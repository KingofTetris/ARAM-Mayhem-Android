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
 * 符文卡片列表适配器（核心UI模块）
 *
 * 功能：将 AugmentUiModel 列表数据绑定到 RecyclerView 卡片视图
 * 数据源：AugmentUiModel 列表（来自 Repository 层转换）
 * 布局：item_augment_card.xml（包含图标、名称、套装标签、品质边框）
 * 交互：点击卡片通过 OnAugmentClickListener 回调跳转符文详情页
 * 性能优化：使用 DiffUtil 进行局部刷新，避免全量更新
 */
public class AugmentCardAdapter extends ListAdapter<AugmentUiModel, AugmentCardAdapter.ViewHolder> {

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
     * 构造函数：初始化 DiffUtil 回调
     */
    public AugmentCardAdapter() {
        super(new AugmentDiffCallback());
    }

    /**
     * 设置符文点击事件监听器
     *
     * @param listener 点击回调实现
     */
    public void setOnAugmentClickListener(OnAugmentClickListener listener) {
        this.listener = listener;
    }

    /**
     * 创建 ViewHolder 实例
     *
     * @param parent   RecyclerView 父容器
     * @param viewType 视图类型（本适配器仅支持一种视图类型）
     * @return ViewHolder 实例
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 使用 ViewBinding 方式加载布局，避免 findViewById
        ItemAugmentCardBinding binding = ItemAugmentCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    /**
     * 绑定数据到 ViewHolder
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
     * 视图持有者类：负责单个卡片的视图绑定和事件处理
     */
    class ViewHolder extends RecyclerView.ViewHolder {

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
         * 绑定符文数据到视图
         *
         * @param augment 符文 UI 模型
         */
        void bind(@NonNull AugmentUiModel augment) {
            // 设置符文名称
            binding.textAugmentName.setText(augment.getName());
            // 设置套装显示文本
            binding.textAugmentSynergy.setText(augment.getSynergyDisplay());

            // 根据品质设置卡片边框颜色
            int qualityColor = binding.getRoot().getContext().getColor(augment.getQualityColorRes());
            binding.cardAugment.setStrokeColor(qualityColor);

            // 如果是版本陷阱符文，强制设置红色警告边框
            if (augment.isTrap()) {
                binding.cardAugment.setStrokeColor(
                        binding.cardAugment.getContext().getColor(
                                com.aram.mayhem.ui.R.color.trap_warning));
            }

            // 使用 Glide 加载符文图标（带占位图和错误图）
            Glide.with(binding.imageAugmentIcon.getContext())
                    .load(augment.getIconUrl())
                    .placeholder(com.aram.mayhem.ui.R.drawable.ic_augments)
                    .error(com.aram.mayhem.ui.R.drawable.ic_augments)
                    .into(binding.imageAugmentIcon);

            // 设置点击事件：回调给上层处理跳转
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAugmentClick(augment);
                }
            });
        }
    }

    /**
     * DiffUtil 回调类：用于计算新旧数据差异，实现高效刷新
     */
    static class AugmentDiffCallback extends DiffUtil.ItemCallback<AugmentUiModel> {

        /**
         * 判断是否为同一数据项（通过 ID 比较）
         *
         * @param oldItem 旧数据项
         * @param newItem 新数据项
         * @return 是否为同一数据项
         */
        @Override
        public boolean areItemsTheSame(@NonNull AugmentUiModel oldItem, @NonNull AugmentUiModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /**
         * 判断数据项内容是否发生变化
         *
         * @param oldItem 旧数据项
         * @param newItem 新数据项
         * @return 内容是否相同
         */
        @Override
        public boolean areContentsTheSame(@NonNull AugmentUiModel oldItem, @NonNull AugmentUiModel newItem) {
            // 比较关键字段：ID 和陷阱标记（这两个字段变化需要刷新视图）
            return oldItem.getId() == newItem.getId()
                    && oldItem.isTrap() == newItem.isTrap();
        }
    }
}
