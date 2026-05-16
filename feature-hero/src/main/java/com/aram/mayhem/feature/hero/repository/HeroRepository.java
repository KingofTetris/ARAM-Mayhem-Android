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
 * 数据源策略：优先网络 → 失败时回退本地缓存
 * 功能：英雄列表分页查询、英雄详情查询、本地缓存更新
 * 关联：HeroApi, HeroDao, HeroEntity, HeroResponse
 */
@Singleton
public class HeroRepository {

    private final HeroApi heroApi;
    private final HeroDao heroDao;

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
     * 获取英雄列表（英雄模块）
     *
     * 作用：从服务器分页获取英雄列表，支持关键词搜索、梯级筛选、排序
     * 实现：优先请求网络 → 成功写入本地缓存 → 失败时回退本地缓存
     *
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param keyword 搜索关键词（可为null表示不筛选）
     * @param tier 梯级筛选（S_PLUS/S/A/B/C/null表示不筛选）
     * @param sortBy 排序规则（name/winRate/pickRate）
     * @return LiveData<List<HeroUiModel>> 可观察的英雄UI模型列表
     */
    public LiveData<List<HeroUiModel>> getHeroes(int page, int size, String keyword, String tier, String sortBy) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<List<HeroUiModel>> result = new MutableLiveData<>();

        // 调用 Retrofit 接口发起异步网络请求：获取英雄列表
        heroApi.getHeroes(keyword, tier, sortBy, page, size).enqueue(new Callback<Result<PageResponse<HeroResponse>>>() {
            @Override
            public void onResponse(Call<Result<PageResponse<HeroResponse>>> call, Response<Result<PageResponse<HeroResponse>>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    PageResponse<HeroResponse> pageData = response.body().getData();
                    // 判断分页数据有效：数据不为空且记录列表不为空
                    if (pageData != null && pageData.getRecords() != null) {
                        // 网络数据 → UI模型转换（网络数据直接用于显示）
                        List<HeroUiModel> uiModels = pageData.getRecords().stream()
                                .map(HeroRepository.this::convertToUiModel)
                                .collect(Collectors.toList());
                        result.setValue(uiModels);

                        // 异步写入本地缓存：网络数据 → 数据实体 → Room数据库
                        new Thread(() -> {
                            List<HeroEntity> entities = pageData.getRecords().stream()
                                    .map(HeroRepository.this::convertToEntity)
                                    .collect(Collectors.toList());
                            heroDao.insertAll(entities);
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
            public void onFailure(Call<Result<PageResponse<HeroResponse>>> call, Throwable t) {
                // 网络异常：回退到本地缓存
                loadFromCache(result);
            }
        });

        return result;
    }

    /**
     * 获取英雄详情（英雄模块）
     *
     * 作用：根据英雄ID获取完整英雄信息，包括技能、出装、克制关系等
     * 实现：优先请求网络 → 成功写入本地缓存 → 失败时回退本地缓存
     *
     * @param heroId 英雄ID
     * @return LiveData<HeroDetailUiModel> 可观察的英雄详情UI模型
     */
    public LiveData<HeroDetailUiModel> getHeroDetail(long heroId) {
        // 创建可修改的 MutableLiveData，用于对外提供可观察的数据结果
        MutableLiveData<HeroDetailUiModel> result = new MutableLiveData<>();

        // 调用 Retrofit 接口发起异步网络请求：获取英雄详情
        heroApi.getHeroDetail(heroId).enqueue(new Callback<Result<HeroDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<HeroDetailResponse>> call, Response<Result<HeroDetailResponse>> response) {
                // 判断：HTTP请求成功 + 响应体不为空 + 业务状态码成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    HeroDetailResponse detail = response.body().getData();
                    // 判断详情数据有效
                    if (detail != null) {
                        // 网络数据 → UI模型转换并设置到 LiveData
                        result.setValue(convertToDetailUiModel(detail));

                        // 异步写入本地缓存：详情数据 → 数据实体 → Room数据库
                        new Thread(() -> {
                            HeroEntity entity = convertDetailToEntity(detail);
                            List<HeroEntity> list = new ArrayList<>();
                            list.add(entity);
                            heroDao.insertAll(list);
                        }).start();
                    }
                } else {
                    // 业务失败：回退到本地缓存
                    loadDetailFromCache(heroId, result);
                }
            }

            @Override
            public void onFailure(Call<Result<HeroDetailResponse>> call, Throwable t) {
                // 网络异常：回退到本地缓存
                loadDetailFromCache(heroId, result);
            }
        });

        return result;
    }

    /**
     * 从本地缓存加载英雄列表（私有方法）
     *
     * 作用：当网络请求失败时，回退从 Room 数据库加载本地缓存的英雄列表
     * 实现：子线程查询 Room → UI模型转换 → postValue 到 LiveData
     *
     * @param result 可观察的 MutableLiveData，用于接收缓存数据
     */
    private void loadFromCache(MutableLiveData<List<HeroUiModel>> result) {
        // 子线程查询本地缓存
        new Thread(() -> {
            LiveData<List<HeroEntity>> cached = heroDao.getAllHeroes();
            // 判断缓存有效：数据不为空
            if (cached.getValue() != null && !cached.getValue().isEmpty()) {
                // 缓存数据 → UI模型转换
                List<HeroUiModel> uiModels = cached.getValue().stream()
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
     * 从本地缓存加载英雄详情（私有方法）
     *
     * 作用：当网络请求失败时，回退从 Room 数据库加载指定英雄的详情
     * 实现：子线程查询 Room → UI模型转换 → postValue 到 LiveData
     *
     * @param heroId 英雄ID
     * @param result 可观察的 MutableLiveData，用于接收缓存数据
     */
    private void loadDetailFromCache(long heroId, MutableLiveData<HeroDetailUiModel> result) {
        // 子线程查询本地缓存
        new Thread(() -> {
            LiveData<HeroEntity> cached = heroDao.getHeroById(heroId);
            // 判断缓存有效：数据不为空
            if (cached.getValue() != null) {
                // 缓存数据 → 详情UI模型转换
                result.postValue(convertEntityToDetailUiModel(cached.getValue()));
            } else {
                // 缓存为空：设置null
                result.postValue(null);
            }
        }).start();
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
                entity.avatarUrl
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
