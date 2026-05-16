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
 * 我的攻略列表页（个人中心模块）
 *
 * 功能：展示当前用户发布的攻略列表，支持左滑删除和按钮删除
 * 导航：从 ProfileFragment 进入，滑动或点击删除按钮弹出确认对话框
 * 交互：左滑露出红色删除背景 → 松手弹出确认 → 确认后调用 ViewModel 删除
 * 关联：MyStrategiesViewModel, MyStrategiesAdapter, ItemTouchHelper
 *
 * @see MyStrategiesViewModel
 * @see MyStrategiesAdapter
 */
@AndroidEntryPoint
public class MyStrategiesFragment extends Fragment {

    /** 视图绑定对象 */
    private FragmentMyStrategiesBinding binding;
    /** 攻略列表 ViewModel */
    private MyStrategiesViewModel viewModel;
    /** 攻略列表适配器 */
    private MyStrategiesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMyStrategiesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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
     */
    private void setupToolbar() {
        binding.toolbarMyStrategies.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    /**
     * 设置 RecyclerView 和适配器
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
     * 实现：ItemTouchHelper 监听左滑方向（START），滑动时移动前景卡片露出红色删除背景；
     *       松手后弹出确认对话框，取消则回弹 item 到原位
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
                    // 左滑完成后弹出确认对话框
                    showSwipeDeleteConfirmDialog(strategy, position);
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                // 仅移动前景卡片，底层红色删除背景保持不动
                View foregroundView = ((MyStrategiesAdapter.ViewHolder) viewHolder).cardForeground;
                getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // 恢复前景卡片到默认位置
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
     */
    private void observeViewModel() {
        // 攻略列表数据
        viewModel.getMyStrategies().observe(getViewLifecycleOwner(), strategies -> {
            adapter.setStrategies(strategies);
            binding.textEmpty.setVisibility(strategies == null || strategies.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerMyStrategies.setVisibility(strategies == null || strategies.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // 加载状态
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        // 删除成功后刷新列表
        viewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                viewModel.loadMyStrategies();
            }
        });
    }

    /**
     * 显示删除确认对话框（按钮删除触发）
     *
     * @param strategy 待删除的攻略
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
     * 与按钮删除的区别：取消时需要将 item 回弹到原位
     *
     * @param strategy 待删除的攻略
     * @param position 列表中的位置
     */
    private void showSwipeDeleteConfirmDialog(StrategyListResponse strategy, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.my_strategies_delete_confirm)
                .setNegativeButton(R.string.profile_logout_cancel, (dialog, which) -> {
                    // 取消删除：回弹 item 到原位
                    adapter.notifyItemChanged(position);
                })
                .setPositiveButton(R.string.my_strategies_delete_ok, (dialog, which) -> {
                    viewModel.deleteStrategy(strategy.getId(), position);
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
