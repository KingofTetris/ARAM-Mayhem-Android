package com.aram.mayhem.feature.profile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.profile.R;
import com.aram.mayhem.network.dto.StrategyListResponse;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的攻略列表适配器（个人中心模块）
 *
 * 数据源：StrategyListResponse 列表
 * 布局：item_my_strategy.xml（FrameLayout 结构：底层删除背景 + 前景卡片）
 * 功能：展示攻略标题、英雄名、评分，支持滑动删除和按钮删除
 */
public class MyStrategiesAdapter extends RecyclerView.Adapter<MyStrategiesAdapter.ViewHolder> {

    /** 攻略列表数据 */
    private List<StrategyListResponse> strategies = new ArrayList<>();
    /** 删除按钮点击监听器 */
    private OnDeleteClickListener deleteListener;

    /**
     * 删除按钮点击监听器接口
     */
    public interface OnDeleteClickListener {
        void onDeleteClick(StrategyListResponse strategy, int position);
    }

    /**
     * 设置删除按钮点击监听器
     *
     * @param listener 删除回调
     */
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    /**
     * 更新攻略列表数据
     *
     * @param strategies 新的攻略列表
     */
    public void setStrategies(List<StrategyListResponse> strategies) {
        this.strategies = strategies != null ? strategies : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 获取指定位置的攻略数据
     *
     * @param position 列表位置
     * @return 对应的攻略数据，越界返回 null
     */
    public StrategyListResponse getItem(int position) {
        if (position >= 0 && position < strategies.size()) {
            return strategies.get(position);
        }
        return null;
    }

    /**
     * 移除指定位置的 item 并通知 UI 更新
     *
     * @param position 要移除的位置
     */
    public void removeItem(int position) {
        if (position >= 0 && position < strategies.size()) {
            strategies.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_strategy, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StrategyListResponse strategy = strategies.get(position);

        holder.textTitle.setText(strategy.getTitle());
        holder.textHero.setText(strategy.getHeroName());
        holder.textScore.setText(String.format("👍 %d  👎 %d", strategy.getUpvotes(), strategy.getDownvotes()));

        // 删除图标按钮点击回调
        holder.imageDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(strategy, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return strategies.size();
    }

    /**
     * ViewHolder（持有前景卡片和删除背景的引用）
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        /** 前景卡片，ItemTouchHelper 滑动时移动此视图 */
        public MaterialCardView cardForeground;
        /** 攻略标题 */
        TextView textTitle;
        /** 英雄名称 */
        TextView textHero;
        /** 投票评分 */
        TextView textScore;
        /** 删除图标按钮 */
        ImageView imageDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardForeground = itemView.findViewById(R.id.card_foreground);
            textTitle = itemView.findViewById(R.id.text_strategy_title);
            textHero = itemView.findViewById(R.id.text_strategy_hero);
            textScore = itemView.findViewById(R.id.text_strategy_score);
            imageDelete = itemView.findViewById(R.id.image_delete);
        }
    }
}
