package com.aram.mayhem;

import android.os.Bundle;

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
 * 主 Activity —— 应用的入口界面，管理底部导航栏和 Fragment 容器
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这是整个 Android 应用的主界面，包含：
 * 1. 底部导航栏（BottomNavigationView）：5个 Tab 页切换
 * 2. Fragment 容器（NavHostFragment）：承载各功能页面
 * 3. 英雄选中导航回调（OnHeroSelectedListener）：从列表跳转到详情
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、底部导航栏结构
 * ═══════════════════════════════════════════════════════════════════
 *
 * 5个 Tab 页（从左到右）：
 * 1. 英雄列表（Heroes）→ HeroListFragment
 * 2. 符文推荐（Augments）→ AugmentListFragment
 * 3. 公告（Bulletins）→ BulletinListFragment
 * 4. 社区（Community）→ CommunityFeedFragment
 * 5. 个人中心（Profile）→ ProfileFragment
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、导航机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 使用 Jetpack Navigation Component 管理页面导航：
 * - NavController：导航控制器，负责页面跳转
 * - NavHostFragment：承载导航图的 Fragment 容器
 * - NavigationUI：将 BottomNavigationView 与 NavController 绑定
 *
 * 导航图定义在 res/navigation/navigation_graph.xml 中
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、@AndroidEntryPoint 说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * Hilt 依赖注入的入口标记，告诉 Hilt 这个 Activity 需要依赖注入。
 * 加上此注解后，Activity 中可以使用 @Inject 注入依赖。
 *
 * @see OnHeroSelectedListener 英雄选中回调接口
 * @see ProfileFragment 个人中心页面
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements OnHeroSelectedListener {

    /**
     * 视图绑定对象 —— 替代 findViewById 的类型安全方式
     *
     * ViewBinding 会为每个 XML 布局自动生成一个 Binding 类，
     * 可以直接通过 binding.textView 这样的方式访问视图，无需强制类型转换。
     *
     * 在 onDestroy 中设为 null，避免内存泄漏
     */
    private ActivityMainBinding binding;

    /**
     * Activity 创建回调 —— 初始化视图、导航控制器、底部导航栏
     *
     * 执行流程：
     * 1. 调用 super.onCreate() 完成父类初始化
     * 2. 使用 ViewBinding 加载布局（替代 setContentView(R.layout.xxx)）
     * 3. 获取 NavHostFragment（承载导航图的 Fragment 容器）
     * 4. 获取 NavController（导航控制器）
     * 5. 将 BottomNavigationView 与 NavController 绑定
     * 6. 检查是否从退出登录跳转（自动切换到个人中心 Tab）
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }
        NavController navController = navHostFragment.getNavController();

        BottomNavigationView bottomNav = binding.bottomNavigation;
        NavigationUI.setupWithNavController(bottomNav, navController);

        if (getIntent().getBooleanExtra(ProfileFragment.EXTRA_LOGGED_OUT, false)) {
            bottomNav.setSelectedItemId(R.id.navigation_profile);
        }
    }

    /**
     * 英雄选中回调 —— 从英雄列表页跳转到英雄详情页
     *
     * 当用户在 HeroListFragment 中点击某个英雄卡片时，
     * 通过 OnHeroSelectedListener 接口回调到此方法，
     * 然后使用 NavController 导航到 HeroDetailFragment，并传递 heroId。
     *
     * 导航流程：
     * HeroListFragment → onHeroSelected(heroId) → MainActivity → NavController.navigate()
     *
     * 为什么不直接在 Fragment 中导航？
     * - 因为 HeroDetailFragment 不在底部导航栏的 Tab 中
     * - 需要通过 NavController 进行跨 Tab 导航
     * - 由 Activity 统一管理导航逻辑，避免 Fragment 之间的直接耦合
     *
     * @param heroId 选中的英雄 ID（数据库主键）
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

    /**
     * Activity 销毁回调 —— 清理视图绑定引用，防止内存泄漏
     *
     * 将 binding 设为 null，避免 Activity 销毁后仍持有视图引用。
     * 这在使用 ViewBinding 时是标准做法，因为 Binding 对象持有 View 的引用，
     * 而 View 又持有 Activity 的 Context，如果不清理会导致 Activity 无法被 GC 回收。
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
