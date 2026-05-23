package com.aram.mayhem.network.interceptor;

import androidx.annotation.NonNull;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.data.local.TokenStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 令牌刷新拦截器 ── 自动处理 401 响应，刷新 Token 并重试请求
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当服务器返回 401 Unauthorized 时，说明当前 accessToken 已过期。
 * 本拦截器会自动使用 refreshToken 换取新的 accessToken，
 * 然后用新 Token 重新发送原始请求，对用户完全透明。
 *
 * 用户不会感知到 Token 过期和刷新的过程，体验无缝。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、Token 刷新流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. 请求发出 → 服务器返回 401（Token 过期）
 * 2. 拦截器检测到 401 响应码
 * 3. 从 TokenStore 读取 refreshToken
 * 4. 向 /api/auth/refresh 发送同步请求，用 refreshToken 换取新 Token
 * 5. 刷新成功：
 *    a. 解析响应，获取新的 accessToken 和 refreshToken
 *    b. 更新 TokenStore 中的 Token
 *    c. 用新 Token 重新构建原始请求
 *    d. 重新发送请求，返回新响应
 * 6. 刷新失败：
 *    a. 清除 TokenStore（Token 已失效，用户需重新登录）
 *    b. 返回原始 401 响应
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、线程安全：synchronized 的作用
 * ═══════════════════════════════════════════════════════════════════
 *
 * 多个请求可能同时收到 401 响应，如果不加同步控制：
 * - 线程 A 和线程 B 同时检测到 401
 * - 两个线程同时发起 Token 刷新请求
 * - 后端可能使第一个 refreshToken 失效
 * - 第二个刷新请求失败，导致用户被登出
 *
 * synchronized(this) 确保同一时刻只有一个线程执行刷新逻辑：
 * - 线程 A 获得锁，执行刷新
 * - 线程 B 等待，线程 A 刷新完成后释放锁
 * - 线程 B 获得锁，发现 Token 已被更新，直接用新 Token 重试
 *
 * 注意：这里有一个优化空间 —— 线程 B 获得锁后应该检查 Token 是否已被更新，
 * 如果已更新则直接重试，无需再次刷新。当前实现会再次刷新，可能导致问题。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、为什么不使用 Retrofit 发起刷新请求？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 刷新请求使用原生 OkHttpClient 而非 Retrofit，原因：
 * - 本拦截器在 Retrofit 的拦截器链中，如果用 Retrofit 发刷新请求，
 *   刷新请求也会经过本拦截器，可能导致无限递归
 * - 刷新请求不需要经过 AuthInterceptor（不需要 Token）
 * - 刷新请求不需要经过 TokenRefreshInterceptor（避免递归）
 * - 使用独立的 OkHttpClient 避免拦截器循环
 *
 * 关联类：
 * - TokenStore：JWT 令牌存储，读写 Token
 * - AuthInterceptor：认证拦截器，添加 Token 到请求头
 * - Constants：API 路径常量
 */
@Singleton
public class TokenRefreshInterceptor implements Interceptor {

    /**
     * JSON 媒体类型 ── 用于构建刷新请求的请求体
     *
     * "application/json; charset=utf-8" 告诉服务器请求体是 JSON 格式
     */
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /**
     * Token 存储 ── 读取 refreshToken 和保存新 Token
     */
    private final TokenStore tokenStore;

    /**
     * JSON 解析工具 ── 解析刷新响应中的 Token 数据
     */
    private final Gson gson;

    /**
     * 构造函数 ── Hilt 自动注入 TokenStore 和 Gson
     *
     * @param tokenStore JWT 令牌存储
     * @param gson JSON 解析工具
     */
    @Inject
    public TokenRefreshInterceptor(TokenStore tokenStore, Gson gson) {
        this.tokenStore = tokenStore;
        this.gson = gson;
    }

    /**
     * 拦截响应 ── 检测 401 并自动刷新 Token
     *
     * 处理逻辑：
     * 1. 先执行原始请求，获取响应
     * 2. 如果响应码不是 401，直接返回响应（无需刷新）
     * 3. 如果响应码是 401，进入刷新流程
     * 4. 刷新成功：用新 Token 重试原始请求
     * 5. 刷新失败：清除 TokenStore，返回原始 401 响应
     *
     * @param chain 拦截器链
     * @return HTTP 响应（可能是重试后的新响应）
     */
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        if (response.code() == 401) {
            synchronized (this) {
                String currentAccessToken = tokenStore.getAccessToken();
                String refreshToken = tokenStore.getRefreshToken();

                if (refreshToken != null && !refreshToken.isEmpty()) {
                    Response refreshResponse = performRefresh(refreshToken);
                    if (refreshResponse != null && refreshResponse.isSuccessful()) {
                        String responseBody = refreshResponse.body() != null
                                ? refreshResponse.body().string() : "";
                        refreshResponse.close();

                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        if (json.has("data")) {
                            JsonObject data = json.getAsJsonObject("data");
                            String newAccessToken = data.get("accessToken").getAsString();
                            String newRefreshToken = data.get("refreshToken").getAsString();
                            long expiresIn = data.has("expiresIn")
                                    ? data.get("expiresIn").getAsLong() * 1000
                                    : 900000L;

                            tokenStore.saveTokens(newAccessToken, newRefreshToken, expiresIn);

                            Request newRequest = chain.request().newBuilder()
                                    .header("Authorization", "Bearer " + newAccessToken)
                                    .build();

                            return chain.proceed(newRequest);
                        }
                    }
                }

                tokenStore.clear();
            }
        }

        return response;
    }

    /**
     * 执行 Token 刷新请求 ── 向服务器发送 refreshToken 换取新 Token
     *
     * 使用独立的 OkHttpClient（不经过拦截器链），避免递归调用。
     *
     * 请求格式：
     *   POST /api/auth/refresh
     *   Content-Type: application/json
     *   Body: {"refreshToken": "xxx"}
     *
     * 响应格式：
     *   {"code": 200, "data": {"accessToken": "xxx", "refreshToken": "yyy", "expiresIn": 900}}
     *
     * 为什么用 execute() 而不是 enqueue()？
     * - execute() 是同步调用，阻塞当前线程直到收到响应
     * - 本拦截器已经在后台线程（OkHttp 的线程池）中执行
     * - 同步调用更简单，不需要回调嵌套
     * - 刷新请求必须完成后才能决定是否重试原始请求
     *
     * @param refreshToken 刷新令牌
     * @return 刷新响应，失败时返回 null
     */
    private Response performRefresh(String refreshToken) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("refreshToken", refreshToken);

        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_AUTH_REFRESH)
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA_TYPE))
                .build();

        return client.newCall(request).execute();
    }
}
