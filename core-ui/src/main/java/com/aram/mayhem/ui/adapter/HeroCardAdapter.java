package com.aram.mayhem.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.ui.model.HeroUiModel;
import com.aram.mayhem.ui.databinding.ItemHeroCardBinding;
import com.bumptech.glide.Glide;

/**
 * 英雄卡片列表适配器 ── 将 HeroUiModel 数据绑定到 RecyclerView 卡片视图
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroCardAdapter 是 RecyclerView 的适配器，负责：
 * 1. 创建卡片视图（onCreateViewHolder）
 * 2. 将数据绑定到视图（onBindViewHolder）
 * 3. 计算数据差异实现高效刷新（DiffUtil）
 *
 * 简单来说：它把 HeroUiModel 里的数据"贴"到 item_hero_card.xml 布局上
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么用 ListAdapter 而不是 RecyclerView.Adapter？
 * ═══════════════════════════════════════════════════════════════════
 *
 * ListAdapter 是 RecyclerView.Adapter 的子类，内置了 DiffUtil 支持：
 * - 传统 Adapter：调用 notifyDataSetChanged() 全量刷新（闪烁+卡顿）
 * - ListAdapter：调用 submitList() 自动计算差异，只刷新变化的项
 *
 * 性能对比：
 * - 20 条数据中 1 条变化：传统方式刷新 20 项，ListAdapter 只刷新 1 项
 * - 用户感知：传统方式列表会闪烁，ListAdapter 平滑过渡
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * ViewModel.heroList (LiveData<List<HeroUiModel>>)
 *       │
 *       ▼  Fragment 中观察 LiveData
 * adapter.submitList(heroList)
 *       │
 *       ▼  ListAdapter 内部 DiffUtil 计算
 * onCreateViewHolder / onBindViewHolder
 *       │
 *       ▼  ViewHolder.bind(hero)
 * 视图更新（文字、图片、颜色等）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、ViewBinding 说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * ItemHeroCardBinding 是自动生成的绑定类，对应 item_hero_card.xml 布局。
 * 它的作用是替代 findViewById，通过编译时生成的绑定代码直接访问视图：
 *
 * 传统方式：View view = findViewById(R.id.text_hero_name);  // 运行时查找，可能返回 null
 * ViewBinding：binding.textHeroName                         // 编译时生成，类型安全
 *
 * binding 中的视图名称规则：
 * - XML 中的 android:id="@+id/text_hero_name"
 * - Java 中的 binding.textHeroName（下划线转驼峰）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、Glide 图片加载说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * Glide 是 Android 最流行的图片加载库，核心功能：
 * 1. 自动管理图片生命周期（Activity 销毁时自动取消加载）
 * 2. 三级缓存：内存 → 磁盘 → 网络
 * 3. 自动缩放和裁剪
 * 4. 占位图和错误图支持
 *
 * 链式调用说明：
 * Glide.with(context)          // 绑定生命周期
 *   .load(url)                 // 设置图片 URL
 *   .placeholder(R.drawable.x) // 加载中显示的占位图
 *   .error(R.drawable.x)       // 加载失败显示的错误图
 *   .circleCrop()              // 圆形裁剪（头像常用）
 *   .into(imageView)           // 目标 ImageView
 */
public class HeroCardAdapter extends ListAdapter<HeroUiModel, HeroCardAdapter.ViewHolder> {

    /**
     * 英雄点击事件监听器 ── 外部（通常是 Fragment）实现此接口处理点击跳转
     *
     * 使用方式：
     * <pre>
     * adapter.setOnHeroClickListener(hero -> {
     *     // 跳转到英雄详情页
     *     NavHostFragment.findNavController(this)
     *         .navigate(HeroListFragmentDirections.actionToDetail(hero.getId()));
     * });
     * </pre>
     */
    private OnHeroClickListener listener;

    /**
     * 英雄点击事件监听器接口
     *
     * 为什么用接口而不是直接传 Lambda？
     * - 接口可以定义多个方法（如 onClick、onLongClick）
     * - 接口有明确的类型名称，代码可读性更好
     * - Fragment 实现接口时可以看到完整的方法签名
     *
     * @param hero 被点击的英雄数据模型
     */
    public interface OnHeroClickListener {
        void onHeroClick(@NonNull HeroUiModel hero);
    }

    /**
     * 构造函数 ── 初始化 DiffUtil 回调
     *
     * super(new HeroDiffCallback()) 的含义：
     * - ListAdapter 需要一个 DiffUtil.ItemCallback 来判断数据差异
     * - HeroDiffCallback 是自定义的差异计算规则
     * - 在构造时传入，后续 submitList() 会自动调用
     */
    public HeroCardAdapter() {
        super(new HeroDiffCallback());
    }

    /**
     * 设置英雄点击事件监听器
     *
     * @param listener 点击回调实现（通常是 Fragment）
     */
    public void setOnHeroClickListener(OnHeroClickListener listener) {
        this.listener = listener;
    }

    /**
     * 创建 ViewHolder 实例 ── 当 RecyclerView 需要新的卡片视图时调用
     *
     * 调用时机：
     * - 首次加载时，RecyclerView 没有可复用的 ViewHolder
     * - 滚动时，如果可见区域增大需要更多卡片
     *
     * 不会每次滚动都调用！RecyclerView 会复用滑出屏幕的 ViewHolder
     *
     * @param parent   RecyclerView 父容器（用于获取 Context 和 LayoutParams）
     * @param viewType 视图类型（本适配器只有一种类型，忽略此参数）
     * @return ViewHolder 实例，包含卡片视图的引用
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ItemHeroCardBinding.inflate()：从 XML 布局创建视图并生成绑定对象
        // LayoutInflater.from(parent.getContext())：获取布局填充器
        // parent：父容器（用于生成正确的 LayoutParams）
        // false：不立即添加到父容器（RecyclerView 会自行管理）
        ItemHeroCardBinding binding = ItemHeroCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    /**
     * 绑定数据到 ViewHolder ── 将 HeroUiModel 的数据显示到卡片视图上
     *
     * 调用时机：
     * - 新创建的 ViewHolder 首次显示
     * - 滑回屏幕的 ViewHolder 需要更新数据
     * - DiffUtil 检测到数据变化时
     *
     * 注意：此方法应尽量轻量，避免耗时操作
     * - 图片加载用 Glide（异步）
     * - 不要在这里做网络请求或数据库查询
     *
     * @param holder   视图持有者（包含卡片视图的引用）
     * @param position 当前数据项在列表中的位置
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // getItem(position)：ListAdapter 提供的方法，获取当前列表中指定位置的数据
        HeroUiModel hero = getItem(position);
        // 将数据绑定到视图
        holder.bind(hero);
    }

    /**
     * 视图持有者类 ── 持有单个英雄卡片的所有视图引用
     *
     * ViewHolder 模式的核心思想：
     * - 避免每次绑定数据时都调用 findViewById（耗时操作）
     * - 在构造时一次性找到所有子视图，后续直接使用引用
     * - RecyclerView 滑动时复用 ViewHolder，只更新数据不重新创建视图
     *
     * 为什么是内部类？
     * - 只有 HeroCardAdapter 需要使用它
     * - 可以直接访问外部类的 listener 等字段
     * - 逻辑上属于适配器的一部分
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        /** 布局绑定对象 ── 包含卡片中所有视图的引用 */
        private final ItemHeroCardBinding binding;

        /**
         * ViewHolder 构造函数
         *
         * @param binding 布局绑定对象（由 onCreateViewHolder 创建并传入）
         */
        ViewHolder(@NonNull ItemHeroCardBinding binding) {
            // super(binding.getRoot())：必须调用，将根视图传给 RecyclerView.ViewHolder
            // getRoot() 返回绑定布局的根视图（通常是 CardView 或 LinearLayout）
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定英雄数据到视图 ── 核心方法，将数据"贴"到 UI 上
         *
         * 执行步骤：
         * 1. 设置文字（名称、定位、胜率）
         * 2. 设置梯级标签
         * 3. 加载头像图片
         * 4. 设置版本陷阱标记
         * 5. 设置点击事件
         *
         * @param hero 英雄 UI 模型（包含所有需要显示的数据）
         */
        void bind(@NonNull HeroUiModel hero) {
            // 步骤 1：设置英雄名称（中文）
            // binding.textHeroName 对应 XML 中的 android:id="@+id/text_hero_name"
            binding.textHeroName.setText(hero.getNameZh());

            // 设置英雄定位（战士/法师/刺客等）
            binding.textHeroRole.setText(hero.getRole());

            // 设置胜率显示文本（如"胜率 52.3%"）
            // getWinRateDisplay() 是 HeroUiModel 自带的格式化方法
            binding.textWinRate.setText(hero.getWinRateDisplay());

            // 步骤 2：设置梯级标签（S+/S/A/B/C）
            // TierBadgeView 是自定义组件，会根据 Tier 自动设置颜色和文字
            binding.badgeTier.setTier(hero.getTier());

            // 步骤 3：使用 Glide 加载英雄头像（圆形裁剪）
            // Glide.with()：绑定 Context 生命周期
            // .load()：设置图片 URL
            // .placeholder()：加载中显示的默认图标
            // .error()：加载失败显示的默认图标
            // .circleCrop()：圆形裁剪（头像常用效果）
            // .into()：目标 ImageView
            Glide.with(binding.imageHeroAvatar.getContext())
                    .load(hero.getAvatarUrl())
                    .placeholder(com.aram.mayhem.ui.R.drawable.ic_heroes)
                    .error(com.aram.mayhem.ui.R.drawable.ic_heroes)
                    .circleCrop()
                    .into(binding.imageHeroAvatar);

            // 步骤 4：设置版本陷阱标记
            // 如果是陷阱英雄，显示红色边框警告
            if (hero.isTrap()) {
                // getRoot() 返回卡片根视图（MaterialCardView）
                // setStrokeColor() 设置卡片边框颜色
                // trap_warning 是红色警告色
                binding.getRoot().setStrokeColor(
                        binding.getRoot().getContext().getColor(
                                com.aram.mayhem.ui.R.color.trap_warning));
            } else {
                // 非陷阱英雄：移除边框颜色（设为透明）
                binding.getRoot().setStrokeColor(0);
            }

            // 步骤 5：设置点击事件
            // 点击整个卡片时，回调给上层（Fragment）处理跳转
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHeroClick(hero);
                }
            });
        }
    }

    /**
     * DiffUtil 回调类 ── 定义数据差异计算规则
     *
     * ═══════════════════════════════════════════════════════════════
     * DiffUtil 的工作原理：
     * ═══════════════════════════════════════════════════════════════
     *
     * 当调用 submitList(newList) 时，ListAdapter 会在后台线程执行：
     * 1. areItemsTheSame()：判断两个数据项是否代表同一个对象（通常比较 ID）
     * 2. areContentsTheSame()：如果是同一个对象，判断内容是否变化
     * 3. 根据比较结果决定：
     *    - 不同的对象 → 插入/删除动画
     *    - 相同对象内容变化 → 更新动画（只刷新变化的项）
     *    - 相同对象内容不变 → 不做任何操作
     *
     * 性能优化：
     * - areItemsTheSame() 先执行（快速判断，通常只比较 ID）
     * - areContentsTheSame() 只在 areItemsTheSame() 返回 true 时才执行
     * - 比较逻辑应尽量简单，避免复杂计算
     */
    static class HeroDiffCallback extends DiffUtil.ItemCallback<HeroUiModel> {

        /**
         * 判断是否为同一数据项 ── 通过 ID 比较
         *
         * 两个 HeroUiModel 的 id 相同 → 视为同一个英雄
         * 例如：旧列表的 id=157 和新列表的 id=157 是同一个英雄
         *
         * 为什么用 == 而不是 .equals()？
         * - id 是 long 基本类型，== 直接比较数值
         * - 如果 id 是 Long 包装类型，应该用 Objects.equals() 避免拆箱 NPE
         *
         * @param oldItem 旧列表中的数据项
         * @param newItem 新列表中的数据项
         * @return true 表示是同一个数据项（ID 相同）
         */
        @Override
        public boolean areItemsTheSame(@NonNull HeroUiModel oldItem, @NonNull HeroUiModel newItem) {
            return oldItem.getId() == newItem.getId();
        }

        /**
         * 判断数据项内容是否发生变化 ── 比较影响显示的关键字段
         *
         * 只比较 UI 上会变化的字段：
         * - id：标识（理论上不会变，但保险起见也比较）
         * - winRate：胜率会随版本更新变化
         * - tier：梯级会随胜率变化
         * - isTrap：陷阱标记会随版本更新变化
         *
         * 不需要比较的字段：
         * - nameZh/nameEn：英雄名称不会变
         * - role：定位不会变
         * - avatarUrl：头像 URL 不会变
         *
         * 为什么不全比较？
         * - 减少比较次数，提升性能
         * - 只比较可能变化的字段，减少不必要的视图刷新
         *
         * 注意：double 类型的 == 比较在浮点数场景可能有精度问题
         * 但胜率通常只有 2-4 位小数，== 比较足够精确
         *
         * @param oldItem 旧列表中的数据项
         * @param newItem 新列表中的数据项
         * @return true 表示内容相同，不需要刷新视图
         */
        @Override
        public boolean areContentsTheSame(@NonNull HeroUiModel oldItem, @NonNull HeroUiModel newItem) {
            return oldItem.getId() == newItem.getId()
                    && oldItem.getWinRate() == newItem.getWinRate()
                    && oldItem.getTier() == newItem.getTier()
                    && oldItem.isTrap() == newItem.isTrap();
        }
    }
}
