package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.UpdateProfileRequest;
import com.aram.mayhem.network.dto.UserProfileResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;

/**
 * 用户资料 API 接口 ── 提供用户资料的获取和更新功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义了与用户资料相关的两个 HTTP 端点：
 * 1. GET   /api/users/me/profile ── 获取当前用户资料
 * 2. PATCH /api/users/me         ── 更新当前用户资料
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么用 /me 而不是 /{userId}？
 * ═══════════════════════════════════════════════════════════════════
 *
 * /me 是 REST API 中表示"当前用户"的惯例：
 * - 客户端不需要知道自己的 userId
 * - 后端从 JWT Token 中提取 userId
 * - 避免用户通过修改 URL 访问他人资料
 * - 更安全、更简洁
 *
 * 如果需要查看他人资料，应该使用 /api/users/{userId}/profile，
 * 但当前版本只支持查看和修改自己的资料。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、PATCH vs PUT 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * 更新资料使用 PATCH 而非 PUT：
 * - PUT：全量更新，必须提供所有字段（未提供的字段会被设为 null）
 * - PATCH：部分更新，只更新请求中非 null 的字段
 *
 * 例如，用户只想修改昵称：
 * - PUT：必须传 nickname + avatarUrl + displayMode + notificationEnabled
 * - PATCH：只需传 nickname，其他字段保持不变
 *
 * PATCH 更适合资料更新场景，因为用户通常只修改个别字段。
 *
 * 关联类：
 * - UserProfileResponse：用户资料响应 DTO
 * - UpdateProfileRequest：更新资料请求 DTO
 * - ProfileRepository：调用本 API 的 Repository
 */
public interface ProfileApi {

    /**
     * 获取当前用户资料 ── 个人中心页使用
     *
     * 请求：GET /api/users/me/profile
     * 认证：需要 Bearer Token
     *
     * 后端从 JWT Token 中提取 userId，查询该用户的完整资料：
     * - 基本信息：昵称、头像、邮箱
     * - 偏好设置：显示模式、通知开关
     * - 统计数据：发布攻略数、获赞数
     *
     * @return Call<Result<UserProfileResponse>> 用户资料响应
     */
    @GET("api/users/me/profile")
    Call<Result<UserProfileResponse>> getUserProfile();

    /**
     * 更新当前用户资料 ── 个人中心编辑页使用
     *
     * 请求：PATCH /api/users/me
     * 请求体：{"nickname": "新昵称", "notificationEnabled": false}
     * 认证：需要 Bearer Token
     *
     * 部分更新：只更新请求体中非 null 的字段。
     * 例如只传 nickname，则只修改昵称，其他字段保持不变。
     *
     * 可更新字段：
     * - nickname：用户昵称
     * - avatarUrl：头像 URL
     * - displayMode：显示模式（light/dark/auto）
     * - notificationEnabled：是否启用通知
     *
     * @param request 更新请求体，只需包含要修改的字段
     * @return Call<Result<UserProfileResponse>> 更新后的用户资料
     */
    @PATCH("api/users/me")
    Call<Result<UserProfileResponse>> updateProfile(@Body UpdateProfileRequest request);
}
