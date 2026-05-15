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

public class BulletinAdapter extends ListAdapter<BulletinUiModel, BulletinAdapter.BulletinViewHolder> {

    private OnBulletinClickListener listener;

    public interface OnBulletinClickListener {
        void onBulletinClick(BulletinUiModel bulletin);
    }

    public BulletinAdapter() {
        super(new BulletinDiffCallback());
    }

    public void setOnBulletinClickListener(OnBulletinClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BulletinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBulletinBinding binding = ItemBulletinBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BulletinViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BulletinViewHolder holder, int position) {
        BulletinUiModel bulletin = getItem(position);
        holder.bind(bulletin);
    }

    class BulletinViewHolder extends RecyclerView.ViewHolder {

        private final ItemBulletinBinding binding;

        BulletinViewHolder(ItemBulletinBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BulletinUiModel bulletin) {
            binding.textTitle.setText(bulletin.getTitle());
            binding.textTypeBadge.setText(bulletin.getTypeDisplay());
            binding.textDate.setText(bulletin.getCreatedAt());

            if (bulletin.isPinned()) {
                binding.textPinnedBadge.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.textPinnedBadge.setVisibility(android.view.View.GONE);
            }

            if (bulletin.getImageUrl() != null && !bulletin.getImageUrl().isEmpty()) {
                Glide.with(binding.imageBulletin.getContext())
                        .load(bulletin.getImageUrl())
                        .centerCrop()
                        .into(binding.imageBulletin);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBulletinClick(bulletin);
                }
            });
        }
    }

    static class BulletinDiffCallback extends DiffUtil.ItemCallback<BulletinUiModel> {
        @Override
        public boolean areItemsTheSame(@NonNull BulletinUiModel oldItem, @NonNull BulletinUiModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BulletinUiModel oldItem, @NonNull BulletinUiModel newItem) {
            return oldItem.getId() == newItem.getId()
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.isPinned() == newItem.isPinned();
        }
    }
}
