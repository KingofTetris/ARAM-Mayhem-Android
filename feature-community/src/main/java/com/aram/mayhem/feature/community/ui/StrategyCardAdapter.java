package com.aram.mayhem.feature.community.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.community.R;
import com.aram.mayhem.network.dto.StrategyListResponse;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class StrategyCardAdapter extends RecyclerView.Adapter<StrategyCardAdapter.StrategyViewHolder> {

    private List<StrategyListResponse> strategies = new ArrayList<>();
    private OnStrategyClickListener listener;

    public interface OnStrategyClickListener {
        void onStrategyClick(StrategyListResponse strategy);
    }

    public void setOnStrategyClickListener(OnStrategyClickListener listener) {
        this.listener = listener;
    }

    public void setStrategies(List<StrategyListResponse> strategies) {
        this.strategies = strategies != null ? strategies : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addStrategies(List<StrategyListResponse> moreStrategies) {
        if (moreStrategies != null && !moreStrategies.isEmpty()) {
            int startPosition = strategies.size();
            strategies.addAll(moreStrategies);
            notifyItemRangeInserted(startPosition, moreStrategies.size());
        }
    }

    @NonNull
    @Override
    public StrategyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_strategy_card, parent, false);
        return new StrategyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StrategyViewHolder holder, int position) {
        StrategyListResponse strategy = strategies.get(position);
        holder.bind(strategy);
    }

    @Override
    public int getItemCount() {
        return strategies.size();
    }

    class StrategyViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageHero;
        private final TextView textAuthor;
        private final TextView textTime;
        private final TextView textTitle;
        private final TextView textDescription;
        private final LinearLayout layoutAugmentIcons;
        private final TextView textAugments;
        private final LinearLayout layoutItemIcons;
        private final TextView textUpvotes;
        private final TextView textDownvotes;

        StrategyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageHero = itemView.findViewById(R.id.image_hero);
            textAuthor = itemView.findViewById(R.id.text_author);
            textTime = itemView.findViewById(R.id.text_time);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            layoutAugmentIcons = itemView.findViewById(R.id.layout_augment_icons);
            textAugments = itemView.findViewById(R.id.text_augments);
            layoutItemIcons = itemView.findViewById(R.id.layout_item_icons);
            textUpvotes = itemView.findViewById(R.id.text_upvotes);
            textDownvotes = itemView.findViewById(R.id.text_downvotes);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStrategyClick(strategies.get(position));
                }
            });
        }

        void bind(StrategyListResponse strategy) {
            textTitle.setText(strategy.getTitle());
            textDescription.setText(strategy.getDescription());
            textAuthor.setText(strategy.getAuthorNickname());
            textTime.setText(formatTime(strategy.getCreatedAt()));
            textUpvotes.setText(String.valueOf(strategy.getUpvotes() != null ? strategy.getUpvotes() : 0));
            textDownvotes.setText(String.valueOf(strategy.getDownvotes() != null ? strategy.getDownvotes() : 0));

            bindAugmentIcons(strategy);
            bindItemIcons(strategy);

            if (strategy.getHeroIcon() != null && !strategy.getHeroIcon().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(strategy.getHeroIcon())
                        .circleCrop()
                        .into(imageHero);
            }
        }

        private void bindAugmentIcons(StrategyListResponse strategy) {
            layoutAugmentIcons.removeAllViews();
            List<String> icons = strategy.getAugmentIcons();
            if (icons != null && !icons.isEmpty()) {
                layoutAugmentIcons.setVisibility(View.VISIBLE);
                textAugments.setVisibility(View.VISIBLE);
                textAugments.setText(icons.size() + " 个强化符文");
                int maxShow = Math.min(icons.size(), 6);
                for (int i = 0; i < maxShow; i++) {
                    ImageView iconView = new ImageView(itemView.getContext());
                    int size = (int) (28 * itemView.getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    if (i > 0) {
                        params.setMarginStart((int) (4 * itemView.getResources().getDisplayMetrics().density));
                    }
                    iconView.setLayoutParams(params);
                    Glide.with(itemView.getContext())
                            .load(icons.get(i))
                            .centerCrop()
                            .into(iconView);
                    layoutAugmentIcons.addView(iconView);
                }
                if (icons.size() > 6) {
                    TextView moreText = new TextView(itemView.getContext());
                    moreText.setText("+" + (icons.size() - 6));
                    moreText.setTextColor(itemView.getResources().getColor(com.aram.mayhem.ui.R.color.text_hint, null));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMarginStart((int) (4 * itemView.getResources().getDisplayMetrics().density));
                    moreText.setLayoutParams(params);
                    layoutAugmentIcons.addView(moreText);
                }
            } else {
                layoutAugmentIcons.setVisibility(View.GONE);
                textAugments.setVisibility(View.GONE);
            }
        }

        private void bindItemIcons(StrategyListResponse strategy) {
            layoutItemIcons.removeAllViews();
            List<String> icons = strategy.getItemIcons();
            if (icons != null && !icons.isEmpty()) {
                layoutItemIcons.setVisibility(View.VISIBLE);
                int maxShow = Math.min(icons.size(), 6);
                for (int i = 0; i < maxShow; i++) {
                    ImageView iconView = new ImageView(itemView.getContext());
                    int size = (int) (28 * itemView.getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    if (i > 0) {
                        params.setMarginStart((int) (4 * itemView.getResources().getDisplayMetrics().density));
                    }
                    iconView.setLayoutParams(params);
                    Glide.with(itemView.getContext())
                            .load(icons.get(i))
                            .centerCrop()
                            .into(iconView);
                    layoutItemIcons.addView(iconView);
                }
                if (icons.size() > 6) {
                    TextView moreText = new TextView(itemView.getContext());
                    moreText.setText("+" + (icons.size() - 6));
                    moreText.setTextColor(itemView.getResources().getColor(com.aram.mayhem.ui.R.color.text_hint, null));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMarginStart((int) (4 * itemView.getResources().getDisplayMetrics().density));
                    moreText.setLayoutParams(params);
                    layoutItemIcons.addView(moreText);
                }
            } else {
                layoutItemIcons.setVisibility(View.GONE);
            }
        }

        private String formatTime(String createdAt) {
            if (createdAt == null || createdAt.isEmpty()) {
                return "";
            }
            try {
                if (createdAt.contains("T")) {
                    String datePart = createdAt.split("T")[0];
                    return datePart;
                }
                return createdAt;
            } catch (Exception e) {
                return createdAt;
            }
        }
    }
}
