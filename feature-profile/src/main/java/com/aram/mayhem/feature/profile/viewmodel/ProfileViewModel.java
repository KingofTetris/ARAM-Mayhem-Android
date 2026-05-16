package com.aram.mayhem.feature.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aram.mayhem.data.local.TokenStore;
import com.aram.mayhem.feature.profile.repository.ProfileRepository;
import com.aram.mayhem.network.dto.UpdateProfileRequest;
import com.aram.mayhem.network.dto.UserProfileResponse;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

/**
 * 个人中心 ViewModel（个人中心模块）
 *
 * 功能：管理用户资料数据（加载、更新、退出登录）
 * 数据流：ProfileRepository → LiveData<UserProfileResponse> → ProfileFragment
 *
 * @see ProfileRepository
 * @see com.aram.mayhem.feature.profile.ProfileFragment
 */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ProfileRepository profileRepository;
    private final TokenStore tokenStore;

    private final MutableLiveData<UserProfileResponse> userProfile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutEvent = new MutableLiveData<>();

    /**
     * 构造函数
     *
     * @param profileRepository 用户资料仓库（通过 Hilt 依赖注入）
     * @param tokenStore        令牌存储（通过 Hilt 依赖注入）
     */
    @Inject
    public ProfileViewModel(ProfileRepository profileRepository, TokenStore tokenStore) {
        this.profileRepository = profileRepository;
        this.tokenStore = tokenStore;
    }

    public LiveData<UserProfileResponse> getUserProfile() { return userProfile; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getUpdateSuccess() { return updateSuccess; }
    public LiveData<Boolean> getLogoutEvent() { return logoutEvent; }

    /**
     * 加载用户资料
     *
     * 作用：从服务器获取当前登录用户的完整资料信息
     * 实现：调用 ProfileRepository.getUserProfile()，成功后更新 LiveData
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
}
