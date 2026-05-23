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

/**
 * 公告列表适配器 ── 公告列表页的核心展示组件
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个适配器是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * BulletinAdapter 是公告列表页（BulletinListFragment）使用的
 * RecyclerView 适配器。它负责：
 * 1. 将后端返回的公告数据（BulletinUiModel）绑定到卡片视图上
 * 2. 处理公告卡片的点击事件，通知 Fragment 跳转到详情页
 * 3. 使用 DiffUtil 高效计算列表差异，避免不必要的刷新
 * 4. 显示公告的置顶标签（置顶公告有特殊标记）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、卡片布局结构（item_bulletin.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────────────────────┐
 *   │  ┌──────────────────────────────────────────────┐   │
 *   │  │                                              │   │
 *   │  │  公告封面图片                                 │   │  ← imageBulletin
 *   │  │                                              │   │
 *   │  └──────────────────────────────────────────────┘   │
 *   │  [置顶]  [版本更新]                          │   │  ← textPinnedBadge + textTypeBadge
 *   │  公告标题                                            │  ← textTitle
 *   │  2026-05-20                                         │  ← textDate
 *   └──────────────────────────────────────────────────────┘
 *
 *   注意：textPinnedBadge 仅在置顶公告时显示（VISIBLE），
 *         非置顶公告时隐藏（GONE）。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、ListAdapter 与 RecyclerView.Adapter 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本适配器继承 ListAdapter 而非 RecyclerView.Adapter，原因如下：
 *
 *   RecyclerView.Adapter              ListAdapter
 *   ┌──────────────────────┐         ┌──────────────────────┐
 *   │ 手动管理数据列表      │         │ 内部维护数据列表      │
 *   │ 手动调用              │         │ 自动通过 DiffUtil     │
 *   │   notifyDataSetChanged()│       │   计算差异并更新      │
 *   │ 全量刷新，性能差      │         │ 增量刷新，性能好      │
 *   │ 需要自己写数据管理    │         │ submitList() 一行搞定 │
 *   └──────────────────────┘         └──────────────────────┘
 *
 * ListAdapter 的优势：
 * 1. 不需要自己维护数据列表（内部通过 AsyncListDiffer 管理）
 * 2. DiffUtil 在后台线程计算差异，不阻塞 UI
 * 3. 自动执行最小化的刷新操作（添加/删除/移动动画）
 * 4. 只需调用 submitList(newList) 即可更新数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、DiffUtil 工作原理
 * ═══════════════════════════════════════════════════════════════════
 *
 * DiffUtil 使用 Eugene W. Myers 的差异算法来计算两个列表之间的
 * 最小修改操作集。它通过两个回调方法判断差异：
 *
 *   areItemsTheSame(old, new)  → 判断是否是"同一条"数据（用 ID 比较）
 *   areContentsTheSame(old, new) → 判断同一条数据的内容是否变化
 *
 *   旧列表: [A, B, C]        新列表: [A, C, D]
 *                    ↓ DiffUtil 计算
 *   结果: A 不变, B 删除, C 不变, D 新增
 *   → 只对 B 和 D 执行动画，A 和 C 不刷新
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、与其他适配器的对比
 * ═══════════════════════════════════════════════════════════════════
 *
 *   适配器                  基类              数据管理    使用场景
 *   ─────────────────────────────────────────────────────────────
 *   BulletinAdapter         ListAdapter       submitList  公告列表
 *   StrategyCardAdapter     RecyclerView.Adapter 手动管理  社区攻略列表
 *   AugmentListAdapter      RecyclerView.Adapter 手动管理  符文水平列表
 *
 *   BulletinAdapter 使用 ListAdapter 因为公告列表需要频繁更新
 *   （下拉刷新、类型筛选切换），DiffUtil 可以提供流畅的动画效果。
 *
 * @see BulletinUiModel          公告的 UI 数据模型
 * @see BulletinListFragment     使用此适配器的公告列表页
 */
public class BulletinAdapter extends ListAdapter<BulletinUiModel, BulletinAdapter.BulletinViewHolder> {

    /**
     * 公告点击监听器
     *
     * 当用户点击公告卡片时，通过此监听器通知 Fragment，
     * Fragment 收到回调后跳转到公告详情页。
     *
     * 为什么用接口而不是直接在 Adapter 中跳转？
     * ── 适配器只负责"展示数据"，不应该知道"跳转到哪里"。
     *    这是关注点分离原则：Adapter 管展示，Fragment 管导航。
     *    这样 Adapter 可以在不同场景复用（如弹窗中选择公告）。
     */
    private OnBulletinClickListener listener;

    /**
     * 公告点击回调接口
     *
     * Fragment 实现此接口来处理公告点击事件：
     *
     *   adapter.setOnBulletinClickListener(bulletin -> {
     *       BulletinDetailFragment detail = BulletinDetailFragment.newInstance(bulletin.getId());
     *       // 执行页面跳转...
     *   });
     */
    public interface OnBulletinClickListener {
        /**
         * 公告点击事件
         *
         * @param bulletin 被点击的公告数据，包含 ID 等信息，
         *                 Fragment 用 ID 来加载详情
         */
        void onBulletinClick(BulletinUiModel bulletin);
    }

    /**
     * 构造函数
     *
     * 调用父类 ListAdapter 的构造函数，传入 DiffUtil 回调。
     * ListAdapter 内部会使用这个回调来计算列表差异。
     *
     * 为什么 DiffCallback 在构造函数中传入？
     * ── ListAdapter 内部使用 AsyncListDiffer 来管理数据，
     *    AsyncListDiffer 需要 DiffUtil.ItemCallback 来判断
     *    列表项是否相同、内容是否变化。
     *    在构造时就传入，确保后续所有 submitList 调用
     *    都使用同一个差异计算逻辑。
     */
    public BulletinAdapter() {
        super(new BulletinDiffCallback());
    }

    /**
     * 设置公告点击监听器
     *
     * @param listener 点击监听器，由 BulletinListFragment 提供
     */
    public void setOnBulletinClickListener(OnBulletinClickListener listener) {
        this.listener = listener;
    }

    /**
     * 创建 ViewHolder
     *
     * ═══════════════════════════════════════════════════════════
     * ViewHolder 模式详解
     * ═══════════════════════════════════════════════════════════
     *
     * RecyclerView 的核心优化就是 ViewHolder 模式：
     * - onCreateViewHolder：创建 ViewHolder（包含视图引用）
     * - onBindViewHolder：将数据绑定到 ViewHolder 的视图上
     *
     * 为什么分开？
     * ── 创建视图（inflate）是非常耗时的操作（涉及 XML 解析），
     *    但绑定数据（setText）很快。RecyclerView 通过复用
     *    ViewHolder 来避免重复创建视图：
     *
     *    滑动时：
     *    ┌──────────────────────────────────────────────┐
     *    │ 屏幕上方的 ViewHolder 被移出屏幕              │
     *    │         ↓                                     │
     *    │ 放入回收池（RecycledViewPool）                │
     *    │         ↓                                     │
     *    │ 从回收池取出，重新绑定数据（onBindViewHolder）│
     *    │         ↓                                     │
     *    │ 显示在屏幕下方                                │
     *    └──────────────────────────────────────────────┘
     *
     * 这样每个列表项只需要创建一次视图，后续全部复用。
     *
     * @param parent   父容器，即 RecyclerView 本身
     * @param viewType 视图类型（本适配器只有一种类型，忽略此参数）
     * @return 新创建的 BulletinViewHolder
     */
    @NonNull
    @Override
    public BulletinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBulletinBinding binding = ItemBulletinBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BulletinViewHolder(binding);
    }

    /**
     * 将数据绑定到 ViewHolder
     *
     * 此方法在以下时机被调用：
     * 1. ViewHolder 首次显示在屏幕上
     * 2. ViewHolder 被回收复用后需要显示新数据
     * 3. DiffUtil 检测到数据变化需要更新
     *
     * 注意：此方法应尽量快速执行，不要做耗时操作，
     * 否则会导致列表滑动卡顿。
     *
     * @param holder   需要绑定数据的 ViewHolder
     * @param position 列表中的位置，通过 getItem(position) 获取数据
     */
    @Override
    public void onBindViewHolder(@NonNull BulletinViewHolder holder, int position) {
        BulletinUiModel bulletin = getItem(position);
        holder.bind(bulletin);
    }

    /**
     * 公告列表项 ViewHolder
     *
     * ViewHolder 持有列表项视图中各个 UI 组件的引用，
     * 避免每次绑定数据时都调用 findViewById()。
     *
     * 为什么用 static class？
     * ── 声明为 static 内部类意味着它不持有外部类（Adapter）的
     *    隐式引用。如果非 static，ViewHolder 会持有 Adapter 的
     *    引用，而 Adapter 又持有 Activity 的 Context，
     *    这可能导致内存泄漏。
     *
     * 为什么用 ViewBinding 而不是 findViewById？
     * ── ViewBinding 在编译时生成类型安全的视图引用，
     *    不需要强制类型转换，也不会因为 ID 拼写错误而崩溃。
     *    findViewById 是运行时查找，更容易出错。
     */
    class BulletinViewHolder extends RecyclerView.ViewHolder {

        /**
         * 视图绑定对象
         *
         * 包含 item_bulletin.xml 布局中所有带 id 的视图引用：
         * - binding.imageBulletin   → 公告封面图片
         * - binding.textPinnedBadge → 置顶标签
         * - binding.textTypeBadge   → 类型标签
         * - binding.textTitle       → 公告标题
         * - binding.textDate        → 发布日期
         */
        private final ItemBulletinBinding binding;

        /**
         * 构造函数
         *
         * @param binding 视图绑定对象，由 onCreateViewHolder 传入
         */
        BulletinViewHolder(ItemBulletinBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 将公告数据绑定到视图
         *
         * ═══════════════════════════════════════════════════════════
         * 绑定过程详解
         * ═══════════════════════════════════════════════════════════
         *
         * 步骤 1 ── 设置标题
         *   bulletin.getTitle() → "v2.1.0 版本更新公告"
         *   binding.textTitle.setText("v2.1.0 版本更新公告")
         *
         * 步骤 2 ── 设置类型标签
         *   bulletin.getTypeDisplay() → "版本更新"
         *   binding.textTypeBadge.setText("版本更新")
         *
         * 步骤 3 ── 设置日期
         *   bulletin.getCreatedAt() → "2026-05-20"
         *   binding.textDate.setText("2026-05-20")
         *
         * 步骤 4 ── 设置置顶标签
         *   bulletin.isPinned() == true  → VISIBLE（显示"置顶"标签）
         *   bulletin.isPinned() == false → GONE（隐藏，不占空间）
         *
         * 步骤 5 ── 加载封面图片
         *   有图片 → Glide 加载并显示
         *   无图片 → 不处理（布局中 ImageView 默认隐藏或占位）
         *
         * 步骤 6 ── 设置点击事件
         *   点击整个卡片 → 调用 listener.onBulletinClick(bulletin)
         *
         * @param bulletin 公告数据
         */
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

    /**
     * DiffUtil 回调类 ── 定义公告列表的差异计算规则
     *
     * ═══════════════════════════════════════════════════════════
     * DiffUtil.ItemCallback 的两个方法
     * ═══════════════════════════════════════════════════════════
     *
     * areItemsTheSame(old, new)：
     * ── 判断两个对象是否代表"同一条数据"。
     *    通常用唯一标识（ID）来比较。
     *    返回 true → 可能内容变了，需要进一步检查
     *    返回 false → 确定是不同的数据项，执行添加/删除动画
     *
     * areContentsTheSame(old, new)：
     * ── 在 areItemsTheSame 返回 true 时调用。
     *    判断同一条数据的内容是否发生了变化。
     *    返回 true → 内容没变，不需要刷新这个列表项
     *    返回 false → 内容变了，调用 onBindViewHolder 刷新
     *
     * 为什么 areContentsTheSame 只比较部分字段？
     * ── 完整比较所有字段理论上更准确，但：
     *    1. 大部分字段（如正文内容 content）在列表页不显示，
     *       变化了也不需要刷新卡片视图
     *    2. 只比较影响卡片显示的字段（id、title、pinned），
     *       可以减少不必要的刷新，提升性能
     *    3. 如果未来卡片显示更多字段，需要同步更新此方法
     *
     * 为什么声明为 static class？
     * ── 不需要访问外部类 Adapter 的实例变量，
     *    static 类更轻量，不会持有外部类引用。
     */
    static class BulletinDiffCallback extends DiffUtil.ItemCallback<BulletinUiModel> {

        /**
         * 判断两个列表项是否代表同一条公告
         *
         * 使用 ID 比较：如果两条公告的 ID 相同，
         * 说明它们是同一条公告（可能内容有更新）。
         *
         * @param oldItem 旧列表中的公告数据
         * @param newItem 新列表中的公告数据
         * @return ID 相同返回 true，否则返回 false
         */
        @Override
        public boolean areItemsTheSame(@NonNull BulletinUiModel oldItem, @NonNull BulletinUiModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /**
         * 判断同一条公告的内容是否发生变化
         *
         * 只比较影响卡片显示的字段：
         * - id：标识是否同一条数据
         * - title：标题变化需要刷新卡片
         * - isPinned：置顶状态变化需要显示/隐藏标签
         *
         * 未比较的字段（content、imageUrl、type 等）：
         * ── 这些字段在列表卡片中不直接显示或变化较少，
         *    如果它们变了但 title 和 pinned 没变，
         *    卡片外观不会变化，无需刷新。
         *
         * @param oldItem 旧列表中的公告数据
         * @param newItem 新列表中的公告数据
         * @return 内容完全相同返回 true，有变化返回 false
         */
        @Override
        public boolean areContentsTheSame(@NonNull BulletinUiModel oldItem, @NonNull BulletinUiModel newItem) {
            return oldItem.getId() == newItem.getId()
                    && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.isPinned() == newItem.isPinned();
        }
    }
}
