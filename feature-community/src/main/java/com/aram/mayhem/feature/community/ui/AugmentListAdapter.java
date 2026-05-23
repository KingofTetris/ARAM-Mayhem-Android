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
 * 符文列表适配器 ── 攻略详情页和发布页中的符文展示组件
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个适配器是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentListAdapter 用于在攻略详情页（StrategyDetailFragment）和
 * 发布页（PublishStrategyFragment）中展示攻略关联的强化符文列表。
 * 每个列表项显示一个符文的图标和中文名称。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、使用场景
 * ═══════════════════════════════════════════════════════════════════
 *
 * 场景 1 ── 攻略详情页（StrategyDetailFragment）：
 *   ┌──────────────────────────────────────────┐
 *   │  推荐符文                                 │
 *   │  ┌──────┐ ┌──────┐ ┌──────┐             │
 *   │  │ 图标 │ │ 图标 │ │ 图标 │  ← 水平滚动  │
 *   │  │名称1 │ │名称2 │ │名称3 │             │
 *   │  └──────┘ └──────┘ └──────┘             │
 *   └──────────────────────────────────────────┘
 *   RecyclerView 方向：HORIZONTAL（水平）
 *
 * 场景 2 ── 发布页（PublishStrategyFragment）：
 *   同上，展示用户已选择的符文列表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与 StrategyCardAdapter 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * ┌──────────────────┬──────────────────────┬──────────────────────┐
 * │ 对比项           │ StrategyCardAdapter  │ AugmentListAdapter   │
 * ├──────────────────┼──────────────────────┼──────────────────────┤
 * │ 数据类型         │ StrategyListResponse │ AugmentResponse      │
 * │ 列表方向         │ 垂直                 │ 水平                 │
 * │ 布局复杂度       │ 高（动态子视图）     │ 低（固定布局）       │
 * │ 点击事件         │ 有（跳转详情）       │ 无（仅展示）         │
 * │ 数据更新方式     │ set + add            │ 仅 set               │
 * │ ViewHolder 类型  │ 非静态内部类         │ 静态内部类           │
 * └──────────────────┴──────────────────────┴──────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、列表项布局结构（item_augment_simple.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────┐
 *   │   ┌──────┐   │
 *   │   │ 图标 │   │  ← imageIcon（48×48dp，centerCrop）
 *   │   └──────┘   │
 *   │  符文名称    │  ← textName
 *   └──────────────┘
 *
 * 布局非常简单：一个图标 + 一个名称，垂直排列。
 * 不需要动态创建子视图，因为每个列表项的结构是固定的。
 */
public class AugmentListAdapter extends RecyclerView.Adapter<AugmentListAdapter.AugmentViewHolder> {

    /**
     * 符文数据列表 ── 存储要展示的符文信息
     *
     * 数据类型是 StrategyDetailResponse.AugmentResponse，这是攻略详情
     * 响应中嵌套的符文数据类，包含：
     * - id：符文ID
     * - nameZh：符文中文名称
     * - icon：符文图标 URL
     */
    private List<StrategyDetailResponse.AugmentResponse> augments = new ArrayList<>();

    /**
     * 替换整个符文列表 ── 设置新的符文数据并刷新视图
     *
     * 与 StrategyCardAdapter 不同，本适配器没有 addAugments() 方法，
     * 因为符文列表不需要分页加载。符文数据通常在一次请求中全部获取，
     * 直接替换即可。
     *
     * @param augments 新的符文列表，如果传入 null 则视为空列表
     */
    public void setAugments(List<StrategyDetailResponse.AugmentResponse> augments) {
        this.augments = augments != null ? augments : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 创建 ViewHolder ── 从布局文件创建符文列表项视图
     *
     * @param parent   RecyclerView 本身
     * @param viewType 视图类型（本适配器只有一种类型，忽略此参数）
     * @return 新创建的 AugmentViewHolder 实例
     */
    @NonNull
    @Override
    public AugmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_augment_simple, parent, false);
        return new AugmentViewHolder(view);
    }

    /**
     * 绑定数据到 ViewHolder ── 将符文数据填充到列表项视图中
     *
     * @param holder   要绑定数据的 ViewHolder
     * @param position 符文在列表中的位置索引
     */
    @Override
    public void onBindViewHolder(@NonNull AugmentViewHolder holder, int position) {
        StrategyDetailResponse.AugmentResponse augment = augments.get(position);
        holder.bind(augment);
    }

    /**
     * 获取符文数据总数
     *
     * @return 当前符文列表的大小
     */
    @Override
    public int getItemCount() {
        return augments.size();
    }

    /**
     * 符文列表项 ViewHolder ── 持有符文列表项视图中控件的引用
     *
     * ═══════════════════════════════════════════════════════════════
     * 为什么用 static 内部类？
     * ═══════════════════════════════════════════════════════════════
     *
     * 与 StrategyCardAdapter.StrategyViewHolder（非静态内部类）不同，
     * 本 ViewHolder 使用 static 修饰，原因如下：
     *
     * 1. 本 ViewHolder 不需要访问外部类的任何字段（没有点击事件等）
     * 2. static 内部类不持有外部类引用，可以避免潜在的内存泄漏
     * 3. 符合 Android 官方推荐的最佳实践：
     *    "如果 ViewHolder 不需要访问外部 Adapter 的成员，应使用 static"
     *
     * 内存泄漏风险说明：
     * 非静态内部类隐式持有外部类引用。如果 ViewHolder 的生命周期
     * 比 Adapter 长（例如被某个异步回调持有），就会导致 Adapter
     * 无法被垃圾回收，造成内存泄漏。static 内部类没有这个问题。
     */
    static class AugmentViewHolder extends RecyclerView.ViewHolder {

        /** 符文图标 ── 显示符文的小图标，使用 Glide 加载网络图片 */
        private final ImageView imageIcon;

        /** 符文名称 ── 显示符文的中文本地化名称 */
        private final TextView textName;

        /**
         * 构造函数 ── 初始化视图引用
         *
         * @param itemView 由 onCreateViewHolder() 中 inflate 出来的列表项根视图
         */
        AugmentViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.image_icon);
            textName = itemView.findViewById(R.id.text_name);
        }

        /**
         * 绑定符文数据到视图 ── 将一个 AugmentResponse 的数据填充到列表项中
         *
         * 绑定逻辑：
         * 1. 设置符文名称（中文名，始终显示）
         * 2. 如果图标 URL 不为空，使用 Glide 加载网络图片
         *    - centerCrop：等比缩放并裁剪，填满整个 ImageView
         *    - 如果 URL 为空，ImageView 保持默认状态（空白或占位图）
         *
         * Glide 的优势：
         * - 自动处理图片缓存（内存缓存 + 磁盘缓存）
         * - 自动处理 ImageView 的生命周期（Activity 销毁时取消加载）
         * - 支持占位图和错误图
         * - 列表滚动时自动取消离开屏幕的图片加载请求
         *
         * @param augment 符文数据对象，包含 nameZh 和 icon 字段
         */
        void bind(StrategyDetailResponse.AugmentResponse augment) {
            textName.setText(augment.getNameZh());
            if (augment.getIcon() != null && !augment.getIcon().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(augment.getIcon())
                        .centerCrop()
                        .into(imageIcon);
            }
        }
    }
}
