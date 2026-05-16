package com.aram.mayhem.feature.community.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.community.repository.StrategyRepository;
import com.aram.mayhem.network.dto.StrategyListResponse;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
/**
 * 社区攻略流 ViewModel
 *
 * 功能：管理攻略列表数据（分页加载、排序）
 * 数据流：StrategyRepository → LiveData<StrategyListResponse> → CommunityFeedFragment
 */
public class StrategyFeedViewModel extends AndroidViewModel {

    private final StrategyRepository strategyRepository;

    private final MutableLiveData<List<StrategyListResponse>> strategies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);

    private int currentPage = 1;
    private final int pageSize = 10;
    private String currentSort = "hot";

    @Inject
    public StrategyFeedViewModel(@NonNull Application application, StrategyRepository strategyRepository) {
        super(application);
        this.strategyRepository = strategyRepository;
        loadStrategies(true);
    }

    public LiveData<List<StrategyListResponse>> getStrategies() {
        return strategies;
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

    public void setSort(String sort) {
        if (!currentSort.equals(sort)) {
            currentSort = sort;
            loadStrategies(true);
        }
    }

    public void loadMore() {
        if (loadingMore.getValue() == null || !loadingMore.getValue()) {
            if (isLastPage.getValue() == null || !isLastPage.getValue()) {
                loadStrategies(false);
            }
        }
    }

    public void retry() {
        loadStrategies(true);
    }

    private void loadStrategies(boolean reset) {
        if (reset) {
            currentPage = 1;
            strategies.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        loading.setValue(reset);
        loadingMore.setValue(!reset);
        error.setValue(null);

        strategyRepository.getStrategies(currentSort, currentPage, pageSize).observeForever(newStrategies -> {
            loading.setValue(false);
            loadingMore.setValue(false);

            if (newStrategies != null && !newStrategies.isEmpty()) {
                List<StrategyListResponse> current = new ArrayList<>();
                if (!reset && strategies.getValue() != null) {
                    current.addAll(strategies.getValue());
                }
                current.addAll(newStrategies);
                strategies.setValue(current);
                isLastPage.setValue(newStrategies.size() < pageSize);
                currentPage++;
            } else {
                if (reset) {
                    strategies.setValue(new ArrayList<>());
                }
                isLastPage.setValue(true);
            }
        });
    }
}