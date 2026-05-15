package com.aram.mayhem.network.di;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.data.local.TokenStore;
import com.aram.mayhem.network.api.AugmentApi;
import com.aram.mayhem.network.api.AuthApi;
import com.aram.mayhem.network.api.BulletinApi;
import com.aram.mayhem.network.api.CommunityApi;
import com.aram.mayhem.network.api.HeroApi;
import com.aram.mayhem.network.interceptor.AuthInterceptor;
import com.aram.mayhem.network.interceptor.TokenRefreshInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
                .setLenient()
                .create();
    }

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(
            AuthInterceptor authInterceptor,
            TokenRefreshInterceptor tokenRefreshInterceptor,
            HttpLoggingInterceptor loggingInterceptor
    ) {
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(tokenRefreshInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(Constants.CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    public AuthApi provideAuthApi(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }

    @Provides
    @Singleton
    public HeroApi provideHeroApi(Retrofit retrofit) {
        return retrofit.create(HeroApi.class);
    }

    @Provides
    @Singleton
    public AugmentApi provideAugmentApi(Retrofit retrofit) {
        return retrofit.create(AugmentApi.class);
    }

    @Provides
    @Singleton
    public CommunityApi provideCommunityApi(Retrofit retrofit) {
        return retrofit.create(CommunityApi.class);
    }

    @Provides
    @Singleton
    public BulletinApi provideBulletinApi(Retrofit retrofit) {
        return retrofit.create(BulletinApi.class);
    }
}
