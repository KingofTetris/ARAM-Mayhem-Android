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
 * 公告轮播图组件（核心UI模块）
 *
 * 功能：首页顶部公告轮播展示，支持自动滚动和手动滑动
 * 自动滚动间隔：AUTO_SCROLL_INTERVAL = 5000ms（5秒）
 * 数据源：BulletinUiModel 列表（包含公告ID、标题、图片URL等）
 * 特性：ViewPager2 实现、底部指示器、生命周期自动启停
 * 关联：BulletinListFragment，用于首页公告展示
 */
public class BulletinCarouselView extends LinearLayout {

    /** ViewPager2 实例（轮播内容容器） */
    private ViewPager2 viewPager;
    /** 指示器容器（底部小圆点） */
    private LinearLayout indicatorContainer;
    /** 自动滚动 Handler（主线程） */
    private Handler autoScrollHandler;
    /** 自动滚动任务（Runnable） */
    private Runnable autoScrollRunnable;
    /** 自动滚动间隔时间（毫秒） */
    private static final long AUTO_SCROLL_INTERVAL = 5000;
    /** 公告数据列表 */
    private List<BulletinUiModel> bulletins = new ArrayList<>();
    /** 公告点击回调监听器 */
    private OnBulletinClickListener listener;

    /**
     * 公告点击事件监听器接口
     *
     * @param bulletin 被点击的公告数据模型
     */
    public interface OnBulletinClickListener {
        void onBulletinClick(BulletinUiModel bulletin);
    }

    /**
     * 构造函数（代码调用）
     *
     * @param context 上下文
     */
    public BulletinCarouselView(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     *
     * @param context 上下文
     * @param attrs   属性集合
     */
    public BulletinCarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数（带样式）
     *
     * @param context      上下文
     * @param attrs        属性集合
     * @param defStyleAttr 默认样式属性
     */
    public BulletinCarouselView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 设置垂直方向布局（ViewPager2 + 指示器）
        setOrientation(VERTICAL);
        
        // 创建 ViewPager2（轮播内容区域，占满剩余空间）
        viewPager = new ViewPager2(context);
        viewPager.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        addView(viewPager);

        // 创建指示器容器（水平居中）
        indicatorContainer = new LinearLayout(context);
        indicatorContainer.setOrientation(HORIZONTAL);
        indicatorContainer.setGravity(android.view.Gravity.CENTER);
        LayoutParams indicatorParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        indicatorParams.topMargin = 8;
        indicatorContainer.setLayoutParams(indicatorParams);
        addView(indicatorContainer);

        // 初始化自动滚动机制
        autoScrollHandler = new Handler(Looper.getMainLooper());
        autoScrollRunnable = () -> {
            // 只有多于1条数据时才自动滚动
            if (bulletins.size() > 1) {
                // 计算下一个位置（循环滚动）
                int nextItem = (viewPager.getCurrentItem() + 1) % bulletins.size();
                viewPager.setCurrentItem(nextItem, true);
            }
            // 继续启动下一次滚动
            startAutoScroll();
        };
    }

    /**
     * 设置公告数据列表
     *
     * @param data 公告数据列表
     */
    public void setBulletins(List<BulletinUiModel> data) {
        this.bulletins = data != null ? data : new ArrayList<>();
        // 设置适配器
        setupAdapter();
        // 设置指示器
        setupIndicators();
        // 默认选中第一个
        updateIndicatorSelection(0);

        // 监听页面切换，同步更新指示器状态
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateIndicatorSelection(position);
            }
        });
    }

    /**
     * 设置公告点击回调监听器
     *
     * @param listener 点击回调实现
     */
    public void setOnBulletinClickListener(OnBulletinClickListener listener) {
        this.listener = listener;
    }

    /**
     * 启动自动滚动
     */
    public void startAutoScroll() {
        // 先停止之前的任务（防止重复执行）
        stopAutoScroll();
        // 延迟执行下一次滚动
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_INTERVAL);
    }

    /**
     * 停止自动滚动
     */
    public void stopAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    /**
     * 设置轮播适配器
     */
    private void setupAdapter() {
        CarouselAdapter adapter = new CarouselAdapter();
        viewPager.setAdapter(adapter);
    }

    /**
     * 设置指示器（底部小圆点）
     */
    private void setupIndicators() {
        // 先清空之前的指示器（防止重复）
        indicatorContainer.removeAllViews();
        
        // 根据公告数量创建指示器小圆点
        for (int i = 0; i < bulletins.size(); i++) {
            View dot = new View(getContext());
            // 圆点大小（8dp 转换为像素）
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);
            indicatorContainer.addView(dot);
        }
    }

    /**
     * 更新指示器选中状态
     *
     * @param position 当前选中位置
     */
    private void updateIndicatorSelection(int position) {
        int activeColor = 0xFF1976D2;    // 选中状态颜色（蓝色）
        int inactiveColor = 0xFFBDBDBD;  // 未选中状态颜色（灰色）
        
        for (int i = 0; i < indicatorContainer.getChildCount(); i++) {
            View dot = indicatorContainer.getChildAt(i);
            // 当前位置显示选中颜色，其他显示未选中颜色
            dot.setBackgroundColor(i == position ? activeColor : inactiveColor);
        }
    }

    /**
     * 视图绑定到窗口时启动自动滚动（生命周期回调）
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAutoScroll();
    }

    /**
     * 视图从窗口分离时停止自动滚动（防止内存泄漏）
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoScroll();
    }

    /**
     * 轮播适配器内部类：负责将公告数据绑定到图片视图
     */
    private class CarouselAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CarouselViewHolder> {

        /**
         * 创建 ViewHolder（每个轮播项是一个 ImageView）
         *
         * @param parent   父容器
         * @param viewType 视图类型（本适配器仅支持一种）
         * @return ViewHolder 实例
         */
        @NonNull
        @Override
        public CarouselViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new CarouselViewHolder(imageView);
        }

        /**
         * 绑定数据到 ViewHolder
         *
         * @param holder   视图持有者
         * @param position 当前位置
         */
        @Override
        public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
            BulletinUiModel bulletin = bulletins.get(position);
            // 如果有图片 URL，使用 Glide 加载图片
            if (bulletin.getImageUrl() != null && !bulletin.getImageUrl().isEmpty()) {
                Glide.with(getContext())
                        .load(bulletin.getImageUrl())
                        .centerCrop()
                        .into(holder.imageView);
            }
            
            // 设置点击事件：回调给上层处理
            holder.imageView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBulletinClick(bulletin);
                }
            });
        }

        /**
         * 获取数据数量
         *
         * @return 公告数量
         */
        @Override
        public int getItemCount() {
            return bulletins.size();
        }
    }

    /**
     * 轮播视图持有者：持有单个轮播项的 ImageView
     */
    private static class CarouselViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        ImageView imageView;

        CarouselViewHolder(ImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }
    }
}
