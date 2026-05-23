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
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的攻略列表适配器 ── "我的攻略"页面的核心展示组件
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个适配器是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * MyStrategiesAdapter 是"我的攻略"页面（MyStrategiesFragment）使用的
 * RecyclerView 适配器。它负责：
 * 1. 将用户发布的攻略数据绑定到列表项视图上
 * 2. 管理攻略列表的数据集合（设置、获取、移除）
 * 3. 处理删除按钮的点击事件
 * 4. 支持左滑删除手势（配合 ItemTouchHelper）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、列表项布局结构（item_my_strategy.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 * 布局采用 FrameLayout 分层结构，支持左滑删除效果：
 *
 *   ┌──────────────────────────────────────────────────────┐
 *   │  底层：红色删除背景（默认被前景卡片遮挡）            │  ← deleteBackground
 *   │  ┌──────────────────────────────────────────────┐   │
 *   │  │  前景卡片（MaterialCardView）                 │   │  ← cardForeground
 *   │  │  ┌────────────────────────────────────────┐  │   │
 *   │  │  │  攻略标题              🗑 删除按钮     │  │   │  ← textTitle + imageDelete
 *   │  │  │  英雄名称                              │  │   │  ← textHero
 *   │  │  │  👍 12  👎 3                           │  │   │  ← textScore
 *   │  │  └────────────────────────────────────────┘  │   │
 *   │  └──────────────────────────────────────────────┘   │
 *   └──────────────────────────────────────────────────────┘
 *
 * 左滑删除原理：
 *   正常状态 → 前景卡片完全覆盖底层红色背景
 *   左滑时   → ItemTouchHelper 移动前景卡片，露出红色背景
 *   松手后   → 弹出确认对话框
 *   取消删除 → 前景卡片回弹到原位
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、两种删除方式
 * ═══════════════════════════════════════════════════════════════════
 *
 *   方式 1 ── 删除图标按钮（imageDelete）：
 *   点击列表项右侧的删除图标 → 弹出确认对话框 → 确认后删除
 *
 *   方式 2 ── 左滑手势（ItemTouchHelper）：
 *   向左滑动列表项 → 露出红色背景 → 松手弹出确认对话框 → 确认后删除
 *
 *   两种方式最终都调用同一个 ViewModel.deleteStrategy() 方法，
 *   只是触发方式不同。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、与 StrategyCardAdapter 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 *   特性              StrategyCardAdapter      MyStrategiesAdapter
 *   ─────────────────────────────────────────────────────────────
 *   使用页面          社区攻略列表              我的攻略列表
 *   删除功能          无                       有（按钮+滑动）
 *   点击事件          跳转详情页               无
 *   布局结构          单层卡片                 FrameLayout 分层
 *   数据管理          手动 setStrategies       手动 setStrategies
 *   ViewHolder        private class            public static class
 *
 *   ViewHolder 为什么是 public static？
 *   ── 因为 ItemTouchHelper 的 onChildDraw() 方法需要访问
 *      ViewHolder 的 cardForeground 字段来移动前景卡片。
 *      如果 ViewHolder 是 private，外部类（Fragment 中的
 *      ItemTouchHelper 回调）无法访问它。
 *
 * @see StrategyListResponse    攻略列表项的数据模型
 * @see MyStrategiesFragment    使用此适配器的 Fragment
 */
public class MyStrategiesAdapter extends RecyclerView.Adapter<MyStrategiesAdapter.ViewHolder> {

    /**
     * 攻略列表数据
     *
     * 初始为空列表，ViewModel 加载完成后通过 setStrategies() 更新。
     */
    private List<StrategyListResponse> strategies = new ArrayList<>();

    /**
     * 删除按钮点击监听器
     *
     * Fragment 实现此接口来处理删除操作：
     *   adapter.setOnDeleteClickListener((strategy, position) -> {
     *       showDeleteConfirmDialog(strategy, position);
     *   });
     */
    private OnDeleteClickListener deleteListener;

    /**
     * 删除按钮点击监听器接口
     */
    public interface OnDeleteClickListener {
        /**
         * 删除按钮被点击
         *
         * @param strategy 待删除的攻略数据
         * @param position 列表中的位置
         */
        void onDeleteClick(StrategyListResponse strategy, int position);
    }

    /**
     * 设置删除按钮点击监听器
     *
     * @param listener 删除回调，由 MyStrategiesFragment 提供
     */
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    /**
     * 更新攻略列表数据
     *
     * 替换整个数据列表并通知 RecyclerView 刷新所有列表项。
     *
     * 为什么用 notifyDataSetChanged() 而不是 DiffUtil？
     * ── "我的攻略"列表数据量通常很小（用户发布的攻略不会很多），
     *    全量刷新的性能开销可以忽略不计。
     *    使用 DiffUtil 需要额外编写 DiffCallback，对于小列表
     *    来说收益不大，反而增加代码复杂度。
     *
     * @param strategies 新的攻略列表，null 时替换为空列表
     */
    public void setStrategies(List<StrategyListResponse> strategies) {
        this.strategies = strategies != null ? strategies : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 获取指定位置的攻略数据
     *
     * ItemTouchHelper 的 onSwiped() 回调中使用此方法获取
     * 被滑动项的攻略数据，以便弹出删除确认对话框。
     *
     * @param position 列表位置
     * @return 对应的攻略数据，越界返回 null
     */
    public StrategyListResponse getItem(int position) {
        if (position >= 0 && position < strategies.size()) {
            return strategies.get(position);
        }
        return null;
    }

    /**
     * 移除指定位置的 item 并通知 UI 更新
     *
     * 此方法目前未被使用（删除后采用重新加载策略），
     * 但保留以备将来需要乐观删除（本地先移除，失败再回滚）时使用。
     *
     * @param position 要移除的位置
     */
    public void removeItem(int position) {
        if (position >= 0 && position < strategies.size()) {
            strategies.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * 创建 ViewHolder
     *
     * 使用传统的 findViewById 方式而非 ViewBinding，
     * 因为 item_my_strategy.xml 的 cardForeground 需要
     * 被 ItemTouchHelper 直接访问，ViewBinding 生成的
     * 字段是 private 的，外部无法访问。
     *
     * @param parent   父容器（RecyclerView）
     * @param viewType 视图类型（本适配器只有一种类型）
     * @return 新创建的 ViewHolder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_strategy, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 将数据绑定到 ViewHolder
     *
     * 绑定内容：
     * - textTitle  → 攻略标题
     * - textHero   → 英雄名称
     * - textScore  → 投票数（👍 upvotes  👎 downvotes）
     * - imageDelete → 点击事件（触发删除确认）
     *
     * @param holder   需要绑定数据的 ViewHolder
     * @param position 列表中的位置
     */
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

    /**
     * 列表项 ViewHolder
     *
     * ═══════════════════════════════════════════════════════════
     * 为什么是 public static class？
     * ═══════════════════════════════════════════════════════════
     *
     * 1. public ── Fragment 中的 ItemTouchHelper 回调需要访问
     *    ViewHolder 的 cardForeground 字段来移动前景卡片：
     *    View foregroundView = ((MyStrategiesAdapter.ViewHolder) viewHolder).cardForeground;
     *
     * 2. static ── 不持有外部类 Adapter 的隐式引用，避免内存泄漏
     *
     * 字段说明：
     * - cardForeground → 前景卡片，ItemTouchHelper 滑动时移动此视图
     * - textTitle      → 攻略标题
     * - textHero       → 英雄名称
     * - textScore      → 投票评分
     * - imageDelete    → 删除图标按钮
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public MaterialCardView cardForeground;
        TextView textTitle;
        TextView textHero;
        TextView textScore;
        ImageView imageDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardForeground = itemView.findViewById(R.id.card_foreground);
            textTitle = itemView.findViewById(R.id.text_strategy_title);
            textHero = itemView.findViewById(R.id.text_strategy_hero);
            textScore = itemView.findViewById(R.id.text_strategy_score);
            imageDelete = itemView.findViewById(R.id.image_delete);
        }
    }
}
