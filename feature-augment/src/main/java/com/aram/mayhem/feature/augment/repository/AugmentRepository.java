package com.aram.mayhem.feature.augment.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
 * 符文数据仓库
 *
 * 数据源策略：优先网络 → 失败时回退本地缓存
 * 功能：符文列表分页查询、符文详情查询、套装进度查询、推荐查询、本地缓存更新
 * 关联：AugmentApi, AugmentDao, AugmentEntity, AugmentResponse
 */
@Singleton
public class AugmentRepository {

    private final AugmentApi augmentApi;
    private final AugmentDao augmentDao;

    @Inject
    public AugmentRepository(AugmentApi augmentApi, AugmentDao augmentDao) {
        this.augmentApi = augmentApi;
        this.augmentDao = augmentDao;
    }

    public AugmentDao getAugmentDao() {
        return augmentDao;
    }

    /**
     * 获取符文列表（符文模块）
     *
     * 作用：从服务器分页获取强化符文列表，支持品质筛选、套装筛选
     * 实现：优先请求网络 → 成功写入本地缓存 → 失败时回退本地缓存
     *
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param quality 品质筛选（LEGENDARY/EPIC/RARE/null表示不筛选）
     * @param synergySet 套装筛选（null表示不筛选）
     * @return LiveData<List<AugmentUiModel>> 可观察的符文UI模型列表
     */
    public LiveData<List<AugmentUiModel>> getAugments(int page, int size, String quality, String synergySet) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<List<AugmentUiModel>> result = new MutableLiveData<>();

        // 调用 Retrofit 接口发起异步网络请求：获取符文列表
        augmentApi.getAugments(quality, synergySet, page, size).enqueue(new Callback<Result<PageResponse<AugmentResponse>>>() {
            @Override
            public void onResponse(Call<Result<PageResponse<AugmentResponse>>> call, Response<Result<PageResponse<AugmentResponse>>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<AugmentResponse> pageData = response.body().getData();
                    // 判断分页数据有效：数据不为空且记录列表不为空
                    if (pageData != null && pageData.getRecords() != null) {
                        // 网络数据 → UI模型转换
                        List<AugmentUiModel> uiModels = pageData.getRecords().stream()
                                .map(AugmentRepository.this::convertToUiModel)
                                .collect(Collectors.toList());
                        result.setValue(uiModels);

                        // 异步写入本地缓存
                        new Thread(() -> {
                            List<AugmentEntity> entities = pageData.getRecords().stream()
                                    .map(AugmentRepository.this::convertToEntity)
                                    .collect(Collectors.toList());
                            augmentDao.insertAll(entities);
                        }).start();
                    } else {
                        // 数据为空：设置空列表
                        result.setValue(Collections.emptyList());
                    }
                } else {
                    // 业务失败：回退到本地缓存
                    loadFromCache(result);
                }
            }

            @Override
            public void onFailure(Call<Result<PageResponse<AugmentResponse>>> call, Throwable t) {
                // 网络异常：回退到本地缓存
                loadFromCache(result);
            }
        });

        return result;
    }

    /**
     * 获取符文详情（符文模块）
     *
     * 作用：根据符文ID获取完整符文信息
     * 实现：优先请求网络 → 成功写入本地缓存 → 失败时回退本地缓存
     *
     * @param augmentId 符文ID
     * @return LiveData<AugmentUiModel> 可观察的符文详情UI模型
     */
    public LiveData<AugmentUiModel> getAugmentDetail(long augmentId) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<AugmentUiModel> result = new MutableLiveData<>();

        // 调用 Retrofit 接口发起异步网络请求：获取符文详情
        augmentApi.getAugmentDetail(augmentId).enqueue(new Callback<Result<AugmentDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<AugmentDetailResponse>> call, Response<Result<AugmentDetailResponse>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AugmentDetailResponse detail = response.body().getData();
                    // 判断详情数据有效
                    if (detail != null) {
                        // 网络数据 → UI模型转换并设置到 LiveData
                        AugmentUiModel uiModel = convertDetailToUiModel(detail);
                        result.setValue(uiModel);

                        // 异步写入本地缓存
                        new Thread(() -> {
                            AugmentEntity entity = convertDetailToEntity(detail);
                            List<AugmentEntity> list = new ArrayList<>();
                            list.add(entity);
                            augmentDao.insertAll(list);
                        }).start();
                    }
                } else {
                    // 业务失败：回退到本地缓存
                    loadDetailFromCache(augmentId, result);
                }
            }

            @Override
            public void onFailure(Call<Result<AugmentDetailResponse>> call, Throwable t) {
                // 网络异常：回退到本地缓存
                loadDetailFromCache(augmentId, result);
            }
        });

        return result;
    }

    /**
     * 获取套装进度（符文模块）
     *
     * 作用：根据已选符文ID列表获取各套装的收集进度
     * 实现：直接请求网络，无缓存回退
     *
     * @param augmentIds 已选符文ID列表
     * @return LiveData<List<SynergyProgressResponse>> 可观察的套装进度列表
     */
    public LiveData<List<SynergyProgressResponse>> getSynergyProgress(List<Long> augmentIds) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<List<SynergyProgressResponse>> result = new MutableLiveData<>();

        // 转换列表为逗号分隔的字符串参数
        String idsParam = augmentIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        // 调用 Retrofit 接口发起异步网络请求：获取套装进度
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
     * 获取符文推荐（符文模块）
     *
     * 作用：根据当前已选符文和英雄，获取智能推荐的符文搭配方案
     * 实现：直接请求网络，无缓存回退
     *
     * @param heroId 英雄ID
     * @param selectedAugmentIds 当前已选的符文ID列表
     * @return LiveData<List<AugmentRecommendResponse>> 可观察的推荐方案列表
     */
    public LiveData<List<AugmentRecommendResponse>> getRecommendations(long heroId, List<Long> selectedAugmentIds) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<List<AugmentRecommendResponse>> result = new MutableLiveData<>();

        // 构建推荐请求体，封装英雄ID和已选符文列表
        AugmentRecommendRequest request = new AugmentRecommendRequest(heroId, selectedAugmentIds);

        // 调用 Retrofit 接口发起异步网络请求：获取符文推荐
        augmentApi.getRecommendations(request).enqueue(new Callback<Result<List<AugmentRecommendResponse>>>() {
            @Override
            public void onResponse(Call<Result<List<AugmentRecommendResponse>>> call, Response<Result<List<AugmentRecommendResponse>>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<AugmentRecommendResponse> data = response.body().getData();
                    // 数据保护：确保列表不为null
                    result.setValue(data != null ? data : Collections.emptyList());
                } else {
                    // 业务失败：设置空列表
                    result.setValue(Collections.emptyList());
                }
            }

            @Override
            public void onFailure(Call<Result<List<AugmentRecommendResponse>>> call, Throwable t) {
                // 网络异常：设置空列表
                result.setValue(Collections.emptyList());
            }
        });

        return result;
    }

    /**
     * 从本地缓存加载符文列表（私有方法）
     *
     * 作用：当网络请求失败时，回退从 Room 数据库加载本地缓存的符文列表
     * 实现：子线程查询 Room → UI模型转换 → postValue 到 LiveData
     *
     * @param result 可观察的 MutableLiveData，用于接收缓存数据
     */
    private void loadFromCache(MutableLiveData<List<AugmentUiModel>> result) {
        // 子线程查询本地缓存
        new Thread(() -> {
            List<AugmentEntity> cached = augmentDao.getAllAugmentsSync();
            // 判断缓存有效：数据不为空
            if (cached != null && !cached.isEmpty()) {
                // 缓存数据 → UI模型转换
                List<AugmentUiModel> uiModels = cached.stream()
                        .map(this::convertEntityToUiModel)
                        .collect(Collectors.toList());
                // postValue：子线程切换到主线程设置数据
                result.postValue(uiModels);
            } else {
                // 缓存为空：设置空列表
                result.postValue(Collections.emptyList());
            }
        }).start();
    }

    /**
     * 从本地缓存加载符文详情（私有方法）
     *
     * 作用：当网络请求失败时，回退从 Room 数据库加载指定符文的详情
     * 实现：子线程查询 Room → UI模型转换 → postValue 到 LiveData
     *
     * @param augmentId 符文ID
     * @param result 可观察的 MutableLiveData，用于接收缓存数据
     */
    private void loadDetailFromCache(long augmentId, MutableLiveData<AugmentUiModel> result) {
        // 子线程查询本地缓存
        new Thread(() -> {
            AugmentEntity cached = augmentDao.getAugmentByIdSync(augmentId);
            // 判断缓存有效：数据不为空
            if (cached != null) {
                // 缓存数据 → UI模型转换
                result.postValue(convertEntityToUiModel(cached));
            } else {
                // 缓存为空：设置null
                result.postValue(null);
            }
        }).start();
    }

    /**
     * 网络响应 → UI模型转换（私有方法）
     *
     * 作用：将服务器返回的 AugmentResponse DTO 转换为 UI层使用的 AugmentUiModel
     * 转换规则：字段一一对应，null 值转为默认值
     *
     * @param response 服务器返回的符文列表项数据
     * @return AugmentUiModel UI层使用的符文展示模型
     */
    private AugmentUiModel convertToUiModel(AugmentResponse response) {
        return new AugmentUiModel(
                response.getId() != null ? response.getId() : 0,
                response.getNameZh(),
                response.getNameEn(),
                null,
                response.getQuality(),
                response.getSynergySet(),
                null,
                null,
                response.getIconUrl(),
                response.getWinRate() != null ? response.getWinRate() : 0.0,
                response.getPickRate() != null ? response.getPickRate() : 0.0,
                response.getAvgPlacement() != null ? response.getAvgPlacement() : 0.0,
                response.getTier(),
                false
        );
    }

    /**
     * 详情响应 → UI模型转换（私有方法）
     *
     * 作用：将服务器返回的 AugmentDetailResponse DTO 转换为 UI层使用的 AugmentUiModel
     * 转换规则：字段一一对应，null 值转为默认值
     *
     * @param detail 服务器返回的符文详情数据
     * @return AugmentUiModel UI层使用的符文展示模型
     */
    private AugmentUiModel convertDetailToUiModel(AugmentDetailResponse detail) {
        return new AugmentUiModel(
                detail.getId() != null ? detail.getId() : 0,
                detail.getNameZh(),
                detail.getNameEn(),
                detail.getDescription(),
                detail.getQuality(),
                detail.getSynergySet(),
                detail.getSynergySet2(),
                detail.getSynergySet3(),
                detail.getIconUrl(),
                detail.getWinRate() != null ? detail.getWinRate() : 0.0,
                detail.getPickRate() != null ? detail.getPickRate() : 0.0,
                detail.getAvgPlacement() != null ? detail.getAvgPlacement() : 0.0,
                detail.getTier(),
                detail.getIsTrap() != null ? detail.getIsTrap() : false
        );
    }

    /**
     * 网络响应 → 数据实体转换（私有方法）
     *
     * 作用：将服务器返回的 AugmentResponse DTO 转换为 Room 存储的 AugmentEntity
     * 转换规则：字段一一对应，null 值转为默认值，isTrap 固定为 false
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
     * 详情响应 → 数据实体转换（私有方法）
     *
     * 作用：将服务器返回的 AugmentDetailResponse DTO 转换为 Room 存储的 AugmentEntity
     * 转换规则：字段一一对应，null 值转为默认值
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
     * 缓存实体 → UI模型转换（私有方法）
     *
     * 作用：将 Room 数据库的 AugmentEntity 转换为 UI层使用的 AugmentUiModel
     * 转换规则：字段一一对应
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