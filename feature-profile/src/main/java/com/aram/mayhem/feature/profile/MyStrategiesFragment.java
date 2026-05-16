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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.feature.profile.adapter.MyStrategiesAdapter;
import com.aram.mayhem.feature.profile.databinding.FragmentMyStrategiesBinding;
import com.aram.mayhem.feature.profile.viewmodel.MyStrategiesViewModel;
import com.aram.mayhem.network.dto.StrategyListResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 我的攻略列表页（个人中心模块）
 *
 * 功能：展示当前用户发布的攻略列表，支持删除操作
 * 导航：从 ProfileFragment 进入，点击删除按钮弹出确认对话框
 * 关联：MyStrategiesViewModel, MyStrategiesAdapter
 */
@AndroidEntryPoint
public class MyStrategiesFragment extends Fragment {

    private FragmentMyStrategiesBinding binding;
    private MyStrategiesViewModel viewModel;
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
     * 观察 ViewModel 数据变化
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
     * 显示删除确认对话框
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
