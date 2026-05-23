package com.aram.mayhem.di;

import android.content.Context;

import com.aram.mayhem.data.local.TokenStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * 应用级 Hilt 依赖注入模块 —— 提供应用级别的单例依赖
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * Hilt 依赖注入模块，负责提供应用级别的单例对象。
 * 当某个类不能被直接构造（如没有 @Inject 构造函数的第三方类），
 * 或者需要特定配置才能创建的对象，就需要在 Module 中手动提供。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Module：标记这是一个 Hilt 模块，包含依赖提供方法
 * @InstallIn(SingletonComponent.class)：将此模块安装到应用级组件中
 *   - SingletonComponent 的生命周期 = Application 的生命周期
 *   - 模块中 @Singleton 标记的对象在整个应用中只有一个实例
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、提供的依赖
 * ═══════════════════════════════════════════════════════════════════
 *
 * TokenStore：JWT Token 本地存储，用于持久化 Access Token 和 Refresh Token
 *   - 需要 Context 参数（通过 @ApplicationContext 注入 Application Context）
 *   - 使用 SharedPreferences 存储 Token
 *   - @Singleton 确保全局只有一个 TokenStore 实例
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    /**
     * 提供 TokenStore 单例 —— JWT Token 的本地持久化存储
     *
     * @param context Application Context（由 Hilt 自动注入）
     * @return TokenStore 实例（全局单例）
     *
     * 为什么用 @Singleton？
     * - TokenStore 管理全局的 Token 状态，多个地方需要读取/写入同一个 Token
     * - 如果创建多个实例，会导致 Token 状态不一致
     *
     * 为什么用 @ApplicationContext？
     * - TokenStore 使用 SharedPreferences，需要 Context 才能创建
     * - 使用 Application Context 而非 Activity Context，避免内存泄漏
     * - Application Context 的生命周期 = 整个应用，与 SingletonComponent 匹配
     */
    @Provides
    @Singleton
    public TokenStore provideTokenStore(@ApplicationContext Context context) {
        return new TokenStore(context);
    }
}