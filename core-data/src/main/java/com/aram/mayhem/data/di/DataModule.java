package com.aram.mayhem.data.di;

import android.content.Context;

import com.aram.mayhem.data.local.AppDatabase;
import com.aram.mayhem.data.local.dao.AugmentDao;
import com.aram.mayhem.data.local.dao.HeroDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * 数据层 Hilt 依赖注入模块
 *
 * 提供：AppDatabase（单例）、HeroDao、AugmentDao
 * 生命周期：SingletonComponent（应用级单例）
 */
@Module
@InstallIn(SingletonComponent.class)
public class DataModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return AppDatabase.getInstance(context);
    }

    @Provides
    public HeroDao provideHeroDao(AppDatabase database) {
        return database.heroDao();
    }

    @Provides
    public AugmentDao provideAugmentDao(AppDatabase database) {
        return database.augmentDao();
    }
}
