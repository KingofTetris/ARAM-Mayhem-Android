package com.aram.mayhem;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MayhemApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
