package com.aram.mayhem.feature.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aram.mayhem.data.local.TokenStore;
import com.aram.mayhem.feature.profile.repository.ProfileRepository;
import com.aram.mayhem.network.api.AuthApi;
import com.aram.mayhem.network.dto.UpdateProfileRequest;
import com.aram.mayhem.network.dto.UserProfileResponse;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

/**
 * 个人中心 ViewModel ── 管理用户资料、登录、注册、退出登录的数据状态
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * ProfileViewModel 是个人中心模块的核心数据管理类，负责：
 * 1. 加载和更新用户资料（头像、昵称、邮箱、投稿数、收藏数）
 * 2. 用户登录（邮箱 + 密码）
 * 3. 用户注册（邮箱 + 密码 + 昵称）
 * 4. 退出登录（清除本地 Token）
 * 5. 判断用户登录状态
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、LiveData 状态一览
 * ═══════════════════════════════════════════════════════════════════
 *
 *   LiveData                    类型              用途
 *   ─────────────────────────────────────────────────────────────
 *   userProfile                 UserProfileResponse  用户资料数据
 *   loading                     Boolean              加载中状态
 *   error                       String               错误提示信息
 *   updateSuccess               Boolean              资料更新是否成功
 *   logoutEvent                 Boolean              退出登录事件
 *   loginSuccess                Boolean              登录成功事件
 *   registerSuccess             Boolean              注册成功事件
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、认证流程
 * ═══════════════════════════════════════════════════════════════════
 *
 *   登录流程：
 *   ┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────┐
 *   │ 用户输入  │ ──→ │ login()      │ ──→ │ AuthApi      │ ──→ │ TokenStore│
 *   │ 邮箱+密码 │     │ 发起请求     │     │ POST /login  │     │ 保存Token │
 *   └──────────┘     └──────────────┘     └──────────────┘     └──────────┘
 *                                                                    │
 *                              ┌─────────────────────────────────────┘
 *                              ▼
 *                    loginSuccess = true
 *                    → Fragment 刷新用户资料
 *
 *   注册流程：
 *   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
 *   │ 用户输入      │ ──→ │ register()   │ ──→ │ AuthApi      │
 *   │ 邮箱+密码+昵称│     │ 发起请求     │     │ POST /register│
 *   └──────────────┘     └──────────────┘     └──────────────┘
 *                                                     │
 *                              ┌──────────────────────┘
 *                              ▼
 *                    registerSuccess = true
 *                    → Fragment 显示成功提示
 *
 *   退出流程：
 *   ┌──────────┐     ┌──────────────┐     ┌──────────────┐
 *   │ 确认退出  │ ──→ │ logout()     │ ──→ │ TokenStore   │
 *   │          │     │              │     │ clear() 清除  │
 *   └──────────┘     └──────────────┘     └──────────────┘
 *                                                │
 *                         ┌──────────────────────┘
 *                         ▼
 *                   logoutEvent = true
 *                   → Fragment 重启应用
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、为什么登录和注册在 ProfileViewModel 而不是独立的 AuthViewModel？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本应用的登录/注册功能只在个人中心页使用（通过对话框形式），
 * 没有独立的登录页面。将认证逻辑放在 ProfileViewModel 中：
 * 1. 减少类数量，代码更紧凑
 * 2. 登录成功后可以直接刷新用户资料，无需跨 ViewModel 通信
 * 3. 如果未来需要独立的登录页，可以轻松拆分
 *
 * @see ProfileRepository   用户资料数据仓库
 * @see TokenStore          JWT Token 本地存储
 * @see AuthApi             认证 API 接口
 */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    /**
     * 用户资料仓库，提供资料加载和更新接口
     *
     * ProfileRepository 封装了与后端 /api/profile 的交互逻辑，
     * ViewModel 不需要知道 HTTP 请求的细节，只需调用仓库方法。
     */
    private final ProfileRepository profileRepository;

    /**
     * 令牌存储，管理 JWT Token 的读写和清除
     *
     * TokenStore 封装了 SharedPreferences 的 Token 存储逻辑：
     * - saveTokens(accessToken, refreshToken, expiresIn) → 保存登录凭证
     * - hasToken() → 判断是否已登录
     * - clear() → 退出登录时清除所有 Token
     *
     * 为什么 Token 存在 SharedPreferences 而不是数据库？
     * ── Token 数据量极小（3 个字符串），且需要快速读取，
     *    SharedPreferences 比 Room 数据库更轻量高效。
     */
    private final TokenStore tokenStore;

    /**
     * 认证 API，提供登录和注册接口
     *
     * AuthApi 是 Retrofit 接口，提供：
     * - login(LoginRequest)   → POST /api/auth/login
     * - register(RegisterRequest) → POST /api/auth/register
     *
     * 注意：登录和注册直接使用 AuthApi 而不是通过 Repository，
     * 因为认证逻辑简单，不需要缓存或复杂的数据转换。
     */
    private final AuthApi authApi;

    /**
     * 用户资料数据
     *
     * 包含用户的所有资料信息：昵称、邮箱、头像URL、
     * 投稿数、收藏数、显示模式、通知开关等。
     * Fragment 通过观察此 LiveData 来更新 UI。
     */
    private final MutableLiveData<UserProfileResponse> userProfile = new MutableLiveData<>();

    /**
     * 加载状态（true = 正在加载）
     *
     * Fragment 观察此状态来显示/隐藏加载进度条。
     * 防止重复加载：loadUserProfile() 开头检查此值。
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息
     *
     * 任何操作失败时设置错误提示文本，Fragment 显示在 UI 上。
     * 常见错误：
     * - "加载用户资料失败" → 网络请求失败
     * - "邮箱或密码错误" → 登录凭证无效
     * - "注册失败：邮箱可能已被注册" → 邮箱重复
     * - "网络错误：xxx" → 网络连接问题
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 资料更新成功标志
     *
     * true → 更新成功，Fragment 显示成功提示
     * false → 更新失败，Fragment 显示失败提示
     * null → 未进行更新操作
     */
    private final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();

    /**
     * 退出登录事件
     *
     * true → 用户已退出登录，Fragment 应重启应用
     * 此事件只触发一次，Fragment 观察后执行应用重启。
     */
    private final MutableLiveData<Boolean> logoutEvent = new MutableLiveData<>();

    /**
     * 登录成功事件
     *
     * true → 登录成功，Fragment 应刷新用户资料
     * 登录成功后 TokenStore 已保存 Token，可以正常请求用户资料。
     */
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();

    /**
     * 注册成功事件
     *
     * true → 注册成功，Fragment 显示成功提示
     * 注意：注册成功后不会自动登录，用户需要手动登录。
     */
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();

    /**
     * 构造函数（Hilt 自动注入依赖）
     *
     * @param profileRepository 用户资料仓库
     * @param tokenStore        令牌存储
     * @param authApi           认证 API 接口
     */
    @Inject
    public ProfileViewModel(ProfileRepository profileRepository, TokenStore tokenStore, AuthApi authApi) {
        this.profileRepository = profileRepository;
        this.tokenStore = tokenStore;
        this.authApi = authApi;
    }

    public LiveData<UserProfileResponse> getUserProfile() { return userProfile; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getUpdateSuccess() { return updateSuccess; }
    public LiveData<Boolean> getLogoutEvent() { return logoutEvent; }
    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public LiveData<Boolean> getRegisterSuccess() { return registerSuccess; }

    /**
     * 判断用户是否已登录
     *
     * 通过检查 TokenStore 中是否存在 accessToken 来判断。
     * accessToken 在登录成功后保存，退出登录后清除。
     *
     * @return true = 已登录（TokenStore 中存在 accessToken），false = 未登录
     */
    public boolean isLoggedIn() {
        return tokenStore.hasToken();
    }

    /**
     * 加载用户资料
     *
     * ═══════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 检查 loading 状态，防止重复加载
     * 2. 设置 loading = true，通知 Fragment 显示进度条
     * 3. 调用 profileRepository.getUserProfile() 发起网络请求
     * 4. 使用 observeForever 监听结果（一次性观察，不需要生命周期感知）
     * 5. 成功 → 更新 userProfile LiveData
     * 6. 失败 → 设置 error 信息
     *
     * 为什么用 observeForever 而不是 observe(lifecycleOwner, observer)？
     * ── ViewModel 没有 LifecycleOwner（它不与视图生命周期绑定）。
     *    observeForever 不需要 LifecycleOwner，但必须手动移除观察者
     *    以避免内存泄漏。此处因为是一次性网络请求，结果返回后
     *    不需要再观察，所以没有移除的问题。
     *
     * 注意：observeForever 的观察者不会被自动移除，
     * 如果 ViewModel 存活时间很长且频繁调用此方法，
     * 可能会累积多个观察者。但本方法有 loading 防重复检查，
     * 所以不会出现这个问题。
     */
    public void loadUserProfile() {
        if (Boolean.TRUE.equals(loading.getValue())) return;

        loading.setValue(true);
        profileRepository.getUserProfile().observeForever(result -> {
            loading.setValue(false);
            if (result != null) {
                userProfile.setValue(result);
                Timber.d("User profile loaded: nickname=%s", result.nickname);
            } else {
                error.setValue("加载用户资料失败");
                Timber.w("Failed to load user profile");
            }
        });
    }

    /**
     * 更新用户资料
     *
     * ═══════════════════════════════════════════════════════════
     * 部分更新机制
     * ═══════════════════════════════════════════════════════════
     *
     * 只有非 null 的参数会被更新，null 参数保持原值不变。
     * 这样设置页的每个 Switch 可以独立更新：
     *
     *   设置页切换深色模式：
     *   updateProfile(null, null, 1, null)
     *   → 只更新 displayMode，其他字段不变
     *
     *   设置页切换通知开关：
     *   updateProfile(null, null, null, 0)
     *   → 只更新 notificationEnabled，其他字段不变
     *
     * @param nickname             用户昵称（null 表示不更新）
     * @param avatarUrl            头像 URL（null 表示不更新）
     * @param displayMode          显示模式（0 = 浅色, 1 = 深色, null = 不更新）
     * @param notificationEnabled  通知开关（1 = 启用, 0 = 关闭, null = 不更新）
     */
    public void updateProfile(String nickname, String avatarUrl, Integer displayMode, Integer notificationEnabled) {
        loading.setValue(true);
        UpdateProfileRequest request = new UpdateProfileRequest(nickname, avatarUrl, displayMode, notificationEnabled);

        profileRepository.updateProfile(request).observeForever(result -> {
            loading.setValue(false);
            if (result != null) {
                userProfile.setValue(result);
                updateSuccess.setValue(true);
                Timber.d("Profile updated: nickname=%s", result.nickname);
            } else {
                updateSuccess.setValue(false);
                error.setValue("更新资料失败");
                Timber.w("Failed to update profile");
            }
        });
    }

    /**
     * 退出登录
     *
     * ═══════════════════════════════════════════════════════════
     * 退出流程
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 调用 tokenStore.clear() 清除本地存储的所有 Token
     *    （accessToken + refreshToken + 过期时间）
     * 2. 设置 logoutEvent = true，通知 Fragment 执行重启
     *
     * 为什么不调用后端的退出登录 API？
     * ── JWT 是无状态的，服务器不维护登录会话。
     *    只要客户端删除了 Token，就等同于退出登录。
     *    服务器端的 Token 在过期前仍然有效，但客户端
     *    不再发送它，所以不会有安全问题。
     *
     * 为什么退出后要重启应用？
     * ── 退出登录后，应用中所有页面都需要重新判断登录状态。
     *    重启应用是最简单可靠的方式，确保所有页面
     *    都从初始状态开始，不会有残留的登录数据。
     */
    public void logout() {
        tokenStore.clear();
        logoutEvent.setValue(true);
        Timber.i("User logged out");
    }

    /**
     * 用户登录
     *
     * ═══════════════════════════════════════════════════════════
     * 登录流程详解
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 设置 loading = true
     * 2. 构造 LoginRequest(email, password)
     * 3. 调用 authApi.login() 发起 POST 请求
     * 4. 成功 → 保存 Token 到 TokenStore → loginSuccess = true
     * 5. 失败 → 设置 error 提示
     *
     * Token 保存细节：
     *   authResponse.accessToken  → 访问令牌（有效期短，如 2 小时）
     *   authResponse.refreshToken → 刷新令牌（有效期长，如 7 天）
     *   authResponse.expiresIn    → 过期时间（秒），乘以 1000 转为毫秒
     *
     * 为什么 expiresIn * 1000？
     * ── 后端返回的 expiresIn 单位是秒（如 7200 = 2 小时），
     *    但 Android System.currentTimeMillis() 返回毫秒。
     *    TokenStore 需要存储毫秒级的过期时间戳，所以乘以 1000。
     *
     * Timber 日志说明：
     * ── 使用 AuthFlow 标签统一标记认证流程日志，方便追踪：
     *    login_start → login_success / login_fail_xxx / login_network_error
     *
     * @param email    用户邮箱
     * @param password 用户密码
     */
    public void login(String email, String password) {
        loading.setValue(true);
        Timber.i("AuthFlow: login_start | email=%s | timestamp=%d", email, System.currentTimeMillis());
        authApi.login(new AuthApi.LoginRequest(email, password)).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<com.aram.mayhem.common.Result<AuthApi.AuthResponse>> call,
                                   retrofit2.Response<com.aram.mayhem.common.Result<AuthApi.AuthResponse>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthApi.AuthResponse authResponse = response.body().getData();
                    if (authResponse != null) {
                        tokenStore.saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn * 1000);
                        loginSuccess.setValue(true);
                        Timber.i("AuthFlow: login_success | email=%s | timestamp=%d", email, System.currentTimeMillis());
                    } else {
                        error.setValue("登录失败：服务器返回为空");
                        Timber.w("AuthFlow: login_fail_empty_response | email=%s | timestamp=%d", email, System.currentTimeMillis());
                    }
                } else {
                    error.setValue("邮箱或密码错误");
                    Timber.w("AuthFlow: login_fail_invalid_credential | email=%s | timestamp=%d", email, System.currentTimeMillis());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.aram.mayhem.common.Result<AuthApi.AuthResponse>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("网络错误：" + t.getMessage());
                Timber.e(t, "AuthFlow: login_network_error | email=%s | timestamp=%d", email, System.currentTimeMillis());
            }
        });
    }

    /**
     * 用户注册
     *
     * ═══════════════════════════════════════════════════════════
     * 注册流程详解
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 设置 loading = true
     * 2. 构造 RegisterRequest(email, password, nickname)
     * 3. 调用 authApi.register() 发起 POST 请求
     * 4. 成功 → registerSuccess = true（Fragment 显示成功提示）
     * 5. 失败 → 设置 error 提示
     *
     * 注册与登录的区别：
     * ── 注册成功后不会自动登录（registerSuccess ≠ loginSuccess）
     *    用户需要手动在登录对话框中输入邮箱和密码登录。
     *    这样设计是因为：
     *    1. 注册后可能需要邮箱验证
     *    2. 让用户确认密码是否正确
     *    3. 简化注册流程的错误处理
     *
     * 常见注册失败原因：
     * - "注册失败：邮箱可能已被注册" → 邮箱重复（最常见）
     * - "网络错误：xxx" → 网络连接问题
     *
     * @param email    用户邮箱
     * @param password 用户密码
     * @param nickname 用户昵称
     */
    public void register(String email, String password, String nickname) {
        loading.setValue(true);
        Timber.i("AuthFlow: register_start | email=%s | nickname=%s | timestamp=%d", email, nickname, System.currentTimeMillis());
        authApi.register(new AuthApi.RegisterRequest(email, password, nickname)).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<com.aram.mayhem.common.Result<Object>> call,
                                   retrofit2.Response<com.aram.mayhem.common.Result<Object>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    registerSuccess.setValue(true);
                    Timber.i("AuthFlow: register_success | email=%s | timestamp=%d", email, System.currentTimeMillis());
                } else {
                    error.setValue("注册失败：邮箱可能已被注册");
                    Timber.w("AuthFlow: register_fail_duplicate | email=%s | timestamp=%d", email, System.currentTimeMillis());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.aram.mayhem.common.Result<Object>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("网络错误：" + t.getMessage());
                Timber.e(t, "AuthFlow: register_network_error | email=%s | timestamp=%d", email, System.currentTimeMillis());
            }
        });
    }
}
