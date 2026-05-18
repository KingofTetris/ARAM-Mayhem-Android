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

import com.aram.mayhem.feature.profile.databinding.FragmentSettingsBinding;
import com.aram.mayhem.feature.profile.viewmodel.ProfileViewModel;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * 设置页（个人中心模块）
 *
 * 功能：深色模式切换、通知开关、版本信息
 * 导航：从 ProfileFragment 进入，返回按钮回到 ProfileFragment
 * 关联：ProfileViewModel（更新用户偏好设置）
 */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        setupToolbar();
        setupSwitches();
        setupVersionInfo();
        observeViewModel();
    }

    /**
     * 设置工具栏返回按钮
     */
    private void setupToolbar() {
        binding.toolbarSettings.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    /**
     * 设置开关监听器
     */
    private void setupSwitches() {
        binding.switchDisplayMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int displayMode = isChecked ? 1 : 0;
            Timber.i("Settings: display_mode_change | value=%d | timestamp=%d", displayMode, System.currentTimeMillis());
            viewModel.updateProfile(null, null, displayMode, null);
        });

        binding.switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int notificationEnabled = isChecked ? 1 : 0;
            Timber.i("Settings: notification_change | value=%d | timestamp=%d", notificationEnabled, System.currentTimeMillis());
            viewModel.updateProfile(null, null, null, notificationEnabled);
        });
    }

    /**
     * 设置版本信息
     */
    private void setupVersionInfo() {
        try {
            String versionName = requireActivity().getPackageManager()
                    .getPackageInfo(requireActivity().getPackageName(), 0).versionName;
            binding.textVersion.setText(getString(R.string.settings_version, versionName));
        } catch (Exception e) {
            binding.textVersion.setText(getString(R.string.settings_version, "1.0.0"));
        }
    }

    /**
     * 观察 ViewModel 数据变化，初始化开关状态
     */
    private void observeViewModel() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) return;

            binding.switchDisplayMode.setOnCheckedChangeListener(null);
            binding.switchNotification.setOnCheckedChangeListener(null);

            binding.switchDisplayMode.setChecked(profile.displayMode == 1);
            binding.switchNotification.setChecked(profile.notificationEnabled == 1);

            setupSwitches();
        });

        viewModel.getUpdateSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Timber.i("Settings: save_success | timestamp=%d", System.currentTimeMillis());
                android.widget.Toast.makeText(requireContext(), "设置已保存", android.widget.Toast.LENGTH_SHORT).show();
            } else if (Boolean.FALSE.equals(success)) {
                Timber.w("Settings: save_fail | timestamp=%d", System.currentTimeMillis());
                android.widget.Toast.makeText(requireContext(), "保存失败，请重试", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
