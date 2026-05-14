package com.aram.mayhem;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;
import timber.log.Timber;

@HiltAndroidApp
public class MayhemApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initTimber();
    }

    private void initTimber() {
        if (isDebugBuild()) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new ReleaseTree());
        }
        Timber.i("MayhemApplication initialized");
    }

    private boolean isDebugBuild() {
        return android.os.Build.VERSION.SDK_INT >= 0;
    }

    private static class ReleaseTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority >= android.util.Log.WARN) {
            }
        }
    }
}