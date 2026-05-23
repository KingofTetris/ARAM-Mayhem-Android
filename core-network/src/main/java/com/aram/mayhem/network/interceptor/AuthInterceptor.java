package com.aram.mayhem.network.interceptor;

import androidx.annotation.NonNull;

import com.aram.mayhem.data.local.TokenStore;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 认证拦截器 ── 自动为每个 HTTP 请求添加 JWT Token
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * OkHttp 拦截器（Interceptor）是一种 AOP（面向切面编程）机制，
 * 可以在请求发出前和响应返回后插入自定义逻辑。
 *
 * 本拦截器的职责：在请求发出前，自动从 TokenStore 读取 accessToken，
 * 添加到请求头 Authorization: Bearer <token> 中。
 *
 * 这样 Repository 层不需要手动为每个请求添加 Token，
 * 只需要调用 api.getHeroes()，拦截器会自动附加认证信息。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、拦截器工作流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. 请求到达 AuthInterceptor
 * 2. 从 TokenStore 读取 accessToken
 * 3. 如果 Token 存在：
 *    a. 创建新的 Request，添加 Authorization 头
 *    b. 将新请求传递给下一个拦截器
 * 4. 如果 Token 不存在（用户未登录）：
 *    a. 直接将原始请求传递给下一个拦截器
 *    b. 公开接口（如英雄列表）不需要 Token 也能访问
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、为什么不直接在 Repository 里添加 Token？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 如果不用拦截器，每个 Repository 方法都需要：
 *   String token = tokenStore.getAccessToken();
 *   api.getHeroes("Bearer " + token, page, size);
 *
 * 问题：
 * - 大量重复代码，每个 API 调用都要手动添加 Token
 * - 容易遗漏，某个接口忘记添加 Token 导致认证失败
 * - 修改 Token 格式时需要改所有 Repository 方法
 *
 * 使用拦截器后，Token 的添加逻辑集中在一处，
 * Repository 只需要调用 api.getHeroes(page, size) 即可。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、@Singleton 的必要性
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本拦截器被注册到 OkHttpClient 中，OkHttpClient 是单例的，
 * 所以拦截器也必须是单例，否则每次创建 OkHttpClient 都会创建新的拦截器实例。
 * 更重要的是，TokenStore 是单例的，拦截器持有同一个 TokenStore 引用，
 * 确保所有请求读取的 Token 是一致的。
 *
 * 关联类：
 * - TokenStore：JWT 令牌存储，提供 accessToken
 * - TokenRefreshInterceptor：令牌刷新拦截器，处理 401 响应
 * - NetworkModule：注册本拦截器到 OkHttpClient
 */
@Singleton
public class AuthInterceptor implements Interceptor {

    /**
     * Token 存储 ── 从中读取 accessToken
     *
     * 由 Hilt 注入，全局共享同一个 TokenStore 实例
     */
    private final TokenStore tokenStore;

    /**
     * 构造函数 ── Hilt 自动调用，注入 TokenStore
     *
     * @Inject 告诉 Hilt 这是依赖注入的入口点
     * Hilt 会自动找到 TokenStore 的 @Provides 方法并注入
     *
     * @param tokenStore JWT 令牌存储
     */
    @Inject
    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * 拦截请求 ── 在请求发出前添加 Authorization 头
     *
     * OkHttp 拦截器的核心方法，每个 HTTP 请求都会经过此方法。
     *
     * 处理逻辑：
     * 1. 获取原始请求对象
     * 2. 从 TokenStore 读取 accessToken
     * 3. 如果 Token 为空（用户未登录），直接放行原始请求
     * 4. 如果 Token 存在，创建新请求并添加 Authorization 头
     * 5. 将请求传递给下一个拦截器（chain.proceed）
     *
     * 为什么创建新请求而不是修改原始请求？
     * - OkHttp 的 Request 是不可变对象（Immutable），不能直接修改
     * - 使用 newBuilder() 基于原始请求创建新的 Builder
     * - 在 Builder 上添加头部，然后 build() 生成新请求
     *
     * @param chain 拦截器链，用于将请求传递给下一个拦截器
     * @return HTTP 响应
     */
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();

        String accessToken = tokenStore.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return chain.proceed(original);
        }

        Request.Builder builder = original.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .method(original.method(), original.body());

        return chain.proceed(builder.build());
    }
}
