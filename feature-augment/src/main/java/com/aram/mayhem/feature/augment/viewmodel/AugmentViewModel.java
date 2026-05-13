package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.ui.model.AugmentUiModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class AugmentViewModel extends AndroidViewModel {

    private final AugmentRepository augmentRepository;

    private final MutableLiveData<List<AugmentUiModel>> augments = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    private int currentPage = 1;
    private final int pageSize = 20;
    private String currentQuality = "";
    private String currentSynergySet = "";

    @Inject
    public AugmentViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
        loadAugments(true);
    }

    public LiveData<List<AugmentUiModel>> getAugments() {
        return augments;
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

    public void filterByQuality(String quality) {
        currentQuality = quality != null ? quality : "";
        loadAugments(true);
    }

    public void filterBySynergy(String synergySet) {
        currentSynergySet = synergySet != null ? synergySet : "";
        loadAugments(true);
    }

    public void loadMore() {
        if (loadingMore.getValue() == null || !loadingMore.getValue()) {
            if (isLastPage.getValue() == null || !isLastPage.getValue()) {
                loadAugments(false);
            }
        }
    }

    public void retry() {
        loadAugments(true);
    }

    private void loadAugments(boolean reset) {
        if (reset) {
            currentPage = 1;
            augments.setValue(new ArrayList<>());
            isLastPage.setValue(false);
        }

        loading.setValue(reset);
        loadingMore.setValue(!reset);
        isOffline.setValue(false);

        augmentRepository.getAugments(currentPage, pageSize, currentQuality, currentSynergySet)
                .observeForever(uiModels -> {
                    loading.setValue(false);
                    loadingMore.setValue(false);

                    if (uiModels != null && !uiModels.isEmpty()) {
                        List<AugmentUiModel> current = new ArrayList<>();
                        if (!reset && augments.getValue() != null) {
                            current.addAll(augments.getValue());
                        }
                        current.addAll(uiModels);
                        augments.setValue(current);
                        isLastPage.setValue(uiModels.size() < pageSize);
                        currentPage++;
                    } else {
                        if (reset) {
                            augments.setValue(new ArrayList<>());
                        }
                        isLastPage.setValue(true);
                    }
                });
    }
}