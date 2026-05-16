package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.UpdateProfileRequest;
import com.aram.mayhem.network.dto.UserProfileResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;

/**
 * 用户资料 API 接口（个人中心模块）
 *
 * 端点：GET /api/users/me/profile（获取资料）、PATCH /api/users/me（更新资料）
 * 认证：需 Bearer Token
 *
 * @see UserProfileResponse
 * @see UpdateProfileRequest
 */
public interface ProfileApi {

    /**
     * 获取当前用户资料
     *
     * 作用：从 JWT Token 中提取用户ID，返回该用户的完整资料信息
     * 包含：基本信息、偏好设置、统计数据
     *
     * @return Call<Result<UserProfileResponse>> 用户资料响应
     */
    @GET("api/users/me/profile")
    Call<Result<UserProfileResponse>> getUserProfile();

    /**
     * 更新当前用户资料
     *
     * 作用：部分更新当前用户信息，仅更新请求中非 null 的字段
     * 可更新字段：nickname、avatarUrl、displayMode、notificationEnabled
     *
     * @param request 更新请求体
     * @return Call<Result<UserProfileResponse>> 更新后的用户资料
     */
    @PATCH("api/users/me")
    Call<Result<UserProfileResponse>> updateProfile(@Body UpdateProfileRequest request);
}
