package com.aram.mayhem.feature.profile;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

/**
 * 个人中心页 ── 用户资料展示、登录/注册/退出的统一入口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * ProfileFragment 是应用底部导航栏"个人中心"Tab 对应的主页面。
 * 它承担两大职责：
 *
 *   职责 1 ── 已登录用户：
 *   显示用户头像、昵称、邮箱、投稿数、收藏数，
 *   提供"我的投稿"和"设置"两个子页面入口，
 *   提供"退出登录"按钮。
 *
 *   职责 2 ── 未登录用户：
 *   显示"未登录"提示，点击用户信息区域弹出登录对话框，
 *   点击"我的投稿"也弹出登录对话框（引导用户先登录）。
 *   "设置"页面无需登录即可进入。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_profile.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────────────────┐
 *   │  ┌──────┐  昵称：召唤师小明                      │  ← layoutUserInfo
 *   │  │ 头像 │  邮箱：user@example.com                │     (imageAvatar + textNickname + textEmail)
 *   │  └──────┘  投稿：12  收藏：8                     │     (textStrategyCount + textFavoriteCount)
 *   ├──────────────────────────────────────────────────┤
 *   │  📝 我的投稿                            >       │  ← menuMyStrategies
 *   ├──────────────────────────────────────────────────┤
 *   │  ⚙️ 设置                                >       │  ← menuSettings
 *   ├──────────────────────────────────────────────────┤
 *   │                                                  │
 *   │            [ 退出登录 ]                          │  ← btnLogout
 *   │                                                  │
 *   └──────────────────────────────────────────────────┘
 *   ┌──────────────────────────────────────────────────┐
 *   │  加载中...                                       │  ← progressLoading（默认隐藏）
 *   └──────────────────────────────────────────────────┘
 *
 *   未登录状态：
 *   - 昵称显示"未登录"，邮箱显示"点击登录"
 *   - 投稿数和收藏数归零
 *   - 退出登录按钮隐藏
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌─────────────┐     ┌───────────────┐     ┌──────────────┐
 *   │ ProfileFragment│ ←── │ ProfileViewModel│ ←── │ ProfileRepository│
 *   │   (View层)    │     │  (ViewModel层) │     │  (数据层)     │
 *   └─────────────┘     └───────────────┘     └──────────────┘
 *         │                     │                     │
 *         │  观察 LiveData      │  调用仓库方法       │  发起 HTTP 请求
 *         │  更新 UI            │  处理业务逻辑       │  返回响应数据
 *         │                     │                     │
 *         ▼                     ▼                     ▼
 *   ┌──────────┐        ┌──────────────┐     ┌──────────────┐
 *   │ 布局控件  │        │ LiveData:    │     │ AuthApi      │
 *   │ 文本/图片 │        │ userProfile  │     │ POST /login  │
 *   │ 进度条    │        │ loading      │     │ POST /register│
 *   └──────────┘        │ error        │     │ GET /profile │
 *                       │ loginSuccess │     └──────────────┘
 *                       │ logoutEvent  │
 *                       └──────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、认证流程（登录/注册/退出）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   登录流程：
 *   点击用户信息区域 → showLoginDialog() → 输入邮箱+密码
 *       → viewModel.login() → AuthApi.POST /login
 *       → 成功：TokenStore 保存 Token → loginSuccess = true
 *       → Fragment 观察到 loginSuccess → viewModel.loadUserProfile()
 *       → 用户资料显示在页面上
 *
 *   注册流程：
 *   登录对话框中点击"去注册" → showRegisterDialog()
 *       → 输入昵称+邮箱+密码+确认密码
 *       → viewModel.register() → AuthApi.POST /register
 *       → 成功：registerSuccess = true → Toast 提示
 *       → 用户需要手动登录（注册不会自动登录）
 *
 *   退出流程：
 *   点击"退出登录" → showLogoutConfirmDialog() → 确认
 *       → viewModel.logout() → TokenStore.clear()
 *       → logoutEvent = true → navigateToLogin()
 *       → 重启应用（清除 Activity 栈，附带 EXTRA_LOGGED_OUT 标志）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、离线检测机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   本 Fragment 注册了 ConnectivityManager.NetworkCallback 来监听网络变化：
 *
 *   网络断开（onLost）：
 *   → wasOffline = true
 *   → 显示 Snackbar "网络已断开，部分功能不可用"
 *
 *   网络恢复（onAvailable）：
 *   → 如果 wasOffline = true（之前断开过）
 *   → 显示 Snackbar "网络已恢复"
 *   → 自动重新加载用户资料（viewModel.loadUserProfile()）
 *   → wasOffline = false
 *
 *   为什么需要 wasOffline 标志？
 *   ── onAvailable 在首次注册回调时也会触发（即使网络一直正常），
 *      如果没有 wasOffline 标志，每次进入页面都会弹出"网络已恢复"
 *      并重新加载数据，这是不必要且扰民的。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、导航关系
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ProfileFragment（本页）
 *       ├── menuMyStrategies → MyStrategiesFragment（我的攻略列表）
 *       ├── menuSettings     → SettingsFragment（设置页）
 *       └── navigateToLogin  → 重启应用（退出登录后）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、为什么登录/注册用对话框而不是独立页面？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. 用户体验：登录是个人中心的附属功能，不是主流程，
 *    用对话框可以保持页面上下文，用户登录后直接看到资料。
 * 2. 代码简洁：不需要额外的 Activity/Fragment 和导航路由。
 * 3. 快速切换：登录和注册对话框之间可以一键切换，
 *    比页面跳转更流畅。
 *
 * @see ProfileViewModel      个人中心 ViewModel，管理用户数据和认证状态
 * @see MyStrategiesFragment  我的攻略列表页
 * @see SettingsFragment      设置页
 * @see UserProfileResponse   用户资料数据模型
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    /**
     * 退出登录标志的 Intent Extra Key
     *
     * 退出登录后重启应用时，通过此 Key 向 MainActivity 传递标志，
     * 让 MainActivity 知道是因为退出登录而重启的，
     * 从而自动切换到个人中心 Tab（而不是默认的英雄列表 Tab）。
     *
     * 使用方式：
     *   intent.putExtra(EXTRA_LOGGED_OUT, true);
     *   startActivity(intent);
     *
     * 在 MainActivity 中读取：
     *   if (getIntent().getBooleanExtra(ProfileFragment.EXTRA_LOGGED_OUT, false)) {
     *       // 切换到个人中心 Tab
     *   }
     */
    public static final String EXTRA_LOGGED_OUT = "extra_logged_out";

    /**
     * 视图绑定对象
     *
     * 通过 FragmentProfileBinding 可以直接访问布局中的所有控件，
     * 无需 findViewById()。绑定对象在 onCreateView 中初始化，
     * 在 onDestroyView 中置空以避免内存泄漏。
     *
     * 为什么 binding 可能为 null？
     * ── Fragment 的视图在 onDestroyView 后被销毁，
     *    此时如果异步回调（如 LiveData 观察）尝试访问 binding，
     *    会抛出 NullPointerException。所以所有访问 binding 的代码
     *    都应在 onViewCreated 和 observeViewModel 之间执行，
     *    或者在访问前检查 getView() != null。
     */
    private FragmentProfileBinding binding;

    /**
     * 个人中心 ViewModel
     *
     * 通过 ViewModelProvider 获取，作用域为当前 Fragment。
     * ViewModel 的生命周期比 Fragment 的视图更长，
     * 在屏幕旋转等配置变更时不会重新创建。
     *
     * 为什么用 ViewModelProvider(this) 而不是 ViewModelProvider(requireActivity())？
     * ── ProfileFragment 是个人中心的主页面，拥有自己的 ViewModel 实例。
     *    SettingsFragment 使用 requireActivity() 共享同一个 ViewModel，
     *    因为设置页需要读取和更新用户资料，与主页共享数据。
     */
    private ProfileViewModel viewModel;

    /**
     * 网络状态回调
     *
     * 用于监听网络连接的断开和恢复事件。
     * 在 onViewCreated 中注册，在 onDestroyView 中注销，
     * 避免回调在视图销毁后仍然触发导致崩溃。
     *
     * 为什么不直接在 AndroidManifest 中注册 NetworkReceiver？
     * ── 静态广播接收器在应用未运行时也会被唤醒，消耗电量。
     *    NetworkCallback 只在注册期间生效，更节能。
     *    且本 Fragment 只需要在自己可见时检测网络状态。
     */
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * 是否曾经离线标志
     *
     * 用于区分"首次注册回调时的 onAvailable"和"从离线恢复的 onAvailable"：
     * - false：网络一直正常，onAvailable 是初始化触发，不需要提示
     * - true：之前断网了，onAvailable 是恢复触发，需要提示并重新加载
     *
     * 只有当 onLost 触发后才会设为 true，
     * onAvailable 触发后重置为 false。
     */
    private boolean wasOffline = false;

    /**
     * 创建 Fragment 的视图
     *
     * 这是 Fragment 生命周期的第一步：创建视图层次结构。
     * 此方法只负责 inflate 布局，不做任何业务逻辑初始化。
     *
     * 为什么业务逻辑放在 onViewCreated 而不是 onCreateView？
     * ── onCreateView 只负责创建视图，此时视图还未附加到 Window。
     *    onViewCreated 在视图创建完成后调用，此时可以安全地：
     *    - 设置点击监听器
     *    - 观察 LiveData
     *    - 初始化 RecyclerView 等
     *
     * @param inflater  布局填充器，用于将 XML 布局转换为 View 对象
     * @param container 父容器，Fragment 的视图将附加到此容器
     * @param savedInstanceState 保存的实例状态（屏幕旋转等场景恢复数据用）
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化
     *
     * ═══════════════════════════════════════════════════════════
     * 初始化顺序（顺序很重要！）
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 获取 ViewModel 实例
     *    → 必须最先执行，后续所有操作都依赖 ViewModel
     *
     * 2. setupClickListeners()
     *    → 设置 UI 控件的点击事件
     *
     * 3. setupOfflineDetection()
     *    → 注册网络状态监听
     *
     * 4. observeViewModel()
     *    → 开始观察 LiveData，必须在 ViewModel 获取之后
     *
     * 5. 根据登录状态决定初始操作
     *    → 已登录：加载用户资料
     *    → 未登录：显示未登录状态
     *
     * @param view               Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupClickListeners();
        setupOfflineDetection();
        observeViewModel();

        if (viewModel.isLoggedIn()) {
            viewModel.loadUserProfile();
        } else {
            showNotLoggedInState();
        }
    }

    /**
     * 设置各 UI 元素的点击监听器
     *
     * ═══════════════════════════════════════════════════════════
     * 点击行为一览
     * ═══════════════════════════════════════════════════════════
     *
     *   控件              已登录行为              未登录行为
     *   ────────────────────────────────────────────────────────
     *   layoutUserInfo    无响应                  弹出登录对话框
     *   menuMyStrategies  跳转我的投稿页          弹出登录对话框
     *   menuSettings      跳转设置页              跳转设置页（无需登录）
     *   btnLogout         弹出退出确认对话框       隐藏（不可见）
     *
     * 设计考量：
     * - "设置"页面无需登录即可进入，因为深色模式等偏好设置
     *   与登录状态无关，用户可能想先调整设置再登录。
     * - "我的投稿"需要登录，因为数据依赖用户身份。
     * - 点击用户信息区域弹出登录对话框，是一种常见的引导登录方式，
     *   比单独放一个"登录"按钮更自然。
     */
    private void setupClickListeners() {
        binding.layoutUserInfo.setOnClickListener(v -> {
            if (!viewModel.isLoggedIn()) {
                showLoginDialog();
            }
        });

        binding.menuMyStrategies.setOnClickListener(v -> {
            if (viewModel.isLoggedIn()) {
                Navigation.findNavController(v).navigate(R.id.navigation_my_strategies);
            } else {
                showLoginDialog();
            }
        });

        binding.menuSettings.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_settings);
        });

        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }

    /**
     * 注册网络状态监听回调
     *
     * ═══════════════════════════════════════════════════════════
     * 实现细节
     * ═══════════════════════════════════════════════════════════
     *
     * 使用 ConnectivityManager.registerNetworkCallback() 注册回调，
     * 而不是 registerDefaultNetworkCallback()，因为前者可以指定
     * NetworkRequest 过滤条件（只监听有互联网能力的网络）。
     *
     * 回调方法在哪个线程执行？
     * ── ConnectivityManager 的回调默认在主线程执行，
     *    但官方文档不保证这一点，所以使用 requireActivity().runOnUiThread()
     *    确保 UI 操作在主线程执行，避免 "CalledFromWrongThreadException"。
     *
     * 为什么检查 getView() != null？
     * ── 网络回调可能在 Fragment 视图销毁后触发（onDestroyView 已执行），
     *    此时 binding 已经是 null，访问会崩溃。
     *    getView() != null 确保视图还存在时才操作 UI。
     *
     * NetworkRequest 过滤条件：
     * ── NET_CAPABILITY_INTERNET 表示网络具有互联网访问能力，
     *    过滤掉没有互联网的局域网（如蓝牙共享网络）。
     */
    private void setupOfflineDetection() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            /**
             * 网络断开回调
             *
             * 触发场景：WiFi 断开、移动数据关闭、飞行模式开启等。
             * 注意：onLost 只在当前活跃网络断开时触发，
             *       如果 WiFi 断开但移动数据仍可用，不会触发。
             */
            @Override
            public void onLost(@NonNull Network network) {
                wasOffline = true;
                if (getView() != null) {
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(getView(), "网络已断开，部分功能不可用", Snackbar.LENGTH_LONG).show()
                    );
                }
            }

            /**
             * 网络可用回调
             *
             * 触发场景：WiFi 连接、移动数据开启等。
             *
             * 只有 wasOffline = true 时才执行恢复逻辑：
             * 1. 显示"网络已恢复"提示
             * 2. 重新加载用户资料
             * 3. 重置 wasOffline 标志
             *
             * 为什么不无条件重新加载？
             * ── 首次注册回调时 onAvailable 也会触发（即使网络一直正常），
             *    无条件加载会导致进入页面时重复请求（onViewCreated 已加载过一次）。
             */
            @Override
            public void onAvailable(@NonNull Network network) {
                if (wasOffline && getView() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Snackbar.make(getView(), "网络已恢复", Snackbar.LENGTH_SHORT).show();
                        if (viewModel.isLoggedIn()) {
                            viewModel.loadUserProfile();
                        }
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
     * 观察 ViewModel 的 LiveData 数据变化并更新 UI
     *
     * ═══════════════════════════════════════════════════════════
     * LiveData 观察一览
     * ═══════════════════════════════════════════════════════════
     *
     *   LiveData          观察行为                    触发时机
     *   ─────────────────────────────────────────────────────────
     *   userProfile       bindUserProfile()           加载/更新资料成功
     *   loading           控制进度条可见性            请求开始/结束
     *   error             显示错误信息                请求失败
     *   logoutEvent       navigateToLogin()           退出登录
     *   loginSuccess      loadUserProfile()           登录成功
     *   registerSuccess   显示注册成功 Toast          注册成功
     *
     * 为什么用 getViewLifecycleOwner() 而不是 this？
     * ── Fragment 的生命周期比视图的生命周期长。
     *    使用 getViewLifecycleOwner() 确保观察者在视图销毁时自动移除，
     *    避免在视图重建后收到旧观察者的回调。
     *    如果用 this（Fragment 自身），在屏幕旋转时视图重建，
     *    旧观察者不会被移除，可能导致重复回调或崩溃。
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

        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                viewModel.loadUserProfile();
            }
        });

        viewModel.getRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                android.widget.Toast.makeText(requireContext(), R.string.register_success, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 将用户资料数据绑定到 UI 控件
     *
     * ═══════════════════════════════════════════════════════════
     * 绑定映射
     * ═══════════════════════════════════════════════════════════
     *
     *   UserProfileResponse 字段    →  UI 控件
     *   ────────────────────────────────────────────────
     *   nickname                    →  textNickname
     *   email                       →  textEmail
     *   strategyCount               →  textStrategyCount
     *   favoriteCount               →  textFavoriteCount
     *   avatarUrl                   →  imageAvatar（Glide 圆形裁剪）
     *   (已登录状态)                →  btnLogout 显示
     *
     * 头像加载使用 Glide 的 circleCrop() 变换：
     * ── 将方形头像图片裁剪为圆形，符合社交应用的设计惯例。
     *    Glide 会自动处理图片缓存、占位图、错误图等。
     *
     * 为什么检查 avatarUrl 是否为空？
     * ── 用户可能没有设置头像，后端返回的 avatarUrl 可能为 null 或空字符串。
     *    此时跳过 Glide 加载，使用布局中设置的默认头像（占位图）。
     *    如果传空字符串给 Glide，会导致不必要的错误请求。
     *
     * @param profile 用户资料响应对象，null 时跳过绑定
     */
    private void bindUserProfile(UserProfileResponse profile) {
        if (profile == null) return;

        binding.textNickname.setText(profile.nickname);
        binding.textEmail.setText(profile.email);
        binding.textStrategyCount.setText(String.valueOf(profile.strategyCount));
        binding.textFavoriteCount.setText(String.valueOf(profile.favoriteCount));
        binding.btnLogout.setVisibility(View.VISIBLE);

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
     * ═══════════════════════════════════════════════════════════
     * UI 变化
     * ═══════════════════════════════════════════════════════════
     *
     *   控件              显示内容          说明
     *   ────────────────────────────────────────────────
     *   textNickname      "未登录"          引导用户点击登录
     *   textEmail         "点击登录"        提示可点击区域
     *   textStrategyCount "0"              无数据
     *   textFavoriteCount "0"              无数据
     *   btnLogout         GONE             未登录不需要退出按钮
     *   imageAvatar       默认占位图       布局 XML 中已设置
     *
     * 为什么用 GONE 而不是 INVISIBLE？
     * ── GONE 不占布局空间，INVISIBLE 保留空间但不可见。
     *    退出按钮消失后，下方内容应该上移填补空间，
     *    所以用 GONE 更合理。
     */
    private void showNotLoggedInState() {
        binding.textNickname.setText(R.string.profile_nickname_default);
        binding.textEmail.setText(R.string.profile_email_default);
        binding.textStrategyCount.setText("0");
        binding.textFavoriteCount.setText("0");
        binding.btnLogout.setVisibility(View.GONE);
    }

    /**
     * 显示登录对话框
     *
     * ═══════════════════════════════════════════════════════════
     * 对话框布局结构（dialog_login.xml）
     * ═══════════════════════════════════════════════════════════
     *
     *   ┌──────────────────────────────────────────┐
     *   │  登录                                    │  ← 标题
     *   ├──────────────────────────────────────────┤
     *   │  📧 邮箱                                 │  ← editEmail (TextInputEditText)
     *   │  🔒 密码                                 │  ← editPassword (TextInputEditText)
     *   │  ⚠️ 错误提示                             │  ← textLoginError（默认隐藏）
     *   │  还没有账号？去注册                       │  ← textGotoRegister
     *   ├──────────────────────────────────────────┤
     *   │         [取消]    [登录]                  │  ← 按钮
     *   └──────────────────────────────────────────┘
     *
     * ═══════════════════════════════════════════════════════════
     * 交互流程
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 用户输入邮箱和密码
     * 2. 点击"登录"按钮
     * 3. 校验：邮箱或密码为空 → 显示错误提示，不关闭对话框
     * 4. 校验通过 → viewModel.login(email, password)
     * 5. ViewModel 处理登录结果：
     *    - 成功 → loginSuccess = true → Fragment 刷新资料 → 对话框自动消失
     *    - 失败 → error = "邮箱或密码错误" → 显示在主页面上
     *
     * 点击"去注册"：
     * → 关闭登录对话框 → 打开注册对话框
     *
     * 为什么错误提示在对话框内而不是 Toast？
     * ── 对话框内的错误（如空字段）是输入校验错误，
     *    应该在对话框内就近提示，用户可以直接修改。
     *    而 ViewModel 返回的错误（如密码错误）是服务器端错误，
     *    此时对话框已关闭，错误显示在主页面上。
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

        /**
         * "去注册"链接点击事件
         *
         * 关闭登录对话框，然后打开注册对话框。
         * 使用 Timber 记录导航行为，方便追踪用户在认证流程中的路径。
         */
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
     * ═══════════════════════════════════════════════════════════
     * 对话框布局结构（dialog_register.xml）
     * ═══════════════════════════════════════════════════════════
     *
     *   ┌──────────────────────────────────────────┐
     *   │  注册                                    │  ← 标题
     *   ├──────────────────────────────────────────┤
     *   │  👤 昵称                                 │  ← editRegisterNickname
     *   │  📧 邮箱                                 │  ← editRegisterEmail
     *   │  🔒 密码                                 │  ← editRegisterPassword
     *   │  🔒 确认密码                             │  ← editRegisterConfirmPassword
     *   │  ⚠️ 错误提示                             │  ← textRegisterError（默认隐藏）
     *   │  已有账号？去登录                         │  ← textGotoLogin
     *   ├──────────────────────────────────────────┤
     *   │         [取消]    [注册]                  │  ← 按钮
     *   └──────────────────────────────────────────┘
     *
     * ═══════════════════════════════════════════════════════════
     * 表单校验规则
     * ═══════════════════════════════════════════════════════════
     *
     *   校验项              条件                    错误提示
     *   ────────────────────────────────────────────────────────
     *   空字段检查          任一字段为空            "请填写所有字段"
     *   密码一致性检查      password ≠ confirmPwd   "两次密码输入不一致"
     *   邮箱格式           由后端校验              "注册失败：邮箱可能已被注册"
     *   邮箱唯一性         由后端校验              同上
     *
     * 为什么邮箱格式不在前端校验？
     * ── 前端校验越复杂，维护成本越高。
     *    邮箱格式的正则表达式有很多种实现，且可能遗漏边界情况。
     *    后端已经有完整的校验逻辑，前端只需确保字段非空即可。
     *
     * 注册成功后不会自动登录：
     * ── 用户需要手动回到登录对话框输入凭证。
     *    这是一种安全实践：确保用户确实知道自己注册的密码。
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
     * 防止用户误触退出按钮。确认后调用 viewModel.logout()，
     * ViewModel 会清除 Token 并设置 logoutEvent = true，
     * Fragment 观察到 logoutEvent 后执行 navigateToLogin() 重启应用。
     *
     * 对话框按钮：
     * - "取消"：关闭对话框，不做任何操作
     * - "确认退出"：调用 viewModel.logout()
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
     * ═══════════════════════════════════════════════════════════
     * 重启流程详解
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 获取应用的启动 Intent（相当于点击桌面图标的效果）
     * 2. 添加 FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK
     *    → 清除所有 Activity 栈，从零开始
     * 3. 附带 EXTRA_LOGGED_OUT = true
     *    → 让 MainActivity 知道是退出登录后重启的
     *    → 自动切换到个人中心 Tab
     * 4. 启动新 Activity + 结束当前 Activity
     *
     * 为什么退出登录要重启应用而不是只刷新 UI？
     * ── 退出登录后，应用中多个页面可能缓存了用户数据：
     *    - 社区页面的投票状态
     *    - 个人中心的用户资料
     *    - 网络请求的 Authorization Header
     *    重启应用可以确保所有状态被完全清除，
     *    比逐个页面清理更可靠。
     *
     * FLAG_ACTIVITY_CLEAR_TASK 的作用：
     * ── 清除 Task 中所有已存在的 Activity。
     *    如果不加此标志，新的 MainActivity 会被压入
     *    现有的 Activity 栈中，用户按返回键还能回到
     *    退出前的页面（此时已无 Token，会报错）。
     *
     * FLAG_ACTIVITY_NEW_TASK 的作用：
     * ── 在新的 Task 中启动 Activity。
     *    与 CLEAR_TASK 配合使用时，效果是：
     *    先清空旧 Task，再在新 Task 中启动 Activity。
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

    /**
     * Fragment 视图销毁时的清理
     *
     * ═══════════════════════════════════════════════════════════
     * 清理内容
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 注销网络状态回调
     *    → 避免视图销毁后回调仍然触发导致崩溃
     *    → 如果不注销，ConnectivityManager 会持有回调的引用，
     *      导致 Fragment 无法被垃圾回收（内存泄漏）
     *
     * 2. 置空 binding 引用
     *    → 避免异步回调（如 LiveData 观察）在视图销毁后访问 binding
     *    → Fragment 的视图生命周期和 Fragment 生命周期不同：
     *      Fragment 可能存活但视图已被销毁（如返回栈中的 Fragment）
     *
     * 为什么先检查 cm != null？
     * ── 极端情况下（如 Fragment 未附加到 Activity），
     *    requireContext() 可能抛出异常。
     *    但此处使用 requireContext() 是安全的，因为
     *    onDestroyView 时 Fragment 仍附加在 Activity 上。
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
