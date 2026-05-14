package com.aram.mayhem.di;

import android.content.Context;

import com.aram.mayhem.data.local.TokenStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public TokenStore provideTokenStore(@ApplicationContext Context context) {
        return new TokenStore(context);
    }
}