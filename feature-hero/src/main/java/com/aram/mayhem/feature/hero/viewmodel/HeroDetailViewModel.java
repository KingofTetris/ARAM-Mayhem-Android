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

/**
 * 英雄详情 ViewModel（英雄模块）
 *
 * 功能：加载英雄详情数据、管理加载状态和错误处理
 * 数据流：HeroRepository → LiveData<HeroDetailUiModel> → HeroDetailFragment
 *
 * @see HeroRepository
 * @see com.aram.mayhem.feature.hero.HeroDetailFragment
 */
@HiltViewModel
public class HeroDetailViewModel extends AndroidViewModel {

    /** 英雄数据仓库 */
    private final HeroRepository heroRepository;

    /** 英雄详情数据 */
    private final MutableLiveData<HeroDetailUiModel> heroDetail = new MutableLiveData<>();
    /** 是否正在加载 */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 错误信息 */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    /** 是否处于离线模式 */
    private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

    /**
     * 构造函数
     *
     * @param application Android 应用上下文
     * @param heroRepository 英雄数据仓库（通过 Hilt 依赖注入）
     */
    @Inject
    public HeroDetailViewModel(@NonNull Application application, HeroRepository heroRepository) {
        super(application);
        this.heroRepository = heroRepository;
    }

    /**
     * 获取英雄详情的可观察数据
     *
     * @return LiveData<HeroDetailUiModel> 英雄详情
     */
    public LiveData<HeroDetailUiModel> getHeroDetail() {
        return heroDetail;
    }

    /**
     * 获取加载状态的可观察数据
     *
     * @return LiveData<Boolean> 是否正在加载
     */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * 获取错误信息的可观察数据
     *
     * @return LiveData<String> 错误信息
     */
    public LiveData<String> getError() {
        return error;
    }

    /**
     * 获取离线状态的可观察数据
     *
     * @return LiveData<Boolean> 是否处于离线模式
     */
    public LiveData<Boolean> getIsOffline() {
        return isOffline;
    }

    /**
     * 加载英雄详情
     *
     * @param heroId 英雄ID
     */
    public void loadHeroDetail(long heroId) {
        // 设置加载状态
        loading.setValue(true);
        isOffline.setValue(false);

        // 调用 Repository 获取英雄详情
        heroRepository.getHeroDetail(heroId).observeForever(detail -> {
            // 加载完成，更新状态
            loading.setValue(false);
            if (detail != null) {
                // 加载成功
                heroDetail.setValue(detail);
            } else {
                // 加载失败
                error.setValue("获取英雄详情失败");
            }
        });
    }
}