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
 * 套装进度适配器 ── 展示符文套装的收集进度
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个适配器是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * SynergyProgressAdapter 将套装进度数据（SynergyProgressResponse）
 * 绑定到套装进度卡片布局（item_synergy_progress.xml）。
 *
 * 每个卡片展示：
 * - 套装名称（如"刺客"、"法师"）
 * - 收集进度（如"2/3"，表示已收集2个，需要3个激活）
 * - 进度条（颜色根据状态变化：已完成/部分完成/未激活）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、使用场景
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. AugmentRecommendFragment：水平滚动展示套装进度
 * 2. AugmentDetailBottomSheet：展示符文所属套装的进度
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与 AugmentRecommendAdapter 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * - SynergyProgressAdapter：继承 RecyclerView.Adapter，使用 notifyDataSetChanged()
 * - AugmentRecommendAdapter：继承 ListAdapter，使用 DiffUtil 增量更新
 *
 * 为什么不用 DiffUtil？
 * - 套装进度数据量小（通常3~5个），全量刷新性能足够
 * - 代码更简单，不需要实现 DiffUtil.ItemCallback
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、进度条颜色逻辑
 * ═══════════════════════════════════════════════════════════════════
 *
 *   status="completed"  →  绿色（synergy_completed）  套装已激活
 *   status="partial"    →  黄色（synergy_partial）    部分收集
 *   status="inactive"   →  灰色（synergy_inactive）   未开始收集
 *
 * @see SynergyProgressResponse
 * @see AugmentRecommendFragment
 */
public class SynergyProgressAdapter extends RecyclerView.Adapter<SynergyProgressAdapter.ViewHolder> {

    /**
     * 套装进度数据列表 ── 适配器的数据源
     */
    private List<SynergyProgressResponse> items = new ArrayList<>();

    /**
     * 更新数据 ── 替换整个列表并刷新
     *
     * 使用 notifyDataSetChanged() 全量刷新。
     * 对于少量数据（3~5个套装），性能完全足够。
     *
     * @param newItems 新的套装进度列表，null 时设为空列表
     */
    public void submitList(List<SynergyProgressResponse> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 创建 ViewHolder ── 填充套装进度卡片布局
     *
     * @param parent   父视图容器（RecyclerView）
     * @param viewType 视图类型（本适配器只有一种类型）
     * @return 包含套装进度卡片视图的 ViewHolder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_synergy_progress, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 绑定数据 ── 将 SynergyProgressResponse 数据绑定到 ViewHolder
     *
     * @param holder   ViewHolder 实例
     * @param position 列表位置
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    /**
     * 获取数据条数
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder ── 持有套装进度卡片中的 View 引用
     *
     * 职责：
     * 1. 缓存 View 引用（避免重复 findViewById）
     * 2. 提供 bind() 方法绑定数据到 View
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        /**
         * 套装名称 ── 如"刺客"、"法师"
         */
        private final TextView textSynergyName;

        /**
         * 收集进度文本 ── 如"2/3"
         */
        private final TextView textProgressCount;

        /**
         * 进度条 ── 可视化展示收集进度
         */
        private final ProgressBar progressSynergy;

        /**
         * 构造函数 ── 从 item_synergy_progress.xml 中获取 View 引用
         *
         * @param itemView 套装进度卡片的根视图
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textSynergyName = itemView.findViewById(R.id.text_synergy_name);
            textProgressCount = itemView.findViewById(R.id.text_progress_count);
            progressSynergy = itemView.findViewById(R.id.progress_synergy);
        }

        /**
         * 绑定数据 ── 将套装进度数据设置到 UI 控件
         *
         * 绑定内容：
         * 1. 套装名称
         * 2. 收集进度文本（currentCount/totalCount）
         * 3. 进度条百分比
         * 4. 进度条颜色（根据状态）
         *
         * @param item 套装进度数据
         */
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
