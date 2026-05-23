package com.aram.mayhem.feature.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aram.mayhem.feature.profile.adapter.MyStrategiesAdapter;
import com.aram.mayhem.feature.profile.databinding.FragmentMyStrategiesBinding;
import com.aram.mayhem.feature.profile.viewmodel.MyStrategiesViewModel;
import com.aram.mayhem.network.dto.StrategyListResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 我的攻略列表页 ── 展示和管理当前用户发布的攻略
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * MyStrategiesFragment 是个人中心模块的子页面，用于展示当前登录用户
 * 发布的所有攻略列表，并提供删除功能。
 *
 * 核心功能：
 * 1. 展示用户发布的攻略列表（标题、英雄名、投票数）
 * 2. 支持两种删除方式：左滑删除 + 按钮删除
 * 3. 删除前弹出确认对话框，防止误操作
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_my_strategies.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────────────────┐
 *   │  ← 返回    我的攻略                              │  ← toolbarMyStrategies
 *   ├──────────────────────────────────────────────────┤
 *   │                                                  │
 *   │  ┌──────────────────────────────────────────┐   │
 *   │  │  攻略标题                    🗑           │   │  ← item_my_strategy
 *   │  │  英雄名称                                │   │
 *   │  │  👍 12  👎 3                             │   │
 *   │  └──────────────────────────────────────────┘   │
 *   │  ┌──────────────────────────────────────────┐   │
 *   │  │  攻略标题                    🗑           │   │
 *   │  │  英雄名称                                │   │
 *   │  │  👍 8   👎 1                             │   │
 *   │  └──────────────────────────────────────────┘   │
 *   │  ...                                             │
 *   │                                                  │
 *   │  （空状态）暂无攻略                              │  ← textEmpty（列表为空时显示）
 *   │                                                  │
 *   ├──────────────────────────────────────────────────┤
 *   │  加载中...                                       │  ← progressLoading（默认隐藏）
 *   └──────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌─────────────────────┐     ┌────────────────────┐     ┌──────────────┐
 *   │ MyStrategiesFragment │ ←── │ MyStrategiesViewModel│ ←── │ CommunityApi │
 *   │   (View层)           │     │   (ViewModel层)      │     │  (网络层)    │
 *   └─────────────────────┘     └────────────────────┘     └──────────────┘
 *         │                              │                        │
 *         │  观察 LiveData               │  调用 API 方法         │  HTTP 请求
 *         │  更新 RecyclerView           │  处理删除逻辑          │
 *         ▼                              ▼                        ▼
 *   ┌──────────────────┐        ┌──────────────────┐     ┌──────────────┐
 *   │ MyStrategiesAdapter│       │ LiveData:        │     │ GET          │
 *   │ 列表项绑定/删除    │       │ myStrategies     │     │  /strategies/mine│
 *   │                    │       │ loading          │     │ DELETE       │
 *   │ ItemTouchHelper    │       │ deleteSuccess    │     │  /strategies/{id}│
 *   │ 左滑手势处理       │       │ error            │     └──────────────┘
 *   └──────────────────┘        └──────────────────┘
 *
 *   为什么直接用 CommunityApi 而不是 StrategyRepository？
 *   ── "我的攻略"页面数据量小且不需要缓存（每次打开都重新加载），
 *      直接使用 API 更简单高效。StrategyRepository 是社区模块的仓库，
 *      包含缓存逻辑，对本页面来说是过度设计。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、两种删除方式
 * ═══════════════════════════════════════════════════════════════════
 *
 *   方式 1 ── 删除图标按钮（imageDelete）：
 *   ┌────────────────────────────────────────────────────────┐
 *   │ 用户点击列表项右侧的 🗑 图标                            │
 *   │   ↓                                                    │
 *   │ adapter.setOnDeleteClickListener 回调                   │
 *   │   ↓                                                    │
 *   │ showDeleteConfirmDialog(strategy, position)             │
 *   │   ↓                                                    │
 *   │ 用户确认 → viewModel.deleteStrategy(id, position)       │
 *   │   ↓                                                    │
 *   │ 删除成功 → viewModel.loadMyStrategies() 重新加载        │
 *   └────────────────────────────────────────────────────────┘
 *
 *   方式 2 ── 左滑手势（ItemTouchHelper）：
 *   ┌────────────────────────────────────────────────────────┐
 *   │ 用户向左滑动列表项                                      │
 *   │   ↓                                                    │
 *   │ ItemTouchHelper.onSwiped() 回调                         │
 *   │   ↓                                                    │
 *   │ showSwipeDeleteConfirmDialog(strategy, position)        │
 *   │   ↓                                                    │
 *   │ 用户确认 → viewModel.deleteStrategy(id, position)       │
 *   │   用户取消 → adapter.notifyItemChanged(position) 回弹   │
 *   │   ↓                                                    │
 *   │ 删除成功 → viewModel.loadMyStrategies() 重新加载        │
 *   └────────────────────────────────────────────────────────┘
 *
 *   两种方式的关键区别：
 *   - 按钮删除：取消时无需额外操作（item 位置没变）
 *   - 左滑删除：取消时需要回弹 item 到原位（item 已被滑动偏移）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、左滑删除的实现原理
 * ═══════════════════════════════════════════════════════════════════
 *
 *   列表项布局采用 FrameLayout 分层结构：
 *
 *   ┌──────────────────────────────────────────┐
 *   │  底层：红色删除背景（deleteBackground）   │  ← 始终存在，被前景遮挡
 *   │  ┌──────────────────────────────────┐    │
 *   │  │  前景卡片（cardForeground）       │    │  ← ItemTouchHelper 移动此层
 *   │  │  攻略内容...                      │    │
 *   │  └──────────────────────────────────┘    │
 *   └──────────────────────────────────────────┘
 *
 *   正常状态：前景卡片完全覆盖底层红色背景
 *   左滑时：  ItemTouchHelper 移动前景卡片，露出红色背景
 *   松手后：  弹出确认对话框
 *   取消删除：前景卡片回弹到原位
 *
 *   ItemTouchHelper 关键回调：
 *   - onSwiped()：滑动超过阈值后触发，弹出确认对话框
 *   - onChildDraw()：滑动过程中调用，只移动前景卡片（不移动底层）
 *   - clearView()：滑动结束时调用，恢复前景卡片到默认位置
 *   - getSwipeThreshold()：返回 0.5f，表示滑动超过 item 宽度 50% 触发 onSwiped
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、删除策略：重新加载 vs. 本地移除
 * ═══════════════════════════════════════════════════════════════════
 *
 *   本项目采用"删除成功后重新加载列表"策略：
 *   - 删除成功 → deleteSuccess = true → loadMyStrategies() 重新请求
 *   - 而不是直接从本地列表中移除 item
 *
 *   选择重新加载的原因：
 *   1. 数据一致性：确保本地列表与服务器完全一致
 *   2. 实现简单：不需要处理本地移除后的边界情况
 *   3. 数据量小：用户发布的攻略通常不多，重新加载很快
 *
 *   如果未来需要更快的响应速度，可以改为乐观删除：
 *   - 先从本地列表移除 item
 *   - 如果后端删除失败，再把 item 加回来
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、导航关系
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ProfileFragment → menuMyStrategies → MyStrategiesFragment（本页）
 *   MyStrategiesFragment → toolbar 返回按钮 → navigateUp() → ProfileFragment
 *
 * @see MyStrategiesViewModel  我的攻略 ViewModel，管理列表数据和删除操作
 * @see MyStrategiesAdapter    列表适配器，处理数据绑定和删除按钮
 * @see StrategyListResponse   攻略列表项的数据模型
 */
@AndroidEntryPoint
public class MyStrategiesFragment extends Fragment {

    /**
     * 视图绑定对象
     *
     * 通过 FragmentMyStrategiesBinding 访问布局中的所有控件。
     * 在 onCreateView 中初始化，在 onDestroyView 中置空。
     */
    private FragmentMyStrategiesBinding binding;

    /**
     * 我的攻略 ViewModel
     *
     * 通过 ViewModelProvider(this) 获取，作用域为当前 Fragment。
     * 与 ProfileFragment 的 ViewModel 不同，MyStrategiesViewModel
     * 是独立的，不需要与其他 Fragment 共享数据。
     *
     * 为什么不需要共享？
     * ── "我的攻略"页面是独立的功能页面，数据自给自足。
     *    不需要与 ProfileFragment 通信（删除操作后，
     *    ProfileFragment 会在 onResume 时重新加载用户资料，
     *    自动更新投稿数）。
     */
    private MyStrategiesViewModel viewModel;

    /**
     * 攻略列表适配器
     *
     * MyStrategiesAdapter 负责将攻略数据绑定到列表项视图上，
     * 并处理删除按钮的点击事件。
     *
     * 与社区模块的 StrategyCardAdapter 的区别：
     * - 本适配器有删除功能（按钮 + 左滑）
     * - 本适配器没有点击跳转详情功能
     * - 本适配器的 ViewHolder 是 public static（ItemTouchHelper 需要访问）
     */
    private MyStrategiesAdapter adapter;

    /**
     * 创建 Fragment 的视图
     *
     * @param inflater           布局填充器
     * @param container          父容器
     * @param savedInstanceState 保存的实例状态
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyStrategiesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化
     *
     * ═══════════════════════════════════════════════════════════
     * 初始化顺序
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 获取 ViewModel 实例
     * 2. setupToolbar() → 设置返回按钮
     * 3. setupRecyclerView() → 初始化列表和适配器
     * 4. setupSwipeToDelete() → 注册左滑删除手势
     * 5. observeViewModel() → 观察数据变化
     * 6. viewModel.loadMyStrategies() → 首次加载攻略列表
     *
     * @param view               Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MyStrategiesViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupSwipeToDelete();
        observeViewModel();

        viewModel.loadMyStrategies();
    }

    /**
     * 设置工具栏返回按钮
     *
     * 点击返回按钮调用 Navigation.navigateUp()，
     * 返回到 ProfileFragment。
     */
    private void setupToolbar() {
        binding.toolbarMyStrategies.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    /**
     * 设置 RecyclerView 和适配器
     *
     * ═══════════════════════════════════════════════════════════
     * 配置详情
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 创建 MyStrategiesAdapter 实例
     * 2. 设置删除按钮点击监听器 → showDeleteConfirmDialog()
     * 3. 设置 LinearLayoutManager（垂直列表）
     * 4. 将适配器绑定到 RecyclerView
     *
     * 为什么用 LinearLayoutManager 而不是 GridLayoutManager？
     * ── 攻略列表项包含标题、英雄名、投票数等信息，
     *    水平空间需要完整展示，不适合多列网格布局。
     */
    private void setupRecyclerView() {
        adapter = new MyStrategiesAdapter();
        adapter.setOnDeleteClickListener(this::showDeleteConfirmDialog);

        binding.recyclerMyStrategies.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMyStrategies.setAdapter(adapter);
    }

    /**
     * 设置左滑删除手势
     *
     * ═══════════════════════════════════════════════════════════
     * ItemTouchHelper 配置
     * ═══════════════════════════════════════════════════════════
     *
     * SimpleCallback 参数：
     * - dragDirs = 0：不支持拖拽排序
     * - swipeDirs = ItemTouchHelper.START：只支持向左滑动
     *   （START 在从左到右的语言环境中等于向左滑）
     *
     * ═══════════════════════════════════════════════════════════
     * 回调方法详解
     * ═══════════════════════════════════════════════════════════
     *
     * onMove()：返回 false，不支持拖拽排序
     *
     * onSwiped()：滑动超过阈值后触发
     *   → 获取被滑动项的攻略数据
     *   → 弹出确认对话框
     *   → 注意：此时 item 的视觉位置已经偏移，如果取消需要手动回弹
     *
     * onChildDraw()：滑动过程中持续调用
     *   → 只移动前景卡片（cardForeground），底层红色背景保持不动
     *   → 使用 getDefaultUIUtil().onDraw() 处理默认的滑动动画
     *   → 如果不重写此方法，整个 item（包括红色背景）都会被移动，
     *     就看不到左滑删除的视觉效果了
     *
     * clearView()：滑动交互结束时调用
     *   → 恢复前景卡片到默认位置
     *   → 使用 getDefaultUIUtil().clearView() 清除滑动状态
     *
     * getSwipeThreshold()：返回 0.5f
     *   → 滑动距离超过 item 宽度的 50% 时触发 onSwiped
     *   → 阈值越高，需要滑得越远才能触发删除
     *   → 0.5f 是一个平衡值：不会太容易误触，也不会太难触发
     */
    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                StrategyListResponse strategy = adapter.getItem(position);
                if (strategy != null) {
                    showSwipeDeleteConfirmDialog(strategy, position);
                }
            }

            /**
             * 滑动过程中的绘制回调
             *
             * 关键：只移动前景卡片，不移动底层红色背景。
             *
             * 实现方式：
             * 1. 从 ViewHolder 中获取 cardForeground 视图
             * 2. 将 foregroundView 传给 getDefaultUIUtil().onDraw()
             * 3. getDefaultUIUtil() 会处理平移动画和透明度变化
             *
             * 为什么需要强制转型为 MyStrategiesAdapter.ViewHolder？
             * ── RecyclerView.ViewHolder 是通用类型，没有 cardForeground 字段。
             *    MyStrategiesAdapter.ViewHolder 是 public static class，
             *    暴露了 cardForeground 字段供 ItemTouchHelper 访问。
             */
            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View foregroundView = ((MyStrategiesAdapter.ViewHolder) viewHolder).cardForeground;
                getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive);
            }

            /**
             * 滑动交互结束时的清理
             *
             * 恢复前景卡片到默认位置，清除滑动状态。
             * 此方法在以下场景调用：
             * - 用户松手且未达到滑动阈值（item 回弹）
             * - 用户松手且达到滑动阈值（onSwiped 已触发）
             */
            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                View foregroundView = ((MyStrategiesAdapter.ViewHolder) viewHolder).cardForeground;
                getDefaultUIUtil().clearView(foregroundView);
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.5f;
            }
        }).attachToRecyclerView(binding.recyclerMyStrategies);
    }

    /**
     * 观察 ViewModel 数据变化
     *
     * ═══════════════════════════════════════════════════════════
     * LiveData 观察一览
     * ═══════════════════════════════════════════════════════════
     *
     *   LiveData        观察行为                          触发时机
     *   ──────────────────────────────────────────────────────────────
     *   myStrategies    更新适配器数据 + 控制空状态显示   加载完成
     *   loading         控制进度条可见性                  请求开始/结束
     *   deleteSuccess   删除成功后重新加载列表            删除操作完成
     *
     * 空状态处理：
     * - 列表为空或 null → 显示 textEmpty，隐藏 RecyclerView
     * - 列表非空 → 隐藏 textEmpty，显示 RecyclerView
     */
    private void observeViewModel() {
        viewModel.getMyStrategies().observe(getViewLifecycleOwner(), strategies -> {
            adapter.setStrategies(strategies);
            binding.textEmpty.setVisibility(strategies == null || strategies.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerMyStrategies.setVisibility(strategies == null || strategies.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                viewModel.loadMyStrategies();
            }
        });
    }

    /**
     * 显示删除确认对话框（按钮删除触发）
     *
     * ═══════════════════════════════════════════════════════════
     * 与滑动删除确认对话框的区别
     * ═══════════════════════════════════════════════════════════
     *
     * 按钮删除的取消操作：
     * - 不需要回弹 item（item 位置没有变化）
     * - 直接关闭对话框即可
     *
     * 滑动删除的取消操作：
     * - 需要回弹 item（item 已被滑动偏移）
     * - 调用 adapter.notifyItemChanged(position) 恢复原位
     *
     * @param strategy 待删除的攻略数据
     * @param position 列表中的位置
     */
    private void showDeleteConfirmDialog(StrategyListResponse strategy, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.my_strategies_delete_confirm)
                .setNegativeButton(R.string.profile_logout_cancel, null)
                .setPositiveButton(R.string.my_strategies_delete_ok, (dialog, which) -> {
                    viewModel.deleteStrategy(strategy.getId(), position);
                })
                .show();
    }

    /**
     * 显示滑动删除确认对话框（左滑触发）
     *
     * ═══════════════════════════════════════════════════════════
     * 关键区别：取消时需要回弹 item
     * ═══════════════════════════════════════════════════════════
     *
     * 左滑触发 onSwiped() 后，item 的视觉位置已经偏移
     * （前景卡片被滑到了左侧）。如果用户取消删除，
     * 需要调用 adapter.notifyItemChanged(position) 让
     * RecyclerView 重新绑定该 item，从而将前景卡片
     * 恢复到原始位置。
     *
     * 如果不回弹会怎样？
     * ── item 会停留在滑动后的偏移位置，前景卡片
     *    遮挡不住底层红色背景，视觉效果很奇怪。
     *
     * 为什么按钮删除不需要回弹？
     * ── 按钮删除时 item 没有被滑动，位置没有变化，
     *    取消对话框后 item 保持原样即可。
     *
     * @param strategy 待删除的攻略数据
     * @param position 列表中的位置
     */
    private void showSwipeDeleteConfirmDialog(StrategyListResponse strategy, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.my_strategies_delete_confirm)
                .setNegativeButton(R.string.profile_logout_cancel, (dialog, which) -> {
                    adapter.notifyItemChanged(position);
                })
                .setPositiveButton(R.string.my_strategies_delete_ok, (dialog, which) -> {
                    viewModel.deleteStrategy(strategy.getId(), position);
                })
                .show();
    }

    /**
     * Fragment 视图销毁时的清理
     *
     * 置空 binding 引用以避免内存泄漏。
     * Fragment 的视图在 onDestroyView 后被销毁，
     * 此时 binding 持有的 View 引用已无效，
     * 必须置空以避免异步回调访问已销毁的视图。
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
