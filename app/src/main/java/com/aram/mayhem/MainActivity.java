package com.aram.mayhem;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.aram.mayhem.databinding.ActivityMainBinding;
import com.aram.mayhem.feature.hero.OnHeroSelectedListener;
import com.aram.mayhem.feature.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 主 Activity
 *
 * 功能：底部导航栏管理、Fragment 容器、英雄选中导航回调、退出登录后自动切换到个人中心 Tab
 * 导航：英雄列表 → 英雄详情、符文列表、公告列表、社区攻略、个人中心
 * 关联：OnHeroSelectedListener, NavController, BottomNavigationView
 *
 * @see OnHeroSelectedListener
 * @see ProfileFragment
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements OnHeroSelectedListener {

    /** 视图绑定对象 */
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = binding.bottomNavigation;
        NavigationUI.setupWithNavController(bottomNav, navController);

        // 退出登录后重启应用时，自动切换到个人中心 Tab
        if (getIntent().getBooleanExtra(ProfileFragment.EXTRA_LOGGED_OUT, false)) {
            bottomNav.setSelectedItemId(R.id.navigation_profile);
        }
    }

    /**
     * 英雄选中回调
     *
     * 作用：从英雄列表点击英雄后，导航到英雄详情页并传递 heroId
     *
     * @param heroId 选中的英雄 ID
     */
    @Override
    public void onHeroSelected(long heroId) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            Bundle args = new Bundle();
            args.putLong("heroId", heroId);
            navController.navigate(R.id.navigation_hero_detail, args);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
