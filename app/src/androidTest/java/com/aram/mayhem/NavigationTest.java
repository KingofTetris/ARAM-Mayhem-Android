package com.aram.mayhem;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class NavigationTest {

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
    public void bottomNavigation_ShouldHave5Tabs() {
        scenario.onActivity(activity -> {
            BottomNavigationView navView = activity.findViewById(R.id.bottom_navigation);
            assertNotNull(navView);
            assertEquals(5, navView.getMenu().size());
        });
    }

    @Test
    public void bottomNavigation_FirstTabIsHeroes() {
        scenario.onActivity(activity -> {
            BottomNavigationView navView = activity.findViewById(R.id.bottom_navigation);
            assertEquals(R.id.navigation_heroes, navView.getMenu().getItem(0).getItemId());
        });
    }

    @Test
    public void bottomNavigation_SecondTabIsAugments() {
        scenario.onActivity(activity -> {
            BottomNavigationView navView = activity.findViewById(R.id.bottom_navigation);
            assertEquals(R.id.navigation_augments, navView.getMenu().getItem(1).getItemId());
        });
    }

    @Test
    public void bottomNavigation_ThirdTabIsCommunity() {
        scenario.onActivity(activity -> {
            BottomNavigationView navView = activity.findViewById(R.id.bottom_navigation);
            assertEquals(R.id.navigation_community, navView.getMenu().getItem(2).getItemId());
        });
    }

    @Test
    public void bottomNavigation_FourthTabIsBulletin() {
        scenario.onActivity(activity -> {
            BottomNavigationView navView = activity.findViewById(R.id.bottom_navigation);
            assertEquals(R.id.navigation_bulletin, navView.getMenu().getItem(3).getItemId());
        });
    }

    @Test
    public void bottomNavigation_FifthTabIsProfile() {
        scenario.onActivity(activity -> {
            BottomNavigationView navView = activity.findViewById(R.id.bottom_navigation);
            assertEquals(R.id.navigation_profile, navView.getMenu().getItem(4).getItemId());
        });
    }

    @Test
    public void bottomNavigation_DefaultSelectedIsHeroes() {
        scenario.onActivity(activity -> {
            BottomNavigationView navView = activity.findViewById(R.id.bottom_navigation);
            assertEquals(R.id.navigation_heroes, navView.getSelectedItemId());
        });
    }

    @Test
    public void mainActivity_ShouldLaunchSuccessfully() {
        scenario.onActivity(activity -> assertNotNull(activity));
    }

    @Test
    public void mainActivity_ShouldHaveContentView() {
        scenario.onActivity(activity -> {
            assertNotNull(activity.findViewById(android.R.id.content));
        });
    }

    @Test
    public void mainActivity_ShouldHaveNavHostFragment() {
        scenario.onActivity(activity -> {
            assertNotNull(activity.findViewById(R.id.nav_host_fragment));
        });
    }
}
