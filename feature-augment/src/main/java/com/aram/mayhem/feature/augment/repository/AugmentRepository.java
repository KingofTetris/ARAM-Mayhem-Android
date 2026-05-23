package com.aram.mayhem.feature.augment.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.common.Result;
import com.aram.mayhem.data.local.dao.AugmentDao;
import com.aram.mayhem.data.local.entity.AugmentEntity;
import com.aram.mayhem.network.api.AugmentApi;
import com.aram.mayhem.network.dto.AugmentDetailResponse;
import com.aram.mayhem.network.dto.AugmentRecommendRequest;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.aram.mayhem.network.dto.AugmentResponse;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.network.dto.SynergyProgressResponse;
import com.aram.mayhem.ui.model.AugmentUiModel;

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
 * 符文数据仓库 ── 符文模块的统一数据访问层
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、Repository 模式是什么？
 * ═══════════════════════════════════════════════════════════════════
 *
 * Repository 是 MVVM 架构中的"数据管家"，它：
 * 1. 屏蔽了数据来源的复杂性（网络？本地？内存？）
 * 2. 对 ViewModel 只暴露简单的 LiveData 接口
 * 3. 决定什么时候用网络数据，什么时候用缓存数据
 *
 *   ViewModel 不需要知道数据从哪里来：
 *
 *   ViewModel  →  Repository  →  网络API（远程数据）
 *                          ↘  Room DAO（本地缓存）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据源策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本仓库采用"网络优先"策略：
 *
 *   ┌─────────────────────────────────────────────────────┐
 *   │  网络请求成功？                                      │
 *   │    ├─ 是 → 返回网络数据 + 异步写入本地缓存            │
 *   │    └─ 否 → 回退到本地缓存                            │
 *   │           ├─ 缓存有数据 → 返回缓存数据               │
 *   │           └─ 缓存无数据 → 返回空列表/null            │
 *   └─────────────────────────────────────────────────────┘
 *
 * 特殊情况：
 * - getSynergyProgress() 和 getRecommendations() 没有本地缓存
 *   因为套装进度和推荐结果是动态计算的，不适合缓存
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据转换流程
 * ═══════════════════════════════════════════════════════════════════
 *
 *   网络层 DTO              仓库层 Entity              UI层 Model
 *   ┌──────────────┐      ┌──────────────┐          ┌──────────────┐
 *   │AugmentResponse│ ──→  │AugmentEntity │          │AugmentUiModel│
 *   │(Retrofit 反序列化)    │(Room 存储)    │ ──→     │(UI 展示)      │
 *   └──────────────┘      └──────────────┘          └──────────────┘
 *
 *   转换方法：
 *   - convertToUiModel()：列表 DTO → UI Model
 *   - convertDetailToUiModel()：详情 DTO → UI Model
 *   - convertToEntity()：列表 DTO → Entity（缓存写入）
 *   - convertDetailToEntity()：详情 DTO → Entity（缓存写入）
 *   - convertEntityToUiModel()：Entity → UI Model（缓存读取）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、与 HeroRepository 的对比
 * ═══════════════════════════════════════════════════════════════════
 *
 * 两者结构几乎相同，差异点：
 * - HeroRepository：搜索 + 梯级筛选
 * - AugmentRepository：品质筛选 + 套装筛选 + 推荐查询
 * - 符文多了套装进度和推荐两个无缓存接口
 *
 * @see AugmentApi
 * @see AugmentDao
 * @see AugmentEntity
 * @see AugmentUiModel
 */
@Singleton
public class AugmentRepository {

    /**
     * 符文网络接口 ── Retrofit 生成的 API 实现
     *
     * 提供以下网络请求方法：
     * - getAugments()：分页获取符文列表
     * - getAugmentDetail()：获取符文详情
     * - getSynergyProgress()：获取套装进度
     * - getRecommendations()：获取推荐结果
     */
    private final AugmentApi augmentApi;

    /**
     * 符文本地数据访问对象 ── Room 生成的 DAO 实现
     *
     * 提供以下本地数据库操作：
     * - insertAll()：批量插入符文（缓存写入）
     * - getAllAugmentsSync()：同步获取所有符文（缓存回退）
     * - getAugmentByIdSync()：同步获取指定符文（缓存回退）
     */
    private final AugmentDao augmentDao;

    /**
     * 构造函数 ── Hilt 自动注入依赖
     *
     * @param augmentApi  符文网络接口（Hilt 自动注入单例）
     * @param augmentDao  符文本地数据访问对象（Hilt 自动注入单例）
     */
    @Inject
    public AugmentRepository(AugmentApi augmentApi, AugmentDao augmentDao) {
        this.augmentApi = augmentApi;
        this.augmentDao = augmentDao;
    }

    /**
     * 获取 DAO 对象 ── 供外部模块访问本地数据库
     *
     * @return AugmentDao 实例
     */
    public AugmentDao getAugmentDao() {
        return augmentDao;
    }

    /**
     * 获取符文列表 ── 分页查询，支持品质和套装筛选
     *
     * 由 AugmentViewModel.loadAugments() 调用。
     *
     * 执行流程：
     * 1. 创建 MutableLiveData 作为结果容器
     * 2. 调用 augmentApi.getAugments() 发起异步网络请求
     * 3. 网络成功：
     *    a. 将 AugmentResponse 列表转换为 AugmentUiModel 列表
     *    b. 设置 result 值，通知 ViewModel
     *    c. 异步写入本地缓存（新线程，不阻塞 UI）
     * 4. 网络失败/业务失败：
     *    a. 回退到本地缓存 loadFromCache()
     *
     * @param page       页码（从1开始）
     * @param size       每页数量（固定20）
     * @param quality    品质筛选（"PRISMATIC"/"GOLD"/"SILVER"/""表示全部）
     * @param synergySet 套装筛选（""表示不筛选）
     * @return LiveData<List<AugmentUiModel>> 可观察的符文UI模型列表
     */
    public LiveData<List<AugmentUiModel>> getAugments(int page, int size, String quality, String synergySet) {
        MutableLiveData<List<AugmentUiModel>> result = new MutableLiveData<>();

        augmentApi.getAugments(quality, synergySet, page, size).enqueue(new Callback<Result<PageResponse<AugmentResponse>>>() {
            @Override
            public void onResponse(Call<Result<PageResponse<AugmentResponse>>> call, Response<Result<PageResponse<AugmentResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<AugmentResponse> pageData = response.body().getData();
                    if (pageData != null && pageData.getRecords() != null) {
                        List<AugmentUiModel> uiModels = pageData.getRecords().stream()
                                .map(AugmentRepository.this::convertToUiModel)
                                .collect(Collectors.toList());
                        result.setValue(uiModels);

                        new Thread(() -> {
                            List<AugmentEntity> entities = pageData.getRecords().stream()
                                    .map(AugmentRepository.this::convertToEntity)
                                    .collect(Collectors.toList());
                            augmentDao.insertAll(entities);
                        }).start();
                    } else {
                        result.setValue(Collections.emptyList());
                    }
                } else {
                    loadFromCache(result);
                }
            }

            @Override
            public void onFailure(Call<Result<PageResponse<AugmentResponse>>> call, Throwable t) {
                loadFromCache(result);
            }
        });

        return result;
    }

    /**
     * 获取符文详情 ── 根据 ID 获取完整符文信息
     *
     * 由 AugmentDetailViewModel.loadAugmentDetail() 调用。
     *
     * 与 getAugments() 的区别：
     * - 列表接口返回简略信息（无描述、无第二/三套装）
     * - 详情接口返回完整信息（有描述、有全部套装、有陷阱标记）
     *
     * @param augmentId 符文唯一标识符
     * @return LiveData<AugmentUiModel> 可观察的符文详情UI模型
     */
    public LiveData<AugmentUiModel> getAugmentDetail(long augmentId) {
        MutableLiveData<AugmentUiModel> result = new MutableLiveData<>();

        augmentApi.getAugmentDetail(augmentId).enqueue(new Callback<Result<AugmentDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<AugmentDetailResponse>> call, Response<Result<AugmentDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AugmentDetailResponse detail = response.body().getData();
                    if (detail != null) {
                        AugmentUiModel uiModel = convertDetailToUiModel(detail);
                        result.setValue(uiModel);

                        new Thread(() -> {
                            AugmentEntity entity = convertDetailToEntity(detail);
                            List<AugmentEntity> list = new ArrayList<>();
                            list.add(entity);
                            augmentDao.insertAll(list);
                        }).start();
                    }
                } else {
                    loadDetailFromCache(augmentId, result);
                }
            }

            @Override
            public void onFailure(Call<Result<AugmentDetailResponse>> call, Throwable t) {
                loadDetailFromCache(augmentId, result);
            }
        });

        return result;
    }

    /**
     * 获取套装进度 ── 查询已选符文触发的套装收集进度
     *
     * 由 SynergyProgressViewModel 和 AugmentRecommendViewModel 调用。
     *
     * 注意：此接口没有本地缓存！
     * 原因：套装进度是动态计算的，依赖当前已选符文组合，
     * 缓存意义不大（每次选择变化都需要重新计算）。
     *
     * @param augmentIds 已选符文 ID 列表
     * @return LiveData<List<SynergyProgressResponse>> 可观察的套装进度列表
     */
    public LiveData<List<SynergyProgressResponse>> getSynergyProgress(List<Long> augmentIds) {
        MutableLiveData<List<SynergyProgressResponse>> result = new MutableLiveData<>();

        String idsParam = augmentIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        augmentApi.getSynergyProgress(idsParam).enqueue(new Callback<Result<List<SynergyProgressResponse>>>() {
            @Override
            public void onResponse(Call<Result<List<SynergyProgressResponse>>> call, Response<Result<List<SynergyProgressResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SynergyProgressResponse> data = response.body().getData();
                    result.setValue(data != null ? data : Collections.emptyList());
                } else {
                    result.setValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<Result<List<SynergyProgressResponse>>> call, Throwable t) {
                result.setValue(Collections.emptyList());
            }
        });

        return result;
    }

    /**
     * 获取符文推荐 ── 基于英雄和已选符文的智能推荐
     *
     * 由 AugmentRecommendViewModel.refreshData() 调用。
     *
     * 推荐算法考虑因素：
     * 1. 英雄类型（法师/战士/刺客等）→ 推荐匹配的符文
     * 2. 已选符文的套装进度 → 推荐能完成套装的符文
     * 3. 符文胜率 → 推荐高胜率符文
     *
     * 注意：此接口也没有本地缓存，原因同 getSynergyProgress()。
     *
     * @param heroId              英雄 ID
     * @param selectedAugmentIds  当前已选的符文 ID 列表
     * @return LiveData<List<AugmentRecommendResponse>> 可观察的推荐结果列表
     */
    public LiveData<List<AugmentRecommendResponse>> getRecommendations(long heroId, List<Long> selectedAugmentIds) {
        MutableLiveData<List<AugmentRecommendResponse>> result = new MutableLiveData<>();

        AugmentRecommendRequest request = new AugmentRecommendRequest(heroId, selectedAugmentIds);

        augmentApi.getRecommendations(request).enqueue(new Callback<Result<List<AugmentRecommendResponse>>>() {
            @Override
            public void onResponse(Call<Result<List<AugmentRecommendResponse>>> call, Response<Result<List<AugmentRecommendResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<AugmentRecommendResponse> data = response.body().getData();
                    result.setValue(data != null ? data : Collections.emptyList());
                } else {
                    result.setValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<Result<List<AugmentRecommendResponse>>> call, Throwable t) {
                result.setValue(Collections.emptyList());
            }
        });

        return result;
    }

    /**
     * 从本地缓存加载符文列表 ── 网络失败时的回退方案
     *
     * 使用场景：
     * - 网络不可达（onFailure）
     * - 服务器返回业务错误（isSuccess=false）
     *
     * 执行流程：
     * 1. 在子线程查询 Room 数据库
     * 2. 将 AugmentEntity 转换为 AugmentUiModel
     * 3. 使用 postValue() 切换到主线程设置数据
     *
     * 注意：必须使用 postValue() 而不是 setValue()，
     * 因为 Room 查询在子线程执行，setValue() 只能在主线程调用。
     *
     * @param result 可观察的 MutableLiveData，用于接收缓存数据
     */
    private void loadFromCache(MutableLiveData<List<AugmentUiModel>> result) {
        new Thread(() -> {
            List<AugmentEntity> cached = augmentDao.getAllAugmentsSync();
            if (cached != null && !cached.isEmpty()) {
                List<AugmentUiModel> uiModels = cached.stream()
                        .map(this::convertEntityToUiModel)
                        .collect(Collectors.toList());
                result.postValue(uiModels);
            } else {
                result.postValue(Collections.emptyList());
            }
        }).start();
    }

    /**
     * 从本地缓存加载符文详情 ── 网络失败时的回退方案
     *
     * 与 loadFromCache() 的区别：
     * - loadFromCache()：加载所有符文（列表场景）
     * - loadDetailFromCache()：加载指定 ID 的符文（详情场景）
     *
     * @param augmentId 符文 ID
     * @param result    可观察的 MutableLiveData，用于接收缓存数据
     */
    private void loadDetailFromCache(long augmentId, MutableLiveData<AugmentUiModel> result) {
        new Thread(() -> {
            AugmentEntity cached = augmentDao.getAugmentByIdSync(augmentId);
            if (cached != null) {
                result.postValue(convertEntityToUiModel(cached));
            } else {
                result.postValue(null);
            }
        }).start();
    }

    /**
     * 列表 DTO → UI 模型转换
     *
     * AugmentResponse 是列表接口返回的简略数据，缺少以下字段：
     * - description（符文描述）
     * - synergySet2 / synergySet3（第二/三套装）
     * - isTrap（是否陷阱符文）
     *
     * 这些字段在列表页不需要展示，所以设为 null/false。
     *
     * iconUrl 处理：如果服务器返回相对路径（以 / 开头），
     * 需要拼接 BASE_URL 转为完整 URL。
     *
     * @param response 服务器返回的符文列表项数据
     * @return AugmentUiModel UI层使用的符文展示模型
     */
    private AugmentUiModel convertToUiModel(AugmentResponse response) {
        String iconUrl = response.getIconUrl();
        if (iconUrl != null && iconUrl.startsWith("/")) {
            iconUrl = Constants.BASE_URL + iconUrl.substring(1);
        }
        return new AugmentUiModel(
                response.getId() != null ? response.getId() : 0,
                response.getNameZh(),
                response.getNameEn(),
                null,
                response.getQuality(),
                response.getSynergySet(),
                null,
                null,
                iconUrl,
                response.getWinRate() != null ? response.getWinRate() : 0.0,
                response.getPickRate() != null ? response.getPickRate() : 0.0,
                response.getAvgPlacement() != null ? response.getAvgPlacement() : 0.0,
                response.getTier(),
                false
        );
    }

    /**
     * 详情 DTO → UI 模型转换
     *
     * AugmentDetailResponse 是详情接口返回的完整数据，包含所有字段。
     * 与 convertToUiModel() 的区别：description、synergySet2/3、isTrap 有值。
     *
     * @param detail 服务器返回的符文详情数据
     * @return AugmentUiModel UI层使用的符文展示模型
     */
    private AugmentUiModel convertDetailToUiModel(AugmentDetailResponse detail) {
        String iconUrl = detail.getIconUrl();
        if (iconUrl != null && iconUrl.startsWith("/")) {
            iconUrl = Constants.BASE_URL + iconUrl.substring(1);
        }
        return new AugmentUiModel(
                detail.getId() != null ? detail.getId() : 0,
                detail.getNameZh(),
                detail.getNameEn(),
                detail.getDescription(),
                detail.getQuality(),
                detail.getSynergySet(),
                detail.getSynergySet2(),
                detail.getSynergySet3(),
                iconUrl,
                detail.getWinRate() != null ? detail.getWinRate() : 0.0,
                detail.getPickRate() != null ? detail.getPickRate() : 0.0,
                detail.getAvgPlacement() != null ? detail.getAvgPlacement() : 0.0,
                detail.getTier(),
                detail.getIsTrap() != null ? detail.getIsTrap() : false
        );
    }

    /**
     * 列表 DTO → 数据库实体转换（缓存写入用）
     *
     * 将网络数据转换为 Room 数据库实体，用于异步写入本地缓存。
     * 列表 DTO 缺少 description、synergySet2/3、isTrap，
     * 这些字段在 Entity 中保持默认值（null/false）。
     *
     * updatedAt 记录缓存写入时间，可用于后续缓存过期判断。
     *
     * @param response 服务器返回的符文列表项数据
     * @return AugmentEntity Room 数据库存储的符文数据实体
     */
    private AugmentEntity convertToEntity(AugmentResponse response) {
        AugmentEntity entity = new AugmentEntity();
        entity.id = response.getId() != null ? response.getId() : 0;
        entity.nameZh = response.getNameZh();
        entity.nameEn = response.getNameEn();
        entity.quality = response.getQuality();
        entity.synergySet = response.getSynergySet();
        entity.iconUrl = response.getIconUrl();
        entity.winRate = response.getWinRate() != null ? response.getWinRate() : 0.0;
        entity.pickRate = response.getPickRate() != null ? response.getPickRate() : 0.0;
        entity.avgPlacement = response.getAvgPlacement() != null ? response.getAvgPlacement() : 0.0;
        entity.tier = response.getTier();
        entity.isTrap = false;
        entity.updatedAt = System.currentTimeMillis();
        return entity;
    }

    /**
     * 详情 DTO → 数据库实体转换（缓存写入用）
     *
     * 与 convertToEntity() 的区别：包含完整字段。
     * 详情数据写入缓存后，后续离线访问也能展示完整信息。
     *
     * @param detail 服务器返回的符文详情数据
     * @return AugmentEntity Room 数据库存储的符文数据实体
     */
    private AugmentEntity convertDetailToEntity(AugmentDetailResponse detail) {
        AugmentEntity entity = new AugmentEntity();
        entity.id = detail.getId() != null ? detail.getId() : 0;
        entity.nameZh = detail.getNameZh();
        entity.nameEn = detail.getNameEn();
        entity.description = detail.getDescription();
        entity.quality = detail.getQuality();
        entity.synergySet = detail.getSynergySet();
        entity.synergySet2 = detail.getSynergySet2();
        entity.synergySet3 = detail.getSynergySet3();
        entity.iconUrl = detail.getIconUrl();
        entity.winRate = detail.getWinRate() != null ? detail.getWinRate() : 0.0;
        entity.pickRate = detail.getPickRate() != null ? detail.getPickRate() : 0.0;
        entity.avgPlacement = detail.getAvgPlacement() != null ? detail.getAvgPlacement() : 0.0;
        entity.tier = detail.getTier();
        entity.isTrap = detail.getIsTrap() != null ? detail.getIsTrap() : false;
        entity.updatedAt = System.currentTimeMillis();
        return entity;
    }

    /**
     * 数据库实体 → UI 模型转换（缓存读取用）
     *
     * 从 Room 数据库读取缓存数据后，转换为 UI 层使用的模型。
     * Entity 的字段结构与 AugmentUiModel 几乎一一对应，
     * 直接映射即可，无需特殊处理。
     *
     * @param entity Room 数据库存储的符文数据实体
     * @return AugmentUiModel UI层使用的符文展示模型
     */
    private AugmentUiModel convertEntityToUiModel(AugmentEntity entity) {
        return new AugmentUiModel(
                entity.id,
                entity.nameZh,
                entity.nameEn,
                entity.description,
                entity.quality,
                entity.synergySet,
                entity.synergySet2,
                entity.synergySet3,
                entity.iconUrl,
                entity.winRate,
                entity.pickRate,
                entity.avgPlacement,
                entity.tier,
                entity.isTrap
        );
    }
}
