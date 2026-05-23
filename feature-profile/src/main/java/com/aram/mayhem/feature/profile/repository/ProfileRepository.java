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
 * 用户资料数据仓库 ── 个人中心模块的统一数据访问层
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个仓库是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * ProfileRepository 是个人中心模块的数据访问层，负责：
 * 1. 获取当前登录用户的资料信息（头像、昵称、邮箱等）
 * 2. 更新用户资料（昵称、头像、显示模式、通知开关）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据源策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 与其他仓库不同，ProfileRepository 采用"纯网络"策略（无本地缓存）：
 *
 *   StrategyRepository     → 缓存优先 + 网络刷新
 *   BulletinListViewModel  → 缓存优先 + 网络刷新
 *   AugmentRepository      → 网络优先
 *   ProfileRepository      → 纯网络（无缓存）  ← 本仓库
 *
 * 为什么不缓存用户资料？
 * ── 1. 用户资料数据量小（仅几个字段），缓存收益低
 *    2. 用户资料更新频率低，每次打开页面重新请求即可
 *    3. 资料更新后需要立即看到最新数据，缓存可能导致显示旧数据
 *    4. 简化代码逻辑，无需维护本地数据库表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ViewModel                    ProfileRepository              后端 API
 *   ┌──────────────┐            ┌──────────────────┐          ┌──────────┐
 *   │ loadProfile()│ ─────────→ │ getUserProfile() │ ───────→ │ GET      │
 *   │              │ ←───────── │ 返回 LiveData    │ ←─────── │ /profile │
 *   ├──────────────┤            ├──────────────────┤          ├──────────┤
 *   │ updateProfile│ ─────────→ │ updateProfile()  │ ───────→ │ PUT      │
 *   │ ()           │ ←───────── │ 返回 LiveData    │ ←─────── │ /profile │
 *   └──────────────┘            └──────────────────┘          └──────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、@Singleton 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Singleton 表示整个应用中只创建一个 ProfileRepository 实例。
 * 所有注入此仓库的 ViewModel 共享同一个实例。
 *
 * 为什么仓库需要单例？
 * ── 仓库本身不持有可变状态（每次方法调用都创建新的 MutableLiveData），
 *    单例的主要目的是避免重复创建对象，节省内存。
 *    即使多个 ViewModel 同时使用此仓库，也不会有数据冲突问题，
 *    因为每次方法调用返回的是独立的 LiveData 对象。
 *
 * @see ProfileApi             用户资料相关的 Retrofit API 接口
 * @see UserProfileResponse    用户资料的响应数据模型
 * @see UpdateProfileRequest   更新资料的请求体
 */
@Singleton
public class ProfileRepository {

    /**
     * 用户资料 API 接口
     *
     * ProfileApi 是 Retrofit 自动实现的接口，提供两个 HTTP 端点：
     * - getUserProfile()  → GET  /api/profile     获取当前用户资料
     * - updateProfile()   → PUT  /api/profile     更新当前用户资料
     *
     * Retrofit 的工作原理：
     *   接口方法定义 + 注解（@GET/@PUT）→ Retrofit 在运行时生成实现类
     *   → 调用方法时自动构造 HTTP 请求 → 通过 OkHttp 发送 → 解析响应
     */
    private final ProfileApi profileApi;

    /**
     * 构造函数（Hilt 自动注入）
     *
     * @param profileApi 用户资料 API 接口，由 Hilt 提供的 Retrofit 实例创建
     */
    @Inject
    public ProfileRepository(ProfileApi profileApi) {
        this.profileApi = profileApi;
    }

    /**
     * 获取当前用户资料
     *
     * ═══════════════════════════════════════════════════════════
     * 方法执行流程
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 创建 MutableLiveData<UserProfileResponse> 作为结果容器
     * 2. 调用 profileApi.getUserProfile() 发起异步 HTTP 请求
     * 3. 请求成功（HTTP 2xx + 业务成功）→ setValue() 设置数据
     * 4. 请求失败或业务失败 → setValue(null)
     * 5. 返回 LiveData 供 ViewModel 观察
     *
     * 为什么用 MutableLiveData 而不是直接返回 API 响应？
     * ── Fragment 不能直接调用 Repository 方法并同步等待结果
     *    （网络请求是异步的，不能阻塞 UI 线程）。
     *    LiveData 是 Android 推荐的异步数据传递方式：
     *    ViewModel 观察 Repository 的 LiveData，
     *    Fragment 观察 ViewModel 的 LiveData，
     *    数据变化时自动通知 UI 更新。
     *
     * 为什么 onFailure 也设置 null 而不是抛异常？
     * ── 抛异常会导致应用崩溃。设置 null 让 ViewModel 判断：
     *    result != null → 成功，使用数据
     *    result == null → 失败，显示错误提示
     *    这是一种简单但有效的错误处理策略。
     *
     * @return LiveData<UserProfileResponse> 可观察的用户资料数据，
     *         成功时包含资料信息，失败时为 null
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
     * 更新当前用户资料
     *
     * ═══════════════════════════════════════════════════════════
     * 部分更新机制
     * ═══════════════════════════════════════════════════════════
     *
     * UpdateProfileRequest 支持部分更新（Partial Update）：
     * 只有非 null 的字段才会被更新，null 字段保持不变。
     *
     *   请求示例 1 ── 只改昵称：
     *   UpdateProfileRequest(nickname="新昵称", avatarUrl=null, displayMode=null, notificationEnabled=null)
     *   → 只更新昵称，其他字段不变
     *
     *   请求示例 2 ── 只改显示模式：
     *   UpdateProfileRequest(nickname=null, avatarUrl=null, displayMode=1, notificationEnabled=null)
     *   → 只更新显示模式，其他字段不变
     *
     * 这种设计的好处：
     * 1. 设置页的每个 Switch 可以独立更新，不需要发送所有字段
     * 2. 减少网络传输数据量
     * 3. 避免并发更新时的字段覆盖问题
     *
     * @param request 更新请求体，非 null 字段将被更新
     * @return LiveData<UserProfileResponse> 更新后的完整用户资料，
     *         成功时包含最新资料，失败时为 null
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
