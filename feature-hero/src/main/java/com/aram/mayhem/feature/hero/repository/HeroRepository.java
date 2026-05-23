package com.aram.mayhem.feature.hero.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.common.Result;
import com.aram.mayhem.common.Tier;
import com.aram.mayhem.data.local.dao.HeroDao;
import com.aram.mayhem.data.local.entity.HeroEntity;
import com.aram.mayhem.network.api.HeroApi;
import com.aram.mayhem.network.dto.HeroDetailResponse;
import com.aram.mayhem.network.dto.HeroResponse;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.ui.model.HeroDetailUiModel;
import com.aram.mayhem.ui.model.HeroUiModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 英雄数据仓库 ── 英雄模块的数据中枢，实现离线优先策略
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Repository 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroRepository 是英雄模块的"数据调度中心"，负责：
 * 1. 从服务器获取英雄列表和英雄详情
 * 2. 将服务器数据缓存到本地 Room 数据库
 * 3. 网络不可用时提供本地缓存数据
 * 4. 在不同数据格式之间做转换（DTO ↔ Entity ↔ UiModel）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、离线优先策略（Offline-First）
 * ═══════════════════════════════════════════════════════════════════
 *
 * 核心思想：先返回缓存数据让用户看到内容，再请求网络获取最新数据。
 *
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │                    在线模式                                  │
 *   │                                                             │
 *   │  1. 读取 Room 缓存 ──→ 有数据？立即返回给 ViewModel          │
 *   │  2. 发起网络请求 ──→ 成功？更新缓存 + 返回最新数据            │
 *   │                    └─→ 失败？保持缓存数据不变                 │
 *   └─────────────────────────────────────────────────────────────┘
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │                    离线模式                                  │
 *   │                                                             │
 *   │  1. 读取 Room 缓存 ──→ 有数据？返回缓存数据                  │
 *   │                    └─→ 无数据？返回空列表                    │
 *   │  2. 跳过网络请求（因为无网络）                                │
 *   └─────────────────────────────────────────────────────────────┘
 *
 * 为什么用离线优先？
 * - 用户打开 App 时立即看到内容（缓存），不用等网络请求
 * - 地铁/电梯等弱网场景下仍可浏览已加载的英雄数据
 * - 网络请求成功后自动更新为最新数据，用户无感知
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据格式转换链
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本仓库管理 3 种数据格式之间的转换：
 *
 *   网络层 DTO          数据库 Entity         UI 层 UiModel
 *   ┌──────────┐       ┌──────────┐        ┌──────────────┐
 *   │HeroResponse│ ──→  │HeroEntity│  ──→   │HeroUiModel   │
 *   │(Retrofit) │       │(Room)    │        │(列表展示)     │
 *   └──────────┘       └──────────┘        └──────────────┘
 *        │                                       ↑
 *        │  convertToUiModel()                   │
 *        └───────────────────────────────────────┘
 *
 *   网络层 DTO              数据库 Entity         UI 层 UiModel
 *   ┌────────────────┐    ┌──────────┐        ┌──────────────────┐
 *   │HeroDetailResponse│→ │HeroEntity│  ──→   │HeroDetailUiModel │
 *   │(Retrofit)       │    │(Room)    │        │(详情展示)         │
 *   └────────────────┘    └──────────┘        └──────────────────┘
 *        │                                         ↑
 *        │  convertToDetailUiModel()               │
 *        └─────────────────────────────────────────┘
 *
 * 6 个转换方法：
 * - convertToUiModel()         ：HeroResponse → HeroUiModel（网络→列表UI）
 * - convertToEntity()          ：HeroResponse → HeroEntity（网络→数据库）
 * - convertDetailToEntity()    ：HeroDetailResponse → HeroEntity（网络→数据库）
 * - convertEntityToUiModel()   ：HeroEntity → HeroUiModel（数据库→列表UI）
 * - convertEntityToDetailUiModel()：HeroEntity → HeroDetailUiModel（数据库→详情UI）
 * - convertToDetailUiModel()   ：HeroDetailResponse → HeroDetailUiModel（网络→详情UI）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、线程模型
 * ═══════════════════════════════════════════════════════════════════
 *
 *   操作              执行线程              原因
 *   ───────────────  ────────────────     ────────────────
 *   Room 缓存读取     new Thread()         Room 不允许主线程读写
 *   Room 缓存写入     new Thread()         Room 不允许主线程读写
 *   Retrofit 网络请求  Retrofit 内部线程    enqueue() 自动异步
 *   LiveData.setValue  主线程               setValue 只能在主线程调用
 *   LiveData.postValue  子线程              postValue 从子线程更新
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、单例模式
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Singleton 注解保证整个 App 只有一个 HeroRepository 实例。
 * 好处：
 * - HeroListViewModel 和 HeroDetailViewModel 共享同一实例
 * - isOffline 状态在所有 ViewModel 之间同步
 * - 避免重复创建 HeroApi 和 HeroDao 的开销
 *
 * @see HeroApi
 * @see HeroDao
 * @see HeroEntity
 * @see com.aram.mayhem.feature.hero.viewmodel.HeroListViewModel
 * @see com.aram.mayhem.feature.hero.viewmodel.HeroDetailViewModel
 */
@Singleton
public class HeroRepository {

    /**
     * 英雄网络 API ── Retrofit 自动实现的 HTTP 请求接口
     *
     * 由 Hilt 通过 NetworkModule 提供，负责与后端 /api/heroes 通信。
     * 调用方式：heroApi.getHeroes(...).enqueue(callback)
     */
    private final HeroApi heroApi;

    /**
     * 英雄数据库 DAO ── Room 自动实现的数据访问对象
     *
     * 由 Hilt 通过 DatabaseModule 提供，负责与 Room 数据库交互。
     * 主要操作：getAllHeroes()、getHeroById()、insertAll()
     */
    private final HeroDao heroDao;

    /**
     * 离线模式标记 ── volatile 保证多线程可见性
     *
     * true = 当前无网络连接，跳过所有网络请求
     * false = 当前有网络，正常发起网络请求
     *
     * 由 ViewModel.setOffline() 调用更新：
     * - Fragment 的 ConnectivityManager.NetworkCallback 检测到网络变化
     * - Fragment 调用 ViewModel.setOffline()
     * - ViewModel 调用 Repository.setOffline()
     *
     * volatile 的作用：
     * - 网络回调在 Retrofit 线程执行
     * - Room 操作在 new Thread() 中执行
     * - volatile 保证 isOffline 的修改对所有线程立即可见
     */
    private volatile boolean isOffline = false;

    /**
     * 构造函数 ── Hilt 自动调用，注入依赖
     *
     * @param heroApi 英雄网络 API（Hilt 自动注入）
     * @param heroDao 英雄数据库 DAO（Hilt 自动注入）
     */
    @Inject
    public HeroRepository(HeroApi heroApi, HeroDao heroDao) {
        this.heroApi = heroApi;
        this.heroDao = heroDao;
    }

    /**
     * 获取 HeroApi 实例 ── 供外部模块访问网络接口
     *
     * @return HeroApi 实例
     */
    public HeroApi getHeroApi() {
        return heroApi;
    }

    /**
     * 获取 HeroDao 实例 ── 供外部模块访问数据库
     *
     * @return HeroDao 实例
     */
    public HeroDao getHeroDao() {
        return heroDao;
    }

    /**
     * 设置离线模式状态
     *
     * 由 ViewModel 调用，通知 Repository 当前网络状态。
     * 离线模式下所有 getHeroes/getHeroDetail 调用跳过网络请求。
     *
     * @param offline true 表示离线，false 表示在线
     */
    public void setOffline(boolean offline) {
        this.isOffline = offline;
    }

    /**
     * 获取离线模式状态
     *
     * @return true 表示当前处于离线模式
     */
    public boolean isOffline() {
        return isOffline;
    }

    /**
     * 获取英雄列表 ── 离线优先策略的核心实现
     *
     * ══════════════════════════════════════════════════════════════
     * 调用链：
     *   HeroListViewModel.loadHeroes()
     *     → heroRepository.getHeroes(page, size, keyword, tier, sortBy)
     *     → 返回 LiveData<List<HeroUiModel>>
     *     → ViewModel observeForever() 接收数据
     * ══════════════════════════════════════════════════════════════
     *
     * 执行流程（在线模式）：
     *
     *   时间线 ─────────────────────────────────────────────────→
     *
     *   [子线程] 读取 Room 缓存
     *      │
     *      ├─ 有缓存 → postValue(缓存数据)  ←── 用户立即看到内容
     *      │
     *   [Retrofit线程] 发起网络请求
     *      │
     *      ├─ 成功 → setValue(最新数据)      ←── 用户看到更新后的内容
     *      │        + new Thread 更新 Room 缓存
     *      │
     *      ├─ 业务失败 → 保持缓存数据不变
     *      │
     *      └─ 网络异常 → 保持缓存数据不变
     *
     * 执行流程（离线模式）：
     *
     *   [子线程] 读取 Room 缓存
     *      │
     *      ├─ 有缓存 → postValue(缓存数据)
     *      └─ 无缓存 → postValue(空列表)
     *      │
     *      └─ 跳过网络请求（isOffline=true）
     *
     * 注意事项：
     * - postValue() 从子线程更新，会被合并到主线程
     * - setValue() 从 Retrofit 回调线程更新（Retrofit 回调默认在主线程）
     * - 如果缓存和网络都返回数据，ViewModel 会收到两次通知
     *
     * @param page    页码（从0开始）
     * @param size    每页数量
     * @param keyword 搜索关键词（null或空字符串表示不筛选）
     * @param tier    梯级筛选（S+/S/A/B/C，null或空字符串表示不筛选）
     * @param sortBy  排序规则（name/winRate/pickRate）
     * @return LiveData<List<HeroUiModel>> 可观察的英雄UI模型列表
     */
    public LiveData<List<HeroUiModel>> getHeroes(int page, int size, String keyword, String tier, String sortBy) {
        MutableLiveData<List<HeroUiModel>> result = new MutableLiveData<>();

        // 步骤1：离线优先 - 先从 Room 缓存读取
        // 为什么用 new Thread？因为 Room 不允许在主线程做数据库操作（防止ANR）
        new Thread(() -> {
            LiveData<List<HeroEntity>> cached = heroDao.getAllHeroes();
            List<HeroEntity> cachedList = cached.getValue();
            if (cachedList != null && !cachedList.isEmpty()) {
                // 有缓存：转换为 UiModel 并通过 postValue 返回
                // postValue 会在主线程分发值，ViewModel 自动收到通知
                List<HeroUiModel> cachedUiModels = cachedList.stream()
                        .map(this::convertEntityToUiModel)
                        .collect(Collectors.toList());
                result.postValue(cachedUiModels);
            }
        }).start();

        // 步骤2：如果离线模式，跳过网络请求
        // 此时 result 可能已经有缓存数据，也可能还是空的
        if (isOffline) {
            return result;
        }

        // 步骤3：发起网络请求获取最新数据
        // enqueue() 是异步调用，不会阻塞当前线程
        heroApi.getHeroes(keyword, tier, sortBy, page, size).enqueue(new Callback<Result<PageResponse<HeroResponse>>>() {
            @Override
            public void onResponse(Call<Result<PageResponse<HeroResponse>>> call, Response<Result<PageResponse<HeroResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // HTTP 200 + 业务成功
                    PageResponse<HeroResponse> pageData = response.body().getData();
                    if (pageData != null && pageData.getRecords() != null) {
                        // 将网络 DTO 转换为 UI 模型并返回
                        List<HeroUiModel> uiModels = pageData.getRecords().stream()
                                .map(HeroRepository.this::convertToUiModel)
                                .collect(Collectors.toList());
                        result.setValue(uiModels);

                        // 异步更新本地缓存（不阻塞当前操作）
                        new Thread(() -> {
                            List<HeroEntity> entities = pageData.getRecords().stream()
                                    .map(HeroRepository.this::convertToEntity)
                                    .collect(Collectors.toList());
                            heroDao.insertAll(entities);
                        }).start();
                    } else {
                        // 分页数据为空：返回空列表
                        result.setValue(Collections.emptyList());
                    }
                } else {
                    // 业务失败（HTTP 4xx/5xx 或 body.isSuccess()=false）
                    // 保持缓存数据不变（步骤1已返回缓存）
                    ensureNonEmptyResult(result);
                }
            }

            @Override
            public void onFailure(Call<Result<PageResponse<HeroResponse>>> call, Throwable t) {
                // 网络异常（超时、DNS解析失败、连接被拒等）
                // 保持缓存数据不变（步骤1已返回缓存）
                ensureNonEmptyResult(result);
            }
        });

        return result;
    }

    /**
     * 获取英雄详情 ── 离线优先策略的详情版
     *
     * ══════════════════════════════════════════════════════════════
     * 调用链：
     *   HeroDetailViewModel.loadHeroDetail(heroId)
     *     → heroRepository.getHeroDetail(heroId)
     *     → 返回 LiveData<HeroDetailUiModel>
     *     → ViewModel observeForever() 接收数据
     * ══════════════════════════════════════════════════════════════
     *
     * 与 getHeroes() 的区别：
     * - getHeroes() 返回列表摘要数据（名称、胜率、头像等）
     * - getHeroDetail() 返回完整详情数据（技能、出装、克制等）
     * - 列表数据从 HeroResponse 转换，详情数据从 HeroDetailResponse 转换
     *
     * 详情数据包含的字段更多：
     * - 技能列表（被动/Q/W/E/R 5个技能）
     * - 克制提示（对抗该英雄的建议）
     * - 协同推荐（与该英雄配合好的英雄）
     * - 推荐出装（装备建议）
     * - 推荐符文（符文搭配建议）
     * - 版本陷阱标记（是否为当前版本陷阱英雄）
     * - KDA 数据（场均击杀/死亡/助攻）
     *
     * @param heroId 英雄ID（数据库主键）
     * @return LiveData<HeroDetailUiModel> 可观察的英雄详情UI模型
     */
    public LiveData<HeroDetailUiModel> getHeroDetail(long heroId) {
        MutableLiveData<HeroDetailUiModel> result = new MutableLiveData<>();

        // 步骤1：离线优先 - 先从 Room 缓存读取详情
        new Thread(() -> {
            LiveData<HeroEntity> cached = heroDao.getHeroById(heroId);
            HeroEntity cachedEntity = cached.getValue();
            if (cachedEntity != null) {
                // 有缓存：转换为详情 UiModel 并返回
                result.postValue(convertEntityToDetailUiModel(cachedEntity));
            }
        }).start();

        // 步骤2：如果离线模式，跳过网络请求
        if (isOffline) {
            return result;
        }

        // 步骤3：发起网络请求获取最新详情
        heroApi.getHeroDetail(heroId).enqueue(new Callback<Result<HeroDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<HeroDetailResponse>> call, Response<Result<HeroDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    HeroDetailResponse detail = response.body().getData();
                    if (detail != null) {
                        // 将网络 DTO 转换为详情 UI 模型并返回
                        result.setValue(convertToDetailUiModel(detail));

                        // 异步更新本地缓存
                        new Thread(() -> {
                            HeroEntity entity = convertDetailToEntity(detail);
                            List<HeroEntity> list = new ArrayList<>();
                            list.add(entity);
                            heroDao.insertAll(list);
                        }).start();
                    }
                } else {
                    // 业务失败：保持缓存数据不变
                }
            }

            @Override
            public void onFailure(Call<Result<HeroDetailResponse>> call, Throwable t) {
                // 网络异常：保持缓存数据不变
            }
        });

        return result;
    }

    /**
     * 确保结果非空 ── 网络失败且缓存也为空时的兜底处理
     *
     * 为什么需要这个方法？
     * - 如果缓存为空且网络也失败，result 的值还是 null
     * - null 值会导致 ViewModel 中的 observeForever 回调不触发
     * - 设置空列表可以让 ViewModel 知道"确实没有数据"（而不是"还没加载完"）
     *
     * @param result 可观察的 MutableLiveData
     */
    private void ensureNonEmptyResult(MutableLiveData<List<HeroUiModel>> result) {
        if (result.getValue() == null) {
            result.postValue(Collections.emptyList());
        }
    }

    /**
     * 网络响应 → 列表UI模型转换
     *
     * 将服务器返回的 HeroResponse（列表项 DTO）转换为 UI 层使用的 HeroUiModel。
     *
     * 转换规则：
     * - 字段一一对应
     * - null 的数值字段转为 0.0（避免 UI 层空指针）
     * - imageUrl 以 "/" 开头时拼接 BASE_URL（后端返回相对路径）
     * - tier 字符串转为 Tier 枚举
     *
     * imageUrl 拼接示例：
     * - 后端返回 "/images/hero/teemo.png"
     * - 拼接后 "http://10.0.2.2:8080/images/hero/teemo.png"
     * - 如果后端返回完整 URL 则不拼接
     *
     * @param response 服务器返回的英雄列表项数据
     * @return HeroUiModel UI层使用的英雄展示模型
     */
    private HeroUiModel convertToUiModel(HeroResponse response) {
        String imageUrl = response.getImageUrl();
        if (imageUrl != null && imageUrl.startsWith("/")) {
            imageUrl = Constants.BASE_URL + imageUrl.substring(1);
        }
        return new HeroUiModel(
                response.getId(),
                response.getNameZh(),
                response.getNameEn(),
                response.getTitle(),
                response.getRole(),
                parseTier(response.getTier()),
                response.getWinRate() != null ? response.getWinRate().doubleValue() : 0.0,
                response.getPickRate() != null ? response.getPickRate().doubleValue() : 0.0,
                imageUrl
        );
    }

    /**
     * 网络响应 → 数据库实体转换
     *
     * 将服务器返回的 HeroResponse 转换为 Room 存储的 HeroEntity。
     * 用于网络请求成功后更新本地缓存。
     *
     * 与 convertToUiModel() 的区别：
     * - convertToUiModel() 转换给 UI 层用（Tier 枚举、完整 URL）
     * - convertToEntity() 转换给数据库用（原始字符串、相对路径）
     *
     * @param response 服务器返回的英雄列表项数据
     * @return HeroEntity Room 数据库存储的英雄数据实体
     */
    private HeroEntity convertToEntity(HeroResponse response) {
        HeroEntity entity = new HeroEntity();
        entity.id = response.getId();
        entity.nameZh = response.getNameZh();
        entity.nameEn = response.getNameEn();
        entity.title = response.getTitle();
        entity.role = response.getRole();
        entity.tier = response.getTier();
        entity.winRate = response.getWinRate() != null ? response.getWinRate().doubleValue() : 0.0;
        entity.pickRate = response.getPickRate() != null ? response.getPickRate().doubleValue() : 0.0;
        entity.avatarUrl = response.getImageUrl();
        entity.isTrap = false;
        entity.updatedAt = System.currentTimeMillis();
        return entity;
    }

    /**
     * 详情响应 → 数据库实体转换
     *
     * 将服务器返回的 HeroDetailResponse 转换为 Room 存储的 HeroEntity。
     * 比列表版转换更复杂，因为详情数据包含更多字段。
     *
     * 额外处理的字段：
     * - description：英雄描述
     * - avgKills/avgDeaths/avgAssists：KDA 数据
     * - recommendedBuild：推荐出装
     * - recommendedAugmentIds：推荐符文ID列表
     * - recommendedAugments：推荐符文详情列表（含名称、品质、图标）
     * - counterTips：克制提示
     * - synergies：协同推荐
     * - isVersionTrap：版本陷阱标记
     * - skills：技能列表（P/Q/W/E/R）
     *
     * 嵌套对象转换：
     * - 推荐符文：HeroDetailResponse.AugmentBrief → HeroEntity.AugmentBriefData
     * - 技能列表：HeroDetailResponse.SkillDto → HeroEntity.SkillData
     *
     * @param detail 服务器返回的英雄详情数据
     * @return HeroEntity Room 数据库存储的英雄数据实体
     */
    private HeroEntity convertDetailToEntity(HeroDetailResponse detail) {
        HeroEntity entity = new HeroEntity();
        entity.id = detail.getId();
        entity.nameZh = detail.getNameZh();
        entity.nameEn = detail.getNameEn();
        entity.title = detail.getTitle();
        entity.role = detail.getRole();
        entity.tier = detail.getTier();
        entity.winRate = detail.getWinRate() != null ? detail.getWinRate().doubleValue() : 0.0;
        entity.pickRate = detail.getPickRate() != null ? detail.getPickRate().doubleValue() : 0.0;
        entity.avatarUrl = detail.getImageUrl();
        entity.description = detail.getDescription();
        entity.avgKills = detail.getAvgKills() != null ? detail.getAvgKills().doubleValue() : 0.0;
        entity.avgDeaths = detail.getAvgDeaths() != null ? detail.getAvgDeaths().doubleValue() : 0.0;
        entity.avgAssists = detail.getAvgAssists() != null ? detail.getAvgAssists().doubleValue() : 0.0;
        entity.recommendedBuild = detail.getRecommendedBuild();
        entity.recommendedAugmentIds = detail.getRecommendedAugmentIds();

        // 推荐符文详情转换：DTO → Entity 嵌套对象
        if (detail.getRecommendedAugments() != null) {
            entity.recommendedAugments = detail.getRecommendedAugments().stream().map(augment -> {
                HeroEntity.AugmentBriefData data = new HeroEntity.AugmentBriefData();
                data.id = augment.getId();
                data.nameZh = augment.getNameZh();
                data.quality = augment.getQuality();
                data.iconUrl = augment.getIconUrl();
                return data;
            }).collect(Collectors.toList());
        }
        entity.counterTips = detail.getCounterTips();
        entity.synergies = detail.getSynergies();
        entity.isTrap = false;
        entity.isVersionTrap = detail.getIsVersionTrap() != null && detail.getIsVersionTrap();
        entity.updatedAt = System.currentTimeMillis();

        // 技能列表转换：DTO → Entity SkillData
        if (detail.getSkills() != null) {
            entity.skills = detail.getSkills().stream().map(skill -> {
                HeroEntity.SkillData data = new HeroEntity.SkillData();
                data.key = skill.getKey();
                data.name = skill.getName();
                data.description = skill.getDescription();
                return data;
            }).collect(Collectors.toList());
        }

        return entity;
    }

    /**
     * 缓存实体 → 列表UI模型转换
     *
     * 将 Room 数据库的 HeroEntity 转换为 UI 层使用的 HeroUiModel。
     * 用于离线模式下从缓存读取数据展示。
     *
     * 与 convertToUiModel(HeroResponse) 的区别：
     * - 数据源不同：Entity 来自数据库，Response 来自网络
     * - 字段名不同：Entity 用 avatarUrl，Response 用 imageUrl
     * - 转换逻辑相同：都需要 URL 拼接和 Tier 解析
     *
     * @param entity Room 数据库存储的英雄数据实体
     * @return HeroUiModel UI层使用的英雄展示模型
     */
    private HeroUiModel convertEntityToUiModel(HeroEntity entity) {
        String imageUrl = entity.avatarUrl;
        if (imageUrl != null && imageUrl.startsWith("/")) {
            imageUrl = Constants.BASE_URL + imageUrl.substring(1);
        }
        return new HeroUiModel(
                entity.id,
                entity.nameZh,
                entity.nameEn,
                entity.title,
                entity.role,
                parseTier(entity.tier),
                entity.winRate,
                entity.pickRate,
                imageUrl
        );
    }

    /**
     * 缓存实体 → 详情UI模型转换
     *
     * 将 Room 数据库的 HeroEntity 转换为 UI 层使用的 HeroDetailUiModel。
     * 用于离线模式下从缓存读取英雄详情展示。
     *
     * 推荐符文的降级处理：
     * - 优先使用 recommendedAugments（含名称、品质、图标的完整数据）
     * - 如果没有完整数据，降级使用 recommendedAugmentIds（只有 ID）
     * - 降级时显示 "符文 #ID" 作为占位名称
     *
     * 技能列表转换：
     * - HeroEntity.SkillData → HeroDetailUiModel.SkillUiModel
     * - 包含 key（P/Q/W/E/R）、name（技能名）、description（描述）
     *
     * @param entity Room 数据库存储的英雄数据实体
     * @return HeroDetailUiModel UI层使用的英雄详情展示模型
     */
    private HeroDetailUiModel convertEntityToDetailUiModel(HeroEntity entity) {
        List<HeroDetailUiModel.SkillUiModel> skills = null;
        if (entity.skills != null) {
            skills = entity.skills.stream().map(skill ->
                    new HeroDetailUiModel.SkillUiModel(skill.key, skill.name, skill.description)
            ).collect(Collectors.toList());
        }

        // 推荐符文转换（含降级处理）
        List<HeroDetailUiModel.AugmentBriefUiModel> augments = null;
        if (entity.recommendedAugments != null && !entity.recommendedAugments.isEmpty()) {
            // 有完整符文数据：直接转换
            augments = entity.recommendedAugments.stream()
                    .map(a -> new HeroDetailUiModel.AugmentBriefUiModel(a.id, a.nameZh, a.quality, a.iconUrl))
                    .collect(Collectors.toList());
        } else if (entity.recommendedAugmentIds != null && !entity.recommendedAugmentIds.isEmpty()) {
            // 只有 ID 列表：降级显示
            augments = entity.recommendedAugmentIds.stream()
                    .map(id -> new HeroDetailUiModel.AugmentBriefUiModel(id, "符文 #" + id, null, null))
                    .collect(Collectors.toList());
        }

        return new HeroDetailUiModel(
                entity.id,
                entity.nameZh,
                entity.nameEn,
                entity.title,
                entity.role,
                parseTier(entity.tier),
                entity.winRate,
                entity.pickRate,
                entity.description,
                skills,
                entity.counterTips,
                entity.synergies,
                entity.avgKills,
                entity.avgDeaths,
                entity.avgAssists,
                entity.recommendedBuild,
                entity.recommendedAugmentIds,
                augments,
                entity.avatarUrl,
                entity.isVersionTrap
        );
    }

    /**
     * 详情响应 → 详情UI模型转换
     *
     * 将服务器返回的 HeroDetailResponse 直接转换为 UI 层使用的 HeroDetailUiModel。
     * 用于在线模式下网络请求成功后直接展示（不经过数据库中转）。
     *
     * 与 convertEntityToDetailUiModel() 的区别：
     * - 数据源不同：HeroDetailResponse 来自网络，HeroEntity 来自数据库
     * - null 处理不同：网络 DTO 的数值字段可能为 null，需要 .doubleValue() 转换
     * - 数据库实体的数值字段是基本类型 double，不需要 null 检查
     *
     * 推荐符文同样有降级处理逻辑（与 Entity 版本一致）。
     *
     * @param detail 服务器返回的英雄详情数据
     * @return HeroDetailUiModel UI层使用的英雄详情展示模型
     */
    private HeroDetailUiModel convertToDetailUiModel(HeroDetailResponse detail) {
        List<HeroDetailUiModel.SkillUiModel> skills = null;
        if (detail.getSkills() != null) {
            skills = detail.getSkills().stream()
                    .map(skill -> new HeroDetailUiModel.SkillUiModel(skill.getKey(), skill.getName(), skill.getDescription()))
                    .collect(Collectors.toList());
        }

        // 推荐符文转换（含降级处理）
        List<HeroDetailUiModel.AugmentBriefUiModel> augments = null;
        if (detail.getRecommendedAugments() != null && !detail.getRecommendedAugments().isEmpty()) {
            augments = detail.getRecommendedAugments().stream()
                    .map(a -> new HeroDetailUiModel.AugmentBriefUiModel(a.getId(), a.getNameZh(), a.getQuality(), a.getIconUrl()))
                    .collect(Collectors.toList());
        } else if (detail.getRecommendedAugmentIds() != null && !detail.getRecommendedAugmentIds().isEmpty()) {
            augments = detail.getRecommendedAugmentIds().stream()
                    .map(id -> new HeroDetailUiModel.AugmentBriefUiModel(id, "符文 #" + id, null, null))
                    .collect(Collectors.toList());
        }

        return new HeroDetailUiModel(
                detail.getId(),
                detail.getNameZh(),
                detail.getNameEn(),
                detail.getTitle(),
                detail.getRole(),
                parseTier(detail.getTier()),
                detail.getWinRate() != null ? detail.getWinRate().doubleValue() : 0.0,
                detail.getPickRate() != null ? detail.getPickRate().doubleValue() : 0.0,
                detail.getDescription(),
                skills,
                detail.getCounterTips(),
                detail.getSynergies(),
                detail.getAvgKills() != null ? detail.getAvgKills().doubleValue() : 0.0,
                detail.getAvgDeaths() != null ? detail.getAvgDeaths().doubleValue() : 0.0,
                detail.getAvgAssists() != null ? detail.getAvgAssists().doubleValue() : 0.0,
                detail.getRecommendedBuild(),
                detail.getRecommendedAugmentIds(),
                augments,
                detail.getImageUrl(),
                detail.getIsVersionTrap() != null && detail.getIsVersionTrap()
        );
    }

    /**
     * 梯级字符串 → 梯级枚举转换
     *
     * 后端返回的梯级是字符串格式（如 "S+"），前端使用 Tier 枚举。
     * 此方法做格式转换，null 或无法识别的值默认返回 Tier.C。
     *
     * 梯级对照表：
     * - "S+" → Tier.S_PLUS（最强梯队）
     * - "S"  → Tier.S（强梯队）
     * - "A"  → Tier.A（中上梯队）
     * - "B"  → Tier.B（中等梯队）
     * - "C"  → Tier.C（较弱梯队）
     * - null/其他 → Tier.C（默认值）
     *
     * @param tierStr 后端返回的梯级字符串
     * @return Tier 梯级枚举，默认值 Tier.C
     */
    private Tier parseTier(String tierStr) {
        if (tierStr == null) return Tier.C;
        switch (tierStr) {
            case "S+": return Tier.S_PLUS;
            case "S": return Tier.S;
            case "A": return Tier.A;
            case "B": return Tier.B;
            case "C": return Tier.C;
            default: return Tier.C;
        }
    }
}
