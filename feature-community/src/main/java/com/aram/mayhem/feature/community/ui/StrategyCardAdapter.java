package com.aram.mayhem.feature.community.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.community.R;
import com.aram.mayhem.network.dto.StrategyListResponse;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * 攻略卡片适配器 ── 社区攻略列表的核心展示组件
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个适配器是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * StrategyCardAdapter 是社区攻略列表页（CommunityFeedFragment）使用的
 * RecyclerView 适配器。它负责：
 * 1. 将后端返回的攻略数据（StrategyListResponse）绑定到卡片视图上
 * 2. 管理攻略列表的数据集合（设置、追加、获取）
 * 3. 处理卡片的点击事件，通知 Fragment 跳转到详情页
 * 4. 动态渲染符文图标和装备图标（数量不固定，需要代码动态创建 ImageView）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、卡片布局结构（item_strategy_card.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────────────────────┐
 *   │  ┌──────┐  英雄名称                                 │
 *   │  │ 头像 │  作者昵称 · 发布时间                       │  ← 顶部区域
 *   │  └──────┘                                            │
 *   ├──────────────────────────────────────────────────────┤
 *   │  攻略标题                                            │  ← textTitle
 *   ├──────────────────────────────────────────────────────┤
 *   │  攻略描述摘要...                                     │  ← textDescription
 *   ├──────────────────────────────────────────────────────┤
 *   │  3 个强化符文                                        │  ← textAugments
 *   │  [图标1][图标2][图标3]                               │  ← layoutAugmentIcons（动态）
 *   ├──────────────────────────────────────────────────────┤
 *   │  [装备1][装备2][装备3]                               │  ← layoutItemIcons（动态）
 *   ├──────────────────────────────────────────────────────┤
 *   │  👍 12    👎 3                                       │  ← 投票计数
 *   └──────────────────────────────────────────────────────┘
 *
 * 注意：符文图标和装备图标是动态创建的，因为每条攻略关联的
 * 符文/装备数量不固定（0~N个），无法在 XML 中预先定义固定数量的 ImageView。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ViewModel                    Adapter                    UI
 *   ┌──────────────┐            ┌──────────────┐           ┌──────────┐
 *   │ strategies   │ ──setStrategies()──→ │ 数据列表     │ ──bind()──→ │ 卡片视图 │
 *   │ (LiveData)   │            │              │           │          │
 *   │ loadMore()   │ ──addStrategies()──→ │ 追加数据     │ ──bind()──→ │ 新卡片   │
 *   └──────────────┘            └──────────────┘           └──────────┘
 *
 *   用户点击卡片 ──→ OnStrategyClickListener ──→ Fragment 导航到详情页
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、为什么不用 DiffUtil？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 与 AugmentRecommendAdapter（使用 DiffUtil）不同，本适配器使用
 * notifyDataSetChanged() 而非 DiffUtil，原因如下：
 * 1. 攻略列表每次刷新都是整页替换（不是增量更新），DiffUtil 的差异计算
 *    在全量替换场景下没有性能优势
 * 2. 分页追加使用 notifyItemRangeInserted() 已经足够高效
 * 3. 攻略卡片包含动态创建的子视图（符文/装备图标），DiffUtil 的
 *    payload 机制难以处理这种动态视图的局部更新
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、图标截断策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当符文/装备数量超过 6 个时，只显示前 6 个图标，并在末尾显示
 * "+N" 文字表示还有更多。例如：
 *   [图1][图2][图3][图4][图5][图6] +3
 *
 * 这样做的目的是：
 * - 避免卡片过高，保持列表的滚动流畅性
 * - 6 个图标足以让用户快速了解攻略的核心搭配
 * - 完整信息可以在详情页查看
 */
public class StrategyCardAdapter extends RecyclerView.Adapter<StrategyCardAdapter.StrategyViewHolder> {

    /**
     * 攻略数据列表 ── 存储当前适配器中所有要展示的攻略数据
     *
     * 初始为空列表，通过以下两种方式更新：
     * 1. setStrategies()：首次加载或下拉刷新时，替换整个列表
     * 2. addStrategies()：上拉加载更多时，在末尾追加新数据
     */
    private List<StrategyListResponse> strategies = new ArrayList<>();

    /**
     * 卡片点击监听器 ── 由 CommunityFeedFragment 实现
     *
     * 当用户点击某张攻略卡片时，适配器通过此监听器通知 Fragment，
     * Fragment 再导航到 StrategyDetailFragment 显示详情。
     *
     * 使用接口而非直接引用 Fragment 的好处：
     * - 解耦：适配器不依赖具体的 Fragment 类
     * - 可测试：可以用 Mock 对象替代 Fragment 进行单元测试
     * - 灵活：任何实现了接口的类都可以作为点击监听器
     */
    private OnStrategyClickListener listener;

    /**
     * 攻略卡片点击监听接口
     *
     * 使用方式：
     *   adapter.setOnStrategyClickListener(new OnStrategyClickListener() {
     *       @Override
     *       public void onStrategyClick(StrategyListResponse strategy) {
     *           // 跳转到攻略详情页，传递 strategyId
     *           Bundle args = new Bundle();
     *           args.putLong("strategyId", strategy.getId());
     *           Navigation.findNavController(v).navigate(R.id.action_to_detail, args);
     *       }
     *   });
     */
    public interface OnStrategyClickListener {
        /**
         * 当用户点击攻略卡片时回调
         *
         * @param strategy 被点击的攻略数据对象，包含攻略ID、标题等，
         *                 Fragment 可以用 strategy.getId() 获取ID并导航到详情页
         */
        void onStrategyClick(StrategyListResponse strategy);
    }

    /**
     * 设置卡片点击监听器
     *
     * 必须在设置适配器之后、用户交互之前调用，否则点击卡片不会有任何反应。
     *
     * @param listener 实现了 OnStrategyClickListener 接口的监听器对象
     */
    public void setOnStrategyClickListener(OnStrategyClickListener listener) {
        this.listener = listener;
    }

    /**
     * 替换整个攻略列表 ── 用于首次加载或下拉刷新
     *
     * 调用此方法后：
     * 1. 旧数据全部丢弃
     * 2. 新数据成为当前列表
     * 3. 通知 RecyclerView 重新绑定所有可见的 ViewHolder
     *
     * 为什么用 notifyDataSetChanged() 而非 DiffUtil？
     * 因为下拉刷新是整页替换，DiffUtil 计算差异的开销
     * 可能比直接全量刷新还大。
     *
     * @param strategies 新的攻略列表，如果传入 null 则视为空列表
     */
    public void setStrategies(List<StrategyListResponse> strategies) {
        this.strategies = strategies != null ? strategies : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * 追加攻略数据 ── 用于上拉加载更多（分页）
     *
     * 与 setStrategies() 不同，此方法不会清除已有数据，而是在末尾追加。
     * 使用 notifyItemRangeInserted() 而非 notifyDataSetChanged()，
     * 这样 RecyclerView 只会为新数据创建 ViewHolder，已有的卡片不会重新绑定，
     * 避免了列表闪烁和滚动位置跳动。
     *
     * 调用示例（在 ViewModel 的 loadMore() 中）：
     *   int oldSize = adapter.getItemCount();  // 假设 10
     *   adapter.addStrategies(newPageData);     // 新增 10 条
     *   // RecyclerView 自动在位置 10~19 插入新卡片
     *
     * @param moreStrategies 要追加的新攻略列表，如果为 null 或空则不做任何操作
     */
    public void addStrategies(List<StrategyListResponse> moreStrategies) {
        if (moreStrategies != null && !moreStrategies.isEmpty()) {
            int startPosition = strategies.size();
            strategies.addAll(moreStrategies);
            notifyItemRangeInserted(startPosition, moreStrategies.size());
        }
    }

    /**
     * 创建 ViewHolder ── RecyclerView 调用此方法创建新的卡片视图持有者
     *
     * RecyclerView 的回收机制：
     * - 滑出屏幕的 ViewHolder 会被放入缓存池（约缓存 2~5 个）
     * - 滑入屏幕时优先从缓存池取 ViewHolder，取不到才调用此方法创建新的
     * - 所以这个方法不会被频繁调用，通常只创建屏幕可见数量 + 缓存数量的 ViewHolder
     *
     * @param parent   RecyclerView 本身，用于获取 Context 和布局参数
     * @param viewType 视图类型（本适配器只有一种类型，忽略此参数）
     * @return 新创建的 StrategyViewHolder 实例
     */
    @NonNull
    @Override
    public StrategyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_strategy_card, parent, false);
        return new StrategyViewHolder(view);
    }

    /**
     * 绑定数据到 ViewHolder ── 将攻略数据填充到卡片视图的各个控件中
     *
     * 此方法在以下时机被调用：
     * 1. 新创建的 ViewHolder 首次进入可见区域
     * 2. 已缓存的 ViewHolder 被复用（此时需要重新绑定不同的数据）
     *
     * 注意：此方法可能被频繁调用（每次滚动都可能触发），所以应保持高效，
     * 避免在此方法中做耗时操作（如网络请求、大量计算等）。
     *
     * @param holder   要绑定数据的 ViewHolder
     * @param position 数据在列表中的位置索引
     */
    @Override
    public void onBindViewHolder(@NonNull StrategyViewHolder holder, int position) {
        StrategyListResponse strategy = strategies.get(position);
        holder.bind(strategy);
    }

    /**
     * 获取数据总数 ── RecyclerView 据此决定列表长度和滚动范围
     *
     * @return 当前攻略列表的大小
     */
    @Override
    public int getItemCount() {
        return strategies.size();
    }

    /**
     * 攻略卡片 ViewHolder ── 持有卡片视图中所有控件的引用
     *
     * ═══════════════════════════════════════════════════════════════
     * ViewHolder 的作用：
     * ═══════════════════════════════════════════════════════════════
     *
     * 每次 findViewById() 都会遍历视图树，是相对耗时的操作。
     * ViewHolder 模式的核心思想是：在创建时执行一次 findViewById()，
     * 将结果缓存在字段中，后续绑定数据时直接使用缓存引用，
     * 避免重复查找，大幅提升列表滚动性能。
     *
     * ═══════════════════════════════════════════════════════════════
     * 为什么用非静态内部类？
     * ═══════════════════════════════════════════════════════════════
     *
     * 与 AugmentListAdapter.AugmentViewHolder（static 内部类）不同，
     * 本 ViewHolder 是非静态内部类，因为它需要访问外部类的 listener 字段
     * 来处理点击事件。如果改为 static，就需要在构造函数中额外传入 listener。
     *
     * 两种方式各有优劣：
     * - 非静态内部类：代码简洁，但持有外部类引用（轻微内存开销）
     * - 静态内部类：不持有外部类引用，但需要额外传参
     * 本适配器选择非静态方式，因为点击事件是核心功能，传参会增加复杂度。
     */
    class StrategyViewHolder extends RecyclerView.ViewHolder {

        /** 英雄头像 ── 显示攻略对应英雄的圆形头像 */
        private final ImageView imageHero;

        /** 作者昵称 ── 显示攻略发布者的昵称 */
        private final TextView textAuthor;

        /** 发布时间 ── 显示攻略的创建时间（只显示日期部分） */
        private final TextView textTime;

        /** 攻略标题 ── 显示攻略的标题文字 */
        private final TextView textTitle;

        /** 攻略描述 ── 显示攻略的描述摘要 */
        private final TextView textDescription;

        /**
         * 符文图标容器 ── 动态添加符文图标 ImageView
         *
         * 这个 LinearLayout 在 XML 中是空的，每次 bind() 时会：
         * 1. removeAllViews() 清空旧图标
         * 2. 根据数据动态创建 ImageView 并添加进来
         *
         * 为什么用 LinearLayout 而不是 RecyclerView？
         * - 符文图标数量少（最多6个），不需要复用机制
         * - LinearLayout 更轻量，适合少量动态子视图
         */
        private final LinearLayout layoutAugmentIcons;

        /** 符文数量文字 ── 显示 "3 个强化符文" 这样的描述 */
        private final TextView textAugments;

        /** 装备图标容器 ── 与 layoutAugmentIcons 同理，动态添加装备图标 */
        private final LinearLayout layoutItemIcons;

        /** 点赞数 ── 显示攻略获得的赞数 */
        private final TextView textUpvotes;

        /** 点踩数 ── 显示攻略获得的踩数 */
        private final TextView textDownvotes;

        /**
         * 构造函数 ── 初始化所有视图引用并设置点击事件
         *
         * @param itemView 由 onCreateViewHolder() 中 inflate 出来的卡片根视图
         */
        StrategyViewHolder(@NonNull View itemView) {
            super(itemView);

            imageHero = itemView.findViewById(R.id.image_hero);
            textAuthor = itemView.findViewById(R.id.text_author);
            textTime = itemView.findViewById(R.id.text_time);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            layoutAugmentIcons = itemView.findViewById(R.id.layout_augment_icons);
            textAugments = itemView.findViewById(R.id.text_augments);
            layoutItemIcons = itemView.findViewById(R.id.layout_item_icons);
            textUpvotes = itemView.findViewById(R.id.text_upvotes);
            textDownvotes = itemView.findViewById(R.id.text_downvotes);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStrategyClick(strategies.get(position));
                }
            });
        }

        /**
         * 绑定攻略数据到视图 ── 将一个 StrategyListResponse 的数据填充到卡片中
         *
         * 绑定顺序：
         * 1. 基础文字信息（标题、描述、作者、时间、投票数）
         * 2. 符文图标列表（动态创建 ImageView）
         * 3. 装备图标列表（动态创建 ImageView）
         * 4. 英雄头像（使用 Glide 加载网络图片）
         *
         * @param strategy 要展示的攻略数据对象
         */
        void bind(StrategyListResponse strategy) {
            textTitle.setText(strategy.getTitle());
            textDescription.setText(strategy.getDescription());
            textAuthor.setText(strategy.getAuthorNickname());
            textTime.setText(formatTime(strategy.getCreatedAt()));

            textUpvotes.setText(String.valueOf(strategy.getUpvotes() != null ? strategy.getUpvotes() : 0));
            textDownvotes.setText(String.valueOf(strategy.getDownvotes() != null ? strategy.getDownvotes() : 0));

            bindAugmentIcons(strategy);
            bindItemIcons(strategy);

            if (strategy.getHeroIcon() != null && !strategy.getHeroIcon().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(strategy.getHeroIcon())
                        .circleCrop()
                        .into(imageHero);
            }
        }

        /**
         * 绑定符文图标 ── 动态创建符文图标 ImageView 并添加到容器中
         *
         * ═══════════════════════════════════════════════════════════
         * 为什么需要动态创建 ImageView？
         * ═══════════════════════════════════════════════════════════
         *
         * 每条攻略关联的符文数量不固定（0~N个），无法在 XML 中
         * 预先定义固定数量的 ImageView。所以每次 bind() 时：
         * 1. 先 removeAllViews() 清空上一次绑定的旧图标
         *    （ViewHolder 可能被复用，旧数据可能残留）
         * 2. 根据当前攻略的符文数量，逐个创建 ImageView
         * 3. 使用 Glide 加载网络图标
         * 4. 将 ImageView 添加到 layoutAugmentIcons 容器中
         *
         * ═══════════════════════════════════════════════════════════
         * 图标尺寸计算
         * ═══════════════════════════════════════════════════════════
         *
         * 28dp 是图标的目标显示尺寸。但 Android 中设置视图尺寸需要像素值，
         * 所以需要将 dp 转换为 px：
         *   px = dp × density
         * 其中 density 是屏幕密度因子（如 2x 屏幕的 density = 2.0）
         *
         * 例如在 2x 屏幕上：28dp × 2.0 = 56px
         *
         * @param strategy 攻略数据，包含 getAugmentIcons() 返回图标 URL 列表
         */
        private void bindAugmentIcons(StrategyListResponse strategy) {
            layoutAugmentIcons.removeAllViews();

            List<String> icons = strategy.getAugmentIcons();

            if (icons != null && !icons.isEmpty()) {
                layoutAugmentIcons.setVisibility(View.VISIBLE);
                textAugments.setVisibility(View.VISIBLE);
                textAugments.setText(icons.size() + " 个强化符文");

                int maxShow = Math.min(icons.size(), 6);

                for (int i = 0; i < maxShow; i++) {
                    ImageView iconView = new ImageView(itemView.getContext());

                    int size = (int) (28 * itemView.getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);

                    if (i > 0) {
                        params.setMarginStart((int) (4 * itemView.getResources().getDisplayMetrics().density));
                    }

                    iconView.setLayoutParams(params);

                    Glide.with(itemView.getContext())
                            .load(icons.get(i))
                            .centerCrop()
                            .into(iconView);

                    layoutAugmentIcons.addView(iconView);
                }

                if (icons.size() > 6) {
                    TextView moreText = new TextView(itemView.getContext());
                    moreText.setText("+" + (icons.size() - 6));
                    moreText.setTextColor(itemView.getResources().getColor(com.aram.mayhem.ui.R.color.text_hint, null));

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMarginStart((int) (4 * itemView.getResources().getDisplayMetrics().density));
                    moreText.setLayoutParams(params);

                    layoutAugmentIcons.addView(moreText);
                }
            } else {
                layoutAugmentIcons.setVisibility(View.GONE);
                textAugments.setVisibility(View.GONE);
            }
        }

        /**
         * 绑定装备图标 ── 动态创建装备图标 ImageView 并添加到容器中
         *
         * 逻辑与 bindAugmentIcons() 几乎相同，区别在于：
         * 1. 数据来源是 strategy.getItemIcons()（装备图标 URL 列表）
         * 2. 没有数量文字提示（装备区域不显示 "N 个装备"）
         * 3. 容器是 layoutItemIcons 而非 layoutAugmentIcons
         *
         * 如果装备列表为空，整个装备区域隐藏（View.GONE）。
         *
         * @param strategy 攻略数据，包含 getItemIcons() 返回图标 URL 列表
         */
        private void bindItemIcons(StrategyListResponse strategy) {
            layoutItemIcons.removeAllViews();

            List<String> icons = strategy.getItemIcons();

            if (icons != null && !icons.isEmpty()) {
                layoutItemIcons.setVisibility(View.VISIBLE);

                int maxShow = Math.min(icons.size(), 6);

                for (int i = 0; i < maxShow; i++) {
                    ImageView iconView = new ImageView(itemView.getContext());

                    int size = (int) (28 * itemView.getResources().getDisplayMetrics().density);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);

                    if (i > 0) {
                        params.setMarginStart((int) (4 * itemView.getResources().getDisplayMetrics().density));
                    }

                    iconView.setLayoutParams(params);

                    Glide.with(itemView.getContext())
                            .load(icons.get(i))
                            .centerCrop()
                            .into(iconView);

                    layoutItemIcons.addView(iconView);
                }

                if (icons.size() > 6) {
                    TextView moreText = new TextView(itemView.getContext());
                    moreText.setText("+" + (icons.size() - 6));
                    moreText.setTextColor(itemView.getResources().getColor(com.aram.mayhem.ui.R.color.text_hint, null));

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMarginStart((int) (4 * itemView.getResources().getDisplayMetrics().density));
                    moreText.setLayoutParams(params);

                    layoutItemIcons.addView(moreText);
                }
            } else {
                layoutItemIcons.setVisibility(View.GONE);
            }
        }

        /**
         * 格式化时间字符串 ── 从 ISO 8601 格式中提取日期部分
         *
         * 后端返回的时间格式为 ISO 8601，例如："2026-05-20T14:30:00"
         * 其中 "T" 分隔日期和时间。本方法只取日期部分 "2026-05-20"，
         * 因为在卡片列表中显示完整时间太长，日期已经足够。
         *
         * 处理逻辑：
         * 1. 输入为 null 或空 → 返回空字符串
         * 2. 包含 "T" → 取 "T" 前面的日期部分
         * 3. 不包含 "T" → 原样返回（可能是已经格式化的日期）
         * 4. 异常 → 原样返回（容错处理）
         *
         * @param createdAt 后端返回的创建时间字符串
         * @return 格式化后的日期字符串，如 "2026-05-20"
         */
        private String formatTime(String createdAt) {
            if (createdAt == null || createdAt.isEmpty()) {
                return "";
            }
            try {
                if (createdAt.contains("T")) {
                    String datePart = createdAt.split("T")[0];
                    return datePart;
                }
                return createdAt;
            } catch (Exception e) {
                return createdAt;
            }
        }
    }
}
