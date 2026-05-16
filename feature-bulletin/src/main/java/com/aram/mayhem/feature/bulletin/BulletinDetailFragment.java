package com.aram.mayhem.feature.bulletin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.aram.mayhem.feature.bulletin.databinding.FragmentBulletinDetailBinding;
import com.aram.mayhem.feature.bulletin.viewmodel.BulletinDetailViewModel;
import com.aram.mayhem.ui.model.BulletinUiModel;
import com.bumptech.glide.Glide;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 公告详情页（公告模块）
 *
 * 功能：展示公告详情内容（标题、时间、正文、图片）
 * 导航：从 BulletinListFragment 点击公告进入
 * 关联组件：BulletinDetailViewModel
 *
 * @see BulletinDetailViewModel
 */
@AndroidEntryPoint
public class BulletinDetailFragment extends Fragment {

    /** 公告ID 参数名 */
    private static final String ARG_BULLETIN_ID = "bulletin_id";

    /** 视图绑定对象 */
    private FragmentBulletinDetailBinding binding;
    /** ViewModel */
    private BulletinDetailViewModel viewModel;
    /** 返回按钮点击监听器 */
    private OnBackListener onBackListener;

    /**
     * 返回按钮回调接口
     */
    public interface OnBackListener {
        /**
         * 返回上一页
         */
        void onBack();
    }

    /**
     * 设置返回按钮点击监听器
     *
     * @param listener 返回监听器
     */
    public void setOnBackListener(OnBackListener listener) {
        this.onBackListener = listener;
    }

    /**
     * 创建公告详情页实例
     *
     * @param bulletinId 公告ID
     * @return BulletinDetailFragment 实例
     */
    public static BulletinDetailFragment newInstance(long bulletinId) {
        BulletinDetailFragment fragment = new BulletinDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_BULLETIN_ID, bulletinId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 创建视图
     *
     * @param inflater 布局填充器
     * @param container 父容器
     * @param savedInstanceState 保存的状态
     * @return 视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBulletinDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化操作
     *
     * @param view 视图
     * @param savedInstanceState 保存的状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BulletinDetailViewModel.class);

        // 设置返回按钮监听
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (onBackListener != null) {
                onBackListener.onBack();
            } else {
                requireActivity().onBackPressed();
            }
        });

        // 获取公告ID并加载详情
        if (getArguments() != null) {
            long bulletinId = getArguments().getLong(ARG_BULLETIN_ID);
            viewModel.loadBulletinDetail(bulletinId);
        }

        observeViewModel();
    }

    /**
     * 观察 ViewModel 数据变化
     */
    private void observeViewModel() {
        // 监听公告详情变化
        viewModel.getBulletinDetail().observe(getViewLifecycleOwner(), this::displayBulletin);
        // 监听加载状态变化
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                binding.progressContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * 显示公告详情数据
     *
     * @param bulletin 公告数据
     */
    private void displayBulletin(BulletinUiModel bulletin) {
        if (bulletin == null) return;

        binding.toolbar.setTitle(bulletin.getTypeDisplay());
        binding.textTitle.setText(bulletin.getTitle());
        binding.textType.setText(bulletin.getTypeDisplay());
        binding.textDate.setText(bulletin.getPublishedAt());
        binding.textContent.setText(bulletin.getContent());

        // 设置公告图片
        if (bulletin.getImageUrl() != null && !bulletin.getImageUrl().isEmpty()) {
            binding.imageBulletin.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(bulletin.getImageUrl())
                    .centerCrop()
                    .into(binding.imageBulletin);
        } else {
            binding.imageBulletin.setVisibility(View.GONE);
        }
    }

    /**
     * 视图销毁时清理资源
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
