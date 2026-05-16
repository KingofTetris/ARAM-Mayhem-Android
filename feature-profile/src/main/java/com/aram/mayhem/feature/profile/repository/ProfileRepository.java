package com.aram.mayhem.feature.profile.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.api.ProfileApi;
import com.aram.mayhem.network.dto.UpdateProfileRequest;
import com.aram.mayhem.network.dto.UserProfileResponse;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 用户资料数据仓库
 *
 * 数据源策略：网络请求（个人资料无本地缓存需求）
 * 功能：获取用户资料、更新用户资料
 * 关联：ProfileApi, UserProfileResponse, UpdateProfileRequest
 */
@Singleton
public class ProfileRepository {

    private final ProfileApi profileApi;

    @Inject
    public ProfileRepository(ProfileApi profileApi) {
        this.profileApi = profileApi;
    }

    /**
     * 获取当前用户资料（个人中心模块）
     *
     * 作用：从服务器获取当前登录用户的完整资料信息
     *
     * @return LiveData<UserProfileResponse> 可观察的用户资料数据
     */
    public LiveData<UserProfileResponse> getUserProfile() {
        MutableLiveData<UserProfileResponse> result = new MutableLiveData<>();

        profileApi.getUserProfile().enqueue(new Callback<Result<UserProfileResponse>>() {
            @Override
            public void onResponse(Call<Result<UserProfileResponse>> call, Response<Result<UserProfileResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(response.body().getData());
                } else {
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Result<UserProfileResponse>> call, Throwable t) {
                result.setValue(null);
            }
        });

        return result;
    }

    /**
     * 更新当前用户资料（个人中心模块）
     *
     * 作用：部分更新当前用户信息，仅更新请求中非 null 的字段
     *
     * @param request 更新请求体
     * @return LiveData<UserProfileResponse> 可观察的更新后用户资料
     */
    public LiveData<UserProfileResponse> updateProfile(UpdateProfileRequest request) {
        MutableLiveData<UserProfileResponse> result = new MutableLiveData<>();

        profileApi.updateProfile(request).enqueue(new Callback<Result<UserProfileResponse>>() {
            @Override
            public void onResponse(Call<Result<UserProfileResponse>> call, Response<Result<UserProfileResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(response.body().getData());
                } else {
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Result<UserProfileResponse>> call, Throwable t) {
                result.setValue(null);
            }
        });

        return result;
    }
}
