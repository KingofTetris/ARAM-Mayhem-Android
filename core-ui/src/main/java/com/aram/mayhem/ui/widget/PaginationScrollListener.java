package com.aram.mayhem.ui.widget;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 分页滚动监听器 ── RecyclerView 滚动到底部时自动触发加载更多
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * PaginationScrollListener 是 RecyclerView 的滚动监听器，用于实现"无限滚动"：
 * - 用户向下滚动列表
 * - 滚动到接近底部时，自动加载下一页数据
 * - 新数据追加到列表末尾，用户可以继续滚动
 *
 * 这种模式也称为"无限列表"或"瀑布流加载"，几乎所有内容类 App 都使用。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、分页加载流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
 * │ 用户滚动到底 │────→│ 触发加载更多 │────→│ 网络请求下一页│
 * └─────────────┘     └─────────────┘     └─────────────┘
 *                                                │
 *                                                ▼
 * ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
 * │ 列表追加新数据│←────│ ViewModel 处理│←────│ 服务器返回数据│
 * └─────────────┘     └─────────────┘     └─────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、触发条件（5 个条件全部满足才触发）
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. dy > 0：向下滚动（用户主动滚动，不是程序滚动）
 * 2. !isLoading()：当前没有正在进行的加载请求（防止重复请求）
 * 3. !isLastPage()：还有更多数据可加载（不是最后一页）
 * 4. 可见项数 + 首个可见位置 >= 总项数：已滚动到接近底部
 * 5. 总项数 >= pageSize：至少有一页数据（避免空列表触发加载）
 *
 * 为什么条件 4 用 ">=" 而不是 "=="？
 * - 滚动速度很快时，可能跳过精确的底部位置
 * - ">=" 确保即使跳过也能触发加载
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、为什么用抽象类而不是接口？
 * ═══════════════════════════════════════════════════════════════════
 *
 * isLoading()、isLastPage()、onLoadMore() 三个方法需要访问 ViewModel 的状态：
 * - isLoading()：检查 ViewModel 是否正在加载
 * - isLastPage()：检查 ViewModel 是否已到最后一页
 * - onLoadMore()：调用 ViewModel 加载下一页
 *
 * 这些状态在 Fragment/ViewModel 中维护，所以必须由子类（Fragment）实现。
 *
 * 使用方式：
 * <pre>
 * recyclerView.addOnScrollListener(new PaginationScrollListener(layoutManager, 20) {
 *     public boolean isLoading() { return viewModel.isLoading(); }
 *     public boolean isLastPage() { return viewModel.isLastPage(); }
 *     public void onLoadMore() { viewModel.loadNextPage(); }
 * });
 * </pre>
 */
public abstract class PaginationScrollListener extends RecyclerView.OnScrollListener {

    /** RecyclerView 布局管理器 ── 用于获取当前滚动位置信息 */
    private final LinearLayoutManager layoutManager;

    /** 每页数据量 ── 用于判断是否至少有一页数据 */
    private final int pageSize;

    /**
     * 构造函数
     *
     * @param layoutManager RecyclerView 的 LinearLayoutManager（用于获取滚动位置）
     * @param pageSize      每页数据量（如 20，用于判断列表是否有足够数据触发分页）
     */
    public PaginationScrollListener(@NonNull LinearLayoutManager layoutManager, int pageSize) {
        this.layoutManager = layoutManager;
        this.pageSize = pageSize;
    }

    /**
     * 滚动监听回调 ── 每次 RecyclerView 滚动时调用
     *
     * 注意：此方法在主线程调用，不要做耗时操作！
     *
     * @param recyclerView RecyclerView 实例
     * @param dx           X 轴滚动偏移（水平滚动量，正值=向左滚）
     * @param dy           Y 轴滚动偏移（垂直滚动量，正值=向下滚）
     */
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        // 条件 1：只处理向下滚动（dy > 0 表示用户向下滚动）
        // dy <= 0：向上滚动或未滚动，不需要加载更多
        if (dy <= 0) return;

        // 获取当前滚动状态
        // getChildCount()：当前屏幕上可见的项数（如 5-8 个）
        int visibleItemCount = layoutManager.getChildCount();
        // getItemCount()：列表中的总项数（如 40 个）
        int totalItemCount = layoutManager.getItemCount();
        // findFirstVisibleItemPosition()：屏幕上第一个可见项的位置（如 35）
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

        // 条件 2-5：判断是否触发加载更多
        // 2. !isLoading()：非加载中（避免重复请求）
        // 3. !isLastPage()：非最后一页（还有更多数据）
        // 4. (visibleItemCount + firstVisibleItemPosition) >= totalItemCount：
        //    可见项 + 首个可见位置 >= 总项数 → 已滚动到底部
        //    例如：5 + 35 = 40 >= 40 → 触发加载
        // 5. firstVisibleItemPosition >= 0：有效位置（-1 表示未布局）
        // 6. totalItemCount >= pageSize：至少有一页数据
        if (!isLoading() && !isLastPage()) {
            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0
                    && totalItemCount >= pageSize) {
                // 所有条件满足，触发加载更多回调
                onLoadMore();
            }
        }
    }

    /**
     * 判断是否正在加载中 ── 由 Fragment/ViewModel 实现
     *
     * 返回 true 时不会触发新的加载请求，防止重复请求：
     * - 网络请求进行中 → 返回 true
     * - 网络请求完成 → 返回 false
     *
     * @return true 表示正在加载，false 表示空闲
     */
    public abstract boolean isLoading();

    /**
     * 判断是否为最后一页 ── 由 Fragment/ViewModel 实现
     *
     * 返回 true 时不再触发加载更多：
     * - 服务器返回的数据量 < pageSize → 最后一页
     * - 服务器返回空列表 → 最后一页
     *
     * @return true 表示已是最后一页，false 表示还有更多数据
     */
    public abstract boolean isLastPage();

    /**
     * 加载更多回调 ── 由 Fragment/ViewModel 实现
     *
     * 在此方法中发起网络请求获取下一页数据：
     * <pre>
     * public void onLoadMore() {
     *     viewModel.loadNextPage(); // 发起网络请求
     * }
     * </pre>
     */
    public abstract void onLoadMore();
}
