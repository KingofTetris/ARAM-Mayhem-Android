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