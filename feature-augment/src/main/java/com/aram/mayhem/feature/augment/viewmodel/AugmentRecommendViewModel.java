package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.aram.mayhem.network.dto.SynergyProgressResponse;
import com.aram.mayhem.ui.model.HeroUiModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
/**
 * 符文推荐 ViewModel
 *
 * 功能：管理已选符文列表、请求套装进度和推荐结果
 * 数据流：AugmentRepository → LiveData → AugmentRecommendFragment
 */
public class AugmentRecommendViewModel extends AndroidViewModel {

    private final AugmentRepository augmentRepository;

    private final MutableLiveData<HeroUiModel> selectedHero = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> selectedAugmentIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<SynergyProgressResponse>> synergyProgress = new MutableLiveData<>();
    private final MutableLiveData<List<AugmentRecommendResponse>> recommendations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    @Inject
    public AugmentRecommendViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
    }

    public LiveData<HeroUiModel> getSelectedHero() {
        return selectedHero;
    }

    public LiveData<List<Long>> getSelectedAugmentIds() {
        return selectedAugmentIds;
    }

    public LiveData<List<SynergyProgressResponse>> getSynergyProgress() {
        return synergyProgress;
    }

    public LiveData<List<AugmentRecommendResponse>> getRecommendations() {
        return recommendations;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void setSelectedHero(HeroUiModel hero) {
        selectedHero.setValue(hero);
        refreshData();
    }

    public void addSelectedAugment(long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current == null) {
            current = new ArrayList<>();
        }
        if (!current.contains(augmentId)) {
            current.add(augmentId);
            selectedAugmentIds.setValue(current);
            refreshData();
        }
    }

    public void removeSelectedAugment(long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current != null) {
            current.remove(augmentId);
            selectedAugmentIds.setValue(new ArrayList<>(current));
            refreshData();
        }
    }

    private void refreshData() {
        HeroUiModel hero = selectedHero.getValue();
        List<Long> ids = selectedAugmentIds.getValue();

        if (hero == null || ids == null || ids.isEmpty()) {
            synergyProgress.setValue(new ArrayList<>());
            recommendations.setValue(new ArrayList<>());
            return;
        }

        loading.setValue(true);

        augmentRepository.getSynergyProgress(ids).observeForever(progress -> {
            synergyProgress.setValue(progress);
        });

        augmentRepository.getRecommendations(hero.getId(), ids).observeForever(recs -> {
            loading.setValue(false);
            recommendations.setValue(recs);
        });
    }
}