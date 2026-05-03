package com.aram.mayhem.data.di;

import android.content.Context;

import com.aram.mayhem.data.local.AppDatabase;
import com.aram.mayhem.data.local.dao.AugmentDao;
import com.aram.mayhem.data.local.dao.HeroDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class DataModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(Context context) {
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
