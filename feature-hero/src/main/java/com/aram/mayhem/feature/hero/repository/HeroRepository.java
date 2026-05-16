package com.aram.mayhem.feature.hero.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
 * 英雄数据仓库
 *
 * 数据源策略：离线优先（Room 缓存 → 网络请求 → 更新缓存）
 * 功能：英雄列表分页查询、英雄详情查询、本地缓存更新、离线模式支持
 * 关联：HeroApi, HeroDao, HeroEntity, HeroResponse
 */
@Singleton
public class HeroRepository {

    private final HeroApi heroApi;
    private final HeroDao heroDao;

    /** 离线模式标记：true 表示当前无网络连接 */
    private volatile boolean isOffline = false;

    @Inject
    public HeroRepository(HeroApi heroApi, HeroDao heroDao) {
        this.heroApi = heroApi;
        this.heroDao = heroDao;
    }

    public HeroApi getHeroApi() {
        return heroApi;
    }

    public HeroDao getHeroDao() {
        return heroDao;
    }

    /**
     * 设置离线模式状态
     *
     * 作用：由 ViewModel 调用，通知 Repository 当前网络状态
     * 影响：离线模式下跳过网络请求，直接返回缓存数据
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
     * 获取英雄列表（英雄模块 - 离线优先策略）
     *
     * 作用：从服务器分页获取英雄列表，支持关键词搜索、梯级筛选、排序
     * 离线优先策略：
     *   1. 先从 Room 缓存读取数据，如果有缓存立即返回
     *   2. 如果在线，发起网络请求获取最新数据
     *   3. 网络成功：更新 Room 缓存 + 返回最新数据
     *   4. 网络失败：保持缓存数据不变
     *
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param keyword 搜索关键词（可为null表示不筛选）
     * @param tier 梯级筛选（S_PLUS/S/A/B/C/null表示不筛选）
     * @param sortBy 排序规则（name/winRate/pickRate）
     * @return LiveData<List<HeroUiModel>> 可观察的英雄UI模型列表
     */
    public LiveData<List<HeroUiModel>> getHeroes(int page, int size, String keyword, String tier, String sortBy) {
        MutableLiveData<List<HeroUiModel>> result = new MutableLiveData<>();

        // 步骤1：离线优先 - 先从 Room 缓存读取
        new Thread(() -> {
            LiveData<List<HeroEntity>> cached = heroDao.getAllHeroes();
            List<HeroEntity> cachedList = cached.getValue();
            if (cachedList != null && !cachedList.isEmpty()) {
                List<HeroUiModel> cachedUiModels = cachedList.stream()
                        .map(this::convertEntityToUiModel)
                        .collect(Collectors.toList());
                result.postValue(cachedUiModels);
            }
        }).start();

        // 步骤2：如果离线模式，跳过网络请求
        if (isOffline) {
            return result;
        }

        // 步骤3：发起网络请求获取最新数据
        heroApi.getHeroes(keyword, tier, sortBy, page, size).enqueue(new Callback<Result<PageResponse<HeroResponse>>>() {
            @Override
            public void onResponse(Call<Result<PageResponse<HeroResponse>>> call, Response<Result<PageResponse<HeroResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<HeroResponse> pageData = response.body().getData();
                    if (pageData != null && pageData.getRecords() != null) {
                        List<HeroUiModel> uiModels = pageData.getRecords().stream()
                                .map(HeroRepository.this::convertToUiModel)
                                .collect(Collectors.toList());
                        result.setValue(uiModels);

                        // 异步更新本地缓存
                        new Thread(() -> {
                            List<HeroEntity> entities = pageData.getRecords().stream()
                                    .map(HeroRepository.this::convertToEntity)
                                    .collect(Collectors.toList());
                            heroDao.insertAll(entities);
                        }).start();
                    } else {
                        result.setValue(Collections.emptyList());
                    }
                } else {
                    // 业务失败：保持缓存数据（步骤1已返回缓存）
                    ensureNonEmptyResult(result);
                }
            }

            @Override
            public void onFailure(Call<Result<PageResponse<HeroResponse>>> call, Throwable t) {
                // 网络异常：保持缓存数据（步骤1已返回缓存）
                ensureNonEmptyResult(result);
            }
        });

        return result;
    }

    /**
     * 获取英雄详情（英雄模块 - 离线优先策略）
     *
     * 作用：根据英雄ID获取完整英雄信息，包括技能、出装、克制关系等
     * 离线优先策略：
     *   1. 先从 Room 缓存读取详情，如果有缓存立即返回
     *   2. 如果在线，发起网络请求获取最新数据
     *   3. 网络成功：更新 Room 缓存 + 返回最新数据
     *   4. 网络失败：保持缓存数据不变
     *
     * @param heroId 英雄ID
     * @return LiveData<HeroDetailUiModel> 可观察的英雄详情UI模型
     */
    public LiveData<HeroDetailUiModel> getHeroDetail(long heroId) {
        MutableLiveData<HeroDetailUiModel> result = new MutableLiveData<>();

        // 步骤1：离线优先 - 先从 Room 缓存读取详情
        new Thread(() -> {
            LiveData<HeroEntity> cached = heroDao.getHeroById(heroId);
            HeroEntity cachedEntity = cached.getValue();
            if (cachedEntity != null) {
                result.postValue(convertEntityToDetailUiModel(cachedEntity));
            }
        }).start();

        // 步骤2：如果离线模式，跳过网络请求
        if (isOffline) {
            return result;
        }

        // 步骤3：发起网络请求获取最新数据
        heroApi.getHeroDetail(heroId).enqueue(new Callback<Result<HeroDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<HeroDetailResponse>> call, Response<Result<HeroDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    HeroDetailResponse detail = response.body().getData();
                    if (detail != null) {
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
                    // 业务失败：保持缓存数据（步骤1已返回缓存）
                }
            }

            @Override
            public void onFailure(Call<Result<HeroDetailResponse>> call, Throwable t) {
                // 网络异常：保持缓存数据（步骤1已返回缓存）
            }
        });

        return result;
    }

    /**
     * 确保结果非空（私有方法）
     *
     * 作用：当网络请求失败且缓存也为空时，设置空列表避免 UI 显示异常
     *
     * @param result 可观察的 MutableLiveData
     */
    private void ensureNonEmptyResult(MutableLiveData<List<HeroUiModel>> result) {
        if (result.getValue() == null) {
            result.postValue(Collections.emptyList());
        }
    }

    /**
     * 网络响应 → UI模型转换（私有方法）
     *
     * 作用：将服务器返回的 HeroResponse DTO 转换为 UI层使用的 HeroUiModel
     * 转换规则：字段一一对应，null 值转为 0.0
     *
     * @param response 服务器返回的英雄列表项数据
     * @return HeroUiModel UI层使用的英雄展示模型
     */
    private HeroUiModel convertToUiModel(HeroResponse response) {
        return new HeroUiModel(
                response.getId(),
                response.getNameZh(),
                response.getNameEn(),
                response.getTitle(),
                response.getRole(),
                parseTier(response.getTier()),
                response.getWinRate() != null ? response.getWinRate().doubleValue() : 0.0,
                response.getPickRate() != null ? response.getPickRate().doubleValue() : 0.0,
                response.getImageUrl()
        );
    }

    /**
     * 网络响应 → 数据实体转换（私有方法）
     *
     * 作用：将服务器返回的 HeroResponse DTO 转换为 Room 存储的 HeroEntity
     * 转换规则：字段一一对应，null 值转为默认值，isTrap 固定为 false
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
     * 详情响应 → 数据实体转换（私有方法）
     *
     * 作用：将服务器返回的 HeroDetailResponse DTO 转换为 Room 存储的 HeroEntity
     * 转换规则：字段一一对应，null 值转为默认值，isTrap 固定为 false，isVersionTrap 从响应读取
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
     * 缓存实体 → UI模型转换（私有方法）
     *
     * 作用：将 Room 数据库的 HeroEntity 转换为 UI层使用的 HeroUiModel
     * 转换规则：字段一一对应，tier 字符串转为 Tier 枚举
     *
     * @param entity Room 数据库存储的英雄数据实体
     * @return HeroUiModel UI层使用的英雄展示模型
     */
    private HeroUiModel convertEntityToUiModel(HeroEntity entity) {
        return new HeroUiModel(
                entity.id,
                entity.nameZh,
                entity.nameEn,
                entity.title,
                entity.role,
                parseTier(entity.tier),
                entity.winRate,
                entity.pickRate,
                entity.avatarUrl
        );
    }

    /**
     * 缓存实体 → 详情UI模型转换（私有方法）
     *
     * 作用：将 Room 数据库的 HeroEntity 转换为 UI层使用的 HeroDetailUiModel
     * 转换规则：字段一一对应，tier 字符串转为 Tier 枚举，技能列表单独转换
     *
     * @param entity Room 数据库存储的英雄数据实体
     * @return HeroDetailUiModel UI层使用的英雄详情展示模型
     */
    private HeroDetailUiModel convertEntityToDetailUiModel(HeroEntity entity) {
        // 技能列表转换：Entity SkillData → UI SkillUiModel
        List<HeroDetailUiModel.SkillUiModel> skills = null;
        if (entity.skills != null) {
            skills = entity.skills.stream().map(skill ->
                    new HeroDetailUiModel.SkillUiModel(skill.key, skill.name, skill.description)
            ).collect(Collectors.toList());
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
                entity.avatarUrl,
                false
        );
    }

    /**
     * 详情响应 → UI模型转换（私有方法）
     *
     * 作用：将服务器返回的 HeroDetailResponse DTO 转换为 UI层使用的 HeroDetailUiModel
     * 转换规则：字段一一对应，null 值转为 0.0 或 false，tier 字符串转为 Tier 枚举
     *
     * @param detail 服务器返回的英雄详情数据
     * @return HeroDetailUiModel UI层使用的英雄详情展示模型
     */
    private HeroDetailUiModel convertToDetailUiModel(HeroDetailResponse detail) {
        // 技能列表转换：DTO → UI SkillUiModel
        List<HeroDetailUiModel.SkillUiModel> skills = null;
        if (detail.getSkills() != null) {
            skills = detail.getSkills().stream()
                    .map(skill -> new HeroDetailUiModel.SkillUiModel(skill.getKey(), skill.getName(), skill.getDescription()))
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
                detail.getImageUrl(),
                detail.getIsVersionTrap() != null && detail.getIsVersionTrap()
        );
    }

    /**
     * 梯级字符串 → 梯级枚举转换（私有方法）
     *
     * 作用：将后端返回的梯级字符串（如 "S+"）转换为前端使用的 Tier 枚举
     * 转换规则：S+ → S_PLUS，S → S，A → A，B → B，C/null → C
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
