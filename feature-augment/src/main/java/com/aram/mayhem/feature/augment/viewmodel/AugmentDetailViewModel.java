package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.ui.model.AugmentUiModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
/**
 * 符文详情 ViewModel
 *
 * 功能：加载符文详情和套装进度数据
 * 数据流：AugmentRepository → LiveData → AugmentDetailBottomSheet
 */
public class AugmentDetailViewModel extends AndroidViewModel {

    private final AugmentRepository augmentRepository;
    private final MutableLiveData<AugmentUiModel> augmentDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    @Inject
    public AugmentDetailViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
    }

    public LiveData<AugmentUiModel> getAugmentDetail() {
        return augmentDetail;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadAugmentDetail(long augmentId) {
        loading.setValue(true);
        augmentRepository.getAugmentDetail(augmentId).observeForever(uiModel -> {
            loading.setValue(false);
            if (uiModel != null) {
                augmentDetail.setValue(uiModel);
            } else {
                error.setValue("无法加载符文详情");
            }
        });
    }
}