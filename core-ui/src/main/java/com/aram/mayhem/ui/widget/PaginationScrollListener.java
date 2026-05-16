package com.aram.mayhem.ui.widget;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 分页滚动监听器
 *
 * 功能：RecyclerView 滚动到底部时触发加载更多回调
 * 用途：英雄列表、符文列表、攻略列表的分页加载
 */
public abstract class PaginationScrollListener extends RecyclerView.OnScrollListener {

    private final LinearLayoutManager layoutManager;
    private final int pageSize;

    public PaginationScrollListener(@NonNull LinearLayoutManager layoutManager, int pageSize) {
        this.layoutManager = layoutManager;
        this.pageSize = pageSize;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (dy <= 0) return;

        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

        if (!isLoading() && !isLastPage()) {
            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0
                    && totalItemCount >= pageSize) {
                onLoadMore();
            }
        }
    }

    public abstract boolean isLoading();

    public abstract boolean isLastPage();

    public abstract void onLoadMore();
}
