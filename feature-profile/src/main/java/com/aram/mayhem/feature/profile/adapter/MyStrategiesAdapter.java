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

import java.util.ArrayList;
import java.util.List;

/**
 * 我的攻略列表适配器（个人中心模块）
 *
 * 数据源：StrategyListResponse 列表
 * 布局：item_my_strategy.xml
 * 功能：展示攻略标题、英雄名、评分，支持删除操作
 */
public class MyStrategiesAdapter extends RecyclerView.Adapter<MyStrategiesAdapter.ViewHolder> {

    private List<StrategyListResponse> strategies = new ArrayList<>();
    private OnDeleteClickListener deleteListener;

    /**
     * 删除按钮点击监听器
     */
    public interface OnDeleteClickListener {
        void onDeleteClick(StrategyListResponse strategy, int position);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public void setStrategies(List<StrategyListResponse> strategies) {
        this.strategies = strategies != null ? strategies : new ArrayList<>();
        notifyDataSetChanged();
    }

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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textHero;
        TextView textScore;
        ImageView imageDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_strategy_title);
            textHero = itemView.findViewById(R.id.text_strategy_hero);
            textScore = itemView.findViewById(R.id.text_strategy_score);
            imageDelete = itemView.findViewById(R.id.image_delete);
        }
    }
}
