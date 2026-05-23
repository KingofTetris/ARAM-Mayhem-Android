package com.aram.mayhem.feature.hero;

/**
 * 英雄选中回调接口 ── Fragment 之间的导航通信桥梁
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当用户在英雄列表页点击某个英雄卡片时，需要跳转到英雄详情页。
 * 但 Fragment 不能直接操作其他 Fragment，必须通过宿主 Activity 来中转。
 * OnHeroSelectedListener 就是这个"中转协议"。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、通信流程
 * ═══════════════════════════════════════════════════════════════════
 *
 *   HeroListFragment          MainActivity           HeroDetailFragment
 *   ┌──────────────┐         ┌──────────────┐       ┌──────────────────┐
 *   │ 用户点击卡片  │         │  实现         │       │                  │
 *   │ heroId=42    │ ──────→ │  接口         │ ────→ │ 接收 heroId=42   │
 *   │              │  调用    │  导航到       │  参数  │ 加载英雄详情     │
 *   │ onHeroSelected(42)     │  DetailFragment│       │                  │
 *   └──────────────┘         └──────────────┘       └──────────────────┘
 *
 * 步骤详解：
 * 1. HeroListFragment 检测到用户点击
 * 2. 调用 heroSelectedListener.onHeroSelected(42)
 * 3. MainActivity 实现了此接口，收到回调
 * 4. MainActivity 执行导航：NavController.navigate(R.id.heroDetailFragment, bundle)
 * 5. HeroDetailFragment 从 bundle 中取出 heroId，加载详情数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、为什么用接口而不用直接引用？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 方案 A（直接引用，不推荐）：
 *   HeroListFragment 直接持有 HeroDetailFragment 的引用
 *   问题：两个 Fragment 强耦合，无法独立复用
 *
 * 方案 B（接口回调，推荐 ✓）：
 *   HeroListFragment 只知道"有人会处理英雄选中事件"
 *   不关心具体是谁、怎么处理
 *   好处：解耦，Fragment 可以在不同 Activity 中复用
 *
 * 这是 Android 官方推荐的 Fragment 通信方式：
 * https://developer.android.com/guide/fragments/communicate
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、实现方式
 * ═══════════════════════════════════════════════════════════════════
 *
 * MainActivity 实现此接口：
 *
 *   public class MainActivity extends AppCompatActivity
 *           implements OnHeroSelectedListener {
 *
 *       @Override
 *       public void onHeroSelected(long heroId) {
 *           // 创建 Bundle 携带 heroId 参数
 *           Bundle args = new Bundle();
 *           args.putLong("heroId", heroId);
 *
 *           // 使用 Navigation 组件导航到详情页
 *           NavController navController = Navigation.findNavController(this, R.id.nav_host);
 *           navController.navigate(R.id.heroDetailFragment, args);
 *       }
 *   }
 *
 * HeroListFragment 在 onAttach 中获取接口实现：
 *
 *   public void onAttach(Context context) {
 *       super.onAttach(context);
 *       if (context instanceof OnHeroSelectedListener) {
 *           heroSelectedListener = (OnHeroSelectedListener) context;
 *       }
 *   }
 */
public interface OnHeroSelectedListener {

    /**
     * 英雄被选中时的回调方法
     *
     * 当用户在英雄列表中点击某个英雄卡片时触发。
     * 实现者（通常是 MainActivity）负责导航到英雄详情页。
     *
     * @param heroId 被选中英雄的唯一标识符（数据库主键）
     *               例如：42 表示 ID 为 42 的英雄
     */
    void onHeroSelected(long heroId);
}
