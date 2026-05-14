package com.aram.mayhem.feature.community.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.community.repository.StrategyRepository;
import com.aram.mayhem.network.dto.StrategyDetailResponse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PublishStrategyViewModel extends AndroidViewModel {

    private final StrategyRepository strategyRepository;

    private final MutableLiveData<Long> selectedHeroId = new MutableLiveData<>();
    private final MutableLiveData<String> title = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> selectedAugmentIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> selectedItemIds = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> publishing = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<StrategyDetailResponse> publishedStrategy = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFormValid = new MutableLiveData<>(false);

    @Inject
    public PublishStrategyViewModel(@NonNull Application application, StrategyRepository strategyRepository) {
        super(application);
        this.strategyRepository = strategyRepository;
    }

    public LiveData<Long> getSelectedHeroId() {
        return selectedHeroId;
    }

    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<String> getDescription() {
        return description;
    }

    public LiveData<List<Long>> getSelectedAugmentIds() {
        return selectedAugmentIds;
    }

    public LiveData<List<Long>> getSelectedItemIds() {
        return selectedItemIds;
    }

    public LiveData<Boolean> getPublishing() {
        return publishing;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<StrategyDetailResponse> getPublishedStrategy() {
        return publishedStrategy;
    }

    public LiveData<Boolean> getIsFormValid() {
        return isFormValid;
    }

    public void setSelectedHeroId(Long heroId) {
        selectedHeroId.setValue(heroId);
        validateForm();
    }

    public void setTitle(String title) {
        this.title.setValue(title);
        validateForm();
    }

    public void setDescription(String description) {
        this.description.setValue(description);
        validateForm();
    }

    public void setSelectedAugmentIds(List<Long> augmentIds) {
        selectedAugmentIds.setValue(augmentIds);
    }

    public void setSelectedItemIds(List<Long> itemIds) {
        selectedItemIds.setValue(itemIds);
    }

    public void addAugment(Long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current == null) {
            current = new ArrayList<>();
        }
        if (!current.contains(augmentId)) {
            current.add(augmentId);
            selectedAugmentIds.setValue(current);
        }
    }

    public void removeAugment(Long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current != null) {
            current.remove(augmentId);
            selectedAugmentIds.setValue(current);
        }
    }

    public void addItem(Long itemId) {
        List<Long> current = selectedItemIds.getValue();
        if (current == null) {
            current = new ArrayList<>();
        }
        if (!current.contains(itemId)) {
            current.add(itemId);
            selectedItemIds.setValue(current);
        }
    }

    public void removeItem(Long itemId) {
        List<Long> current = selectedItemIds.getValue();
        if (current != null) {
            current.remove(itemId);
            selectedItemIds.setValue(current);
        }
    }

    public void publish() {
        Long heroId = selectedHeroId.getValue();
        String titleStr = title.getValue();
        String descStr = description.getValue();

        if (heroId == null || titleStr == null || titleStr.trim().isEmpty() ||
            descStr == null || descStr.trim().length() < 10) {
            error.setValue("请填写完整信息，描述至少10个字");
            return;
        }

        publishing.setValue(true);
        error.setValue(null);

        List<Long> augments = selectedAugmentIds.getValue();
        List<Long> items = selectedItemIds.getValue();

        strategyRepository.publishStrategy(heroId, titleStr.trim(), descStr.trim(),
                augments != null ? augments : new ArrayList<>(),
                items != null ? items : new ArrayList<>())
                .observeForever(result -> {
                    publishing.setValue(false);
                    if (result != null) {
                        publishedStrategy.setValue(result);
                    } else {
                        error.setValue("发布失败，请重试");
                    }
                });
    }

    private void validateForm() {
        Long heroId = selectedHeroId.getValue();
        String titleStr = title.getValue();
        String descStr = description.getValue();

        boolean valid = heroId != null &&
                titleStr != null && !titleStr.trim().isEmpty() &&
                descStr != null && descStr.trim().length() >= 10;

        isFormValid.setValue(valid);
    }

    public void reset() {
        selectedHeroId.setValue(null);
        title.setValue(null);
        description.setValue(null);
        selectedAugmentIds.setValue(new ArrayList<>());
        selectedItemIds.setValue(new ArrayList<>());
        error.setValue(null);
        publishedStrategy.setValue(null);
        isFormValid.setValue(false);
    }
}