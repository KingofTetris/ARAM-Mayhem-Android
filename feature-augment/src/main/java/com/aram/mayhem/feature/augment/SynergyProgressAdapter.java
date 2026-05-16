package com.aram.mayhem.feature.augment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.network.dto.SynergyProgressResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 套装进度适配器
 *
 * 数据源：SynergyProgressResponse 列表
 * 布局：item_synergy_progress.xml
 * 用途：符文详情弹窗和推荐页中的套装进度展示
 */
public class SynergyProgressAdapter extends RecyclerView.Adapter<SynergyProgressAdapter.ViewHolder> {

    private List<SynergyProgressResponse> items = new ArrayList<>();

    public void submitList(List<SynergyProgressResponse> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_synergy_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView textSynergyName;
        private final TextView textProgressCount;
        private final ProgressBar progressSynergy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textSynergyName = itemView.findViewById(R.id.text_synergy_name);
            textProgressCount = itemView.findViewById(R.id.text_progress_count);
            progressSynergy = itemView.findViewById(R.id.progress_synergy);
        }

        public void bind(SynergyProgressResponse item) {
            textSynergyName.setText(item.getDisplayName());
            textProgressCount.setText(String.format("%d/%d", item.getCurrentCount(), item.getTotalCount()));
            progressSynergy.setProgress(item.getProgressPercent());

            String status = item.getStatus();
            int progressColor;
            if ("completed".equals(status)) {
                progressColor = itemView.getContext().getColor(R.color.synergy_completed);
            } else if ("partial".equals(status)) {
                progressColor = itemView.getContext().getColor(R.color.synergy_partial);
            } else {
                progressColor = itemView.getContext().getColor(R.color.synergy_inactive);
            }
            progressSynergy.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));
        }
    }
}