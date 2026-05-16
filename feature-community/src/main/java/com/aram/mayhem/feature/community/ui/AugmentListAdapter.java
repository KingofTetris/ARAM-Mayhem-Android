package com.aram.mayhem.feature.community.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.community.R;
import com.aram.mayhem.network.dto.StrategyDetailResponse;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * 攻略关联符文列表适配器
 *
 * 数据源：符文名称/ID 列表
 * 用途：攻略详情页和发布页中的关联符文展示
 */
public class AugmentListAdapter extends RecyclerView.Adapter<AugmentListAdapter.AugmentViewHolder> {

    private List<StrategyDetailResponse.AugmentResponse> augments = new ArrayList<>();

    public void setAugments(List<StrategyDetailResponse.AugmentResponse> augments) {
        this.augments = augments != null ? augments : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AugmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_augment_simple, parent, false);
        return new AugmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AugmentViewHolder holder, int position) {
        StrategyDetailResponse.AugmentResponse augment = augments.get(position);
        holder.bind(augment);
    }

    @Override
    public int getItemCount() {
        return augments.size();
    }

    static class AugmentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageIcon;
        private final TextView textName;

        AugmentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.image_icon);
            textName = itemView.findViewById(R.id.text_name);
        }

        void bind(StrategyDetailResponse.AugmentResponse augment) {
            textName.setText(augment.getNameZh());
            if (augment.getIcon() != null && !augment.getIcon().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(augment.getIcon())
                        .centerCrop()
                        .into(imageIcon);
            }
        }
    }
}
