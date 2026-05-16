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
/**
 * 应用级 Hilt 依赖注入模块
 *
 * 提供：应用级单例服务（如 TokenStore 等）
 * 生命周期：SingletonComponent（应用级单例）
 */
public class AppModule {

    @Provides
    @Singleton
    public TokenStore provideTokenStore(@ApplicationContext Context context) {
        return new TokenStore(context);
    }
}