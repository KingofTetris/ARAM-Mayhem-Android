package com.aram.mayhem.feature.augment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.augment.R;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.bumptech.glide.Glide;

import java.util.Locale;

/**
 * 符文推荐列表适配器 ── 将推荐符文数据绑定到推荐卡片布局
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个适配器是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentRecommendAdapter 负责将后端返回的推荐符文数据
 * （AugmentRecommendResponse 列表）逐条绑定到推荐卡片布局
 * （item_augment_recommend.xml），展示在 AugmentRecommendFragment 中。
 *
 * 每张推荐卡片展示：
 * 1. 符文图标（Glide 加载网络图片）
 * 2. 符文名称（如"暗影突袭"）
 * 3. 符文品质（如"PRISMATIC"棱彩）
 * 4. 推荐评分（0~100，数字越大越推荐）
 * 5. 推荐理由（如"与已选符文组成刺客3件套"）
 * 6. 胜率（选择该符文后的历史胜率）
 * 7. 选用率（该符文的整体选用率）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、与项目其他适配器的对比
 * ═══════════════════════════════════════════════════════════════════
 *
 * ┌────────────────────────┬──────────────────┬──────────────────────┬─────────────────────┐
 * │ 适配器                 │ 基类             │ 数据类型             │ 布局                │
 * ├────────────────────────┼──────────────────┼──────────────────────┼─────────────────────┤
 * │ AugmentCardAdapter     │ RecyclerView.    │ AugmentUiModel      │ item_augment_card   │
 * │                        │ Adapter          │ (列表项)             │ .xml                │
 * ├────────────────────────┼──────────────────┼──────────────────────┼─────────────────────┤
 * │ SynergyProgressAdapter │ RecyclerView.    │ SynergyProgressResp │ item_synergy_       │
 * │                        │ Adapter          │ (套装进度)           │ progress.xml        │
 * ├────────────────────────┼──────────────────┼──────────────────────┼─────────────────────┤
 * │ AugmentRecommendAdapter│ ListAdapter      │ AugmentRecommendResp│ item_augment_       │
 * │ (本类)                 │ (自带DiffUtil)   │ (推荐项，含评分)     │ recommend.xml       │
 * └────────────────────────┴──────────────────┴──────────────────────┴─────────────────────┘
 *
 * 关键区别：
 * - 本类使用 ListAdapter（而非 RecyclerView.Adapter），自带 DiffUtil 高效更新
 * - AugmentCardAdapter 用 notifyDataSetChanged() 全量刷新（列表数据量小）
 * - 本类用 DiffUtil 增量刷新（推荐结果可能频繁变化，需要精确更新）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、DiffUtil 是什么？为什么用 ListAdapter？
 * ═══════════════════════════════════════════════════════════════════
 *
 * DiffUtil 是 Android 提供的差异计算工具，用于比较新旧列表的差异，
 * 只更新变化的项，而不是全量刷新整个列表。
 *
 *   旧列表: [A, B, C]      新列表: [A, D, C]
 *                     ↓
 *   DiffUtil 计算: B 被移除，D 是新增 → 只更新位置1的项
 *                     ↓
 *   RecyclerView 动画: B 消失动画 + D 出现动画（流畅！）
 *
 * ListAdapter 封装了 DiffUtil 的使用：
 * - 调用 submitList(newList) 即可自动触发差异计算
 * - 不需要手动调用 notifyDataSetChanged()
 * - 内部在后台线程计算差异，不阻塞 UI
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   AugmentRecommendViewModel
 *     → recommendations LiveData 更新
 *     → AugmentRecommendFragment 观察到变化
 *     → adapter.submitList(newRecommendations)
 *     → DiffUtil 计算新旧差异
 *     → onBindViewHolder() 只绑定变化的项
 *     → UI 更新
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、点击事件
 * ═══════════════════════════════════════════════════════════════════
 *
 * 用户点击推荐卡片 → OnAugmentClickListener.onAugmentClick()
 * → AugmentRecommendFragment 接收回调
 * → 可选操作：弹出符文详情、添加到已选列表等
 *
 * @see AugmentRecommendResponse
 * @see AugmentRecommendFragment
 * @see SynergyProgressAdapter
 */
public class AugmentRecommendAdapter extends ListAdapter<AugmentRecommendResponse, AugmentRecommendAdapter.ViewHolder> {

    /**
     * 点击监听器 ── 外部传入的点击回调
     *
     * 由 AugmentRecommendFragment 通过 setOnAugmentClickListener() 设置。
     * 当用户点击某个推荐符文卡片时，调用 listener.onAugmentClick(item)，
     * 将被点击的推荐数据传递给 Fragment 处理。
     *
     * 可能为 null（如果 Fragment 没有设置监听器），
     * 所以点击时需要判空：if (listener != null)
     */
    private OnAugmentClickListener listener;

    /**
     * 构造方法 ── 初始化适配器并传入 DiffUtil 回调
     *
     * ListAdapter 要求在构造时提供 DiffUtil.ItemCallback，
     * 用于定义"两个项是否相同"和"两个项内容是否相同"的判断规则。
     *
     * DIFF_CALLBACK 是静态常量，所有适配器实例共享同一个回调对象，
     * 因为比较逻辑是无状态的，不需要每个实例各持一份。
     */
    public AugmentRecommendAdapter() {
        super(DIFF_CALLBACK);
    }

    /**
     * DiffUtil 回调 ── 定义推荐项的比较规则
     *
     * ═══════════════════════════════════════════════════════════════════
     * DiffUtil 需要回答两个问题：
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. areItemsTheSame：两个项是否代表同一个符文？
     *    → 比较 id，id 相同就是同一个符文
     *    → 用途：DiffUtil 判断某项是否被移动/删除/新增
     *
     * 2. areContentsTheSame：同一个符文的内容是否完全相同？
     *    → 比较 id + score，两者都相同才算内容不变
     *    → 用途：DiffUtil 判断某项是否需要重新绑定（调用 onBindViewHolder）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 为什么只比较 id 和 score？
     * ═══════════════════════════════════════════════════════════════════
     *
     * - id 不变 + score 不变 → 推荐结果没变，不需要重新绑定
     * - id 不变 + score 变了 → 推荐评分更新了，需要重新绑定显示新分数
     * - id 变了 → 完全是不同的符文，必须重新绑定
     *
     * 其他字段（nameZh, quality, reason 等）在同一符文中不会变化，
     * 所以不需要参与比较，减少比较开销。
     *
     * ═══════════════════════════════════════════════════════════════════
     * 与 SynergyProgressAdapter 的区别
     * ═══════════════════════════════════════════════════════════════════
     *
     * SynergyProgressAdapter 使用 RecyclerView.Adapter + notifyDataSetChanged()
     * 因为套装进度数据量小（通常3~5个），全量刷新开销可忽略。
     *
     * 本类使用 ListAdapter + DiffUtil，因为推荐结果可能频繁变化
     * （用户每次添加/移除符文都会重新推荐），需要精确更新。
     */
    private static final DiffUtil.ItemCallback<AugmentRecommendResponse> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AugmentRecommendResponse>() {
                /**
                 * 判断两个项是否代表同一个符文
                 *
                 * 比较规则：id 相同 → 同一个符文
                 *
                 * @param oldItem 旧列表中的项
                 * @param newItem 新列表中的项
                 * @return true 表示是同一个符文（可能内容有变化）
                 */
                @Override
                public boolean areItemsTheSame(@NonNull AugmentRecommendResponse oldItem, @NonNull AugmentRecommendResponse newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                /**
                 * 判断同一个符文的内容是否完全相同
                 *
                 * 比较规则：id 相同 + score 相同 → 内容没变
                 *
                 * 只有 areItemsTheSame 返回 true 时才会调用此方法。
                 * 如果返回 false，DiffUtil 会认为该项内容变化了，
                 * 需要重新调用 onBindViewHolder() 更新 UI。
                 *
                 * @param oldItem 旧列表中的项
                 * @param newItem 新列表中的项
                 * @return true 表示内容完全相同，不需要重新绑定
                 */
                @Override
                public boolean areContentsTheSame(@NonNull AugmentRecommendResponse oldItem, @NonNull AugmentRecommendResponse newItem) {
                    return oldItem.getId().equals(newItem.getId()) &&
                            oldItem.getScore() == newItem.getScore();
                }
            };

    /**
     * 设置点击监听器 ── 由 Fragment 调用，注册推荐卡片的点击回调
     *
     * 使用方式（在 AugmentRecommendFragment 中）：
     * <pre>
     * adapter.setOnAugmentClickListener(augment -> {
     *     // 用户点击了推荐符文，弹出详情或添加到已选列表
     *     viewModel.addSelectedAugment(augment.getId());
     * });
     * </pre>
     *
     * @param listener 点击监听器实现，null 表示取消监听
     */
    public void setOnAugmentClickListener(OnAugmentClickListener listener) {
        this.listener = listener;
    }

    /**
     * 创建 ViewHolder ── 当 RecyclerView 需要新的卡片容器时调用
     *
     * ═══════════════════════════════════════════════════════════════════
     * 这个方法什么时候被调用？
     * ═══════════════════════════════════════════════════════════════════
     *
     * RecyclerView 首次显示时，会调用此方法创建足够填满屏幕的 ViewHolder。
     * 之后滚动时，会复用滑出屏幕的 ViewHolder，不再创建新的。
     * 只有当列表项数量增加（如加载更多）时，才会再创建新的 ViewHolder。
     *
     * ═══════════════════════════════════════════════════════════════════
     * 布局结构（item_augment_recommend.xml）
     * ═══════════════════════════════════════════════════════════════════
     *
     *   ┌──────────────────────────────────────────┐
     *   │ ┌──────┐                                 │
     *   │ │ 图标  │  符文名称        评分: 85       │
     *   │ │image │  品质: PRISMATIC                │
     *   │ └──────┘  推荐理由: 组成刺客3件套         │
     *   │           胜率: 53.2%  选用: 12.1%       │
     *   └──────────────────────────────────────────┘
     *
     * @param parent   RecyclerView 本身，用于获取 Context
     * @param viewType 视图类型（本适配器只有一种类型，忽略此参数）
     * @return 新创建的 ViewHolder 实例
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_augment_recommend, parent, false);
        return new ViewHolder(view);
    }

    /**
     * 绑定数据 ── 将推荐符文数据设置到 ViewHolder 的 UI 控件
     *
     * 当 RecyclerView 需要显示某个位置的推荐项时调用。
     * 可能是首次显示，也可能是复用滑入屏幕的旧 ViewHolder。
     *
     * ListAdapter 的 getItem(position) 会返回当前列表中
     * 指定位置的数据项，不需要自己维护数据列表。
     *
     * @param holder   要绑定的 ViewHolder（包含 UI 控件的引用）
     * @param position 列表中的位置索引（0 开始）
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    /**
     * ViewHolder ── 推荐卡片的视图容器，持有布局中各 UI 控件的引用
     *
     * ═══════════════════════════════════════════════════════════════════
     * ViewHolder 的作用
     * ═══════════════════════════════════════════════════════════════════
     *
     * RecyclerView 滚动时需要频繁更新列表项的内容（文字、图片等）。
     * 如果每次都调用 findViewById() 查找控件，性能会很差。
     *
     * ViewHolder 的解决方案：
     * 1. 在构造时一次性找到所有控件的引用
     * 2. 保存为成员变量
     * 3. bind() 时直接使用引用设置内容，无需重复查找
     *
     *   构造时: imageAugmentIcon = itemView.findViewById(R.id.image_augment_icon)
     *   绑定时: imageAugmentIcon.setImageResource(...)  ← 直接用引用，快！
     *
     * ═══════════════════════════════════════════════════════════════════
     * 本 ViewHolder 持有的 UI 控件
     * ═══════════════════════════════════════════════════════════════════
     *
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 控件名               │ 类型      │ 展示内容               │
     * ├──────────────────────┼───────────┼────────────────────────┤
     * │ imageAugmentIcon     │ ImageView │ 符文图标               │
     * │ textAugmentName      │ TextView  │ 符文名称（中文）       │
     * │ textAugmentQuality   │ TextView  │ 符文品质               │
     * │ textScore            │ TextView  │ 推荐评分               │
     * │ textReason           │ TextView  │ 推荐理由               │
     * │ textWinRate          │ TextView  │ 胜率百分比             │
     * │ textPickRate         │ TextView  │ 选用率百分比           │
     * └──────────────────────┴───────────┴────────────────────────┘
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageAugmentIcon;
        private final TextView textAugmentName;
        private final TextView textAugmentQuality;
        private final TextView textScore;
        private final TextView textReason;
        private final TextView textWinRate;
        private final TextView textPickRate;

        /**
         * ViewHolder 构造方法 ── 初始化 UI 控件引用和点击事件
         *
         * 做两件事：
         * 1. 通过 findViewById 获取布局中各控件的引用（只做一次）
         * 2. 设置整个卡片的点击事件
         *
         * @param itemView 由 onCreateViewHolder 创建的卡片根视图
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAugmentIcon = itemView.findViewById(R.id.image_augment_icon);
            textAugmentName = itemView.findViewById(R.id.text_augment_name);
            textAugmentQuality = itemView.findViewById(R.id.text_augment_quality);
            textScore = itemView.findViewById(R.id.text_score);
            textReason = itemView.findViewById(R.id.text_reason);
            textWinRate = itemView.findViewById(R.id.text_win_rate);
            textPickRate = itemView.findViewById(R.id.text_pick_rate);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAugmentClick(getItem(pos));
                }
            });
        }

        /**
         * 绑定数据 ── 将推荐符文数据设置到各 UI 控件
         *
         * ═══════════════════════════════════════════════════════════════════
         * 绑定内容详解
         * ═══════════════════════════════════════════════════════════════════
         *
         * 1. 符文名称：直接设置中文文本
         *    例："暗影突袭"
         *
         * 2. 符文品质：直接设置品质字符串
         *    例："PRISMATIC"（棱彩）
         *
         * 3. 推荐评分：格式化为整数百分比
         *    getScorePercent() 返回 0~100 的整数
         *    例：85 → 显示 "85"
         *
         * 4. 推荐理由：直接设置理由文本
         *    例："与已选符文组成刺客3件套，攻击力+20%"
         *
         * 5. 胜率：后端返回 0.0~1.0 的小数，乘以100转为百分比
         *    例：0.532 × 100 = 53.2 → 显示 "胜率: 53.2%"
         *    null 安全：如果后端没返回胜率，默认显示 0.0%
         *
         * 6. 选用率：同胜率处理逻辑
         *    例：0.121 × 100 = 12.1 → 显示 "选用: 12.1%"
         *
         * 7. 符文图标：使用 Glide 加载网络图片
         *    - iconUrl 非空 → Glide 异步加载，加载中显示占位图
         *    - iconUrl 为空 → 直接显示默认图标
         *
         * ═══════════════════════════════════════════════════════════════════
         * Locale.getDefault() 的作用
         * ═══════════════════════════════════════════════════════════════════
         *
         * String.format(Locale.getDefault(), "%.1f%%", 53.2)
         * 使用系统默认语言环境格式化数字。
         * 不同地区的数字格式可能不同：
         * - 中文/英文：53.2%
         * - 德语：53,2%（逗号作小数点）
         * 使用 Locale.getDefault() 确保格式符合用户设备设置。
         *
         * @param item 推荐符文数据（AugmentRecommendResponse），
         *             继承自 AugmentResponse，包含基础符文信息 + 推荐评分和理由
         */
        public void bind(AugmentRecommendResponse item) {
            textAugmentName.setText(item.getNameZh());
            textAugmentQuality.setText(item.getQuality());
            textScore.setText(String.format(Locale.getDefault(), "%d", item.getScorePercent()));
            textReason.setText(item.getRecommendationReason());

            double winRate = item.getWinRate() != null ? item.getWinRate() * 100 : 0;
            double pickRate = item.getPickRate() != null ? item.getPickRate() * 100 : 0;
            textWinRate.setText(String.format(Locale.getDefault(), "胜率: %.1f%%", winRate));
            textPickRate.setText(String.format(Locale.getDefault(), "选用: %.1f%%", pickRate));

            if (item.getIconUrl() != null && !item.getIconUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getIconUrl())
                        .placeholder(R.drawable.ic_augments)
                        .into(imageAugmentIcon);
            } else {
                imageAugmentIcon.setImageResource(R.drawable.ic_augments);
            }
        }
    }

    /**
     * 推荐符文点击监听接口 ── 定义推荐卡片的点击回调协议
     *
     * ═══════════════════════════════════════════════════════════════════
     * 接口设计
     * ═══════════════════════════════════════════════════════════════════
     *
     * 与 OnAugmentSelectedListener（Fragment 间通信）不同，
     * 本接口是适配器内部的点击回调，用于：
     * - 通知 Fragment 用户点击了哪个推荐符文
     * - Fragment 决定后续操作（弹出详情、添加到已选列表等）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 与 OnAugmentSelectedListener 的区别
     * ═══════════════════════════════════════════════════════════════════
     *
     * ┌──────────────────────────┬──────────────────────────────────┐
     * │ OnAugmentClickListener   │ OnAugmentSelectedListener        │
     * ├──────────────────────────┼──────────────────────────────────┤
     * │ 适配器 → Fragment 通信    │ Fragment → Activity → Fragment   │
     * │ 传递 AugmentRecommendResp│ 传递 long augmentId              │
     * │ 用于推荐列表的点击响应    │ 用于列表页 → 详情弹窗的导航      │
     * │ 接口定义在 Adapter 内部   │ 接口定义在独立文件               │
     * └──────────────────────────┴──────────────────────────────────┘
     *
     * 为什么本接口定义在 Adapter 内部？
     * - 只被本 Adapter 和 AugmentRecommendFragment 使用
     * - 不需要跨 Fragment 通信，不需要独立文件
     * - Android 推荐做法：简单回调定义在使用它的类内部
     */
    public interface OnAugmentClickListener {
        /**
         * 推荐符文被点击时的回调
         *
         * @param augment 被点击的推荐符文数据，
         *                包含完整信息（id, 名称, 品质, 评分, 理由, 胜率等）
         */
        void onAugmentClick(AugmentRecommendResponse augment);
    }
}
