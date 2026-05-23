package com.aram.mayhem.feature.community.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.data.local.dao.StrategyDao;
import com.aram.mayhem.data.local.entity.StrategyEntity;
import com.aram.mayhem.network.api.CommunityApi;
import com.aram.mayhem.network.dto.CreateStrategyRequest;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.network.dto.StrategyDetailResponse;
import com.aram.mayhem.network.dto.StrategyListResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 攻略数据仓库 ── 社区模块的统一数据访问层
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、Repository 模式是什么？
 * ═══════════════════════════════════════════════════════════════════
 *
 * Repository 是 MVVM 架构中的"数据管家"，它：
 * 1. 屏蔽数据来源的复杂性（网络？本地？）
 * 2. 对 ViewModel 只暴露简单的 LiveData 接口
 * 3. 决定什么时候用网络数据，什么时候用缓存数据
 *
 *   ViewModel 不需要知道数据从哪里来：
 *
 *   ViewModel  →  Repository  →  CommunityApi（远程数据）
 *                          ↘  StrategyDao（本地缓存）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据源策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本仓库采用"缓存优先 + 网络刷新"策略（与 AugmentRepository 的"网络优先"不同）：
 *
 *   ┌───────────────────────────────────────────────────────────────┐
 *   │  1. 先从本地缓存读取数据，立即返回给 UI（快速响应）           │
 *   │  2. 再发起网络请求获取最新数据                               │
 *   │  3. 网络成功 → 更新 UI + 异步写入缓存                       │
 *   │  4. 网络失败 → 保留缓存数据（用户仍能看到旧数据）            │
 *   │  5. 离线模式 → 跳过网络请求，只返回缓存                     │
 *   └───────────────────────────────────────────────────────────────┘
 *
 * 为什么用"缓存优先"而不是"网络优先"？
 * - 社区攻略列表更新频率低，缓存数据通常仍然有效
 * - 先显示缓存可以让用户立即看到内容，不用等网络请求
 * - 符文数据更新频率高（胜率实时变化），所以用"网络优先"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与其他 Repository 的对比
 * ═══════════════════════════════════════════════════════════════════
 *
 * ┌──────────────────────┬──────────────────┬──────────────────────┐
 * │ Repository           │ 数据策略         │ 离线支持             │
 * ├──────────────────────┼──────────────────┼──────────────────────┤
 * │ HeroRepository       │ 网络优先         │ 网络失败回退缓存     │
 * │ AugmentRepository    │ 网络优先         │ 网络失败回退缓存     │
 * │ StrategyRepository   │ 缓存优先+网络刷新│ 离线模式跳过网络     │
 * └──────────────────────┴──────────────────┴──────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数据转换流程
 * ═══════════════════════════════════════════════════════════════════
 *
 *   网络层 DTO                仓库层 Entity              UI层
 *   ┌──────────────────┐    ┌──────────────┐           ┌──────────────┐
 *   │StrategyListResp   │──→│StrategyEntity│           │直接使用 DTO   │
 *   │(Retrofit 反序列化) │    │(Room 存储)    │──→       │(不转UiModel) │
 *   └──────────────────┘    └──────────────┘           └──────────────┘
 *
 *   注意：社区模块直接使用 StrategyListResponse/StrategyDetailResponse，
 *   没有像英雄/符文模块那样转换为 UiModel。
 *   原因：社区模块数据结构简单，DTO 字段直接对应 UI 展示需求。
 *
 * @see CommunityApi
 * @see StrategyDao
 * @see StrategyEntity
 */
@Singleton
public class StrategyRepository {

    /**
     * 社区 API 接口 ── Retrofit 定义的 HTTP 请求方法
     *
     * 提供的接口方法：
     * - getStrategies(sort, page, size)：获取攻略列表
     * - getStrategyDetail(id)：获取攻略详情
     * - publishStrategy(request)：发布攻略
     * - vote(strategyId, request)：投票
     * - cancelVote(strategyId)：取消投票
     * - getMyStrategies()：获取我的攻略
     */
    private final CommunityApi communityApi;

    /**
     * 攻略本地缓存 DAO ── Room 数据库访问对象
     *
     * 提供的数据库操作：
     * - getStrategiesByLatest()：按最新排序查询缓存
     * - getStrategiesByHot()：按热门排序查询缓存
     * - insertAll(entities)：批量插入缓存
     */
    private final StrategyDao strategyDao;

    /**
     * 离线模式标记 ── volatile 保证多线程可见性
     *
     * true：跳过所有网络请求，只返回本地缓存
     * false：正常发起网络请求
     *
     * 由 CommunityFeedFragment 的网络监听器设置：
     * - 网络断开 → setOffline(true)
     * - 网络恢复 → setOffline(false)
     *
     * volatile 关键字的作用：
     * - 确保 isOffline 的修改对所有线程立即可见
     * - 因为网络请求在后台线程，UI 在主线程，需要线程安全
     */
    private volatile boolean isOffline = false;

    /**
     * 构造方法 ── 通过 Hilt 依赖注入初始化
     *
     * @param communityApi 社区网络接口（Retrofit 代理对象）
     * @param strategyDao  攻略数据库访问对象（Room 代理对象）
     */
    @Inject
    public StrategyRepository(CommunityApi communityApi, StrategyDao strategyDao) {
        this.communityApi = communityApi;
        this.strategyDao = strategyDao;
    }

    /**
     * 设置离线模式 ── 由 Fragment 的网络监听器调用
     *
     * @param offline true=离线模式（跳过网络请求），false=在线模式
     */
    public void setOffline(boolean offline) {
        this.isOffline = offline;
    }

    /**
     * 查询当前是否离线
     *
     * @return true=离线模式，false=在线模式
     */
    public boolean isOffline() {
        return isOffline;
    }

    /**
     * 获取攻略列表 ── 分页查询，支持排序和离线缓存
     *
     * ═══════════════════════════════════════════════════════════════════
     * 执行流程（缓存优先 + 网络刷新）
     * ═══════════════════════════════════════════════════════════════════
     *
     *   步骤1: 后台线程读取本地缓存
     *     ├─ 缓存有数据 → postValue 立即返回给 UI（快速响应）
     *     └─ 缓存无数据 → 等待网络请求
     *
     *   步骤2: 检查离线模式
     *     ├─ isOffline=true → 跳过网络请求，直接返回
     *     └─ isOffline=false → 发起网络请求
     *
     *   步骤3: 网络请求回调
     *     ├─ 成功 → setValue 更新 UI + 异步写入缓存
     *     └─ 失败 → ensureNonEmptyResult 确保不返回 null
     *
     * ═══════════════════════════════════════════════════════════════════
     * postValue vs setValue 的区别
     * ═══════════════════════════════════════════════════════════════════
     *
     * - postValue：在后台线程调用，自动切换到主线程更新
     *   用于：缓存读取（在 new Thread 中执行）
     * - setValue：必须在主线程调用
     *   用于：Retrofit 回调（onResponse 已在主线程）
     *
     * @param sort 排序方式："hot"=热门排序，"latest"=最新排序
     * @param page 页码（从1开始）
     * @param size 每页数量（固定10）
     * @return LiveData<List<StrategyListResponse>> 可观察的攻略列表
     */
    public LiveData<List<StrategyListResponse>> getStrategies(String sort, int page, int size) {
        MutableLiveData<List<StrategyListResponse>> result = new MutableLiveData<>();

        new Thread(() -> {
            LiveData<List<StrategyEntity>> cached;
            if ("latest".equals(sort)) {
                cached = strategyDao.getStrategiesByLatest();
            } else {
                cached = strategyDao.getStrategiesByHot();
            }
            List<StrategyEntity> cachedList = cached.getValue();
            if (cachedList != null && !cachedList.isEmpty()) {
                List<StrategyListResponse> cachedResponses = cachedList.stream()
                        .map(this::convertEntityToResponse)
                        .collect(Collectors.toList());
                result.postValue(cachedResponses);
            }
        }).start();

        if (isOffline) {
            return result;
        }

        communityApi.getStrategies(sort, page, size).enqueue(new Callback<Result<PageResponse<StrategyListResponse>>>() {
            @Override
            public void onResponse(Call<Result<PageResponse<StrategyListResponse>>> call, Response<Result<PageResponse<StrategyListResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<StrategyListResponse> data = response.body().getData();
                    if (data != null && data.getRecords() != null) {
                        result.setValue(data.getRecords());
                        new Thread(() -> {
                            List<StrategyEntity> entities = data.getRecords().stream()
                                    .map(StrategyRepository.this::convertResponseToEntity)
                                    .collect(Collectors.toList());
                            strategyDao.insertAll(entities);
                        }).start();
                    } else {
                        result.setValue(Collections.emptyList());
                    }
                } else {
                    ensureNonEmptyResult(result);
                }
            }

            @Override
            public void onFailure(Call<Result<PageResponse<StrategyListResponse>>> call, Throwable t) {
                ensureNonEmptyResult(result);
            }
        });

        return result;
    }

    /**
     * 确保结果非空 ── 网络失败时的兜底逻辑
     *
     * 如果缓存也没有数据，返回空列表而非 null，
     * 防止 UI 层出现 NullPointerException。
     *
     * @param result 待检查的 MutableLiveData
     */
    private void ensureNonEmptyResult(MutableLiveData<List<StrategyListResponse>> result) {
        if (result.getValue() == null || result.getValue().isEmpty()) {
            result.setValue(Collections.emptyList());
        }
    }

    /**
     * Entity → Response 转换 ── 从本地缓存读取时使用
     *
     * 将 Room 数据库的 StrategyEntity 转换为网络层的 StrategyListResponse，
     * 因为 ViewModel 和 Fragment 直接使用 DTO 类型。
     *
     * @param entity 数据库实体
     * @return 网络层 DTO
     */
    private StrategyListResponse convertEntityToResponse(StrategyEntity entity) {
        StrategyListResponse response = new StrategyListResponse();
        response.setId(entity.id);
        response.setUserId(entity.userId);
        response.setAuthorNickname(entity.authorNickname);
        response.setAuthorAvatar(entity.authorAvatar);
        response.setHeroId(entity.heroId);
        response.setHeroName(entity.heroName);
        response.setHeroIcon(entity.heroIcon);
        response.setTitle(entity.title);
        response.setDescription(entity.description);
        response.setUpvotes(entity.upvotes);
        response.setDownvotes(entity.downvotes);
        response.setScore(entity.score);
        response.setCreatedAt(entity.createdAt);
        return response;
    }

    /**
     * Response → Entity 转换 ── 写入本地缓存时使用
     *
     * 将网络层的 StrategyListResponse 转换为 Room 数据库的 StrategyEntity，
     * 包含 null 安全处理（后端可能返回 null 字段）。
     *
     * @param response 网络层 DTO
     * @return 数据库实体
     */
    private StrategyEntity convertResponseToEntity(StrategyListResponse response) {
        StrategyEntity entity = new StrategyEntity();
        entity.id = response.getId() != null ? response.getId() : 0L;
        entity.userId = response.getUserId() != null ? response.getUserId() : 0L;
        entity.authorNickname = response.getAuthorNickname();
        entity.authorAvatar = response.getAuthorAvatar();
        entity.heroId = response.getHeroId() != null ? response.getHeroId() : 0L;
        entity.heroName = response.getHeroName();
        entity.heroIcon = response.getHeroIcon();
        entity.title = response.getTitle();
        entity.description = response.getDescription();
        entity.upvotes = response.getUpvotes() != null ? response.getUpvotes() : 0;
        entity.downvotes = response.getDownvotes() != null ? response.getDownvotes() : 0;
        entity.score = response.getScore() != null ? response.getScore() : 0;
        entity.createdAt = response.getCreatedAt();
        entity.updatedAt = System.currentTimeMillis();
        return entity;
    }

    /**
     * 获取攻略详情 ── 根据攻略ID获取完整攻略内容
     *
     * ═══════════════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════════════
     *
     *   1. 创建 MutableLiveData 作为结果容器
     *   2. 调用 communityApi.getStrategyDetail() 发起异步网络请求
     *   3. 成功 → 设置详情数据到 LiveData
     *   4. 失败 → 设置 null 表示获取失败
     *
     * 注意：详情数据没有本地缓存（与列表不同）
     * 原因：详情数据访问频率低，且内容可能随时变化（投票数等）
     *
     * @param strategyId 攻略唯一标识符
     * @return LiveData<StrategyDetailResponse> 详情数据，null 表示失败
     */
    public LiveData<StrategyDetailResponse> getStrategyDetail(long strategyId) {
        MutableLiveData<StrategyDetailResponse> result = new MutableLiveData<>();

        communityApi.getStrategyDetail(strategyId).enqueue(new Callback<Result<StrategyDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<StrategyDetailResponse>> call, Response<Result<StrategyDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(response.body().getData());
                } else {
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Result<StrategyDetailResponse>> call, Throwable t) {
                result.setValue(null);
            }
        });

        return result;
    }

    /**
     * 发布攻略 ── 将用户编辑的攻略提交到服务器
     *
     * ═══════════════════════════════════════════════════════════════════
     * 执行流程
     * ═══════════════════════════════════════════════════════════════════
     *
     *   1. 构建 CreateStrategyRequest 请求体
     *   2. 调用 communityApi.publishStrategy() 发起 POST 请求
     *   3. 成功 → 返回发布后的攻略详情（服务器会分配 ID 和时间戳）
     *   4. 失败 → 返回 null
     *
     * ═══════════════════════════════════════════════════════════════════
     * 发布成功后为什么返回 StrategyDetailResponse？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 服务器创建攻略后会返回完整数据，包括：
     * - 服务器分配的 ID
     * - 创建时间
     * - 初始投票数（0赞0踩）
     * - 作者信息（从 JWT Token 中提取）
     *
     * UI 可以直接使用这个数据跳转到详情页或成功页。
     *
     * @param heroId      关联英雄ID
     * @param title       攻略标题
     * @param description 攻略描述/内容
     * @param augmentIds  关联的强化符文ID列表
     * @param itemIds     关联的装备ID列表
     * @return LiveData<StrategyDetailResponse> 发布后的攻略详情，null 表示失败
     */
    public LiveData<StrategyDetailResponse> publishStrategy(Long heroId, String title, String description,
                                                            List<Long> augmentIds, List<Long> itemIds) {
        MutableLiveData<StrategyDetailResponse> result = new MutableLiveData<>();

        CreateStrategyRequest request = new CreateStrategyRequest(heroId, title, description, augmentIds, itemIds);

        communityApi.publishStrategy(request).enqueue(new Callback<Result<StrategyDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<StrategyDetailResponse>> call, Response<Result<StrategyDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(response.body().getData());
                } else {
                    result.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<Result<StrategyDetailResponse>> call, Throwable t) {
                result.setValue(null);
            }
        });

        return result;
    }

    /**
     * 为攻略投票 ── 用户对攻略进行点赞或点踩
     *
     * ═══════════════════════════════════════════════════════════════════
     * 投票规则
     * ═══════════════════════════════════════════════════════════════════
     *
     * - 同一用户对同一攻略只能投一次票
     * - 可以点赞(UP)或点踩(DOWN)
     * - 投票后可以取消(cancelVote)
     * - 取消后可以重新投票（可以改投另一种）
     *
     * ═══════════════════════════════════════════════════════════════════
     * 投票成功后的 UI 更新策略
     * ═══════════════════════════════════════════════════════════════════
     *
     * 投票结果只返回 Boolean（成功/失败），不返回最新投票数。
     * StrategyDetailViewModel 在投票成功后会本地更新投票计数：
     * - vote("UP") 成功 → upvotes + 1
     * - vote("DOWN") 成功 → downvotes + 1
     * - cancelVote() 成功 → 对应类型 - 1
     *
     * 这是一种"乐观更新"策略：先假设服务器成功，本地立即更新 UI，
     * 不需要再发一次请求获取最新数据。
     *
     * @param strategyId 攻略ID
     * @param voteType   投票类型："UP"=点赞，"DOWN"=点踩
     * @return LiveData<Boolean> true=投票成功，false=投票失败
     */
    public LiveData<Boolean> vote(long strategyId, String voteType) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        CommunityApi.VoteRequest voteRequest = new CommunityApi.VoteRequest(voteType);

        communityApi.vote(strategyId, voteRequest).enqueue(new Callback<Result<Void>>() {
            @Override
            public void onResponse(Call<Result<Void>> call, Response<Result<Void>> response) {
                result.setValue(response.isSuccessful() && response.body() != null && response.body().isSuccess());
            }

            @Override
            public void onFailure(Call<Result<Void>> call, Throwable t) {
                result.setValue(false);
            }
        });

        return result;
    }

    /**
     * 取消攻略投票 ── 撤销已投的票
     *
     * 只能取消自己投过的票。取消后该攻略的投票数减1。
     *
     * @param strategyId 攻略ID
     * @return LiveData<Boolean> true=取消成功，false=取消失败
     */
    public LiveData<Boolean> cancelVote(long strategyId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();

        communityApi.cancelVote(strategyId).enqueue(new Callback<Result<Void>>() {
            @Override
            public void onResponse(Call<Result<Void>> call, Response<Result<Void>> response) {
                result.setValue(response.isSuccessful() && response.body() != null && response.body().isSuccess());
            }

            @Override
            public void onFailure(Call<Result<Void>> call, Throwable t) {
                result.setValue(false);
            }
        });

        return result;
    }

    /**
     * 获取我的攻略列表 ── 查询当前登录用户发布的所有攻略
     *
     * 用于个人中心页面展示"我发布的攻略"。
     * 不分页（用户发布的攻略数量通常不多）。
     * 不缓存（每次都从服务器获取最新状态）。
     *
     * @return LiveData<List<StrategyListResponse>> 攻略列表，空列表表示失败或无数据
     */
    public LiveData<List<StrategyListResponse>> getMyStrategies() {
        MutableLiveData<List<StrategyListResponse>> result = new MutableLiveData<>();

        communityApi.getMyStrategies().enqueue(new Callback<Result<List<StrategyListResponse>>>() {
            @Override
            public void onResponse(Call<Result<List<StrategyListResponse>>> call, Response<Result<List<StrategyListResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<StrategyListResponse> data = response.body().getData();
                    result.setValue(data != null ? data : Collections.emptyList());
                } else {
                    result.setValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<Result<List<StrategyListResponse>>> call, Throwable t) {
                result.setValue(Collections.emptyList());
            }
        });

        return result;
    }
}
