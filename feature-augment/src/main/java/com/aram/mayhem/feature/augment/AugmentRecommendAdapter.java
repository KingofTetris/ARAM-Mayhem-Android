package com.aram.mayhem.feature.augment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.augment.R;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.bumptech.glide.Glide;

import java.util.Locale;

/**
 * 符文推荐列表适配器
 *
 * 数据源：AugmentRecommendResponse 列表
 * 布局：item_augment_recommend.xml
 * 用途：AugmentRecommendFragment 中的推荐结果展示
 */
public class AugmentRecommendAdapter extends ListAdapter<AugmentRecommendResponse, AugmentRecommendAdapter.ViewHolder> {

    private OnAugmentClickListener listener;

    public AugmentRecommendAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<AugmentRecommendResponse> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AugmentRecommendResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull AugmentRecommendResponse oldItem, @NonNull AugmentRecommendResponse newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AugmentRecommendResponse oldItem, @NonNull AugmentRecommendResponse newItem) {
                    return oldItem.getId().equals(newItem.getId()) &&
                            oldItem.getScore() == newItem.getScore();
                }
            };

    public void setOnAugmentClickListener(OnAugmentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_augment_recommend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageAugmentIcon;
        private final TextView textAugmentName;
        private final TextView textAugmentQuality;
        private final TextView textScore;
        private final TextView textReason;
        private final TextView textWinRate;
        private final TextView textPickRate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAugmentIcon = itemView.findViewById(R.id.image_augment_icon);
            textAugmentName = itemView.findViewById(R.id.text_augment_name);
            textAugmentQuality = itemView.findViewById(R.id.text_augment_quality);
            textScore = itemView.findViewById(R.id.text_score);
            textReason = itemView.findViewById(R.id.text_reason);
            textWinRate = itemView.findViewById(R.id.text_win_rate);
            textPickRate = itemView.findViewById(R.id.text_pick_rate);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAugmentClick(getItem(pos));
                }
            });
        }

        public void bind(AugmentRecommendResponse item) {
            textAugmentName.setText(item.getNameZh());
            textAugmentQuality.setText(item.getQuality());
            textScore.setText(String.format(Locale.getDefault(), "%d", item.getScorePercent()));
            textReason.setText(item.getRecommendationReason());

            double winRate = item.getWinRate() != null ? item.getWinRate() * 100 : 0;
            double pickRate = item.getPickRate() != null ? item.getPickRate() * 100 : 0;
            textWinRate.setText(String.format(Locale.getDefault(), "胜率: %.1f%%", winRate));
            textPickRate.setText(String.format(Locale.getDefault(), "选用: %.1f%%", pickRate));

            if (item.getIconUrl() != null && !item.getIconUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getIconUrl())
                        .placeholder(R.drawable.ic_augments)
                        .into(imageAugmentIcon);
            } else {
                imageAugmentIcon.setImageResource(R.drawable.ic_augments);
            }
        }
    }

    public interface OnAugmentClickListener {
        void onAugmentClick(AugmentRecommendResponse augment);
    }
}