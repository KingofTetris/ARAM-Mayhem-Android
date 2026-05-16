package com.aram.mayhem.feature.hero.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.hero.repository.HeroRepository;
import com.aram.mayhem.ui.model.HeroDetailUiModel;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
/**
 * 英雄详情 ViewModel
 *
 * 功能：加载英雄详情数据，转换为 HeroDetailUiModel
 * 数据流：HeroRepository → LiveData<HeroDetailUiModel> → HeroDetailFragment
 */
public class HeroDetailViewModel extends AndroidViewModel {

    private final HeroRepository heroRepository;

    private final MutableLiveData<HeroDetailUiModel> heroDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    @Inject
    public HeroDetailViewModel(@NonNull Application application, HeroRepository heroRepository) {
        super(application);
        this.heroRepository = heroRepository;
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

    public LiveData<Boolean> getIsOffline() {
        return isOffline;
    }

    public void loadHeroDetail(long heroId) {
        loading.setValue(true);
        isOffline.setValue(false);

        heroRepository.getHeroDetail(heroId).observeForever(detail -> {
            loading.setValue(false);
            if (detail != null) {
                heroDetail.setValue(detail);
            } else {
                error.setValue("获取英雄详情失败");
            }
        });
    }
}
