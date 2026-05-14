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

public class ItemListAdapter extends RecyclerView.Adapter<ItemListAdapter.ItemViewHolder> {

    private List<StrategyDetailResponse.ItemResponse> items = new ArrayList<>();

    public void setItems(List<StrategyDetailResponse.ItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_item_simple, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        StrategyDetailResponse.ItemResponse item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageIcon;
        private final TextView textName;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.image_icon);
            textName = itemView.findViewById(R.id.text_name);
        }

        void bind(StrategyDetailResponse.ItemResponse item) {
            textName.setText(item.getNameZh());
            if (item.getIcon() != null && !item.getIcon().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getIcon())
                        .centerCrop()
                        .into(imageIcon);
            }
        }
    }
}
