package com.aram.mayhem.feature.hero;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.common.Tier;
import com.aram.mayhem.feature.hero.databinding.FragmentHeroListBinding;
import com.aram.mayhem.feature.hero.viewmodel.HeroListViewModel;
import com.aram.mayhem.ui.adapter.HeroCardAdapter;
import com.aram.mayhem.ui.model.HeroUiModel;
import com.aram.mayhem.ui.widget.PaginationScrollListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 英雄列表页 ── App 的主页面，展示所有英雄的卡片列表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroListFragment 是用户打开 App 后看到的第一个页面，负责：
 * 1. 展示英雄卡片列表（每个卡片显示头像、名称、胜率、梯级等）
 * 2. 支持关键词搜索（搜索框实时过滤）
 * 3. 支持梯级筛选（S+/S/A/B/C 筛选标签）
 * 4. 支持分页加载（滑到底部自动加载更多）
 * 5. 支持离线模式（网络断开时显示缓存数据）
 * 6. 点击卡片跳转到英雄详情页
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构
 * ═══════════════════════════════════════════════════════════════════
 *
 *   fragment_hero_list.xml
 *   ┌─────────────────────────────────────────────────────────┐
 *   │  SearchToolbar（搜索栏）                                 │
 *   │  ┌─────────────────────────────────────────────────┐   │
 *   │  │ 🔍 [搜索英雄...]                                │   │
 *   │  └─────────────────────────────────────────────────┘   │
 *   │                                                         │
 *   │  ChipGroup（梯级筛选标签）                               │
 *   │  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐                   │
 *   │  │ S+ │ │ S  │ │ A  │ │ B  │ │ C  │                   │
 *   │  └────┘ └────┘ └────┘ └────┘ └────┘                   │
 *   │                                                         │
 *   │  StatefulLayout（状态容器：Loading/Content/Error/Empty）  │
 *   │  ┌─────────────────────────────────────────────────┐   │
 *   │  │  RecyclerView（英雄卡片列表）                     │   │
 *   │  │  ┌───────────────────────────────────────────┐  │   │
 *   │  │  │ [头像] 提莫    胜率 52.3%  S+             │  │   │
 *   │  │  ├───────────────────────────────────────────┤  │   │
 *   │  │  │ [头像] 亚索    胜率 48.1%  B              │  │   │
 *   │  │  ├───────────────────────────────────────────┤  │   │
 *   │  │  │ [头像] 金克丝  胜率 50.7%  A              │  │   │
 *   │  │  ├───────────────────────────────────────────┤  │   │
 *   │  │  │ ...更多英雄...                             │  │   │
 *   │  │  └───────────────────────────────────────────┘  │   │
 *   │  └─────────────────────────────────────────────────┘   │
 *   └─────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、Fragment 生命周期与初始化顺序
 * ═══════════════════════════════════════════════════════════════════
 *
 *   onAttach()           → 获取宿主 Activity 的回调接口
 *   onCreateView()       → 加载 XML 布局，创建 binding
 *   onViewCreated()      → 初始化所有组件（5个 setup 方法）
 *     ├─ setupRecyclerView()    → 配置 RecyclerView + 适配器 + 分页
 *     ├─ setupSearchToolbar()   → 配置搜索框文本监听
 *     ├─ setupTierFilterChips() → 配置梯级筛选标签监听
 *     ├─ setupOfflineDetection()→ 注册网络状态监听
 *     └─ observeViewModel()     → 订阅 ViewModel 的 LiveData
 *   onDestroyView()      → 注销网络监听、释放 binding
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、MVVM 数据绑定
 * ═══════════════════════════════════════════════════════════════════
 *
 *   HeroListViewModel                    HeroListFragment
 *   ┌──────────────────┐                ┌──────────────────┐
 *   │ heroes: LiveData  │ ──observe──→ │ updateHeroList()  │
 *   │ loading: LiveData │ ──observe──→ │ updateLoadingState│
 *   │ error: LiveData   │ ──observe──→ │ showError()       │
 *   └──────────────────┘                └──────────────────┘
 *
 *   用户交互 → ViewModel 方法调用：
 *   - 输入搜索词 → searchHeroes()
 *   - 点击筛选标签 → filterByTier()
 *   - 滑到底部 → loadMore()
 *   - 网络断开 → setOffline()
 *   - 点击重试 → retry()
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、Fragment 间导航
 * ═══════════════════════════════════════════════════════════════════
 *
 *   HeroListFragment              MainActivity             HeroDetailFragment
 *   ┌──────────────┐             ┌──────────────┐         ┌──────────────────┐
 *   │ 点击英雄卡片  │             │ 实现          │         │                  │
 *   │ heroId=42    │ ──────────→ │ OnHeroSelected│ ──────→ │ 接收 heroId=42   │
 *   │              │  回调接口    │ Listener      │  导航    │ 加载英雄详情     │
 *   └──────────────┘             └──────────────┘         └──────────────────┘
 *
 * @see HeroListViewModel
 * @see HeroCardAdapter
 * @see OnHeroSelectedListener
 * @see PaginationScrollListener
 */
@AndroidEntryPoint
public class HeroListFragment extends Fragment {

    /**
     * ViewBinding 对象 ── 持有布局中所有视图的引用
     *
     * 通过 binding.recyclerHeroList、binding.chipGroupTier 等方式
     * 直接访问 XML 中定义的视图，无需 findViewById。
     *
     * 生命周期：onCreateView() 创建 → onDestroyView() 置 null
     * 为什么 onDestroyView 要置 null？
     * - Fragment 的 View 被销毁后 binding 引用的视图也不存在了
     * - 如果保留引用会导致内存泄漏和空指针异常
     */
    private FragmentHeroListBinding binding;

    /**
     * 搜索输入框 ── 用户输入关键词搜索英雄
     *
     * 从 SearchToolbar 内部获取的 EditText 引用。
     * 添加 TextWatcher 监听输入变化，实时触发搜索。
     */
    private EditText searchEditText;

    /**
     * 英雄列表 ViewModel ── 管理列表数据和业务逻辑
     *
     * 通过 ViewModelProvider 获取，Hilt 自动注入依赖。
     * ViewModel 在配置变更（旋转屏幕）时不会被销毁。
     */
    private HeroListViewModel viewModel;

    /**
     * 英雄卡片适配器 ── 将 HeroUiModel 数据渲染为卡片视图
     *
     * 定义在 core-ui 模块中，被多个 Fragment 共用。
     * 使用 ListAdapter + DiffUtil 实现高效增量更新。
     */
    private HeroCardAdapter adapter;

    /**
     * 英雄选中回调接口 ── 用于通知宿主 Activity 导航到详情页
     *
     * Fragment 不能直接操作其他 Fragment，必须通过 Activity 中转。
     * 在 onAttach() 中从宿主 Activity 获取此接口的实现。
     */
    private OnHeroSelectedListener heroSelectedListener;

    /**
     * 网络状态回调 ── 监听网络连接/断开事件
     *
     * 注册时机：setupOfflineDetection() 中注册
     * 注销时机：onDestroyView() 中注销
     * 必须成对使用，否则会内存泄漏
     */
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * 之前是否离线 ── 用于判断网络恢复时是否需要提示
     *
     * true = 之前处于离线状态，网络恢复时需要提示用户
     * false = 一直在线，网络恢复时不需要提示
     *
     * 使用场景：
     * - 用户一直在线 → 网络短暂断开又恢复 → 需要提示并刷新
     * - 用户从未离线 → 不需要提示
     */
    private boolean wasOffline = false;

    /**
     * Fragment 附加到 Activity 时调用 ── 获取回调接口
     *
     * onAttach() 是 Fragment 生命周期中最早的方法。
     * 在这里检查宿主 Activity 是否实现了 OnHeroSelectedListener 接口。
     *
     * 为什么用 instanceof 检查？
     * - 如果 Activity 没实现接口，heroSelectedListener 会是 null
     * - 点击英雄卡片时 navigateToHeroDetail() 会做 null 检查
     * - 这样比强制转换抛出 ClassCastException 更安全
     *
     * @param context 宿主 Activity 的上下文
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnHeroSelectedListener) {
            heroSelectedListener = (OnHeroSelectedListener) context;
        }
    }

    /**
     * 创建 Fragment 视图 ── 加载 XML 布局
     *
     * 使用 ViewBinding 加载布局，比 setContentView 更类型安全。
     *
     * 为什么在这里获取 searchEditText？
     * - SearchToolbar 是独立的自定义组件，其内部的 EditText 不在
     *   FragmentHeroListBinding 的直接子视图中
     * - 需要通过 binding.getRoot().findViewById() 手动查找
     * - 使用 com.aram.mayhem.ui.R.id.edit_search 引用 core-ui 模块的资源 ID
     *
     * @param inflater  布局填充器
     * @param container 父视图容器
     * @param savedInstanceState 保存的实例状态（配置变更时非 null）
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHeroListBinding.inflate(inflater, container, false);
        searchEditText = binding.getRoot().findViewById(com.aram.mayhem.ui.R.id.edit_search);
        return binding.getRoot();
    }

    /**
     * 视图创建完成 ── 初始化所有组件
     *
     * onViewCreated() 在 onCreateView() 之后调用，此时布局已加载完成，
     * 可以安全地访问所有视图并设置监听器。
     *
     * 初始化顺序很重要：
     * 1. 先获取 ViewModel（后续 setup 方法都需要）
     * 2. setupRecyclerView（适配器创建后，搜索和筛选才能更新列表）
     * 3. setupSearchToolbar（搜索触发 ViewModel 方法，需要 ViewModel 已初始化）
     * 4. setupTierFilterChips（筛选触发 ViewModel 方法）
     * 5. setupOfflineDetection（网络监听需要 ViewModel.setOffline()）
     * 6. observeViewModel（最后订阅，避免遗漏数据更新）
     *
     * @param view               Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HeroListViewModel.class);
        setupRecyclerView();
        setupSearchToolbar();
        setupTierFilterChips();
        setupOfflineDetection();
        observeViewModel();
    }

    /**
     * 设置离线检测 ── 监听网络连接状态变化
     *
     * 使用 ConnectivityManager 注册 NetworkCallback 来监听网络事件：
     * - onLost()：网络断开 → 标记离线 + 提示用户
     * - onAvailable()：网络恢复 → 刷新数据 + 提示用户
     *
     * NetworkRequest 配置：
     * - NET_CAPABILITY_INTERNET：监听有互联网能力的网络
     * - 不指定特定传输类型（WiFi/蜂窝都监听）
     *
     * 为什么用 registerNetworkCallback 而不是 getActiveNetwork()？
     * - getActiveNetwork() 只能查询当前状态，无法监听变化
     * - registerNetworkCallback 可以实时响应网络状态变化
     * - 这样用户断网/恢复时可以立即做出响应
     *
     * Snackbar 提示：
     * - 断网时显示"网络已断开，正在显示缓存数据"（LENGTH_LONG）
     * - 恢复时显示"网络已恢复，正在刷新数据"（LENGTH_SHORT）
     */
    private void setupOfflineDetection() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@NonNull Network network) {
                // 网络断开
                wasOffline = true;
                viewModel.setOffline(true);
                if (getView() != null) {
                    Snackbar.make(getView(), "网络已断开，正在显示缓存数据", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onAvailable(@NonNull Network network) {
                // 网络恢复（只在之前离线过的情况下提示）
                if (wasOffline && getView() != null) {
                    Snackbar.make(getView(), "网络已恢复，正在刷新数据", Snackbar.LENGTH_SHORT).show();
                    viewModel.retry();
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
     * 设置 RecyclerView ── 配置列表视图、适配器和分页加载
     *
     * 执行步骤：
     * 1. 创建 HeroCardAdapter 实例
     * 2. 设置卡片点击监听器（点击 → 导航到详情页）
     * 3. 将适配器绑定到 RecyclerView
     * 4. 设置 LinearLayoutManager（垂直列表）
     * 5. 添加分页滚动监听器
     *
     * PaginationScrollListener 的工作原理：
     * - 监听 RecyclerView 的滚动事件
     * - 当最后一个可见项接近列表底部时（阈值 20 项）触发 onLoadMore()
     * - 通过 isLoading() 和 isLastPage() 防止重复加载
     *
     * 分页加载流程：
     *   用户滑到底部
     *     → PaginationScrollListener.onLoadMore()
     *     → viewModel.loadMore()
     *     → viewModel.loadHeroes(false)
     *     → heroRepository.getHeroes(nextPage, ...)
     *     → 新数据追加到列表末尾
     */
    private void setupRecyclerView() {
        adapter = new HeroCardAdapter();
        adapter.setOnHeroClickListener(this::navigateToHeroDetail);
        binding.recyclerHeroList.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerHeroList.setLayoutManager(layoutManager);

        binding.recyclerHeroList.addOnScrollListener(new PaginationScrollListener(layoutManager, 20) {
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
     * 设置搜索工具栏 ── 监听搜索框的文本变化
     *
     * 使用 TextWatcher 监听输入变化，每次输入都触发搜索。
     *
     * TextWatcher 三个回调：
     * - beforeTextChanged()：输入前（本项目中不使用）
     * - onTextChanged()：输入中（本项目中不使用）
     * - afterTextChanged()：输入完成后触发搜索
     *
     * 为什么用 afterTextChanged 而不是 onTextChanged？
     * - afterTextChanged 在文本已经更新后调用，获取的是最新文本
     * - onTextChanged 在文本即将变化时调用，可能获取到旧文本
     *
     * 注意：这里没有做防抖（debounce），每次按键都会触发搜索。
     * 实际上 SearchToolbar 内部应该有 300ms 防抖机制。
     * 如果没有防抖，快速输入"提莫"会触发 2 次搜索（"提"和"提莫"）。
     */
    private void setupSearchToolbar() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.searchHeroes(s.toString());
            }
        });
    }

    /**
     * 设置梯级筛选标签 ── 监听 ChipGroup 的选中变化
     *
     * ChipGroup 是 Material Design 组件，显示一组可选标签。
     * 用户点击某个梯级标签时，触发 ViewModel 的筛选方法。
     *
     * 筛选流程：
     *   用户点击 "S+" 标签
     *     → onCheckedChanged 回调
     *     → checkedChip.getText() = "S+"
     *     → mapChipTextToTier("S+") = Tier.S_PLUS
     *     → viewModel.filterByTier(Tier.S_PLUS)
     *     → ViewModel 重新加载筛选后的数据
     *
     * 取消筛选：
     * - Material ChipGroup 默认支持取消选中（再次点击已选中的标签）
     * - 取消选中时 checkedId = -1，checkedChip = null
     * - 此时 filterByTier(null) 传入 null，ViewModel 清除筛选条件
     */
    private void setupTierFilterChips() {
        binding.chipGroupTier.setOnCheckedChangeListener((group, checkedId) -> {
            Chip checkedChip = group.findViewById(checkedId);
            if (checkedChip != null) {
                String tierText = checkedChip.getText().toString();
                Tier selectedTier = mapChipTextToTier(tierText);
                viewModel.filterByTier(selectedTier);
            }
        });
    }

    /**
     * 标签文本 → 梯级枚举映射
     *
     * 将 Chip 上显示的文字（如 "S+"）转换为 Tier 枚举。
     * 无法识别的文本返回 null，表示取消筛选。
     *
     * @param text Chip 上显示的梯级文字
     * @return Tier 枚举，或 null（取消筛选）
     */
    private Tier mapChipTextToTier(String text) {
        if ("S+".equals(text)) return Tier.S_PLUS;
        if ("S".equals(text)) return Tier.S;
        if ("A".equals(text)) return Tier.A;
        if ("B".equals(text)) return Tier.B;
        if ("C".equals(text)) return Tier.C;
        return null;
    }

    /**
     * 订阅 ViewModel 的 LiveData ── 建立数据观察关系
     *
     * 使用 getViewLifecycleOwner() 作为 LifecycleOwner：
     * - 只在 Fragment 视图存活期间接收数据更新
     * - 视图销毁后自动取消订阅，避免内存泄漏
     * - 比 this（Fragment 生命周期）更精确
     *
     * 三个观察关系：
     * 1. heroes → updateHeroList()：列表数据变化时更新 RecyclerView
     * 2. loading → updateLoadingState()：加载状态变化时切换 StatefulLayout
     * 3. error → showError()：错误信息变化时显示错误页面
     */
    private void observeViewModel() {
        viewModel.getHeroes().observe(getViewLifecycleOwner(), this::updateHeroList);
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::updateLoadingState);
        viewModel.getError().observe(getViewLifecycleOwner(), this::showError);
    }

    /**
     * 更新英雄列表 ── 将新数据提交给适配器
     *
     * StatefulLayout 状态切换逻辑：
     * - 有数据（heroes 非空非空列表）→ showContent()
     * - 无数据且不在加载中 → showEmpty()
     * - 其他情况保持当前状态
     *
     * adapter.submitList() 的工作原理：
     * - ListAdapter 内部使用 DiffUtil 比较新旧列表
     * - 只更新发生变化的项（增/删/改），不是全部刷新
     * - 在后台线程执行 DiffUtil 计算，不阻塞 UI
     *
     * @param heroes 新的英雄列表数据
     */
    private void updateHeroList(List<HeroUiModel> heroes) {
        adapter.submitList(heroes);
        if (heroes != null && !heroes.isEmpty()) {
            binding.statefulLayout.showContent();
        } else if (heroes != null && heroes.isEmpty() && viewModel.getLoading().getValue() == false) {
            binding.statefulLayout.showEmpty();
        }
    }

    /**
     * 更新加载状态 ── 切换 StatefulLayout 的 Loading/Content 状态
     *
     * 只在 isLoading=true 时切换到 Loading 状态。
     * 切换回 Content 状态由 updateHeroList() 负责（因为需要等数据到达）。
     *
     * @param isLoading 是否正在加载
     */
    private void updateLoadingState(Boolean isLoading) {
        if (isLoading != null) {
            if (isLoading) {
                binding.statefulLayout.showLoading();
            }
        }
    }

    /**
     * 显示错误信息 ── 切换到 Error 状态并提供重试按钮
     *
     * 错误展示方式：
     * 1. StatefulLayout 显示错误页面（带重试按钮）
     * 2. Snackbar 弹出错误提示（带重试操作）
     *
     * 两种展示方式同时使用的原因：
     * - StatefulLayout：占据整个页面，适合首次加载失败
     * - Snackbar：底部弹出提示，适合加载更多失败
     *
     * @param message 错误信息
     */
    private void showError(String message) {
        binding.statefulLayout.showError(message);
        binding.statefulLayout.setOnRetryListener(() -> viewModel.retry());

        if (getView() != null) {
            Snackbar.make(getView(), message != null ? message : "加载失败", Snackbar.LENGTH_LONG)
                    .setAction("重试", v -> viewModel.retry())
                    .show();
        }
    }

    /**
     * 导航到英雄详情页 ── 通过回调接口通知 Activity
     *
     * 执行流程：
     * 1. 用户点击英雄卡片
     * 2. HeroCardAdapter 触发 OnHeroClickListener 回调
     * 3. 本方法被调用，获取 hero.getId()
     * 4. 通过 OnHeroSelectedListener 通知 MainActivity
     * 5. MainActivity 执行导航到 HeroDetailFragment
     *
     * 为什么不直接用 Navigation Component？
     * - 本项目使用手动 Fragment 事务导航
     * - 通过接口回调让 Activity 控制导航，更灵活
     * - 方便在导航时添加过渡动画和参数传递
     *
     * @param hero 被点击的英雄 UI 模型
     */
    private void navigateToHeroDetail(HeroUiModel hero) {
        if (heroSelectedListener != null) {
            heroSelectedListener.onHeroSelected(hero.getId());
        }
    }

    /**
     * Fragment 视图销毁时调用 ── 释放资源和注销监听
     *
     * 必须执行两个清理操作：
     * 1. 注销 NetworkCallback：防止内存泄漏（ConnectivityManager 持有回调引用）
     * 2. binding 置 null：防止视图泄漏（Fragment 视图已销毁但 binding 仍引用）
     *
     * 为什么不注销会导致内存泄漏？
     * - ConnectivityManager 是系统级服务，生命周期比 Fragment 长
     * - 如果不注销，它会持有 networkCallback 的引用
     * - networkCallback 是匿名内部类，隐式持有 Fragment 的引用
     * - 导致 Fragment 无法被 GC 回收
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
        searchEditText = null;
    }
}
