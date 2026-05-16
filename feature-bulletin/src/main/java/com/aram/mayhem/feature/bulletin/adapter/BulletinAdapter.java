package com.aram.mayhem.feature.bulletin.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.bulletin.databinding.ItemBulletinBinding;
import com.aram.mayhem.ui.model.BulletinUiModel;
import com.bumptech.glide.Glide;

/**
 * 公告列表适配器（公告模块）
 *
 * 数据源：BulletinUiModel 列表
 * 布局：item_bulletin.xml
 * 用途：BulletinListFragment 中的公告列表展示
 * 特性：使用 DiffUtil 实现高效列表更新，支持点击回调
 */
public class BulletinAdapter extends ListAdapter<BulletinUiModel, BulletinAdapter.BulletinViewHolder> {

    /** 公告点击监听器 */
    private OnBulletinClickListener listener;

    /**
     * 公告点击回调接口
     */
    public interface OnBulletinClickListener {
        /**
         * 公告点击事件
         *
         * @param bulletin 被点击的公告数据
         */
        void onBulletinClick(BulletinUiModel bulletin);
    }

    /**
     * 构造函数
     */
    public BulletinAdapter() {
        super(new BulletinDiffCallback());
    }

    /**
     * 设置公告点击监听器
     *
     * @param listener 点击监听器
     */
    public void setOnBulletinClickListener(OnBulletinClickListener listener) {
        this.listener = listener;
    }

    /**
     * 创建 ViewHolder
     *
     * @param parent 父容器
     * @param viewType 视图类型
     * @return BulletinViewHolder
     */
    @NonNull
    @Override
    public BulletinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBulletinBinding binding = ItemBulletinBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BulletinViewHolder(binding);
    }

    /**
     * 绑定数据到 ViewHolder
     *
     * @param holder ViewHolder
     * @param position 位置
     */
    @Override
    public void onBindViewHolder(@NonNull BulletinViewHolder holder, int position) {
        BulletinUiModel bulletin = getItem(position);
        holder.bind(bulletin);
    }

    /**
     * 公告列表项 ViewHolder
     */
    class BulletinViewHolder extends RecyclerView.ViewHolder {

        /** 视图绑定对象 */
        private final ItemBulletinBinding binding;

        /**
         * 构造函数
         *
         * @param binding 视图绑定对象
         */
        BulletinViewHolder(ItemBulletinBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定公告数据到视图
         *
         * @param bulletin 公告数据
         */
        void bind(BulletinUiModel bulletin) {
            // 设置标题
            binding.textTitle.setText(bulletin.getTitle());
            // 设置类型标签
            binding.textTypeBadge.setText(bulletin.getTypeDisplay());
            // 设置日期
            binding.textDate.setText(bulletin.getCreatedAt());

            // 设置置顶标签可见性
            if (bulletin.isPinned()) {
                binding.textPinnedBadge.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.textPinnedBadge.setVisibility(android.view.View.GONE);
            }

            // 设置封面图片
            if (bulletin.getImageUrl() != null && !bulletin.getImageUrl().isEmpty()) {
                Glide.with(binding.imageBulletin.getContext())
                        .load(bulletin.getImageUrl())
                        .centerCrop()
                        .into(binding.imageBulletin);
            }

            // 设置点击事件
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBulletinClick(bulletin);
                }
            });
        }
    }

    /**
     * DiffUtil 回调类，用于高效计算列表差异
     */
    static class BulletinDiffCallback extends DiffUtil.ItemCallback<BulletinUiModel> {
        /**
         * 判断两个 item 是否为同一个对象
         *
         * @param oldItem 旧数据
         * @param newItem 新数据
         * @return 是否为同一对象
         */
        @Override
        public boolean areItemsTheSame(@NonNull BulletinUiModel oldItem, @NonNull BulletinUiModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /**
         * 判断两个 item 的内容是否相同
         *
         * @param oldItem 旧数据
         * @param newItem 新数据
         * @return 内容是否相同
         */
        @Override
        public boolean areContentsTheSame(@NonNull BulletinUiModel oldItem, @NonNull BulletinUiModel newItem) {
            return oldItem.getId() == newItem.getId()
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.isPinned() == newItem.isPinned();
        }
    }
}
