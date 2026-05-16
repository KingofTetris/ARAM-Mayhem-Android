package com.aram.mayhem.feature.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.aram.mayhem.feature.profile.databinding.FragmentProfileBinding;
import com.aram.mayhem.feature.profile.viewmodel.ProfileViewModel;
import com.aram.mayhem.network.dto.UserProfileResponse;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 用户资料页（个人中心模块）
 *
 * 功能：展示用户信息（头像、昵称、邮箱、攻略数、收藏数）、功能菜单、退出登录
 * 导航：我的投稿 → MyStrategiesFragment；设置 → SettingsFragment
 * 关联：ProfileViewModel, UserProfileResponse
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupClickListeners();
        observeViewModel();

        viewModel.loadUserProfile();
    }

    /**
     * 设置点击事件监听器
     */
    private void setupClickListeners() {
        binding.menuMyStrategies.setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_navigation_profile_to_myStrategies);
        });

        binding.menuSettings.setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_navigation_profile_to_settings);
        });

        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }

    /**
     * 观察 ViewModel 数据变化
     */
    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::bindUserProfile);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                binding.textNickname.setText(error);
            }
        });

        viewModel.getLogoutEvent().observe(getViewLifecycleOwner(), loggedOut -> {
            if (Boolean.TRUE.equals(loggedOut)) {
                navigateToLogin();
            }
        });
    }

    /**
     * 绑定用户资料数据到 UI
     *
     * @param profile 用户资料响应
     */
    private void bindUserProfile(UserProfileResponse profile) {
        if (profile == null) return;

        binding.textNickname.setText(profile.nickname);
        binding.textEmail.setText(profile.email);
        binding.textStrategyCount.setText(String.valueOf(profile.strategyCount));
        binding.textFavoriteCount.setText(String.valueOf(profile.favoriteCount));

        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(profile.avatarUrl)
                    .circleCrop()
                    .into(binding.imageAvatar);
        }
    }

    /**
     * 显示退出登录确认对话框
     */
    private void showLogoutConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_logout)
                .setMessage(R.string.profile_logout_confirm)
                .setNegativeButton(R.string.profile_logout_cancel, null)
                .setPositiveButton(R.string.profile_logout_ok, (dialog, which) -> viewModel.logout())
                .show();
    }

    /**
     * 退出登录后跳转到登录页
     *
     * 作用：清除 Token 后重定向到登录界面
     * 实现：通过导航图 action 跳转，或启动 LoginActivity
     */
    private void navigateToLogin() {
        Intent loginIntent = new Intent();
        loginIntent.setPackage(requireActivity().getPackageName());
        loginIntent.setAction("com.aram.mayhem.ACTION_LOGIN");
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (loginIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(loginIntent);
            requireActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
