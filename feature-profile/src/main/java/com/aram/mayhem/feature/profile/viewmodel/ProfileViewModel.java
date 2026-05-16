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
 * 个人中心 ViewModel（个人中心模块）
 *
 * 功能：管理用户资料数据（加载、更新、登录、退出登录）
 * 数据流：ProfileRepository/AuthApi → LiveData → ProfileFragment
 *
 * @see ProfileRepository
 * @see AuthApi
 * @see com.aram.mayhem.feature.profile.ProfileFragment
 */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    /** 用户资料仓库，提供资料加载和更新接口 */
    private final ProfileRepository profileRepository;
    /** 令牌存储，管理 JWT Token 的读写和清除 */
    private final TokenStore tokenStore;
    /** 认证 API，提供登录接口 */
    private final AuthApi authApi;

    /** 用户资料数据 */
    private final MutableLiveData<UserProfileResponse> userProfile = new MutableLiveData<>();
    /** 加载状态（true=加载中） */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 错误信息 */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    /** 资料更新成功标志 */
    private final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();
    /** 退出登录事件（true=已退出） */
    private final MutableLiveData<Boolean> logoutEvent = new MutableLiveData<>();
    /** 登录成功事件（true=登录成功） */
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();

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

    /** @return 用户资料 LiveData */
    public LiveData<UserProfileResponse> getUserProfile() { return userProfile; }
    /** @return 加载状态 LiveData */
    public LiveData<Boolean> getLoading() { return loading; }
    /** @return 错误信息 LiveData */
    public LiveData<String> getError() { return error; }
    /** @return 资料更新成功标志 LiveData */
    public LiveData<Boolean> getUpdateSuccess() { return updateSuccess; }
    /** @return 退出登录事件 LiveData */
    public LiveData<Boolean> getLogoutEvent() { return logoutEvent; }
    /** @return 登录成功事件 LiveData */
    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }

    /**
     * 判断用户是否已登录
     *
     * @return true=已登录（TokenStore 中存在 accessToken），false=未登录
     */
    public boolean isLoggedIn() {
        return tokenStore.hasToken();
    }

    /**
     * 加载用户资料
     *
     * 作用：从服务器获取当前登录用户的完整资料信息
     * 实现：调用 ProfileRepository.getUserProfile()，成功后更新 LiveData
     */
    public void loadUserProfile() {
        // 防止重复加载
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
     * 作用：部分更新当前用户信息（昵称、头像、显示模式、通知开关）
     * 实现：调用 ProfileRepository.updateProfile()，成功后刷新本地缓存
     *
     * @param nickname             用户昵称
     * @param avatarUrl            头像 URL
     * @param displayMode          显示模式（0=浅色, 1=深色）
     * @param notificationEnabled  通知开关（1=启用, 0=关闭）
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
     * 作用：清除本地存储的 JWT Token，触发退出登录事件
     * 实现：调用 TokenStore.clear() 清除令牌，发送 logoutEvent 通知 UI 跳转登录页
     */
    public void logout() {
        tokenStore.clear();
        logoutEvent.setValue(true);
        Timber.i("User logged out");
    }

    /**
     * 用户登录
     *
     * 作用：使用邮箱和密码进行登录，成功后保存 Token 并触发 loginSuccess 事件
     * 实现：调用 AuthApi.login() 发起异步网络请求，成功后通过 TokenStore 保存令牌
     *
     * @param email    用户邮箱
     * @param password 用户密码
     */
    public void login(String email, String password) {
        loading.setValue(true);
        authApi.login(new AuthApi.LoginRequest(email, password)).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<com.aram.mayhem.common.Result<AuthApi.AuthResponse>> call,
                                   retrofit2.Response<com.aram.mayhem.common.Result<AuthApi.AuthResponse>> response) {
                loading.setValue(false);
                // 登录成功：保存令牌（expiresIn 单位为秒，需转为毫秒）
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthApi.AuthResponse authResponse = response.body().getData();
                    if (authResponse != null) {
                        tokenStore.saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn * 1000);
                        loginSuccess.setValue(true);
                        Timber.i("User logged in: email=%s", email);
                    } else {
                        error.setValue("登录失败：服务器返回为空");
                    }
                } else {
                    // 登录失败：邮箱或密码错误
                    error.setValue("邮箱或密码错误");
                    Timber.w("Login failed: email=%s", email);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.aram.mayhem.common.Result<AuthApi.AuthResponse>> call, Throwable t) {
                loading.setValue(false);
                // 网络错误
                error.setValue("网络错误：" + t.getMessage());
                Timber.e(t, "Login network error: email=%s", email);
            }
        });
    }
}
