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
 * 装备列表适配器 ── 攻略详情页和发布页中的装备展示组件
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个适配器是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * ItemListAdapter 用于在攻略详情页（StrategyDetailFragment）和
 * 发布页（PublishStrategyFragment）中展示攻略推荐的装备列表。
 * 每个列表项显示一个装备的图标和中文名称。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、与 AugmentListAdapter 的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * ItemListAdapter 和 AugmentListAdapter 的代码结构几乎完全相同，
 * 区别仅在于数据类型不同：
 * - AugmentListAdapter → StrategyDetailResponse.AugmentResponse（符文数据）
 * - ItemListAdapter    → StrategyDetailResponse.ItemResponse（装备数据）
 *
 * 为什么不合并成一个通用适配器？
 * 1. 符文和装备是不同的业务概念，分开更清晰
 * 2. 未来可能需要不同的展示逻辑（如装备显示价格、符文显示效果等）
 * 3. 代码量很少（每个不到 80 行），合并带来的复杂度不值得
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、使用场景
 * ═══════════════════════════════════════════════════════════════════
 *
 * 场景 1 ── 攻略详情页（StrategyDetailFragment）：
 *   ┌──────────────────────────────────────────┐
 *   │  推荐出装                                 │
 *   │  ┌──────┐ ┌──────┐ ┌──────┐             │
 *   │  │ 图标 │ │ 图标 │ │ 图标 │  ← 水平滚动  │
 *   │  │装备1 │ │装备2 │ │装备3 │             │
 *   │  └──────┘ └──────┘ └──────┘             │
 *   └──────────────────────────────────────────┘
 *   RecyclerView 方向：HORIZONTAL（水平）
 *
 * 场景 2 ── 发布页（PublishStrategyFragment）：
 *   同上，展示用户已选择的装备列表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、列表项布局结构（item_item_simple.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────┐
 *   │   ┌──────┐   │
 *   │   │ 图标 │   │  ← imageIcon（48×48dp，centerCrop）
 *   │   └──────┘   │
 *   │  装备名称    │  ← textName
 *   └──────────────┘
 *
 * 布局与 item_augment_simple.xml 完全相同（图标 + 名称，垂直排列）。
 * 虽然布局相同，但使用不同的布局文件名，便于未来独立修改样式。
 */
public class ItemListAdapter extends RecyclerView.Adapter<ItemListAdapter.ItemViewHolder> {

    /**
     * 装备数据列表 ── 存储要展示的装备信息
     *
     * 数据类型是 StrategyDetailResponse.ItemResponse，这是攻略详情
     * 响应中嵌套的装备数据类，包含：
     * - id：装备ID
     * - nameZh：装备中文名称
     * - icon：装备图标 URL
     */
    private List<StrategyDetailResponse.ItemResponse> items = new ArrayList<>();

    /**
     * 替换整个装备列表 ── 设置新的装备数据并刷新视图
     *
     * 与 AugmentListAdapter 一样，本适配器没有 addItem() 方法，
     * 因为装备列表不需要分页加载，直接替换即可。
     *
     * @param items 新的装备列表，如果传入 null 则视为空列表
     */
    public void setItems(List<StrategyDetailResponse.ItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 创建 ViewHolder ── 从布局文件创建装备列表项视图
     *
     * @param parent   RecyclerView 本身
     * @param viewType 视图类型（本适配器只有一种类型，忽略此参数）
     * @return 新创建的 ItemViewHolder 实例
     */
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_item_simple, parent, false);
        return new ItemViewHolder(view);
    }

    /**
     * 绑定数据到 ViewHolder ── 将装备数据填充到列表项视图中
     *
     * @param holder   要绑定数据的 ViewHolder
     * @param position 装备在列表中的位置索引
     */
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        StrategyDetailResponse.ItemResponse item = items.get(position);
        holder.bind(item);
    }

    /**
     * 获取装备数据总数
     *
     * @return 当前装备列表的大小
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 装备列表项 ViewHolder ── 持有装备列表项视图中控件的引用
     *
     * 使用 static 内部类的原因与 AugmentListAdapter.AugmentViewHolder 相同：
     * 1. 不需要访问外部 Adapter 的成员
     * 2. 避免非静态内部类持有外部类引用导致的潜在内存泄漏
     * 3. 符合 Android 官方推荐的最佳实践
     */
    static class ItemViewHolder extends RecyclerView.ViewHolder {

        /** 装备图标 ── 显示装备的小图标，使用 Glide 加载网络图片 */
        private final ImageView imageIcon;

        /** 装备名称 ── 显示装备的中文本地化名称 */
        private final TextView textName;

        /**
         * 构造函数 ── 初始化视图引用
         *
         * @param itemView 由 onCreateViewHolder() 中 inflate 出来的列表项根视图
         */
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.image_icon);
            textName = itemView.findViewById(R.id.text_name);
        }

        /**
         * 绑定装备数据到视图 ── 将一个 ItemResponse 的数据填充到列表项中
         *
         * 绑定逻辑：
         * 1. 设置装备名称（中文名，始终显示）
         * 2. 如果图标 URL 不为空，使用 Glide 加载网络图片
         *    - centerCrop：等比缩放并裁剪，填满整个 ImageView
         *    - 如果 URL 为空，ImageView 保持默认状态
         *
         * @param item 装备数据对象，包含 nameZh 和 icon 字段
         */
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
