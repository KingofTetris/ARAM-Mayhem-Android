package com.aram.mayhem.feature.hero.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.common.Tier;
import com.aram.mayhem.feature.hero.repository.HeroRepository;
import com.aram.mayhem.network.api.HeroApi;
import com.aram.mayhem.network.dto.HeroResponse;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.ui.model.HeroUiModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.inject.Inject;

@HiltViewModel
public class HeroListViewModel extends AndroidViewModel {

    private final HeroRepository heroRepository;
    private final HeroApi heroApi;

    private final MutableLiveData<List<HeroUiModel>> heroes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);

    private int currentPage = 1;
    private final int pageSize = 20;
    private String currentKeyword = "";
    private String currentTier = "";
    private String currentSortBy = "winRate";

    @Inject
    public HeroListViewModel(@NonNull Application application, HeroRepository heroRepository, HeroApi heroApi) {
        super(application);
        this.heroRepository = heroRepository;
        this.heroApi = heroApi;
        loadHeroes(true);
    }

    public LiveData<List<HeroUiModel>> getHeroes() {
        return heroes;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getLoadingMore() {
        return loadingMore;
    }

    public LiveData<Boolean> getIsLastPage() {
        return isLastPage;
    }

    public void searchHeroes(String keyword) {
        currentKeyword = keyword;
        loadHeroes(true);
    }

    public void filterByTier(Tier tier) {
        currentTier = tier != null ? tier.getLabel() : "";
        loadHeroes(true);
    }

    public void loadMore() {
        if (!loadingMore.getValue() && !isLastPage.getValue()) {
            loadHeroes(false);
        }
    }

    public void retry() {
        loadHeroes(true);
    }

    private void loadHeroes(boolean reset) {
        if (reset) {
            currentPage = 1;
            heroes.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        loading.setValue(reset);
        loadingMore.setValue(!reset);

        heroApi.getHeroes(currentKeyword, currentTier, currentSortBy, currentPage, pageSize)
                .enqueue(new Callback<Result<PageResponse<HeroResponse>>>() {
                    @Override
                    public void onResponse(@NonNull Call<Result<PageResponse<HeroResponse>>> call,
                                          @NonNull Response<Result<PageResponse<HeroResponse>>> response) {
                        loading.setValue(false);
                        loadingMore.setValue(false);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            PageResponse<HeroResponse> pageResult = response.body().getData();
                            if (pageResult != null) {
                                List<HeroUiModel> heroUiModels = new ArrayList<>();
                                if (!reset && heroes.getValue() != null) {
                                    heroUiModels.addAll(heroes.getValue());
                                }
                                for (HeroResponse hero : pageResult.getRecords()) {
                                    heroUiModels.add(convertToUiModel(hero));
                                }
                                heroes.setValue(heroUiModels);
                                isLastPage.setValue(heroUiModels.size() >= pageResult.getTotal());
                                currentPage++;
                            }
                        } else {
                            String message = response.body() != null ? response.body().getMessage() : "网络请求失败";
                            if (reset) {
                                error.setValue(message);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Result<PageResponse<HeroResponse>>> call, @NonNull Throwable t) {
                        loading.setValue(false);
                        loadingMore.setValue(false);
                        if (reset) {
                            error.setValue(t.getMessage());
                        }
                    }
                });
    }

    private HeroUiModel convertToUiModel(HeroResponse hero) {
        return new HeroUiModel(
                hero.getId(),
                hero.getNameZh(),
                hero.getNameEn(),
                hero.getRole(),
                parseTier(hero.getTier()),
                hero.getWinRate() != null ? hero.getWinRate() : 0.0,
                hero.getPickRate() != null ? hero.getPickRate() : 0.0,
                hero.getImageUrl(),
                false
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
