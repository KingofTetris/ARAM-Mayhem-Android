package com.aram.mayhem.ui.widget;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 分页滚动监听器（核心UI模块）
 *
 * 功能：RecyclerView 滚动到底部时自动触发加载更多回调
 * 触发条件：滚动方向向下 + 可见项数 + 首个可见位置 >= 总项数 + 非加载中 + 非最后一页
 * 用途：英雄列表、符文列表、攻略列表的分页加载场景
 * 注意：需要子类实现 isLoading()、isLastPage()、onLoadMore() 三个抽象方法
 */
public abstract class PaginationScrollListener extends RecyclerView.OnScrollListener {

    /** RecyclerView 布局管理器 */
    private final LinearLayoutManager layoutManager;
    /** 每页数据量（用于判断是否达到分页条件） */
    private final int pageSize;

    /**
     * 构造函数
     *
     * @param layoutManager RecyclerView 的 LinearLayoutManager
     * @param pageSize      每页数据量
     */
    public PaginationScrollListener(@NonNull LinearLayoutManager layoutManager, int pageSize) {
        this.layoutManager = layoutManager;
        this.pageSize = pageSize;
    }

    /**
     * 滚动监听回调
     *
     * @param recyclerView RecyclerView 实例
     * @param dx           X 轴滚动偏移
     * @param dy           Y 轴滚动偏移
     */
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        // 只处理向下滚动（dy > 0）
        if (dy <= 0) return;

        // 获取当前滚动状态
        int visibleItemCount = layoutManager.getChildCount();      // 当前可见项数
        int totalItemCount = layoutManager.getItemCount();          // 总项数
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition(); // 首个可见位置

        // 判断是否触发加载更多：
        // 1. 非加载中（避免重复请求）
        // 2. 非最后一页
        // 3. 可见项 + 首个可见位置 >= 总项数（滚动到底部）
        // 4. 首个可见位置 >= 0（有效位置）
        // 5. 总项数 >= 每页大小（至少有一页数据）
        if (!isLoading() && !isLastPage()) {
            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0
                    && totalItemCount >= pageSize) {
                // 触发加载更多回调
                onLoadMore();
            }
        }
    }

    /**
     * 判断是否正在加载中（由子类实现）
     *
     * @return true 表示正在加载，false 表示空闲
     */
    public abstract boolean isLoading();

    /**
     * 判断是否为最后一页（由子类实现）
     *
     * @return true 表示已是最后一页，false 表示还有更多数据
     */
    public abstract boolean isLastPage();

    /**
     * 加载更多回调（由子类实现）
     * 子类需在此方法中发起网络请求获取下一页数据
     */
    public abstract void onLoadMore();
}
