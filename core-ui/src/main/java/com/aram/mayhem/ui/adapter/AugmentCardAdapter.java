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

public class AugmentCardAdapter extends ListAdapter<AugmentUiModel, AugmentCardAdapter.ViewHolder> {

    private OnAugmentClickListener listener;

    public interface OnAugmentClickListener {
        void onAugmentClick(@NonNull AugmentUiModel augment);
    }

    public AugmentCardAdapter() {
        super(new AugmentDiffCallback());
    }

    public void setOnAugmentClickListener(OnAugmentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAugmentCardBinding binding = ItemAugmentCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AugmentUiModel augment = getItem(position);
        holder.bind(augment);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemAugmentCardBinding binding;

        ViewHolder(@NonNull ItemAugmentCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AugmentUiModel augment) {
            binding.textAugmentName.setText(augment.getName());
            binding.textAugmentSynergy.setText(augment.getSynergyDisplay());

            int qualityColor = binding.getRoot().getContext().getColor(augment.getQualityColorRes());
            binding.cardAugment.setStrokeColor(qualityColor);

            if (augment.isTrap()) {
                binding.cardAugment.setStrokeColor(
                        binding.cardAugment.getContext().getColor(
                                com.aram.mayhem.ui.R.color.trap_warning));
            }

            Glide.with(binding.imageAugmentIcon.getContext())
                    .load(augment.getIconUrl())
                    .placeholder(com.aram.mayhem.ui.R.drawable.ic_augments)
                    .error(com.aram.mayhem.ui.R.drawable.ic_augments)
                    .into(binding.imageAugmentIcon);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAugmentClick(augment);
                }
            });
        }
    }

    static class AugmentDiffCallback extends DiffUtil.ItemCallback<AugmentUiModel> {

        @Override
        public boolean areItemsTheSame(@NonNull AugmentUiModel oldItem, @NonNull AugmentUiModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull AugmentUiModel oldItem, @NonNull AugmentUiModel newItem) {
            return oldItem.getId() == newItem.getId()
                    && oldItem.isTrap() == newItem.isTrap();
        }
    }
}
