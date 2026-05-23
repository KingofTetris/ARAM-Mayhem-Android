package com.aram.mayhem.feature.profile.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aram.mayhem.network.api.CommunityApi;
import com.aram.mayhem.network.dto.StrategyListResponse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

/**
 * 我的攻略 ViewModel ── 管理用户发布的攻略列表数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * MyStrategiesViewModel 负责管理"我的攻略"页面的数据，包括：
 * 1. 加载当前用户发布的攻略列表
 * 2. 删除用户发布的攻略
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、与社区模块的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本 ViewModel 使用 CommunityApi（社区模块的 API 接口）来获取和删除攻略，
 * 因为"我的攻略"本质上是社区攻略的一个子集（按用户 ID 过滤）。
 *
 *   CommunityApi 方法          用途
 *   ─────────────────────────────────────────
 *   getMyStrategies()          获取当前用户的攻略列表
 *   deleteStrategy(id)         删除指定攻略
 *
 * 为什么不通过 StrategyRepository 访问？
 * ── StrategyRepository 是社区模块的仓库，包含缓存逻辑。
 *    "我的攻略"页面数据量小且不需要缓存（每次打开都重新加载），
 *    直接使用 API 更简单高效。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、删除流程
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户左滑/点击删除
 *       ↓
 *   Fragment 弹出确认对话框
 *       ↓
 *   用户确认 → viewModel.deleteStrategy(id, position)
 *       ↓
 *   CommunityApi.deleteStrategy(id) → DELETE /api/strategies/{id}
 *       ↓
 *   成功 → deleteSuccess = true → Fragment 重新加载列表
 *   失败 → error = "删除失败"
 *
 * @see CommunityApi          社区模块的 API 接口
 * @see StrategyListResponse  攻略列表项的数据模型
 */
@HiltViewModel
public class MyStrategiesViewModel extends ViewModel {

    /**
     * 社区 API 接口
     *
     * 提供 getMyStrategies() 和 deleteStrategy() 两个方法。
     * Retrofit 自动实现此接口，发送 HTTP 请求到后端。
     */
    private final CommunityApi communityApi;

    /**
     * 我的攻略列表数据
     *
     * 包含当前用户发布的所有攻略，每条攻略包含：
     * - id：攻略 ID
     * - title：攻略标题
     * - heroName：英雄名称
     * - upvotes / downvotes：投票数
     * - augmentIcons / itemIcons：符文/装备图标列表
     */
    private final MutableLiveData<List<StrategyListResponse>> myStrategies = new MutableLiveData<>();

    /**
     * 加载状态（true = 正在加载）
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 删除成功标志
     *
     * true → 删除成功，Fragment 重新加载列表
     * false → 删除失败，Fragment 显示错误提示
     */
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();

    /**
     * 构造函数（Hilt 自动注入依赖）
     *
     * @param communityApi 社区 API 接口
     */
    @Inject
    public MyStrategiesViewModel(CommunityApi communityApi) {
        this.communityApi = communityApi;
    }

    public LiveData<List<StrategyListResponse>> getMyStrategies() { return myStrategies; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getDeleteSuccess() { return deleteSuccess; }

    /**
     * 加载我的攻略列表
     *
     * ═══════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 检查 loading 状态，防止重复加载
     * 2. 设置 loading = true
     * 3. 调用 communityApi.getMyStrategies() 发起 GET 请求
     * 4. 成功 → 设置 myStrategies 数据
     * 5. 失败 → 设置空列表 + 错误信息
     *
     * 为什么失败时也设置空列表？
     * ── 如果不设置，LiveData 的值可能是上一次加载的旧数据，
     *    Fragment 会继续显示旧数据。设置空列表可以确保
     *    Fragment 显示空状态提示（"暂无攻略"）。
     */
    public void loadMyStrategies() {
        if (Boolean.TRUE.equals(loading.getValue())) return;

        loading.setValue(true);
        communityApi.getMyStrategies().enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<com.aram.mayhem.common.Result<List<StrategyListResponse>>> call,
                                   retrofit2.Response<com.aram.mayhem.common.Result<List<StrategyListResponse>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    myStrategies.setValue(response.body().getData());
                    Timber.d("Loaded %d my strategies", response.body().getData() != null ? response.body().getData().size() : 0);
                } else {
                    myStrategies.setValue(new ArrayList<>());
                    error.setValue("加载我的攻略失败");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.aram.mayhem.common.Result<List<StrategyListResponse>>> call, Throwable t) {
                loading.setValue(false);
                myStrategies.setValue(new ArrayList<>());
                error.setValue("网络错误：" + t.getMessage());
                Timber.e(t, "Failed to load my strategies");
            }
        });
    }

    /**
     * 删除攻略
     *
     * ═══════════════════════════════════════════════════════════
     * 删除流程详解
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 调用 communityApi.deleteStrategy(strategyId) 发起 DELETE 请求
     * 2. 成功 → deleteSuccess = true → Fragment 观察后重新加载列表
     * 3. 失败 → deleteSuccess = false + error 提示
     *
     * 为什么删除成功后不直接从列表中移除 item？
     * ── 两种策略各有优劣：
     *
     *   策略 A（直接移除）：
     *   - 优点：响应快，用户立即看到变化
     *   - 缺点：如果后端删除实际失败（网络延迟），
     *     本地已经移除了，数据不一致
     *
     *   策略 B（重新加载）← 本项目采用：
     *   - 优点：数据一定与后端一致
     *   - 缺点：多一次网络请求，体验稍慢
     *
     *   选择策略 B 的原因：攻略数据量小，重新加载很快，
     *   且确保数据一致性更重要。
     *
     * @param strategyId 攻略 ID，后端用于定位要删除的攻略
     * @param position   列表中的位置（预留参数，当前未使用）
     */
    public void deleteStrategy(long strategyId, int position) {
        communityApi.deleteStrategy(strategyId).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<com.aram.mayhem.common.Result<Void>> call,
                                   retrofit2.Response<com.aram.mayhem.common.Result<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    deleteSuccess.setValue(true);
                    Timber.d("Strategy deleted: id=%d", strategyId);
                } else {
                    deleteSuccess.setValue(false);
                    error.setValue("删除失败");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.aram.mayhem.common.Result<Void>> call, Throwable t) {
                deleteSuccess.setValue(false);
                error.setValue("网络错误：" + t.getMessage());
                Timber.e(t, "Failed to delete strategy: id=%d", strategyId);
            }
        });
    }
}
