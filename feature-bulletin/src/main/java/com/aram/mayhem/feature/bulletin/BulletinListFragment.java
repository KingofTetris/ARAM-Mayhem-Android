package com.aram.mayhem.feature.bulletin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.feature.bulletin.adapter.BulletinAdapter;
import com.aram.mayhem.feature.bulletin.databinding.FragmentBulletinListBinding;
import com.aram.mayhem.feature.bulletin.viewmodel.BulletinListViewModel;
import com.aram.mayhem.ui.model.BulletinUiModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 公告列表页 ── 公告模块的主入口 Fragment
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * BulletinListFragment 是公告模块的"首页"，用户打开公告 Tab 后
 * 看到的就是这个页面。它展示公告列表和顶部轮播图，用户可以：
 * 1. 浏览公告列表（支持按类型筛选：全部/版本更新/活动/通知）
 * 2. 查看顶部轮播图（最新3条公告自动轮播）
 * 3. 点击公告卡片查看详情
 * 4. 离线时自动显示缓存数据，网络恢复后自动刷新
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_bulletin_list.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────────────────────────┐
 *   │  Toolbar（返回按钮 + 标题）                              │  ← toolbar
 *   ├──────────────────────────────────────────────────────────┤
 *   │  轮播图区域                                              │  ← carouselView
 *   │  ┌────────────────────────────────────────────────────┐  │
 *   │  │  最新公告1    最新公告2    最新公告3              │  │  ← 自动轮播
 *   │  └────────────────────────────────────────────────────┘  │
 *   ├──────────────────────────────────────────────────────────┤
 *   │  TabLayout（全部 | 版本更新 | 活动 | 通知）             │  ← tabLayout
 *   ├──────────────────────────────────────────────────────────┤
 *   │  RecyclerView                                           │  ← recyclerBulletins
 *   │  ┌────────────────────────────────────────────────────┐  │
 *   │  │  公告卡片 1                                        │  │  ← BulletinAdapter
 *   │  ├────────────────────────────────────────────────────┤  │
 *   │  │  公告卡片 2                                        │  │
 *   │  ├────────────────────────────────────────────────────┤  │
 *   │  │  ...                                               │  │
 *   │  └────────────────────────────────────────────────────┘  │
 *   ├──────────────────────────────────────────────────────────┤
 *   │  加载中 / 错误提示 + 重试按钮                           │  ← progress/error
 *   └──────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、离线检测机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   使用 ConnectivityManager.NetworkCallback 监听网络状态变化：
 *
 *   ┌───────────────────────────────────────────────────────────┐
 *   │  网络断开（onLost）                                      │
 *   │  → wasOffline = true                                    │
 *   │  → 显示 Snackbar "网络已断开，正在显示缓存数据"          │
 *   │  → ViewModel 自动使用缓存数据                            │
 *   │                                                          │
 *   │  网络恢复（onAvailable）                                 │
 *   │  → 如果 wasOffline 为 true（之前断开过）                 │
 *   │  → 显示 Snackbar "网络已恢复，正在刷新数据"              │
 *   │  → 调用 viewModel.loadBulletins() 刷新数据               │
 *   │  → wasOffline = false                                    │
 *   └───────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、Tab 筛选与类型映射
 * ═══════════════════════════════════════════════════════════════════
 *
 *   Tab 位置    显示文字      传递给 ViewModel 的类型参数
 *   ┌────────┬──────────┬──────────────────┐
 *   │ 0      │ 全部      │ null             │
 *   │ 1      │ 版本更新  │ "version"        │
 *   │ 2      │ 活动      │ "event"          │
 *   │ 3      │ 通知      │ "notice"         │
 *   └────────┴──────────┴──────────────────┘
 *
 * @see BulletinListViewModel
 * @see BulletinAdapter
 */
@AndroidEntryPoint
public class BulletinListFragment extends Fragment {

    /**
     * 视图绑定对象 ── 自动生成的绑定类，替代 findViewById
     *
     * 优势：
     * - 编译时类型安全，避免资源ID错误
     * - 直接访问视图对象，无需类型转换
     * - 在 onDestroyView() 中必须置 null 避免内存泄漏
     */
    private FragmentBulletinListBinding binding;

    /**
     * 公告列表 ViewModel ── 管理公告数据和业务逻辑
     *
     * 通过 ViewModelProvider 获取，Hilt 自动注入依赖。
     * ViewModel 的生命周期与 Fragment 绑定，配置变更时不会重新创建。
     */
    private BulletinListViewModel viewModel;

    /**
     * 公告列表适配器 ── 使用 DiffUtil 高效更新列表
     *
     * BulletinAdapter 继承自 ListAdapter（而非 RecyclerView.Adapter），
     * ListAdapter 内置了 DiffUtil 支持，通过 submitList() 提交数据时
     * 自动计算差异并高效更新。
     */
    private BulletinAdapter adapter;

    /**
     * 网络回调 ── 监听网络状态变化
     *
     * 在 onViewCreated() 中注册，在 onDestroyView() 中注销。
     * 必须注销，否则会泄漏 Context 引用。
     */
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * 是否曾经离线 ── 用于判断网络恢复时是否需要刷新
     *
     * 只有之前断开过网络（wasOffline=true），恢复时才刷新数据。
     * 如果一直在线，网络状态变化不需要额外操作。
     */
    private boolean wasOffline = false;

    /**
     * 公告详情点击监听器 ── 由宿主 Activity/Fragment 实现
     *
     * 当用户点击公告卡片或轮播图时，通过此监听器通知宿主
     * 跳转到 BulletinDetailFragment。
     */
    private OnBulletinDetailListener onBulletinDetailListener;

    /**
     * 公告详情点击回调接口 ── 解耦 Fragment 与导航逻辑
     *
     * 使用接口而非直接导航的好处：
     * - Fragment 不需要知道具体的导航实现
     * - 宿主可以自定义导航行为（如添加转场动画）
     * - 便于单元测试（Mock 监听器）
     */
    public interface OnBulletinDetailListener {
        /**
         * 点击公告跳转到详情页
         *
         * @param bulletinId 公告ID，用于加载详情数据
         */
        void onBulletinDetail(long bulletinId);
    }

    /**
     * 设置公告详情点击监听器
     *
     * @param listener 点击监听器
     */
    public void setOnBulletinDetailListener(OnBulletinDetailListener listener) {
        this.onBulletinDetailListener = listener;
    }

    /**
     * 创建视图 ── 使用 ViewBinding 填充布局
     *
     * @param inflater  布局填充器
     * @param container 父容器
     * @param savedInstanceState 保存的状态（本 Fragment 不使用）
     * @return 页面根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBulletinListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化操作 ── 按顺序设置各个组件
     *
     * 初始化顺序：
     * 1. setupToolbar()     → 工具栏（返回按钮）
     * 2. setupTabs()        → 类型筛选标签页
     * 3. setupRecyclerView()→ 公告列表
     * 4. setupCarousel()    → 轮播图
     * 5. setupOfflineDetection() → 离线检测
     * 6. observeViewModel() → 观察数据变化
     * 7. 加载初始数据       → 轮播 + 列表
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BulletinListViewModel.class);

        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupCarousel();
        setupOfflineDetection();
        observeViewModel();

        viewModel.loadCarouselBulletins();
        viewModel.loadBulletins(null, true);
    }

    /**
     * 设置工具栏 ── 配置返回按钮的点击行为
     *
     * 点击返回按钮时调用 requireActivity().onBackPressed()，
     * 返回到上一个页面。
     */
    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    /**
     * 设置标签页 ── 添加四个类型筛选标签并监听切换事件
     *
     * ═══════════════════════════════════════════════════════════
     * TabLayout 的工作方式：
     * ═══════════════════════════════════════════════════════════
     *
     * 1. addTab() 添加标签页，按顺序对应位置 0、1、2、3
     * 2. OnTabSelectedListener 监听标签选中事件
     * 3. onTabSelected：新标签被选中 → 重新加载对应类型
     * 4. onTabReselected：已选中的标签再次点击 → 也重新加载（刷新）
     * 5. onTabUnselected：标签取消选中 → 无需处理
     *
     * 每次切换标签都会调用 loadBulletins(type, true)：
     * - type = 对应的类型参数（null/"version"/"event"/"notice"）
     * - refresh = true 表示重置页码，替换整个列表
     */
    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("全部"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("版本更新"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("活动"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("通知"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String type = getTypeForTab(tab.getPosition());
                viewModel.loadBulletins(type, true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                String type = getTypeForTab(tab.getPosition());
                viewModel.loadBulletins(type, true);
            }
        });
    }

    /**
     * 根据标签位置获取对应的公告类型参数
     *
     * 位置映射：
     * - 0 → null（全部，不筛选）
     * - 1 → "version"（版本更新）
     * - 2 → "event"（活动）
     * - 3 → "notice"（通知）
     *
     * @param position 标签在 TabLayout 中的位置索引
     * @return 类型字符串，null 表示不筛选
     */
    private String getTypeForTab(int position) {
        switch (position) {
            case 1: return "version";
            case 2: return "event";
            case 3: return "notice";
            default: return null;
        }
    }

    /**
     * 设置 RecyclerView ── 初始化公告列表
     *
     * 配置步骤：
     * 1. 创建 BulletinAdapter 实例
     * 2. 设置公告点击监听器（点击跳转详情）
     * 3. 设置 LinearLayoutManager（垂直列表）
     * 4. 将适配器绑定到 RecyclerView
     *
     * 注意：本 Fragment 没有实现分页滚动监听（PaginationScrollListener），
     * 因为公告数量通常不多，一次加载即可。如果未来需要分页，
     * 可以像 CommunityFeedFragment 一样添加滚动监听。
     */
    private void setupRecyclerView() {
        adapter = new BulletinAdapter();
        adapter.setOnBulletinClickListener(this::showBulletinDetail);
        binding.recyclerBulletins.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerBulletins.setAdapter(adapter);
    }

    /**
     * 设置轮播图 ── 配置轮播图的点击事件
     *
     * carouselView 是自定义轮播组件，支持：
     * - 自动轮播（startAutoScroll / stopAutoScroll）
     * - 点击事件（setOnBulletinClickListener）
     * - 数据绑定（setBulletins）
     */
    private void setupCarousel() {
        binding.carouselView.setOnBulletinClickListener(this::showBulletinDetail);
    }

    /**
     * 跳转到公告详情页 ── 通过监听器通知宿主导航
     *
     * @param bulletin 被点击的公告数据，从中获取 ID 传递给详情页
     */
    private void showBulletinDetail(BulletinUiModel bulletin) {
        if (onBulletinDetailListener != null) {
            onBulletinDetailListener.onBulletinDetail(bulletin.getId());
        }
    }

    /**
     * 观察 ViewModel 数据变化 ── 将数据变化映射到 UI 更新
     *
     * ═══════════════════════════════════════════════════════════
     * 观察的 LiveData 列表：
     * ═══════════════════════════════════════════════════════════
     *
     * 1. bulletins → 公告列表数据变化 → adapter.submitList() 更新列表
     * 2. carouselBulletins → 轮播数据变化 → carouselView.setBulletins()
     * 3. loading → 加载状态变化 → 显示/隐藏进度条
     * 4. error → 错误信息变化 → 显示错误页面 + 重试按钮
     *
     * 使用 getViewLifecycleOwner() 而非 this 的原因：
     * - 观察者的生命周期绑定到视图（View），而非 Fragment
     * - Fragment 的视图销毁后，观察者自动移除，避免回调到已销毁的视图
     */
    private void observeViewModel() {
        viewModel.getBulletins().observe(getViewLifecycleOwner(), bulletins -> {
            if (bulletins != null) {
                adapter.submitList(bulletins);
            }
        });

        viewModel.getCarouselBulletins().observe(getViewLifecycleOwner(), bulletins -> {
            if (bulletins != null && !bulletins.isEmpty()) {
                binding.carouselView.setBulletins(bulletins);
                binding.carouselView.startAutoScroll();
            } else {
                binding.carouselView.setVisibility(View.GONE);
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                binding.progressContainer.setVisibility(View.VISIBLE);
                binding.errorContainer.setVisibility(View.GONE);
            } else {
                binding.progressContainer.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                binding.progressContainer.setVisibility(View.GONE);
                binding.errorContainer.setVisibility(View.VISIBLE);
                binding.textError.setText(errorMessage);
                binding.buttonRetry.setOnClickListener(v -> viewModel.loadBulletins(null, true));
            }
        });
    }

    /**
     * 设置离线检测 ── 注册网络状态监听器
     *
     * ═══════════════════════════════════════════════════════════
     * 实现原理：
     * ═══════════════════════════════════════════════════════════
     *
     * 使用 ConnectivityManager.registerNetworkCallback() 注册网络回调：
     * - NetworkCallback 会在网络状态变化时被系统调用
     * - onLost()：网络断开 → 通知用户 + 标记离线
     * - onAvailable()：网络恢复 → 刷新数据 + 标记在线
     *
     * ═══════════════════════════════════════════════════════════
     * 线程安全注意事项：
     * ═══════════════════════════════════════════════════════════
     *
     * NetworkCallback 的方法在系统后台线程调用，不能直接操作 UI。
     * 必须使用 requireActivity().runOnUiThread() 切换到主线程：
     * - 显示 Snackbar（UI 操作）
     * - 调用 ViewModel 方法（可能触发 setValue，需要主线程）
     */
    private void setupOfflineDetection() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                wasOffline = true;
                if (getView() != null) {
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(getView(), "网络已断开，正在显示缓存数据", Snackbar.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onAvailable(@NonNull Network network) {
                if (wasOffline && getView() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(getView(), "网络已恢复，正在刷新数据", Snackbar.LENGTH_SHORT).show();
                        viewModel.loadBulletins(null, true);
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
     * 视图销毁时清理资源 ── 注销网络回调和停止轮播
     *
     * 必须清理的资源：
     * 1. NetworkCallback → 不注销会泄漏 Context 引用
     * 2. CarouselView 自动轮播 → 不停止会浪费资源
     * 3. ViewBinding 引用 → 置 null 避免持有已销毁的视图
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
        if (binding != null) {
            binding.carouselView.stopAutoScroll();
        }
        binding = null;
    }
}
