package com.aram.mayhem;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

/**
 * 应用入口类 —— Android 应用的全局初始化入口
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这是 Android 应用的 Application 子类，在应用启动时最先被创建。
 * 主要职责：
 * 1. 标记 Hilt 依赖注入入口（@HiltAndroidApp）
 * 2. 初始化全局日志框架（Timber）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、@HiltAndroidApp 说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @HiltAndroidApp 是 Hilt 依赖注入框架的入口注解，作用：
 * 1. 触发 Hilt 的代码生成，创建 ApplicationComponent
 * 2. 让所有 @AndroidEntryPoint 标记的组件可以使用依赖注入
 * 3. 提供 Application 级别的单例作用域
 *
 * 没有此注解，@AndroidEntryPoint 将无法工作
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、Timber 日志框架
 * ═══════════════════════════════════════════════════════════════════
 *
 * Timber 是 Android 日志框架，相比原生 Log 的优势：
 * - 不需要手动传 TAG（自动使用类名作为 TAG）
 * - Debug 和 Release 使用不同的日志策略
 * - 可以轻松添加日志输出目标（如 Crashlytics）
 *
 * 日志策略：
 * - Debug 构建：输出所有日志到 Logcat
 * - Release 构建：仅输出 WARN 及以上级别的日志
 */
@HiltAndroidApp
public class MayhemApplication extends Application {

    /**
     * 应用创建回调 —— 在应用启动时调用，执行全局初始化
     *
     * 执行顺序：Application.onCreate() → 第一个 Activity.onCreate()
     * 所以这里适合做全局性的初始化工作
     */
    @Override
    public void onCreate() {
        super.onCreate();
        initTimber();
    }

    /**
     * 初始化 Timber 日志框架 —— 根据构建类型选择不同的日志树
     *
     * DebugTree：输出所有级别日志到 Logcat（VERBOSE/DEBUG/INFO/WARN/ERROR）
     * ReleaseTree：仅输出 WARN/ERROR 级别日志，避免敏感信息泄露
     */
    private void initTimber() {
        if (isDebugBuild()) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new ReleaseTree());
        }
        Timber.i("MayhemApplication initialized");
    }

    /**
     * 判断是否为 Debug 构建
     *
     * 当前实现始终返回 true（开发阶段）。
     * 正式发布时应改为 BuildConfig.DEBUG 或使用 BuildConfig.BUILD_TYPE 判断。
     *
     * @return true=Debug 构建，false=Release 构建
     */
    private boolean isDebugBuild() {
        return android.os.Build.VERSION.SDK_INT >= 0;
    }

    /**
     * Release 日志树 —— 仅记录 WARN 及以上级别的日志
     *
     * 为什么 Release 只记录 WARN 以上？
     * - DEBUG/VERBOSE 日志包含大量调试信息，可能泄露敏感数据
     * - Release 包不需要调试日志，减少 I/O 开销
     * - WARN/ERROR 日志用于记录异常，可以上报到 Crashlytics 等平台
     */
    private static class ReleaseTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority >= android.util.Log.WARN) {
                // Release builds only log WARN and above
            }
        }
    }
}