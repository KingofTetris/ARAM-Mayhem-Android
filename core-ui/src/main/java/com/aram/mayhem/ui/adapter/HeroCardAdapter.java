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
 * 英雄卡片列表适配器
 *
 * 数据源：HeroUiModel 列表
 * 布局：item_hero_card.xml
 * 点击事件：通过 OnHeroClickListener 回调跳转详情页
 */
public class HeroCardAdapter extends ListAdapter<HeroUiModel, HeroCardAdapter.ViewHolder> {

    private OnHeroClickListener listener;

    public interface OnHeroClickListener {
        void onHeroClick(@NonNull HeroUiModel hero);
    }

    public HeroCardAdapter() {
        super(new HeroDiffCallback());
    }

    public void setOnHeroClickListener(OnHeroClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHeroCardBinding binding = ItemHeroCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HeroUiModel hero = getItem(position);
        holder.bind(hero);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemHeroCardBinding binding;

        ViewHolder(@NonNull ItemHeroCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull HeroUiModel hero) {
            binding.textHeroName.setText(hero.getNameZh());
            binding.textHeroRole.setText(hero.getRole());
            binding.textWinRate.setText(hero.getWinRateDisplay());
            binding.badgeTier.setTier(hero.getTier());

            Glide.with(binding.imageHeroAvatar.getContext())
                    .load(hero.getAvatarUrl())
                    .placeholder(com.aram.mayhem.ui.R.drawable.ic_heroes)
                    .error(com.aram.mayhem.ui.R.drawable.ic_heroes)
                    .circleCrop()
                    .into(binding.imageHeroAvatar);

            if (hero.isTrap()) {
                binding.getRoot().setStrokeColor(
                        binding.getRoot().getContext().getColor(
                                com.aram.mayhem.ui.R.color.trap_warning));
            } else {
                binding.getRoot().setStrokeColor(0);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHeroClick(hero);
                }
            });
        }
    }

    static class HeroDiffCallback extends DiffUtil.ItemCallback<HeroUiModel> {

        @Override
        public boolean areItemsTheSame(@NonNull HeroUiModel oldItem, @NonNull HeroUiModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull HeroUiModel oldItem, @NonNull HeroUiModel newItem) {
            return oldItem.getId() == newItem.getId()
                    && oldItem.getWinRate() == newItem.getWinRate()
                    && oldItem.getTier() == newItem.getTier()
                    && oldItem.isTrap() == newItem.isTrap();
        }
    }
}
