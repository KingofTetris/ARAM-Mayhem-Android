package com.aram.mayhem.feature.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * 个人中心页（个人中心模块）
 *
 * 功能：展示用户资料（头像、昵称、邮箱、投稿数、收藏数）、登录/退出登录、
 *       导航至"我的投稿"和"设置"子页面
 * 导航：我的投稿 → MyStrategiesFragment、设置 → SettingsFragment
 * 关联组件：ProfileViewModel
 *
 * @see ProfileViewModel
 * @see MyStrategiesFragment
 * @see SettingsFragment
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    /** 退出登录后重启 MainActivity 时传递的 Intent 标志键 */
    public static final String EXTRA_LOGGED_OUT = "extra_logged_out";

    /** 视图绑定对象 */
    private FragmentProfileBinding binding;
    /** 个人中心 ViewModel */
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

        // 根据登录状态决定加载用户资料还是显示未登录状态
        if (viewModel.isLoggedIn()) {
            viewModel.loadUserProfile();
        } else {
            showNotLoggedInState();
        }
    }

    /**
     * 设置各 UI 元素的点击监听器
     *
     * 逻辑：用户信息区域和"我的投稿"在未登录时弹出登录对话框；
     *       "设置"无需登录即可进入；退出按钮弹出确认对话框
     */
    private void setupClickListeners() {
        // 未登录时点击用户信息区域弹出登录对话框
        binding.layoutUserInfo.setOnClickListener(v -> {
            if (!viewModel.isLoggedIn()) {
                showLoginDialog();
            }
        });

        // 已登录跳转我的投稿，未登录弹出登录对话框
        binding.menuMyStrategies.setOnClickListener(v -> {
            if (viewModel.isLoggedIn()) {
                Navigation.findNavController(v).navigate(R.id.navigation_my_strategies);
            } else {
                showLoginDialog();
            }
        });

        // 设置页面无需登录即可进入
        binding.menuSettings.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_settings);
        });

        // 退出登录按钮
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }

    /**
     * 观察 ViewModel 的 LiveData 数据变化并更新 UI
     *
     * 监听项：用户资料、加载状态、错误信息、退出登录事件、登录成功事件
     */
    private void observeViewModel() {
        // 监听用户资料变化，绑定到 UI
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::bindUserProfile);

        // 监听加载状态，控制进度条可见性
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        // 监听错误信息，显示在昵称位置
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                binding.textNickname.setText(error);
            }
        });

        // 监听退出登录事件，重启应用并跳转到个人中心页
        viewModel.getLogoutEvent().observe(getViewLifecycleOwner(), loggedOut -> {
            if (Boolean.TRUE.equals(loggedOut)) {
                navigateToLogin();
            }
        });

        // 监听登录成功事件，自动刷新用户资料
        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                viewModel.loadUserProfile();
            }
        });

        // 监听注册成功事件，自动登录
        viewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                android.widget.Toast.makeText(requireContext(), R.string.register_success, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 将用户资料数据绑定到 UI 控件
     *
     * @param profile 用户资料响应对象，null 时跳过绑定
     */
    private void bindUserProfile(UserProfileResponse profile) {
        if (profile == null) return;

        binding.textNickname.setText(profile.nickname);
        binding.textEmail.setText(profile.email);
        binding.textStrategyCount.setText(String.valueOf(profile.strategyCount));
        binding.textFavoriteCount.setText(String.valueOf(profile.favoriteCount));
        // 已登录状态显示退出按钮
        binding.btnLogout.setVisibility(View.VISIBLE);

        // 头像加载：使用 Glide 圆形裁剪
        if (profile.avatarUrl != null && !profile.avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(profile.avatarUrl)
                    .circleCrop()
                    .into(binding.imageAvatar);
        }
    }

    /**
     * 显示未登录状态
     *
     * 逻辑：昵称显示"未登录"、邮箱显示"点击登录"、投稿和收藏归零、隐藏退出按钮
     */
    private void showNotLoggedInState() {
        binding.textNickname.setText(R.string.profile_nickname_default);
        binding.textEmail.setText(R.string.profile_email_default);
        binding.textStrategyCount.setText("0");
        binding.textFavoriteCount.setText("0");
        // 未登录时隐藏退出按钮
        binding.btnLogout.setVisibility(View.GONE);
    }

    /**
     * 显示登录对话框
     *
     * 逻辑：弹出 Material Design 对话框，包含邮箱和密码输入框；
     *       空字段校验 → 调用 ViewModel.login() 发起登录请求
     */
    private void showLoginDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_login, null, false);

        TextInputEditText editEmail = dialogView.findViewById(R.id.edit_email);
        TextInputEditText editPassword = dialogView.findViewById(R.id.edit_password);
        TextView textError = dialogView.findViewById(R.id.text_login_error);
        TextView textGotoRegister = dialogView.findViewById(R.id.text_goto_register);

        androidx.appcompat.app.AlertDialog loginDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.login_title)
                .setView(dialogView)
                .setNegativeButton(R.string.login_cancel, null)
                .setPositiveButton(R.string.login_submit, (dialog, which) -> {
                    String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
                    String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";

                    if (email.isEmpty() || password.isEmpty()) {
                        textError.setText(R.string.login_empty_fields);
                        textError.setVisibility(View.VISIBLE);
                        return;
                    }

                    viewModel.login(email, password);
                })
                .create();

        textGotoRegister.setOnClickListener(v -> {
            Timber.i("Navigation: login_dialog -> register_dialog | userId=anonymous | timestamp=%d", System.currentTimeMillis());
            loginDialog.dismiss();
            showRegisterDialog();
        });

        loginDialog.show();
    }

    /**
     * 显示注册对话框
     *
     * 作用：弹出注册表单，用户输入昵称、邮箱、密码和确认密码
     * 实现：表单校验（空字段、密码一致性）→ 调用 ViewModel.register() → 注册成功后自动登录
     */
    private void showRegisterDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_register, null, false);

        TextInputEditText editNickname = dialogView.findViewById(R.id.edit_register_nickname);
        TextInputEditText editEmail = dialogView.findViewById(R.id.edit_register_email);
        TextInputEditText editPassword = dialogView.findViewById(R.id.edit_register_password);
        TextInputEditText editConfirmPassword = dialogView.findViewById(R.id.edit_register_confirm_password);
        TextView textError = dialogView.findViewById(R.id.text_register_error);
        TextView textGotoLogin = dialogView.findViewById(R.id.text_goto_login);

        androidx.appcompat.app.AlertDialog registerDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.register_title)
                .setView(dialogView)
                .setNegativeButton(R.string.login_cancel, null)
                .setPositiveButton(R.string.register_submit, (dialog, which) -> {
                    String nickname = editNickname.getText() != null ? editNickname.getText().toString().trim() : "";
                    String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
                    String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
                    String confirmPassword = editConfirmPassword.getText() != null ? editConfirmPassword.getText().toString().trim() : "";

                    if (nickname.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                        textError.setText(R.string.register_empty_fields);
                        textError.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (!password.equals(confirmPassword)) {
                        textError.setText(R.string.register_password_mismatch);
                        textError.setVisibility(View.VISIBLE);
                        return;
                    }

                    viewModel.register(email, password, nickname);
                })
                .create();

        textGotoLogin.setOnClickListener(v -> {
            Timber.i("Navigation: register_dialog -> login_dialog | userId=anonymous | timestamp=%d", System.currentTimeMillis());
            registerDialog.dismiss();
            showLoginDialog();
        });

        registerDialog.show();
    }

    /**
     * 显示退出登录确认对话框
     *
     * 逻辑：用户确认后调用 ViewModel.logout() 清除 Token 并触发退出事件
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
     * 退出登录后重启应用
     *
     * 逻辑：通过 PackageManager 获取应用启动 Intent，添加 CLEAR_TASK + NEW_TASK 标志
     *       清除 Activity 栈，附带 EXTRA_LOGGED_OUT 标志让 MainActivity 自动切换到个人中心 Tab
     */
    private void navigateToLogin() {
        Intent intent = requireActivity().getPackageManager()
                .getLaunchIntentForPackage(requireActivity().getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(EXTRA_LOGGED_OUT, true);
            startActivity(intent);
        }
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
