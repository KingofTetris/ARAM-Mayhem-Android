package com.aram.mayhem.feature.hero.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.common.Tier;
import com.aram.mayhem.network.api.HeroApi;
import com.aram.mayhem.network.dto.HeroDetailResponse;
import com.aram.mayhem.ui.model.HeroDetailUiModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class HeroDetailViewModel extends AndroidViewModel {

    private final HeroApi heroApi;

    private final MutableLiveData<HeroDetailUiModel> heroDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    @Inject
    public HeroDetailViewModel(@NonNull Application application, HeroApi heroApi) {
        super(application);
        this.heroApi = heroApi;
    }

    public LiveData<HeroDetailUiModel> getHeroDetail() {
        return heroDetail;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadHeroDetail(long heroId) {
        loading.setValue(true);
        heroApi.getHeroDetail(heroId).enqueue(new Callback<Result<HeroDetailResponse>>() {
            @Override
            public void onResponse(@NonNull Call<Result<HeroDetailResponse>> call,
                                   @NonNull Response<Result<HeroDetailResponse>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    HeroDetailResponse detailResponse = response.body().getData();
                    if (detailResponse != null) {
                        heroDetail.setValue(convertToUiModel(detailResponse));
                    }
                } else {
                    String message = response.body() != null ? response.body().getMessage() : "获取英雄详情失败";
                    error.setValue(message);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Result<HeroDetailResponse>> call, @NonNull Throwable t) {
                loading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    private HeroDetailUiModel convertToUiModel(HeroDetailResponse response) {
        return new HeroDetailUiModel(
                response.getId(),
                response.getNameZh(),
                response.getNameEn(),
                response.getTitle(),
                response.getRole(),
                parseTier(response.getTier()),
                response.getWinRate() != null ? response.getWinRate().doubleValue() : 0.0,
                response.getPickRate() != null ? response.getPickRate().doubleValue() : 0.0,
                response.getDescription(),
                response.getSkills() != null ? convertSkills(response.getSkills()) : null,
                response.getCounterTips(),
                response.getSynergies(),
                response.getAvgKills() != null ? response.getAvgKills().doubleValue() : 0.0,
                response.getAvgDeaths() != null ? response.getAvgDeaths().doubleValue() : 0.0,
                response.getAvgAssists() != null ? response.getAvgAssists().doubleValue() : 0.0,
                response.getRecommendedBuild(),
                response.getImageUrl()
        );
    }

    private java.util.List<HeroDetailUiModel.SkillUiModel> convertSkills(
            java.util.List<HeroDetailResponse.SkillResponse> skills) {
        java.util.List<HeroDetailUiModel.SkillUiModel> uiSkills = new java.util.ArrayList<>();
        for (HeroDetailResponse.SkillResponse skill : skills) {
            uiSkills.add(new HeroDetailUiModel.SkillUiModel(
                    skill.getKey(),
                    skill.getName(),
                    skill.getDescription()
            ));
        }
        return uiSkills;
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
