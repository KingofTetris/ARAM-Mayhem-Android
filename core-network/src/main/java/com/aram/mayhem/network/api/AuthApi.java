package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 认证 API 接口 ── 提供用户注册、登录、令牌刷新功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义了与用户认证相关的三个 HTTP 端点：
 * 1. POST /api/auth/register ── 用户注册（创建新账号）
 * 2. POST /api/auth/login    ── 用户登录（获取 JWT Token）
 * 3. POST /api/auth/refresh  ── 刷新令牌（用 refreshToken 换新 Token）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、Retrofit 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @POST("api/auth/register")：
 *   - 声明这是一个 HTTP POST 请求
 *   - 括号内是相对路径，会拼接到 Retrofit.baseUrl 后面
 *   - 完整 URL = Constants.BASE_URL + "api/auth/register"
 *
 * @Body RegisterRequest：
 *   - 将 Java 对象序列化为 JSON 作为请求体
 *   - Gson 会自动将 RegisterRequest 的字段转为 JSON
 *   - Content-Type 自动设为 application/json
 *
 * Call<Result<AuthResponse>>：
 *   - Retrofit 的异步调用对象
 *   - execute()：同步执行（阻塞当前线程）
 *   - enqueue()：异步执行（回调通知结果）
 *   - Result<AuthResponse>：统一响应格式，data 字段包含认证信息
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、JWT 双 Token 机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本系统使用双 Token 机制（Access Token + Refresh Token）：
 *
 * Access Token：
 * - 短期有效（默认 15 分钟）
 * - 用于所有 API 请求的认证
 * - 携带在 Authorization: Bearer 头中
 * - 过期后需要用 Refresh Token 换取新的
 *
 * Refresh Token：
 * - 长期有效（默认 7 天）
 * - 仅用于刷新 Access Token
 * - 存储在 EncryptedSharedPreferences 中
 * - 使用后后端会签发新的 Refresh Token（旋转策略）
 *
 * 为什么用双 Token？
 * - 单 Token 方案：Token 过期后用户必须重新登录，体验差
 * - 双 Token 方案：Access Token 过期后自动刷新，用户无感知
 * - 安全性：Access Token 短期有效，即使泄露影响有限
 *
 * 关联类：
 * - AuthInterceptor：自动添加 Access Token 到请求头
 * - TokenRefreshInterceptor：自动处理 401 并刷新 Token
 * - TokenStore：安全存储 Token
 */
public interface AuthApi {

    /**
     * 用户注册 ── 创建新账号
     *
     * 请求：POST /api/auth/register
     * 请求体：{"email": "xxx", "password": "xxx", "nickname": "xxx"}
     * 响应体：{"code": 200, "message": "success", "data": null}
     *
     * 注册成功后 data 为 null（不自动登录），用户需要再调用 login。
     * 注册失败可能的原因：
     * - 400：邮箱格式不正确 / 密码太短 / 昵称为空
     * - 409：邮箱已被注册
     *
     * @param request 注册请求体，包含 email、password、nickname
     * @return Call<Result<Object>> 注册结果，成功时 data 为 null
     */
    @POST("api/auth/register")
    Call<Result<Object>> register(@Body RegisterRequest request);

    /**
     * 用户登录 ── 验证身份并获取 JWT Token
     *
     * 请求：POST /api/auth/login
     * 请求体：{"email": "xxx", "password": "xxx"}
     * 响应体：{"code": 200, "data": {"accessToken": "xxx", "refreshToken": "yyy", "expiresIn": 900}}
     *
     * 登录成功后，AuthResponse 包含双 Token：
     * - accessToken：短期令牌，用于 API 认证
     * - refreshToken：长期令牌，用于刷新 accessToken
     * - expiresIn：accessToken 的有效期（秒），默认 900 秒 = 15 分钟
     *
     * 登录失败可能的原因：
     * - 400：邮箱或密码格式不正确
     * - 401：邮箱或密码错误
     *
     * @param request 登录请求体，包含 email 和 password
     * @return Call<Result<AuthResponse>> 登录结果，成功时 data 包含 Token
     */
    @POST("api/auth/login")
    Call<Result<AuthResponse>> login(@Body LoginRequest request);

    /**
     * 刷新令牌 ── 用 refreshToken 换取新的 accessToken
     *
     * 请求：POST /api/auth/refresh
     * 请求体：{"refreshToken": "xxx"}
     * 响应体：{"code": 200, "data": {"accessToken": "xxx", "refreshToken": "yyy", "expiresIn": 900}}
     *
     * 此端点通常不由 Repository 直接调用，
     * 而是由 TokenRefreshInterceptor 在收到 401 响应时自动调用。
     *
     * 后端采用 Refresh Token 旋转策略：
     * - 每次使用 refreshToken 后，后端会签发新的 refreshToken
     * - 旧的 refreshToken 立即失效
     * - 防止 refreshToken 被盗用后长期有效
     *
     * @param request 刷新请求体，包含 refreshToken
     * @return Call<Result<AuthResponse>> 刷新结果，成功时 data 包含新 Token
     */
    @POST("api/auth/refresh")
    Call<Result<AuthResponse>> refresh(@Body RefreshRequest request);

    /**
     * 注册请求体 ── 发送注册请求时携带的数据
     *
     * 字段说明：
     * - email：用户邮箱，作为登录账号，必须唯一
     * - password：登录密码，后端要求至少 6 位
     * - nickname：用户昵称，显示在社区攻略的作者名等位置
     */
    class RegisterRequest {
        /** 用户邮箱，作为登录账号 */
        public String email;
        /** 登录密码 */
        public String password;
        /** 用户昵称，显示在社区等位置 */
        public String nickname;

        /**
         * 构造注册请求体
         *
         * @param email 邮箱
         * @param password 密码
         * @param nickname 昵称
         */
        public RegisterRequest(String email, String password, String nickname) {
            this.email = email;
            this.password = password;
            this.nickname = nickname;
        }
    }

    /**
     * 登录请求体 ── 发送登录请求时携带的数据
     *
     * 只需要 email 和 password，不需要 nickname。
     */
    class LoginRequest {
        /** 用户邮箱 */
        public String email;
        /** 登录密码 */
        public String password;

        /**
         * 构造登录请求体
         *
         * @param email 邮箱
         * @param password 密码
         */
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    /**
     * 刷新令牌请求体 ── 发送刷新请求时携带的数据
     *
     * 只需要 refreshToken，后端验证后签发新的双 Token。
     */
    class RefreshRequest {
        /** 刷新令牌 */
        public String refreshToken;

        /**
         * 构造刷新请求体
         *
         * @param refreshToken 刷新令牌
         */
        public RefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    /**
     * 认证响应体 ── 登录/刷新成功后返回的 Token 数据
     *
     * 包含双 Token 和过期时间：
     * - accessToken：短期令牌，用于 API 认证（15 分钟有效）
     * - refreshToken：长期令牌，用于刷新 accessToken（7 天有效）
     * - expiresIn：accessToken 的有效期（秒）
     *
     * Repository 收到此响应后，会将 Token 保存到 TokenStore。
     */
    class AuthResponse {
        /** 访问令牌，用于 API 认证 */
        public String accessToken;
        /** 刷新令牌，用于获取新的 accessToken */
        public String refreshToken;
        /** accessToken 的有效期（秒），默认 900 秒 = 15 分钟 */
        public long expiresIn;
    }
}
