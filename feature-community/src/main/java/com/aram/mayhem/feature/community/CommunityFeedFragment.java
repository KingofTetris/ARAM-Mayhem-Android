package com.aram.mayhem.feature.community;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.community.databinding.FragmentCommunityFeedBinding;
import com.aram.mayhem.feature.community.ui.StrategyCardAdapter;
import com.aram.mayhem.feature.community.viewmodel.StrategyFeedViewModel;
import com.aram.mayhem.network.dto.StrategyListResponse;
import com.aram.mayhem.ui.widget.PaginationScrollListener;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 社区攻略流页 ── 社区模块的主入口 Fragment
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * CommunityFeedFragment 是社区模块的"首页"，用户打开社区 Tab 后
 * 看到的就是这个页面。它展示一个攻略卡片列表，用户可以：
 * 1. 浏览攻略列表（热门排序 / 最新排序）
 * 2. 下拉刷新获取最新数据
 * 3. 滑到底部自动加载更多（分页）
 * 4. 点击攻略卡片查看详情
 * 5. 离线时自动显示缓存数据，网络恢复后自动刷新
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_community_feed.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────┐
 *   │  排序切换（热门 / 最新）              │  ← RadioGroup toggleSort
 *   ├──────────────────────────────────────┤
 *   │  SwipeRefreshLayout                  │  ← swipeRefresh
 *   │  ┌────────────────────────────────┐  │
 *   │  │  RecyclerView                  │  │  ← recyclerStrategies
 *   │  │  ┌──────────────────────────┐  │  │
 *   │  │  │  攻略卡片 1              │  │  │  ← StrategyCardAdapter
 *   │  │  ├──────────────────────────┤  │  │
 *   │  │  │  攻略卡片 2              │  │  │
 *   │  │  ├──────────────────────────┤  │  │
 *   │  │  │  ...                     │  │  │
 *   │  │  └──────────────────────────┘  │  │
 *   │  └────────────────────────────────┘  │
 *   ├──────────────────────────────────────┤
 *   │  空状态提示（无数据时显示）          │  ← layoutEmpty
 *   └──────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户操作           Fragment              ViewModel           Repository
 *   ┌────────┐       ┌──────────────┐     ┌──────────────┐    ┌──────────┐
 *   │ 下拉刷新│──────→│ retry()      │────→│ loadStrategies│───→│ 网络请求  │
 *   │ 滑到底部│──────→│ loadMore()   │────→│ loadMore()   │───→│ 下一页   │
 *   │ 切换排序│──────→│ setSort()    │────→│ setSort()    │───→│ 重新加载  │
 *   │ 点击卡片│──────→│ onStrategyClick│   │              │    │          │
 *   └────────┘       └──────────────┘     └──────────────┘    └──────────┘
 *                          ↓ 观察 LiveData
 *                    strategies → adapter.setStrategies()
 *                    loading    → swipeRefresh.setRefreshing()
 *                    error      → Toast 提示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、离线检测机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   本 Fragment 注册了 ConnectivityManager.NetworkCallback 来监听网络变化：
 *
 *   网络断开 → onLost() → 显示"网络已断开"Snackbar → Repository 自动使用缓存
 *   网络恢复 → onAvailable() → 显示"网络已恢复"Snackbar → 自动调用 retry() 刷新
 *
 *   这样用户在网络不稳定时仍能看到缓存数据，网络恢复后自动刷新，
 *   不需要手动操作。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、关键依赖
 * ═══════════════════════════════════════════════════════════════════
 *
 * - StrategyFeedViewModel：提供攻略列表数据、加载状态、排序控制
 * - StrategyCardAdapter：将 StrategyListResponse 绑定到攻略卡片视图
 * - PaginationScrollListener：监听滚动到底部，触发加载更多
 * - FragmentCommunityFeedBinding：ViewBinding 生成的布局绑定类
 *
 * 导航关系：
 * - 点击攻略卡片 → StrategyDetailFragment（通过 OnStrategyClickListener 回调）
 * - 发布按钮 → PublishStrategyFragment（由宿主 Activity/Navigation 处理）
 */
@AndroidEntryPoint
public class CommunityFeedFragment extends Fragment implements StrategyCardAdapter.OnStrategyClickListener {

    /**
     * ViewBinding 实例 ── 自动生成的布局绑定类
     *
     * 作用：通过 binding.recyclerStrategies 这样的方式直接访问 XML 中的 View，
     * 替代传统的 findViewById()，避免空指针和类型转换错误。
     *
     * 生命周期注意：
     * - onCreateView 中赋值
     * - onDestroyView 中置 null（防止内存泄漏，因为 View 已销毁但 Fragment 可能还在）
     */
    private FragmentCommunityFeedBinding binding;

    /**
     * 攻略列表 ViewModel ── 管理攻略列表的数据和业务逻辑
     *
     * 通过 ViewModelProvider 获取，Hilt 自动注入依赖。
     * ViewModel 的生命周期比 View 长，屏幕旋转后数据不会丢失。
     */
    private StrategyFeedViewModel viewModel;

    /**
     * 攻略卡片适配器 ── 将数据绑定到 RecyclerView 的每个卡片
     *
     * 数据源：List<StrategyListResponse>
     * 每个 StrategyListResponse 对应一张攻略卡片
     */
    private StrategyCardAdapter adapter;

    /**
     * 网络状态回调 ── 监听网络连接/断开事件
     *
     * 注册时机：setupOfflineDetection() 中注册
     * 注销时机：onDestroyView() 中注销（防止内存泄漏）
     *
     * 工作原理：
     * ConnectivityManager 会持续监听网络状态变化，
     * 当网络连接或断开时回调对应方法。
     */
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * 是否曾经离线 ── 标记网络是否曾经断开过
     *
     * 用途：区分"首次有网络"和"从离线恢复到在线"两种情况。
     * - 首次有网络：不需要提示"网络已恢复"
     * - 从离线恢复：需要提示用户并自动刷新数据
     *
     * 流程：
     * 1. 初始值 false
     * 2. onLost() 被调用 → wasOffline = true
     * 3. onAvailable() 被调用 → 检查 wasOffline，如果为 true 则刷新 → wasOffline = false
     */
    private boolean wasOffline = false;

    /**
     * 攻略点击监听器接口 ── 由宿主 Activity 或父 Fragment 实现
     *
     * 用途：当用户点击攻略卡片时，通知宿主进行页面跳转。
     * Fragment 本身不直接处理导航，而是通过回调让宿主决定如何跳转。
     *
     * 为什么不直接在 Fragment 里导航？
     * - 解耦：Fragment 不需要知道导航的具体实现
     * - 灵活：宿主可以选择用 Navigation Component 或手动 replace Fragment
     * - 可测试：可以在测试中传入 mock 监听器
     */
    public interface OnStrategyClickListener {
        /**
         * 攻略被点击时的回调
         *
         * @param strategyId 被点击攻略的 ID，用于加载攻略详情
         */
        void onStrategyClick(long strategyId);
    }

    /**
     * 宿主设置的攻略点击监听器实例
     *
     * 可以为 null（如果宿主没有设置监听器，点击卡片不会有响应）
     */
    @Nullable
    private OnStrategyClickListener strategyClickListener;

    /**
     * 设置攻略点击监听器 ── 由宿主 Activity 或父 Fragment 调用
     *
     * @param listener 监听器实现，传入 null 可取消监听
     *
     * 使用示例（在宿主 Activity 中）：
     * communityFeedFragment.setOnStrategyClickListener(strategyId -> {
     *     // 跳转到攻略详情页
     *     Bundle args = new Bundle();
     *     args.putLong("strategyId", strategyId);
     *     Navigation.findNavController(view).navigate(R.id.strategyDetailFragment, args);
     * });
     */
    public void setOnStrategyClickListener(@Nullable OnStrategyClickListener listener) {
        this.strategyClickListener = listener;
    }

    /**
     * 创建 Fragment 的视图 ── Fragment 生命周期方法
     *
     * 这个方法在 Fragment 的视图被创建时调用，我们在这里：
     * 1. 使用 ViewBinding 加载布局文件 fragment_community_feed.xml
     * 2. 返回根视图给系统显示
     *
     * @param inflater  布局加载器，用于将 XML 布局转换为 View 对象
     * @param container 父容器，Fragment 的视图将被添加到这个容器中
     * @param savedInstanceState 保存的实例状态（屏幕旋转等场景恢复数据用）
     * @return Fragment 的根视图
     *
     * 为什么用 ViewBinding 而不是 setContentView？
     * Fragment 没有 setContentView 方法，必须返回一个 View。
     * ViewBinding.inflate() 会加载 XML 并生成一个绑定对象，
     * 通过 binding.xxx 可以直接访问 XML 中带 id 的 View。
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable android.os.Bundle savedInstanceState) {
        binding = FragmentCommunityFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化 ── Fragment 生命周期方法
     *
     * 这个方法在 onCreateView 之后调用，此时视图已经创建完毕，
     * 可以安全地访问所有 View。我们在这里完成所有初始化工作：
     *
     * 1. 获取 ViewModel 实例
     * 2. 设置 RecyclerView 和适配器
     * 3. 设置排序切换监听
     * 4. 注册网络状态监听
     * 5. 观察 ViewModel 的 LiveData 数据变化
     *
     * @param view Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable android.os.Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StrategyFeedViewModel.class);

        setupRecyclerView();
        setupSortToggle();
        setupOfflineDetection();
        observeViewModel();
    }

    /**
     * 设置 RecyclerView ── 初始化攻略列表的核心组件
     *
     * ═══════════════════════════════════════════════════════════════════
     * RecyclerView 是什么？
     * ═══════════════════════════════════════════════════════════════════
     *
     * RecyclerView 是 Android 高效列表控件，它只渲染屏幕可见的项，
     * 滚动时复用（回收）离开屏幕的 ViewHolder，避免重复创建 View。
     *
     *   屏幕可见区域
     *   ┌──────────┐
     *   │ 卡片 1   │ ← 正在显示
     *   │ 卡片 2   │ ← 正在显示
     *   │ 卡片 3   │ ← 正在显示
     *   └──────────┘
     *   │ 卡片 4   │ ← 离开屏幕，ViewHolder 被回收复用
     *
     * ═══════════════════════════════════════════════════════════════════
     * 本方法的初始化步骤
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. 创建适配器 StrategyCardAdapter
     * 2. 设置攻略卡片点击监听器（this 实现了 OnStrategyClickListener）
     * 3. 设置 LinearLayoutManager（垂直列表布局）
     * 4. 将适配器绑定到 RecyclerView
     * 5. 添加分页滚动监听器（滑到底部自动加载更多）
     * 6. 设置下拉刷新监听器
     */
    private void setupRecyclerView() {
        adapter = new StrategyCardAdapter();
        adapter.setOnStrategyClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerStrategies.setLayoutManager(layoutManager);
        binding.recyclerStrategies.setAdapter(adapter);

        /**
         * 分页滚动监听器 ── 滑到底部时自动加载下一页
         *
         * PaginationScrollListener 是自定义的滚动监听器，
         * 它会在用户滑到距离底部 10 个 item 以内时触发 onLoadMore()。
         *
         * 三个回调方法的含义：
         * - isLoading()：当前是否正在加载更多数据？避免重复请求
         * - isLastPage()：是否已经到了最后一页？到了就不再加载
         * - onLoadMore()：触发加载更多，调用 ViewModel 的 loadMore()
         *
         *   ┌──────────┐
         *   │ 卡片 1   │
         *   │ 卡片 2   │
         *   │ ...      │
         *   │ 卡片 N-10│ ← 距离底部 10 个 item，触发加载
         *   │ 卡片 N-9 │
         *   │ ...      │
         *   │ 卡片 N   │ ← 屏幕底部
         *   └──────────┘
         *   （后台加载第 N+1 ~ N+10 条数据）
         */
        binding.recyclerStrategies.addOnScrollListener(new PaginationScrollListener(layoutManager, 10) {
            @Override
            public boolean isLoading() {
                Boolean loading = viewModel.getLoadingMore().getValue();
                return loading != null && loading;
            }

            @Override
            public boolean isLastPage() {
                Boolean lastPage = viewModel.getIsLastPage().getValue();
                return lastPage != null && lastPage;
            }

            @Override
            public void onLoadMore() {
                viewModel.loadMore();
            }
        });

        /**
         * 下拉刷新监听器 ── 用户下拉时重新加载第一页数据
         *
         * SwipeRefreshLayout 的工作方式：
         * 1. 用户在列表顶部向下拉
         * 2. 显示一个旋转的刷新指示器
         * 3. 触发 onRefresh 回调
         * 4. 我们调用 viewModel.retry() 重新加载
         * 5. ViewModel 加载完成后更新 loading 状态
         * 6. observeViewModel 中的 loading 观察者会关闭刷新指示器
         */
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.retry());
    }

    /**
     * 设置排序切换 ── 热门/最新 排序切换按钮
     *
     * ═══════════════════════════════════════════════════════════════════
     * 排序逻辑
     * ═══════════════════════════════════════════════════════════════════
     *
     * XML 中有两个 RadioButton（btn_hot 和 btn_latest），放在一个 RadioGroup 中。
     * RadioGroup 保证同一时间只有一个按钮被选中。
     *
     *   ┌──────────────┬──────────────┐
     *   │  ● 热门      │  ○ 最新      │  ← 选中"热门"
     *   └──────────────┴──────────────┘
     *
     * 切换时：
     * - 选中 btn_hot  → viewModel.setSort("hot")  → 按点赞数排序
     * - 选中 btn_latest → viewModel.setSort("latest") → 按发布时间排序
     *
     * ViewModel 的 setSort() 方法会自动重新加载第一页数据。
     */
    private void setupSortToggle() {
        binding.toggleSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == com.aram.mayhem.feature.community.R.id.btn_hot) {
                viewModel.setSort("hot");
            } else if (checkedId == com.aram.mayhem.feature.community.R.id.btn_latest) {
                viewModel.setSort("latest");
            }
        });
    }

    /**
     * 设置离线检测 ── 监听网络状态变化，实现离线缓存和自动刷新
     *
     * ═══════════════════════════════════════════════════════════════════
     * 为什么需要离线检测？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 移动端网络环境不稳定（地铁、电梯、信号差等），我们需要：
     * 1. 网络断开时：告知用户正在查看缓存数据，避免用户以为数据是实时的
     * 2. 网络恢复时：自动刷新数据，让用户看到最新内容
     *
     * ═══════════════════════════════════════════════════════════════════
     * ConnectivityManager 的工作原理
     * ═══════════════════════════════════════════════════════════════════
     *
     * ConnectivityManager 是 Android 系统服务，负责监控网络连接状态。
     * 我们通过 registerNetworkCallback 注册一个回调，系统会在网络状态变化时通知我们。
     *
     *   NetworkRequest 指定我们关心的网络类型：
     *   - NET_CAPABILITY_INTERNET：有互联网访问能力的网络
     *
     *   回调方法：
     *   - onLost()：网络断开（WiFi 断开、移动数据关闭等）
     *   - onAvailable()：网络可用（连接到 WiFi、开启移动数据等）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 注意事项
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. 回调在系统线程执行，不是主线程！UI 操作必须切到主线程：
     *    requireActivity().runOnUiThread(() -> { ... })
     *
     * 2. 必须在 onDestroyView 中注销回调，否则会内存泄漏：
     *    cm.unregisterNetworkCallback(networkCallback)
     *
     * 3. getView() 可能为 null（Fragment 的 View 可能已销毁），
     *    使用前必须检查
     */
    private void setupOfflineDetection() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            /**
             * 网络断开回调 ── WiFi/移动数据断开时触发
             *
             * 执行操作：
             * 1. 标记 wasOffline = true（记录曾经离线过）
             * 2. 在主线程显示 Snackbar 提示用户
             *
             * @param network 断开的网络对象
             */
            @Override
            public void onLost(@NonNull Network network) {
                wasOffline = true;
                if (getView() != null) {
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(getView(), "网络已断开，正在显示缓存数据", Snackbar.LENGTH_LONG).show()
                    );
                }
            }

            /**
             * 网络可用回调 ── 连接到 WiFi/移动数据时触发
             *
             * 执行操作（仅在曾经离线过的情况下）：
             * 1. 显示"网络已恢复"Snackbar
             * 2. 调用 viewModel.retry() 自动刷新数据
             * 3. 重置 wasOffline = false
             *
             * 为什么检查 wasOffline？
             * 因为 onAvailable 在首次注册时也会触发（如果当前有网络），
             * 我们不希望页面刚打开就显示"网络已恢复"的提示。
             *
             * @param network 可用的网络对象
             */
            @Override
            public void onAvailable(@NonNull Network network) {
                if (wasOffline && getView() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(getView(), "网络已恢复，正在刷新数据", Snackbar.LENGTH_SHORT).show();
                        viewModel.retry();
                    });
                }
                wasOffline = false;
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(request, networkCallback);
    }

    /**
     * 观察 ViewModel 的 LiveData ── 建立数据绑定关系
     *
     * ═══════════════════════════════════════════════════════════════════
     * LiveData 观察机制是什么？
     * ═══════════════════════════════════════════════════════════════════
     *
     * LiveData 是一种可观察的数据持有者，它的特点是"生命周期感知"：
     * - 只有当 Fragment 处于 STARTED 或 RESUMED 状态时才通知观察者
     * - Fragment 销毁后自动取消观察，不会造成内存泄漏
     * - 配置变更（如屏幕旋转）后重新接收最新数据
     *
     *   ViewModel                    Fragment
     *   ┌──────────────┐            ┌──────────────────┐
     *   │ strategies   │──变化──→   │ 更新适配器数据    │
     *   │ loading      │──变化──→   │ 显示/隐藏刷新圈  │
     *   │ loadingMore  │──变化──→   │ (预留扩展)       │
     *   │ error        │──变化──→   │ 显示错误提示     │
     *   └──────────────┘            └──────────────────┘
     *
     * getViewLifecycleOwner() 的含义：
     * 传入 Fragment 的视图生命周期所有者，确保只有在视图存活时才接收通知。
     * 这比传入 this 更精确，因为 Fragment 的生命周期比视图长。
     */
    private void observeViewModel() {
        /**
         * 观察攻略列表数据 ── 数据变化时更新适配器
         *
         * 触发时机：
         * - 首次加载完成
         * - 下拉刷新完成
         * - 加载更多完成
         * - 排序切换后重新加载
         *
         * @param strategies 最新的攻略列表数据
         */
        viewModel.getStrategies().observe(getViewLifecycleOwner(), strategies -> {
            adapter.setStrategies(strategies);
            binding.layoutEmpty.setVisibility(strategies.isEmpty() ? View.VISIBLE : View.GONE);
        });

        /**
         * 观察首次加载状态 ── 控制 SwipeRefreshLayout 的刷新指示器
         *
         * loading=true  → 显示旋转刷新圈
         * loading=false → 隐藏刷新圈
         *
         * 注意：这里只控制"首次加载"和"下拉刷新"的指示器，
         * "加载更多"的指示器由 loadingMore 控制（当前未实现 UI 展示）
         */
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.swipeRefresh.setRefreshing(loading);
        });

        /**
         * 观察"加载更多"状态 ── 预留扩展
         *
         * 当前没有在 UI 上展示加载更多的进度指示器，
         * 未来可以在这里添加底部加载动画（如小圆圈 + "加载中..."）
         */
        viewModel.getLoadingMore().observe(getViewLifecycleOwner(), loadingMore -> {
        });

        /**
         * 观察错误信息 ── 显示错误提示
         *
         * error 不为 null 且不为空时，用 Toast 显示错误信息。
         * 常见错误场景：
         * - 网络请求失败
         * - 服务器返回错误
         * - 数据解析异常
         */
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 攻略卡片点击回调 ── 实现 StrategyCardAdapter.OnStrategyClickListener 接口
     *
     * 当用户点击攻略卡片时，StrategyCardAdapter 会调用这个方法。
     * 我们将点击事件传递给宿主设置的 strategyClickListener，
     * 由宿主决定如何导航到攻略详情页。
     *
     * @param strategy 被点击的攻略数据对象
     *
     * 执行流程：
     * 1. 用户点击卡片 → Adapter 调用 listener.onStrategyClick(strategy)
     * 2. 本方法被触发 → 检查 strategyClickListener 是否为 null
     * 3. 不为 null → 调用 strategyClickListener.onStrategyClick(strategyId)
     * 4. 宿主接收到 strategyId → 导航到 StrategyDetailFragment
     *
     * strategy.getId() 为 null 时使用 -1 作为默认值（理论上不应发生）
     */
    @Override
    public void onStrategyClick(StrategyListResponse strategy) {
        if (strategyClickListener != null) {
            strategyClickListener.onStrategyClick(strategy.getId() != null ? strategy.getId() : -1);
        }
    }

    /**
     * Fragment 视图销毁 ── 清理资源，防止内存泄漏
     *
     * ═══════════════════════════════════════════════════════════════════
     * 为什么需要清理？
     * ═══════════════════════════════════════════════════════════════════
     *
     * Fragment 的 View 可能在 Fragment 还存活时就被销毁（比如返回上一页），
     * 如果不清理，这些对象会一直持有对已销毁 View 的引用，造成内存泄漏。
     *
     * 清理内容：
     * 1. 注销网络状态回调（networkCallback）── 否则回调会持续触发，
     *    而且持有 Fragment 的 Context 引用导致无法回收
     * 2. 将 binding 置为 null ── 否则 binding 持有所有 View 的引用
     *
     * ═══════════════════════════════════════════════════════════════════
     * unregisterNetworkCallback 的安全性
     * ═══════════════════════════════════════════════════════════════════
     *
     * 如果 networkCallback 为 null（setupOfflineDetection 中 cm 为 null 的情况），
     * 不会执行注销操作，避免 NullPointerException。
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                cm.unregisterNetworkCallback(networkCallback);
            }
        }
        binding = null;
    }
}
