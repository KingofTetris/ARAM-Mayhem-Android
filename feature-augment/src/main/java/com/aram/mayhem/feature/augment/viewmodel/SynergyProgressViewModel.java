package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.network.dto.SynergyProgressResponse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
/**
 * 套装进度 ViewModel
 *
 * 功能：根据符文 ID 列表查询套装进度
 * 数据流：AugmentRepository → LiveData<List<SynergyProgressResponse>>
 */
public class SynergyProgressViewModel extends AndroidViewModel {

    private final AugmentRepository augmentRepository;
    private final MutableLiveData<List<SynergyProgressResponse>> synergyProgress = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private List<Long> selectedAugmentIds = new ArrayList<>();

    @Inject
    public SynergyProgressViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
    }

    public LiveData<List<SynergyProgressResponse>> getSynergyProgress() {
        return synergyProgress;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void setSelectedAugmentIds(List<Long> ids) {
        this.selectedAugmentIds = ids != null ? ids : new ArrayList<>();
        loadSynergyProgress();
    }

    public void addAugment(long augmentId) {
        if (!selectedAugmentIds.contains(augmentId)) {
            selectedAugmentIds.add(augmentId);
            loadSynergyProgress();
        }
    }

    public void removeAugment(long augmentId) {
        selectedAugmentIds.remove(augmentId);
        loadSynergyProgress();
    }

    public void loadSynergyProgress() {
        if (selectedAugmentIds.isEmpty()) {
            synergyProgress.setValue(new ArrayList<>());
            return;
        }

        loading.setValue(true);
        augmentRepository.getSynergyProgress(selectedAugmentIds).observeForever(progress -> {
            loading.setValue(false);
            if (progress != null) {
                synergyProgress.setValue(progress);
            } else {
                synergyProgress.setValue(new ArrayList<>());
            }
        });
    }

    public List<Long> getSelectedAugmentIds() {
        return new ArrayList<>(selectedAugmentIds);
    }
}