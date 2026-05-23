package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.aram.mayhem.ui.model.BulletinUiModel;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * 公告轮播图组件 ── 首页顶部公告自动轮播展示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * BulletinCarouselView 是首页顶部的公告轮播图，用户打开 App 第一眼看到的内容。
 * 它自动每隔 5 秒切换一条公告，用户也可以手动左右滑动切换。
 *
 * 视觉结构：
 * ┌─────────────────────────────────┐
 * │                                 │
 * │     公告图片（ViewPager2）       │  ← 占据大部分空间
 * │                                 │
 * ├─────────────────────────────────┤
 * │       ● ○ ○ ○                   │  ← 底部指示器小圆点
 * └─────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、核心技术：ViewPager2
 * ═══════════════════════════════════════════════════════════════════
 *
 * ViewPager2 是 Android 官方的滑动页面组件（替代旧的 ViewPager）：
 * - 基于 RecyclerView 实现，性能更好
 * - 支持垂直和水平滑动
 * - 支持 RTL（从右到左）布局
 * - 内置页面切换回调
 *
 * 本组件使用 ViewPager2 实现水平滑动的图片轮播
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、自动滚动机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 自动滚动使用 Handler + Runnable 实现：
 *
 * 1. autoScrollHandler：主线程 Handler，用于延迟执行滚动任务
 * 2. autoScrollRunnable：滚动任务，切换到下一页后再次启动自己
 * 3. AUTO_SCROLL_INTERVAL = 5000ms：5 秒切换一次
 *
 * 循环逻辑：
 * ┌──────────┐     ┌──────────┐     ┌──────────┐
 * │ 等待 5 秒 │────→│ 切换下一页 │────→│ 等待 5 秒 │──→ ...
 * └──────────┘     └──────────┘     └──────────┘
 *
 * 生命周期管理：
 * - onAttachedToWindow()：视图显示时启动自动滚动
 * - onDetachedFromWindow()：视图隐藏时停止自动滚动（防止内存泄漏）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、指示器（底部小圆点）
 * ═══════════════════════════════════════════════════════════════════
 *
 * 指示器是一排小圆点，告诉用户当前在第几页、总共有几页：
 * - 选中状态：蓝色圆点（0xFF1976D2）
 * - 未选中状态：灰色圆点（0xFFBDBDBD）
 *
 * 页面切换时，通过 ViewPager2.OnPageChangeCallback 同步更新指示器
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、内存泄漏防护
 * ═══════════════════════════════════════════════════════════════════
 *
 * Handler + Runnable 如果不正确处理会导致内存泄漏：
 * - Runnable 持有外部类（BulletinCarouselView）的引用
 * - 如果 View 被销毁但 Runnable 还在消息队列中，View 无法被 GC 回收
 *
 * 防护措施：
 * - onDetachedFromWindow() 中调用 stopAutoScroll() 移除所有回调
 * - startAutoScroll() 中先 stopAutoScroll() 防止重复注册
 */
public class BulletinCarouselView extends LinearLayout {

    /** ViewPager2 实例 ── 轮播内容容器，负责页面滑动和复用 */
    private ViewPager2 viewPager;

    /** 指示器容器 ── 底部小圆点的父布局（水平 LinearLayout） */
    private LinearLayout indicatorContainer;

    /** 自动滚动 Handler ── 绑定主线程 Looper，用于延迟执行滚动任务 */
    private Handler autoScrollHandler;

    /** 自动滚动任务 ── 切换到下一页后重新启动自己，形成循环 */
    private Runnable autoScrollRunnable;

    /** 自动滚动间隔时间 ── 5000 毫秒 = 5 秒 */
    private static final long AUTO_SCROLL_INTERVAL = 5000;

    /** 公告数据列表 ── 存储当前展示的所有公告 */
    private List<BulletinUiModel> bulletins = new ArrayList<>();

    /** 公告点击回调监听器 ── 点击轮播图时通知上层（Fragment） */
    private OnBulletinClickListener listener;

    /**
     * 公告点击事件监听器接口 ── Fragment 实现此接口处理点击跳转
     *
     * @param bulletin 被点击的公告数据模型
     */
    public interface OnBulletinClickListener {
        void onBulletinClick(BulletinUiModel bulletin);
    }

    /**
     * 构造函数（代码调用）── 在 Java 代码中动态创建时使用
     * @param context 上下文
     */
    public BulletinCarouselView(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）── 在 XML 中使用 <BulletinCarouselView> 时调用
     * @param context 上下文
     * @param attrs   XML 属性集合
     */
    public BulletinCarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数（带样式）── 支持自定义样式属性
     * @param context      上下文
     * @param attrs        XML 属性集合
     * @param defStyleAttr 默认样式属性
     */
    public BulletinCarouselView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件 ── 创建 ViewPager2、指示器容器和自动滚动机制
     *
     * 布局结构（垂直 LinearLayout）：
     * ┌─────────────────────────────┐
     * │  ViewPager2（weight=1）      │  ← 轮播内容，占满剩余空间
     * ├─────────────────────────────┤
     * │  指示器容器（wrap_content）   │  ← 底部小圆点
     * └─────────────────────────────┘
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 设置垂直方向布局（ViewPager2 在上，指示器在下）
        setOrientation(VERTICAL);

        // 创建 ViewPager2（轮播内容区域）
        // LayoutParams.MATCH_PARENT, 0, 1f：
        // - 宽度：填满父容器
        // - 高度：0（因为使用 weight）
        // - weight：1（占据剩余空间，指示器只占 wrap_content）
        viewPager = new ViewPager2(context);
        viewPager.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(viewPager);

        // 创建指示器容器（水平居中排列小圆点）
        indicatorContainer = new LinearLayout(context);
        indicatorContainer.setOrientation(HORIZONTAL);
        indicatorContainer.setGravity(android.view.Gravity.CENTER);
        LayoutParams indicatorParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        indicatorParams.topMargin = 8; // 与 ViewPager2 间距 8px
        indicatorContainer.setLayoutParams(indicatorParams);
        addView(indicatorContainer);

        // 初始化自动滚动机制
        // Handler(Looper.getMainLooper())：绑定主线程，确保 UI 操作在主线程执行
        autoScrollHandler = new Handler(Looper.getMainLooper());

        // 自动滚动任务：切换到下一页后重新启动自己
        autoScrollRunnable = () -> {
            // 只有多于 1 条数据时才自动滚动（避免无意义的刷新）
            if (bulletins.size() > 1) {
                // 计算下一个位置（循环滚动）
                // (currentItem + 1) % size 实现首尾循环：
                // - 最后一页的下一页是第一页
                // - 例如：3 条数据，2 → 0 → 1 → 2 → 0 ...
                int nextItem = (viewPager.getCurrentItem() + 1) % bulletins.size();
                // setCurrentItem(nextItem, true)：
                // - nextItem：目标页位置
                // - true：显示平滑滚动动画
                viewPager.setCurrentItem(nextItem, true);
            }
            // 继续启动下一次滚动（形成循环）
            startAutoScroll();
        };
    }

    /**
     * 设置公告数据列表 ── Fragment 获取数据后调用此方法
     *
     * 执行流程：
     * 1. 保存数据到 bulletins 字段
     * 2. 创建并设置适配器
     * 3. 创建指示器小圆点
     * 4. 默认选中第一个指示器
     * 5. 注册页面切换回调，同步更新指示器
     *
     * @param data 公告数据列表（可为 null，内部会转为空列表）
     */
    public void setBulletins(List<BulletinUiModel> data) {
        // 防止 null 导致 NPE
        this.bulletins = data != null ? data : new ArrayList<>();
        // 设置适配器（将数据绑定到 ViewPager2）
        setupAdapter();
        // 设置指示器（根据数据数量创建小圆点）
        setupIndicators();
        // 默认选中第一个指示器
        updateIndicatorSelection(0);

        // 监听页面切换，同步更新指示器状态
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // 页面切换时更新指示器选中状态
                updateIndicatorSelection(position);
            }
        });
    }

    /**
     * 设置公告点击回调监听器
     *
     * @param listener 点击回调实现（通常是 Fragment）
     */
    public void setOnBulletinClickListener(OnBulletinClickListener listener) {
        this.listener = listener;
    }

    /**
     * 启动自动滚动 ── 延迟 5 秒后执行滚动任务
     *
     * 调用时机：
     * - onAttachedToWindow()：视图显示时
     * - autoScrollRunnable 执行完毕后：形成循环
     *
     * 注意：先调用 stopAutoScroll() 防止重复注册
     */
    public void startAutoScroll() {
        // 先停止之前的任务（防止重复执行）
        stopAutoScroll();
        // 延迟 AUTO_SCROLL_INTERVAL 毫秒后执行滚动任务
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_INTERVAL);
    }

    /**
     * 停止自动滚动 ── 从消息队列中移除滚动任务
     *
     * 调用时机：
     * - onDetachedFromWindow()：视图隐藏时
     * - startAutoScroll() 内部：防止重复注册
     *
     * 不调用此方法会导致内存泄漏！
     */
    public void stopAutoScroll() {
        // removeCallbacks() 从消息队列中移除未执行的 Runnable
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    /**
     * 设置轮播适配器 ── 将公告数据绑定到 ViewPager2
     */
    private void setupAdapter() {
        CarouselAdapter adapter = new CarouselAdapter();
        viewPager.setAdapter(adapter);
    }

    /**
     * 设置指示器 ── 根据公告数量创建底部小圆点
     *
     * 每个小圆点是一个 View，大小 8dp × 8dp，间距 4dp
     * 初始颜色为灰色（未选中），后续由 updateIndicatorSelection() 更新
     */
    private void setupIndicators() {
        // 先清空之前的指示器（防止数据更新时重复创建）
        indicatorContainer.removeAllViews();

        // 根据公告数量创建指示器小圆点
        for (int i = 0; i < bulletins.size(); i++) {
            View dot = new View(getContext());
            // 圆点大小：8dp 转换为像素
            // density：屏幕密度系数（1dp = density px）
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            // 圆点间距：4dp
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);
            indicatorContainer.addView(dot);
        }
    }

    /**
     * 更新指示器选中状态 ── 切换小圆点颜色
     *
     * @param position 当前选中位置（0 开始）
     */
    private void updateIndicatorSelection(int position) {
        // 选中状态颜色：蓝色（Material Design Blue 700）
        int activeColor = 0xFF1976D2;
        // 未选中状态颜色：灰色（Material Design Grey 400）
        int inactiveColor = 0xFFBDBDBD;

        // 遍历所有指示器小圆点
        for (int i = 0; i < indicatorContainer.getChildCount(); i++) {
            View dot = indicatorContainer.getChildAt(i);
            // 当前位置显示蓝色，其他位置显示灰色
            dot.setBackgroundColor(i == position ? activeColor : inactiveColor);
        }
    }

    /**
     * 视图绑定到窗口时启动自动滚动
     *
     * 这是 View 的生命周期回调，当视图被添加到 Window 时调用
     * 在这里启动自动滚动，确保用户看到轮播图时就开始自动播放
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAutoScroll();
    }

    /**
     * 视图从窗口分离时停止自动滚动 ── 防止内存泄漏
     *
     * 这是 View 的生命周期回调，当视图从 Window 移除时调用
     * 必须在这里停止自动滚动，否则：
     * - Handler 仍然持有 View 的引用
     * - View 无法被垃圾回收
     * - 导致 Activity/Fragment 泄漏
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoScroll();
    }

    /**
     * 轮播适配器内部类 ── 将公告图片绑定到 ViewPager2 的每一页
     *
     * 为什么是内部类？
     * - 只在 BulletinCarouselView 内部使用
     * - 可以直接访问外部类的 bulletins 和 listener
     *
     * 为什么用 RecyclerView.Adapter？
     * - ViewPager2 内部基于 RecyclerView 实现
     * - 必须使用 RecyclerView.Adapter 作为适配器
     */
    private class CarouselAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CarouselViewHolder> {

        /**
         * 创建 ViewHolder ── 每个轮播项是一个全屏 ImageView
         *
         * @param parent   ViewPager2 内部的 RecyclerView
         * @param viewType 视图类型（本适配器只有一种）
         * @return CarouselViewHolder 实例
         */
        @NonNull
        @Override
        public CarouselViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            // 创建全屏 ImageView
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            // CENTER_CROP：等比缩放填满 ImageView，多余部分裁剪
            // 这是轮播图最常用的缩放模式，确保图片填满不留白边
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new CarouselViewHolder(imageView);
        }

        /**
         * 绑定数据到 ViewHolder ── 加载公告图片并设置点击事件
         *
         * @param holder   视图持有者
         * @param position 当前页位置
         */
        @Override
        public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
            BulletinUiModel bulletin = bulletins.get(position);

            // 如果有图片 URL，使用 Glide 加载
            if (bulletin.getImageUrl() != null && !bulletin.getImageUrl().isEmpty()) {
                Glide.with(getContext())
                        .load(bulletin.getImageUrl())
                        .centerCrop()
                        .into(holder.imageView);
            }

            // 设置点击事件：回调给上层（Fragment）处理跳转
            holder.imageView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBulletinClick(bulletin);
                }
            });
        }

        /**
         * 获取数据数量
         * @return 公告数量
         */
        @Override
        public int getItemCount() {
            return bulletins.size();
        }
    }

    /**
     * 轮播视图持有者 ── 持有单个轮播页的 ImageView
     *
     * 为什么用 static class？
     * - 静态内部类不持有外部类引用
     * - 避免通过 ViewHolder 泄漏外部类
     * - 但这里已经是 private 内部类，影响不大
     */
    private static class CarouselViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        /** 轮播页图片视图 */
        ImageView imageView;

        /**
         * @param imageView 轮播页的 ImageView
         */
        CarouselViewHolder(ImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }
    }
}
