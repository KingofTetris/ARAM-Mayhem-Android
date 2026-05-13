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

    public LiveData<List<HeroUiModel>> getHeroes(int page, int size, String keyword, String tier, String sortBy) {
        MutableLiveData<List<HeroUiModel>> result = new MutableLiveData<>();

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
                    loadFromCache(result);
                }
            }

            @Override
            public void onFailure(Call<Result<PageResponse<HeroResponse>>> call, Throwable t) {
                loadFromCache(result);
            }
        });

        return result;
    }

    public LiveData<HeroDetailUiModel> getHeroDetail(long heroId) {
        MutableLiveData<HeroDetailUiModel> result = new MutableLiveData<>();

        heroApi.getHeroDetail(heroId).enqueue(new Callback<Result<HeroDetailResponse>>() {
            @Override
            public void onResponse(Call<Result<HeroDetailResponse>> call, Response<Result<HeroDetailResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    HeroDetailResponse detail = response.body().getData();
                    if (detail != null) {
                        result.setValue(convertToDetailUiModel(detail));

                        new Thread(() -> {
                            HeroEntity entity = convertDetailToEntity(detail);
                            List<HeroEntity> list = new ArrayList<>();
                            list.add(entity);
                            heroDao.insertAll(list);
                        }).start();
                    }
                } else {
                    loadDetailFromCache(heroId, result);
                }
            }

            @Override
            public void onFailure(Call<Result<HeroDetailResponse>> call, Throwable t) {
                loadDetailFromCache(heroId, result);
            }
        });

        return result;
    }

    private void loadFromCache(MutableLiveData<List<HeroUiModel>> result) {
        new Thread(() -> {
            LiveData<List<HeroEntity>> cached = heroDao.getAllHeroes();
            if (cached.getValue() != null && !cached.getValue().isEmpty()) {
                List<HeroUiModel> uiModels = cached.getValue().stream()
                        .map(this::convertEntityToUiModel)
                        .collect(Collectors.toList());
                result.postValue(uiModels);
            } else {
                result.postValue(Collections.emptyList());
            }
        }).start();
    }

    private void loadDetailFromCache(long heroId, MutableLiveData<HeroDetailUiModel> result) {
        new Thread(() -> {
            LiveData<HeroEntity> cached = heroDao.getHeroById(heroId);
            if (cached.getValue() != null) {
                result.postValue(convertEntityToDetailUiModel(cached.getValue()));
            } else {
                result.postValue(null);
            }
        }).start();
    }

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
        entity.updatedAt = System.currentTimeMillis();

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

    private HeroDetailUiModel convertEntityToDetailUiModel(HeroEntity entity) {
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

    private HeroDetailUiModel convertToDetailUiModel(HeroDetailResponse detail) {
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
                detail.getImageUrl()
        );
    }

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
