package com.aram.mayhem;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class MainActivityTest {

    private ActivityScenario<MainActivity> scenario;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void mainActivity_ShouldLaunchSuccessfully() {
        scenario.onActivity(activity -> assertNotNull(activity));
    }

    @Test
    public void mainActivity_ShouldHaveContentView() {
        scenario.onActivity(activity -> {
            View contentView = activity.findViewById(android.R.id.content);
            assertNotNull(contentView);
        });
    }

    @Test
    public void mainActivity_ShouldHaveBottomNavigation() {
        scenario.onActivity(activity -> {
            View navView = activity.findViewById(R.id.bottom_navigation);
            assertNotNull(navView);
        });
    }

    @Test
    public void mainActivity_ShouldHaveNavHostFragment() {
        scenario.onActivity(activity -> {
            View hostFragment = activity.findViewById(R.id.nav_host_fragment);
            assertNotNull(hostFragment);
        });
    }
}
