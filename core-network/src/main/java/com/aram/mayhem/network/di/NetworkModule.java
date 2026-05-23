package com.aram.mayhem.network.di;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.data.local.TokenStore;
import com.aram.mayhem.network.api.AugmentApi;
import com.aram.mayhem.network.api.AuthApi;
import com.aram.mayhem.network.api.BulletinApi;
import com.aram.mayhem.network.api.CommunityApi;
import com.aram.mayhem.network.api.HeroApi;
import com.aram.mayhem.network.api.ProfileApi;
import com.aram.mayhem.network.interceptor.AuthInterceptor;
import com.aram.mayhem.network.interceptor.TokenRefreshInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络层 Hilt 依赖注入模块 ── 集中提供网络相关的所有对象
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是 Hilt 依赖注入框架的"网络工厂"，负责创建和提供：
 * 1. Gson ── JSON 序列化/反序列化工具
 * 2. HttpLoggingInterceptor ── HTTP 请求/响应日志拦截器
 * 3. OkHttpClient ── HTTP 客户端（含拦截器链）
 * 4. Retrofit ── REST API 客户端（基于 OkHttpClient）
 * 5. 各 API 接口实例 ── AuthApi, HeroApi, AugmentApi 等
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、对象创建顺序（依赖链）
 * ═══════════════════════════════════════════════════════════════════
 *
 * Gson ─────────────────────────────────────────┐
 *                                               │
 * HttpLoggingInterceptor ──────────────────────┐ │
 *                                              │ │
 * AuthInterceptor ────────────────────────────┐ │ │
 *                                             │ │ │
 * TokenRefreshInterceptor ───────────────────┐ │ │ │
 *                                            │ │ │ │
 * OkHttpClient(拦截器链) ◄───────────────────┘ │ │ │
 *     │                                       │ │ │
 *     └──► Retrofit(Gson + OkHttpClient) ◄────┘─┘─┘
 *              │
 *              ├──► AuthApi
 *              ├──► HeroApi
 *              ├──► AugmentApi
 *              ├──► CommunityApi
 *              ├──► BulletinApi
 *              └──► ProfileApi
 *
 * Hilt 会自动按依赖顺序创建对象，开发者无需关心创建顺序。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、OkHttp 拦截器链
 * ═══════════════════════════════════════════════════════════════════
 *
 * 拦截器按添加顺序执行，形成一条处理链：
 *
 * 请求方向（发出请求）：
 * AuthInterceptor → TokenRefreshInterceptor → HttpLoggingInterceptor → 服务器
 *   ①添加Token        ②（不修改请求）         ③记录请求日志
 *
 * 响应方向（收到响应）：
 * 服务器 → HttpLoggingInterceptor → TokenRefreshInterceptor → AuthInterceptor
 *          ③记录响应日志            ②处理401自动刷新          ①返回响应
 *
 * AuthInterceptor：为每个请求添加 Authorization: Bearer 头
 * TokenRefreshInterceptor：收到 401 时自动刷新 Token 并重试
 * HttpLoggingInterceptor：记录完整的请求/响应日志（仅 Debug 模式）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、超时配置
 * ═══════════════════════════════════════════════════════════════════
 *
 * 超时值定义在 Constants 类中：
 * - CONNECT_TIMEOUT：连接超时（建立 TCP 连接的最大等待时间）
 * - READ_TIMEOUT：读取超时（等待服务器响应的最大时间）
 * - WRITE_TIMEOUT：写入超时（发送请求数据的最大时间）
 *
 * 超时后 OkHttp 会抛出 SocketTimeoutException，
 * Repository 层会捕获并转换为用户友好的错误提示。
 *
 * 关联类：
 * - Constants：全局常量（BASE_URL、超时时间等）
 * - TokenStore：JWT 令牌存储（AuthInterceptor 和 TokenRefreshInterceptor 使用）
 * - AuthInterceptor：认证拦截器
 * - TokenRefreshInterceptor：令牌刷新拦截器
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    /**
     * 提供 Gson 实例 ── JSON 序列化/反序列化工具
     *
     * setLenient() 的作用：
     * - 默认 Gson 对 JSON 格式要求严格，遇到格式问题直接抛异常
     * - setLenient() 放宽解析限制，容忍一些非标准 JSON 格式
     * - 某些后端 API 可能返回格式不规范的 JSON，Lenient 模式可以避免解析失败
     *
     * 为什么是 @Singleton？
     * - Gson 是线程安全的，全局共享一个实例即可
     * - 避免重复创建 Gson 实例的开销
     *
     * @return Gson 单例实例
     */
    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
                .setLenient()
                .create();
    }

    /**
     * 提供 HTTP 日志拦截器 ── 记录网络请求和响应的详细信息
     *
     * Level.BODY 记录最详细的信息：
     * - 请求 URL、方法、头部
     * - 请求体（POST/PUT 的请求参数）
     * - 响应状态码、头部
     * - 响应体（服务器返回的数据）
     *
     * 注意：生产环境应改为 Level.NONE 或 Level.HEADERS，
     * 因为 BODY 级别会记录敏感数据（如 Token、密码），
     * 且大量日志会影响性能。
     *
     * @return HttpLoggingInterceptor 实例
     */
    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }

    /**
     * 提供 OkHttpClient ── HTTP 客户端，所有网络请求的底层执行者
     *
     * OkHttpClient 是 Retrofit 的底层 HTTP 引擎，负责：
     * 1. 建立 TCP 连接
     * 2. 执行拦截器链
     * 3. 发送请求和接收响应
     * 4. 管理连接池和缓存
     *
     * 拦截器添加顺序很重要：
     * - AuthInterceptor 最先添加，确保所有请求都携带 Token
     * - TokenRefreshInterceptor 其次，处理 401 自动刷新
     * - HttpLoggingInterceptor 最后，记录最终的请求/响应
     *
     * @param authInterceptor 认证拦截器，由 Hilt 自动注入
     * @param tokenRefreshInterceptor 令牌刷新拦截器，由 Hilt 自动注入
     * @param loggingInterceptor 日志拦截器，由 Hilt 自动注入
     * @return OkHttpClient 单例实例
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(
            AuthInterceptor authInterceptor,
            TokenRefreshInterceptor tokenRefreshInterceptor,
            HttpLoggingInterceptor loggingInterceptor
    ) {
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(tokenRefreshInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(Constants.CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    /**
     * 提供 Retrofit 实例 ── REST API 客户端
     *
     * Retrofit 是 OkHttp 的高层封装，提供：
     * 1. 基于接口的 API 定义（用注解声明 HTTP 请求）
     * 2. 自动 JSON 序列化/反序列化（通过 GsonConverterFactory）
     * 3. 异步/同步请求支持
     * 4. 请求/响应类型安全
     *
     * baseUrl：API 服务器的基础地址，所有 API 接口的相对路径都基于此
     * client：使用上面配置好的 OkHttpClient（含拦截器链）
     * addConverterFactory：添加 JSON 转换器，自动将 JSON 响应转为 Java 对象
     *
     * @param okHttpClient HTTP 客户端，由 Hilt 自动注入
     * @param gson JSON 工具，由 Hilt 自动注入
     * @return Retrofit 单例实例
     */
    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    /**
     * 提供 AuthApi ── 认证相关 API（注册、登录、刷新令牌）
     *
     * retrofit.create(AuthApi.class) 的原理：
     * - Retrofit 使用 Java 动态代理（Proxy）生成 AuthApi 接口的实现类
     * - 调用 api.login() 时，Retrofit 根据方法上的注解构建 HTTP 请求
     * - 不需要手动实现 AuthApi 接口，Retrofit 在运行时自动生成实现
     *
     * @param retrofit Retrofit 实例
     * @return AuthApi 实例
     */
    @Provides
    @Singleton
    public AuthApi provideAuthApi(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }

    /**
     * 提供 HeroApi ── 英雄相关 API（列表、详情）
     *
     * @param retrofit Retrofit 实例
     * @return HeroApi 实例
     */
    @Provides
    @Singleton
    public HeroApi provideHeroApi(Retrofit retrofit) {
        return retrofit.create(HeroApi.class);
    }

    /**
     * 提供 AugmentApi ── 符文相关 API（列表、详情、套装进度、推荐）
     *
     * @param retrofit Retrofit 实例
     * @return AugmentApi 实例
     */
    @Provides
    @Singleton
    public AugmentApi provideAugmentApi(Retrofit retrofit) {
        return retrofit.create(AugmentApi.class);
    }

    /**
     * 提供 CommunityApi ── 社区攻略相关 API（列表、详情、发布、投票）
     *
     * @param retrofit Retrofit 实例
     * @return CommunityApi 实例
     */
    @Provides
    @Singleton
    public CommunityApi provideCommunityApi(Retrofit retrofit) {
        return retrofit.create(CommunityApi.class);
    }

    /**
     * 提供 BulletinApi ── 公告相关 API（列表、最新、详情）
     *
     * @param retrofit Retrofit 实例
     * @return BulletinApi 实例
     */
    @Provides
    @Singleton
    public BulletinApi provideBulletinApi(Retrofit retrofit) {
        return retrofit.create(BulletinApi.class);
    }

    /**
     * 提供 ProfileApi ── 用户资料相关 API（获取资料、更新资料）
     *
     * @param retrofit Retrofit 实例
     * @return ProfileApi 实例
     */
    @Provides
    @Singleton
    public ProfileApi provideProfileApi(Retrofit retrofit) {
        return retrofit.create(ProfileApi.class);
    }
}
