package com.aram.mayhem.feature.augment;

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

import com.aram.mayhem.feature.augment.databinding.FragmentAugmentListBinding;
import com.aram.mayhem.feature.augment.viewmodel.AugmentViewModel;
import com.aram.mayhem.ui.adapter.AugmentCardAdapter;
import com.aram.mayhem.ui.model.AugmentUiModel;
import com.aram.mayhem.ui.widget.PaginationScrollListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 符文列表页 ── 展示强化符文列表，支持品质筛选和分页加载
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentListFragment 是符文模块的主页面，负责：
 * 1. 展示符文列表（RecyclerView + 分页加载）
 * 2. 品质筛选（TabLayout：全部/棱彩/金/银）
 * 3. 离线检测（网络断开时提示用户，恢复后自动重试）
 * 4. 点击符文卡片 → 弹出 AugmentDetailBottomSheet
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构
 * ═══════════════════════════════════════════════════════════════════
 *
 *   fragment_augment_list.xml
 *   ┌──────────────────────────────────────────┐
 *   │  TabLayout（品质筛选）                     │
 *   │  [全部] [棱彩] [金] [银]                  │
 *   ├──────────────────────────────────────────┤
 *   │  StatefulLayout（加载/空/错误状态）        │
 *   │  ┌──────────────────────────────────────┐│
 *   │  │  RecyclerView（符文列表）              ││
 *   │  │  ┌────────────────────────────────┐  ││
 *   │  │  │  AugmentCardAdapter            │  ││
 *   │  │  │  ┌──────┐ ┌──────┐ ┌──────┐   │  ││
 *   │  │  │  │ 符文1 │ │ 符文2 │ │ 符文3 │   │  ││
 *   │  │  │  └──────┘ └──────┘ └──────┘   │  ││
 *   │  │  │  ┌──────┐ ┌──────┐ ┌──────┐   │  ││
 *   │  │  │  │ 符文4 │ │ 符文5 │ │ 符文6 │   │  ││
 *   │  │  │  └──────┘ └──────┘ └──────┘   │  ││
 *   │  │  └────────────────────────────────┘  ││
 *   │  └──────────────────────────────────────┘│
 *   └──────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、Fragment 生命周期与初始化顺序
 * ═══════════════════════════════════════════════════════════════════
 *
 *   onAttach()           → 获取 OnAugmentSelectedListener 回调
 *   onCreateView()       → 创建 ViewBinding
 *   onViewCreated()      → 初始化 ViewModel、RecyclerView、TabLayout、离线检测
 *   observeViewModel()   → 订阅 LiveData 数据变化
 *   onDestroyView()      → 注销网络回调、释放 ViewBinding
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、与 HeroListFragment 的对比
 * ═══════════════════════════════════════════════════════════════════
 *
 * 两者结构几乎相同，差异点：
 * - HeroListFragment：搜索栏 + ChipGroup 梯级筛选
 * - AugmentListFragment：TabLayout 品质筛选
 * - 英雄点击 → 全屏详情页（Fragment 导航）
 * - 符文点击 → 底部弹窗（BottomSheet）
 *
 * @see AugmentViewModel
 * @see AugmentCardAdapter
 * @see OnAugmentSelectedListener
 * @see AugmentDetailBottomSheet
 */
@AndroidEntryPoint
public class AugmentListFragment extends Fragment {

    /**
     * ViewBinding ── 类型安全地访问布局中的 View
     *
     * 优势：替代 findViewById()，编译期检查 ID 是否存在，避免空指针。
     * 生命周期：onCreateView 创建，onDestroyView 置 null（防止内存泄漏）。
     */
    private FragmentAugmentListBinding binding;

    /**
     * 符文列表 ViewModel ── 管理符文列表数据和业务逻辑
     *
     * 通过 ViewModelProvider 获取，Hilt 自动注入依赖。
     * 作用域：Fragment 级别，配置变更（如旋转屏幕）时不会重新创建。
     */
    private AugmentViewModel viewModel;

    /**
     * 符文卡片适配器 ── 将 AugmentUiModel 数据绑定到符文卡片布局
     *
     * 位于 core-ui 模块，因为英雄和符文的卡片样式可能复用。
     */
    private AugmentCardAdapter adapter;

    /**
     * 符文选中回调 ── 用于通知 Activity 用户点击了某个符文
     *
     * 通过 onAttach() 从宿主 Activity 获取。
     * Activity 实现此接口后，负责创建 AugmentDetailBottomSheet。
     */
    private OnAugmentSelectedListener augmentSelectedListener;

    /**
     * 网络状态回调 ── 监听网络连接/断开事件
     *
     * 注册时机：onViewCreated()
     * 注销时机：onDestroyView()
     * 必须成对使用，否则会内存泄漏。
     */
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * 是否曾经离线 ── 标记网络是否曾经断开
     *
     * 用途：网络恢复时，只有曾经断开过才显示"网络已恢复"提示。
     * 避免首次连接时不必要地弹出提示。
     */
    private boolean wasOffline = false;

    /**
     * Fragment 附加到 Activity 时调用 ── 获取回调接口
     *
     * 检查宿主 Activity 是否实现了 OnAugmentSelectedListener 接口。
     * 如果实现了，保存引用以便后续调用。
     *
     * 为什么在这里获取？
     * - onAttach() 是 Fragment 生命周期最早能访问 Activity 的时机
     * - 如果 Activity 没实现接口，点击符文时不会弹出详情弹窗
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAugmentSelectedListener) {
            augmentSelectedListener = (OnAugmentSelectedListener) context;
        }
    }

    /**
     * 创建 Fragment 的视图 ── 初始化 ViewBinding
     *
     * 使用 ViewBinding 替代传统的 setContentView / findViewById。
     * inflate() 方法会自动读取 fragment_augment_list.xml 并创建绑定对象。
     *
     * @param inflater  布局填充器
     * @param container 父视图容器
     * @param savedInstanceState 保存的实例状态（配置变更时恢复）
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAugmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成 ── 初始化所有组件
     *
     * 此时 View 已经创建完毕，可以安全地访问 binding 中的 View。
     * 初始化顺序：
     * 1. ViewModel（数据源）
     * 2. RecyclerView（列表展示）
     * 3. TabLayout（筛选控件）
     * 4. 离线检测（网络监听）
     * 5. 观察数据（LiveData 订阅）
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AugmentViewModel.class);
        setupRecyclerView();
        setupTabs();
        setupOfflineDetection();
        observeViewModel();
    }

    /**
     * 初始化 RecyclerView ── 配置列表展示和分页加载
     *
     * 配置内容：
     * 1. 创建 AugmentCardAdapter 并设置点击监听
     * 2. 设置 LinearLayoutManager（垂直滚动）
     * 3. 添加 PaginationScrollListener（滑到底部自动加载更多）
     *
     * 分页加载逻辑：
     * - 滑动到距底部 20 条数据时触发 loadMore()
     * - isLoading=true 或 isLastPage=true 时不触发（防止重复请求）
     */
    private void setupRecyclerView() {
        adapter = new AugmentCardAdapter();
        adapter.setOnAugmentClickListener(this::showAugmentDetail);
        binding.recyclerAugmentList.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerAugmentList.setLayoutManager(layoutManager);

        binding.recyclerAugmentList.addOnScrollListener(new PaginationScrollListener(layoutManager, 20) {
            @Override
            public boolean isLoading() {
                return viewModel.getLoadingMore().getValue() != null && viewModel.getLoadingMore().getValue();
            }

            @Override
            public boolean isLastPage() {
                return viewModel.getIsLastPage().getValue() != null && viewModel.getIsLastPage().getValue();
            }

            @Override
            public void onLoadMore() {
                viewModel.loadMore();
            }
        });
    }

    /**
     * 初始化品质筛选 TabLayout ── 配置品质标签和选中事件
     *
     * Tab 结构：
     *   位置0: "全部"   → 不筛选品质
     *   位置1: "棱彩"   → PRISMATIC（最高品质）
     *   位置2: "金"     → GOLD
     *   位置3: "银"     → SILVER
     *
     * 选中事件：
     * - onTabSelected：切换品质筛选，触发 ViewModel 重新加载
     * - onTabUnselected / onTabReselected：不需要处理
     */
    private void setupTabs() {
        binding.tabQuality.addTab(binding.tabQuality.newTab().setText("全部"));
        binding.tabQuality.addTab(binding.tabQuality.newTab().setText("棱彩"));
        binding.tabQuality.addTab(binding.tabQuality.newTab().setText("金"));
        binding.tabQuality.addTab(binding.tabQuality.newTab().setText("银"));

        binding.tabQuality.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String quality = mapTabToQuality(tab.getPosition());
                viewModel.filterByQuality(quality);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Tab 位置 → 品质参数映射
     *
     * 将 TabLayout 的位置索引转换为后端 API 需要的品质字符串。
     *
     * @param position Tab 位置（0=全部, 1=棱彩, 2=金, 3=银）
     * @return 品质参数字符串（空字符串表示不筛选）
     */
    private String mapTabToQuality(int position) {
        switch (position) {
            case 1:
                return "PRISMATIC";
            case 2:
                return "GOLD";
            case 3:
                return "SILVER";
            default:
                return "";
        }
    }

    /**
     * 初始化离线检测 ── 监听网络状态变化
     *
     * 使用 ConnectivityManager 注册网络回调：
     * - onLost：网络断开 → 显示"网络已断开"提示
     * - onAvailable：网络恢复 → 显示"网络已恢复"提示 + 自动重试
     *
     * 注意事项：
     * - 网络回调在后台线程执行，UI 操作需要切换到主线程
     * - 使用 runOnUiThread() 确保在主线程更新 UI
     * - wasOffline 标记防止首次连接时误弹提示
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
                        Snackbar.make(getView(), "网络已恢复", Snackbar.LENGTH_SHORT).show();
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
     * 观察 ViewModel 数据 ── 订阅 LiveData 数据变化
     *
     * 三个观察者：
     * 1. augments：符文列表数据变化 → 更新 RecyclerView
     * 2. loading：加载状态变化 → 显示/隐藏加载动画
     * 3. error：错误信息变化 → 显示错误提示
     *
     * getViewLifecycleOwner()：
     * - 确保 LiveData 只在 Fragment 视图存活时通知
     * - 视图销毁后自动取消订阅，防止内存泄漏
     */
    private void observeViewModel() {
        viewModel.getAugments().observe(getViewLifecycleOwner(), this::updateAugmentList);
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoadingState);
        viewModel.getError().observe(getViewLifecycleOwner(), this::showError);
    }

    /**
     * 更新符文列表 ── 将新数据提交给适配器
     *
     * 三种情况：
     * 1. 列表非空 → 显示 RecyclerView，隐藏 StatefulLayout
     * 2. 列表空 + 非加载中 → 显示空状态页面
     * 3. 列表空 + 加载中 → 不处理（由 updateLoadingState 处理）
     *
     * @param augments 新的符文列表数据
     */
    private void updateAugmentList(List<AugmentUiModel> augments) {
        adapter.submitList(augments);
        if (augments != null && !augments.isEmpty()) {
            binding.statefulLayout.setVisibility(View.GONE);
            binding.recyclerAugmentList.setVisibility(View.VISIBLE);
        } else if (augments != null && augments.isEmpty() && (viewModel.getLoading().getValue() == null || !viewModel.getLoading().getValue())) {
            binding.statefulLayout.setVisibility(View.VISIBLE);
            binding.recyclerAugmentList.setVisibility(View.GONE);
            binding.statefulLayout.showEmpty();
        }
    }

    /**
     * 更新加载状态 ── 显示/隐藏加载动画
     *
     * 只在列表为空时显示加载状态（首次加载）。
     * 分页加载更多时不显示全屏加载动画（避免打断用户浏览）。
     *
     * @param isLoading true=正在加载
     */
    private void updateLoadingState(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            if (adapter.getItemCount() == 0) {
                binding.statefulLayout.setVisibility(View.VISIBLE);
                binding.recyclerAugmentList.setVisibility(View.GONE);
                binding.statefulLayout.showLoading();
            }
        }
    }

    /**
     * 显示错误提示 ── Snackbar + 重试按钮
     *
     * @param message 错误信息
     */
    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message != null ? message : "加载失败", Snackbar.LENGTH_LONG)
                    .setAction("重试", v -> viewModel.retry())
                    .show();
        }
    }

    /**
     * 显示符文详情 ── 通过回调接口通知 Activity
     *
     * 用户点击符文卡片时触发。
     * 不直接创建 BottomSheet，而是通过 OnAugmentSelectedListener 接口
     * 让 Activity 来处理（Fragment 不应直接操作其他 Fragment）。
     *
     * @param augment 被点击的符文 UI 模型
     */
    private void showAugmentDetail(AugmentUiModel augment) {
        if (augmentSelectedListener != null) {
            augmentSelectedListener.onAugmentSelected(augment.getId());
        }
    }

    /**
     * 视图销毁 ── 清理资源
     *
     * 必须清理的内容：
     * 1. 注销网络回调（防止内存泄漏）
     * 2. 置空 ViewBinding（防止内存泄漏）
     *
     * 为什么 binding 要置 null？
     * - Fragment 的视图可能被销毁但 Fragment 对象仍然存在
     * - 如果 binding 不置 null，会持有已销毁的 View 引用
     * - 后续访问 binding 会抛出 IllegalStateException
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
