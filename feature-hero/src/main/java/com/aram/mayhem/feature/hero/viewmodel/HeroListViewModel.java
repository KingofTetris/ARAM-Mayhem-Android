package com.aram.mayhem.feature.hero.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Tier;
import com.aram.mayhem.feature.hero.repository.HeroRepository;
import com.aram.mayhem.ui.model.HeroUiModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class HeroListViewModel extends AndroidViewModel {

    private final HeroRepository heroRepository;

    private final MutableLiveData<List<HeroUiModel>> heroes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    private int currentPage = 1;
    private final int pageSize = 20;
    private String currentKeyword = "";
    private String currentTier = "";
    private String currentSortBy = "winRate";

    @Inject
    public HeroListViewModel(@NonNull Application application, HeroRepository heroRepository) {
        super(application);
        this.heroRepository = heroRepository;
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

    public LiveData<Boolean> getIsOffline() {
        return isOffline;
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
        isOffline.setValue(false);

        heroRepository.getHeroes(currentPage, pageSize, currentKeyword, currentTier, currentSortBy)
                .observeForever(uiModels -> {
                    loading.setValue(false);
                    loadingMore.setValue(false);

                    if (uiModels != null && !uiModels.isEmpty()) {
                        List<HeroUiModel> current = new ArrayList<>();
                        if (!reset && heroes.getValue() != null) {
                            current.addAll(heroes.getValue());
                        }
                        current.addAll(uiModels);
                        heroes.setValue(current);
                        isLastPage.setValue(uiModels.size() < pageSize);
                        currentPage++;
                    } else {
                        if (reset) {
                            heroes.setValue(new ArrayList<>());
                        }
                        isLastPage.setValue(true);
                    }
                });
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
