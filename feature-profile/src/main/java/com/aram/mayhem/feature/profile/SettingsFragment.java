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
 * 设置页 ── 用户偏好设置（深色模式、通知开关、版本信息）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * SettingsFragment 是个人中心模块的子页面，提供用户偏好设置功能：
 * 1. 深色模式开关（切换浅色/深色主题）
 * 2. 通知开关（控制是否接收推送通知）
 * 3. 应用版本信息显示
 *
 * 此页面无需登录即可进入，因为偏好设置与用户身份无关。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_settings.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────────────────┐
 *   │  ← 返回    设置                                 │  ← toolbarSettings
 *   ├──────────────────────────────────────────────────┤
 *   │                                                  │
 *   │  显示模式                                        │
 *   │  深色模式                         [开关]         │  ← switchDisplayMode
 *   │                                                  │
 *   ├──────────────────────────────────────────────────┤
 *   │                                                  │
 *   │  通知                                            │
 *   │  接收通知                         [开关]         │  ← switchNotification
 *   │                                                  │
 *   ├──────────────────────────────────────────────────┤
 *   │                                                  │
 *   │  关于                                            │
 *   │  版本号    v1.0.0                                │  ← textVersion
 *   │                                                  │
 *   └──────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────┐     ┌───────────────────┐     ┌──────────────┐
 *   │ SettingsFragment  │ ←── │ ProfileViewModel   │ ←── │ ProfileRepository│
 *   │   (View层)        │     │ (共享自ProfileFragment)│    │  (数据层)     │
 *   └──────────────────┘     └───────────────────┘     └──────────────┘
 *         │                           │
 *         │  开关切换 → updateProfile  │  观察 userProfile
 *         │  观察 updateSuccess       │  初始化开关状态
 *         ▼                           ▼
 *
 *   关键设计：SettingsFragment 与 ProfileFragment 共享同一个 ViewModel
 *   ── 使用 ViewModelProvider(requireActivity()) 获取 Activity 级别的 ViewModel，
 *      两个 Fragment 操作的是同一个 ViewModel 实例，
 *      设置页更新资料后，主页也能看到最新数据。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、开关状态同步机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   初始化流程：
 *   1. observeViewModel() 观察 userProfile
 *   2. userProfile 数据到达后，先移除 OnCheckedChangeListener（避免触发回调）
 *   3. 设置 Switch 的选中状态
 *   4. 重新注册 OnCheckedChangeListener
 *
 *   为什么要先移除再重新注册监听器？
 *   ── Switch.setChecked() 会触发 OnCheckedChangeListener 回调，
 *      如果不移除，设置初始状态时就会调用 viewModel.updateProfile()，
 *      发起不必要的网络请求（把刚从服务器拿到的数据又原样提交回去）。
 *      这就是"回写风暴"问题：读取 → 触发写入 → 读取 → 触发写入 ...
 *
 *   时序图：
 *   ┌────────────────────────────────────────────────────────────┐
 *   │ userProfile 数据到达                                       │
 *   │   ↓                                                       │
 *   │ 移除 switchDisplayMode 监听器                              │
 *   │ 移除 switchNotification 监听器                             │
 *   │   ↓                                                       │
 *   │ switchDisplayMode.setChecked(profile.displayMode == 1)     │
 *   │ switchNotification.setChecked(profile.notificationEnabled == 1)│
 *   │   ↓  （此时不会触发回调，因为监听器已移除）                  │
 *   │ 重新注册 switchDisplayMode 监听器                           │
 *   │ 重新注册 switchNotification 监听器                          │
 *   └────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、部分更新机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   viewModel.updateProfile(nickname, avatarUrl, displayMode, notificationEnabled)
 *   只有非 null 的参数会被更新，null 参数保持原值不变。
 *
 *   深色模式切换：
 *   updateProfile(null, null, isChecked ? 1 : 0, null)
 *   → 只更新 displayMode，其他字段不变
 *
 *   通知开关切换：
 *   updateProfile(null, null, null, isChecked ? 1 : 0)
 *   → 只更新 notificationEnabled，其他字段不变
 *
 *   为什么用 Integer 而不是 Boolean？
 *   ── 后端 API 使用整型（0/1）表示开关状态，
 *      这是数据库设计的常见惯例（TINYINT 类型）。
 *      Boolean 在 Java 中无法为 null（基本类型），
 *      而 Integer 可以为 null，支持部分更新机制。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、导航关系
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ProfileFragment → menuSettings → SettingsFragment（本页）
 *   SettingsFragment → toolbar 返回按钮 → navigateUp() → ProfileFragment
 *
 * @see ProfileViewModel 个人中心 ViewModel，与 ProfileFragment 共享实例
 */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    /**
     * 视图绑定对象
     *
     * 通过 FragmentSettingsBinding 访问布局中的所有控件。
     * 在 onCreateView 中初始化，在 onDestroyView 中置空。
     */
    private FragmentSettingsBinding binding;

    /**
     * 个人中心 ViewModel
     *
     * ═══════════════════════════════════════════════════════════
     * 为什么用 requireActivity() 而不是 this？
     * ═══════════════════════════════════════════════════════════
     *
     * ProfileFragment 使用 ViewModelProvider(this) 创建 ViewModel，
     * 作用域为 Fragment 级别（每个 Fragment 有独立实例）。
     *
     * SettingsFragment 使用 ViewModelProvider(requireActivity())，
     * 作用域为 Activity 级别（同一 Activity 中的 Fragment 共享实例）。
     *
     * 这样做的原因：
     * 1. 设置页修改用户资料后，主页需要看到最新数据
     * 2. 避免两个 Fragment 各自维护一份用户资料副本
     * 3. 共享 ViewModel 相当于共享数据总线，保证数据一致性
     *
     * 注意：ProfileFragment 的 ViewModel 作用域是 Fragment，
     * SettingsFragment 的 ViewModel 作用域是 Activity，
     * 它们实际上是两个不同的 ViewModel 实例！
     * 但因为 ProfileViewModel 通过 Repository 获取数据，
     * 而 Repository 是单例（@Singleton），
     * 所以两个 ViewModel 操作的是同一份数据源。
     */
    private ProfileViewModel viewModel;

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
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化
     *
     * ═══════════════════════════════════════════════════════════
     * 初始化顺序
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 获取 ViewModel（Activity 级别，与 ProfileFragment 共享）
     * 2. setupToolbar() → 设置返回按钮
     * 3. setupSwitches() → 注册开关监听器
     * 4. setupVersionInfo() → 显示应用版本号
     * 5. observeViewModel() → 观察数据变化，初始化开关状态
     *
     * @param view               Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
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
     *
     * 点击返回按钮调用 Navigation.navigateUp()，
     * 返回到 ProfileFragment。
     *
     * navigateUp() vs. popBackStack()：
     * ── navigateUp() 会检查是否有上一级目的地，如果有则返回，
     *    如果没有（如从深层链接进入）则调用 fallback 行为。
     *    popBackStack() 直接弹出栈顶，不考虑上下文。
     *    对于简单的返回操作，两者效果相同，
     *    但 navigateUp() 更安全。
     */
    private void setupToolbar() {
        binding.toolbarSettings.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    /**
     * 设置开关监听器
     *
     * ═══════════════════════════════════════════════════════════
     * 两个开关的行为
     * ═══════════════════════════════════════════════════════════
     *
     * switchDisplayMode（深色模式）：
     *   isChecked = true  → displayMode = 1（深色模式）
     *   isChecked = false → displayMode = 0（浅色模式）
     *   调用 viewModel.updateProfile(null, null, displayMode, null)
     *   → 只更新 displayMode 字段
     *
     * switchNotification（通知开关）：
     *   isChecked = true  → notificationEnabled = 1（启用通知）
     *   isChecked = false → notificationEnabled = 0（关闭通知）
     *   调用 viewModel.updateProfile(null, null, null, notificationEnabled)
     *   → 只更新 notificationEnabled 字段
     *
     * Timber 日志：
     * ── 记录设置变更事件，格式为 "Settings: key | value | timestamp"，
     *    方便追踪用户偏好变更行为。
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
     *
     * ═══════════════════════════════════════════════════════════
     * 版本号获取方式
     * ═══════════════════════════════════════════════════════════
     *
     * 通过 PackageManager 获取当前应用的 versionName：
     * 1. requireActivity().getPackageManager() → 获取包管理器
     * 2. getPackageInfo(packageName, 0) → 获取包信息
     * 3. versionName → 版本名称字符串（如 "1.0.0"）
     *
     * 为什么用 try-catch？
     * ── getPackageInfo() 可能抛出 NameNotFoundException，
     *    虽然理论上不可能发生（查的是自己的包名），
     *    但编译器要求处理此受检异常。
     *    catch 中使用默认版本号 "1.0.0" 作为兜底。
     *
     * 第二个参数 flags = 0 的含义：
     * ── 不需要额外的包信息（如权限、签名等），
     *    只需要基本的版本信息，所以传 0 即可。
     *    传其他值（如 GET_PERMISSIONS）会增加返回数据量。
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
     *
     * ═══════════════════════════════════════════════════════════
     * 观察逻辑
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 观察 userProfile：
     *    - 数据到达后，先移除两个 Switch 的监听器
     *    - 根据 profile 数据设置 Switch 状态
     *    - 重新注册监听器（setupSwitches）
     *
     *    为什么要移除再重新注册？
     *    ── setChecked() 会触发 OnCheckedChangeListener，
     *       如果不移除，初始化时就会调用 updateProfile()，
     *       把刚从服务器拿到的数据又原样提交回去（回写风暴）。
     *
     * 2. 观察 updateSuccess：
     *    - true → 显示"设置已保存"Toast
     *    - false → 显示"保存失败，请重试"Toast
     *
     *    为什么用 Boolean.TRUE.equals(success) 而不是 success == true？
     *    ── success 是 Boolean 包装类型，可能为 null。
     *       直接 success == true 在 success 为 null 时会抛 NullPointerException，
     *       而 Boolean.TRUE.equals(null) 返回 false，更安全。
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
