package com.aram.mayhem.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.ui.model.HeroUiModel;
import com.aram.mayhem.ui.databinding.ItemHeroCardBinding;
import com.bumptech.glide.Glide;

/**
 * 英雄卡片列表适配器（核心UI模块）
 *
 * 功能：将 HeroUiModel 列表数据绑定到 RecyclerView 卡片视图
 * 数据源：HeroUiModel 列表（来自 Repository 层转换）
 * 布局：item_hero_card.xml（包含头像、名称、定位、胜率、梯级标签）
 * 交互：点击卡片通过 OnHeroClickListener 回调跳转英雄详情页
 * 性能优化：使用 DiffUtil 进行局部刷新
 */
public class HeroCardAdapter extends ListAdapter<HeroUiModel, HeroCardAdapter.ViewHolder> {

    private OnHeroClickListener listener;

    /**
     * 英雄点击事件监听器接口
     *
     * @param hero 被点击的英雄数据模型
     */
    public interface OnHeroClickListener {
        void onHeroClick(@NonNull HeroUiModel hero);
    }

    /**
     * 构造函数：初始化 DiffUtil 回调
     */
    public HeroCardAdapter() {
        super(new HeroDiffCallback());
    }

    /**
     * 设置英雄点击事件监听器
     *
     * @param listener 点击回调实现
     */
    public void setOnHeroClickListener(OnHeroClickListener listener) {
        this.listener = listener;
    }

    /**
     * 创建 ViewHolder 实例
     *
     * @param parent   RecyclerView 父容器
     * @param viewType 视图类型
     * @return ViewHolder 实例
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHeroCardBinding binding = ItemHeroCardBinding.inflate(
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
        HeroUiModel hero = getItem(position);
        holder.bind(hero);
    }

    /**
     * 视图持有者类：负责单个英雄卡片的视图绑定和事件处理
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemHeroCardBinding binding;

        /**
         * ViewHolder 构造函数
         *
         * @param binding 布局绑定对象
         */
        ViewHolder(@NonNull ItemHeroCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定英雄数据到视图
         *
         * @param hero 英雄 UI 模型
         */
        void bind(@NonNull HeroUiModel hero) {
            // 设置英雄名称（中文）
            binding.textHeroName.setText(hero.getNameZh());
            // 设置英雄定位（战士/法师/刺客等）
            binding.textHeroRole.setText(hero.getRole());
            // 设置胜率显示文本
            binding.textWinRate.setText(hero.getWinRateDisplay());
            // 设置梯级标签（S+/S/A/B/C）
            binding.badgeTier.setTier(hero.getTier());

            // 使用 Glide 加载英雄头像（圆形裁剪）
            Glide.with(binding.imageHeroAvatar.getContext())
                    .load(hero.getAvatarUrl())
                    .placeholder(com.aram.mayhem.ui.R.drawable.ic_heroes)
                    .error(com.aram.mayhem.ui.R.drawable.ic_heroes)
                    .circleCrop()
                    .into(binding.imageHeroAvatar);

            // 设置版本陷阱标记：如果是陷阱英雄，显示红色边框警告
            if (hero.isTrap()) {
                binding.getRoot().setStrokeColor(
                        binding.getRoot().getContext().getColor(
                                com.aram.mayhem.ui.R.color.trap_warning));
            } else {
                // 非陷阱英雄：移除边框
                binding.getRoot().setStrokeColor(0);
            }

            // 设置点击事件：回调给上层处理跳转
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHeroClick(hero);
                }
            });
        }
    }

    /**
     * DiffUtil 回调类：用于计算新旧数据差异
     */
    static class HeroDiffCallback extends DiffUtil.ItemCallback<HeroUiModel> {

        /**
         * 判断是否为同一数据项（通过 ID 比较）
         *
         * @param oldItem 旧数据项
         * @param newItem 新数据项
         * @return 是否为同一数据项
         */
        @Override
        public boolean areItemsTheSame(@NonNull HeroUiModel oldItem, @NonNull HeroUiModel newItem) {
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
        public boolean areContentsTheSame(@NonNull HeroUiModel oldItem, @NonNull HeroUiModel newItem) {
            // 比较关键字段：ID、胜率、梯级、陷阱标记
            return oldItem.getId() == newItem.getId()
                    && oldItem.getWinRate() == newItem.getWinRate()
                    && oldItem.getTier() == newItem.getTier()
                    && oldItem.isTrap() == newItem.isTrap();
        }
    }
}
