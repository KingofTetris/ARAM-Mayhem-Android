package com.aram.mayhem.data.di;

import android.content.Context;

import com.aram.mayhem.data.local.AppDatabase;
import com.aram.mayhem.data.local.dao.AugmentDao;
import com.aram.mayhem.data.local.dao.BulletinDao;
import com.aram.mayhem.data.local.dao.HeroDao;
import com.aram.mayhem.data.local.dao.StrategyDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * 数据层 Hilt 依赖注入模块 —— 集中提供数据库和 DAO 对象
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类是 Hilt 依赖注入框架的"工厂"，负责创建和提供数据层相关的对象：
 * 1. AppDatabase（数据库实例）→ 全局单例
 * 2. HeroDao（英雄数据访问对象）
 * 3. AugmentDao（符文数据访问对象）
 * 4. StrategyDao（攻略数据访问对象）
 * 5. BulletinDao（公告数据访问对象）
 *
 * 当 Repository 需要 DAO 来操作数据库时，不需要手动创建，
 * Hilt 会自动从本模块获取 DAO 实例并注入到 Repository 中。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、Hilt 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Module：标记这是一个 Hilt 模块，告诉 Hilt 这个类提供依赖对象
 *
 * @InstallIn(SingletonComponent.class)：
 *   - 指定本模块的依赖对象生命周期范围
 *   - SingletonComponent = 应用级单例，对象在整个应用生命周期内存在
 *   - 其他选项：ActivityComponent（Activity 级）、ViewModelComponent（ViewModel 级）
 *   - 数据库和 DAO 需要全局共享，所以用 SingletonComponent
 *
 * @Provides：标记一个方法为"依赖提供者"
 *   - Hilt 在需要该类型依赖时，会调用此方法获取实例
 *   - 方法返回类型就是提供的依赖类型
 *
 * @Singleton：标记提供的对象为单例
 *   - 整个应用只创建一个实例，所有注入点共享同一个对象
 *   - AppDatabase 必须是单例（避免多实例导致数据不一致）
 *   - DAO 不需要 @Singleton（Room 内部已保证线程安全）
 *
 * @ApplicationContext：
 *   - 限定符，告诉 Hilt 注入的是应用级 Context 而非 Activity Context
 *   - 避免持有 Activity 引用导致内存泄漏
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、依赖注入流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. HeroRepository 需要 HeroDao
 * 2. HeroRepository 构造函数标注 @Inject
 * 3. Hilt 发现 HeroRepository 需要 HeroDao
 * 4. Hilt 在 DataModule 中找到 provideHeroDao() 方法
 * 5. Hilt 调用 provideHeroDao(database) 获取 HeroDao 实例
 * 6. 但 provideHeroDao 需要 AppDatabase 参数
 * 7. Hilt 在 DataModule 中找到 provideAppDatabase() 方法
 * 8. Hilt 调用 provideAppDatabase(context) 获取 AppDatabase 实例
 * 9. 最终 Hilt 将 HeroDao 注入到 HeroRepository 中
 *
 * 整个过程自动完成，开发者不需要手动创建任何对象。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、为什么 DAO 不需要 @Singleton？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AppDatabase 标注了 @Singleton，因为数据库连接必须全局唯一。
 * 但 DAO 不需要 @Singleton，原因：
 * 1. DAO 是接口，Room 生成的实现类是无状态的（没有成员变量）
 * 2. 每次调用 database.heroDao() 返回的都是同一个 DAO 实例
 *    （Room 内部用 Map 缓存了 DAO 实例）
 * 3. 即使 Hilt 创建多个 DAO 实例，也不会有问题（它们操作同一个数据库）
 *
 * 关联类：
 * - AppDatabase：Room 数据库实例
 * - HeroDao / AugmentDao / StrategyDao / BulletinDao：数据访问对象
 * - HeroRepository / AugmentRepository 等：使用 DAO 的 Repository 类
 */
@Module
@InstallIn(SingletonComponent.class)
public class DataModule {

    /**
     * 提供 AppDatabase 单例 —— 整个应用共享一个数据库连接
     *
     * 为什么是 @Singleton？
     * - RoomDatabase 创建开销大（打开文件、初始化缓存、编译 SQL 等）
     * - 多个数据库实例会导致数据不一致和锁竞争
     * - 单例模式确保所有 DAO 操作同一个数据库连接
     *
     * @param context 应用级上下文，由 Hilt 自动注入
     * @return AppDatabase 单例实例
     */
    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return AppDatabase.getInstance(context);
    }

    /**
     * 提供 HeroDao —— 英雄数据访问对象
     *
     * 从 AppDatabase 中获取 HeroDao 实例。
     * AppDatabase 由上面的 provideAppDatabase() 方法提供。
     *
     * @param database AppDatabase 实例，由 Hilt 自动注入
     * @return HeroDao 实例
     */
    @Provides
    public HeroDao provideHeroDao(AppDatabase database) {
        return database.heroDao();
    }

    /**
     * 提供 AugmentDao —— 符文数据访问对象
     *
     * @param database AppDatabase 实例
     * @return AugmentDao 实例
     */
    @Provides
    public AugmentDao provideAugmentDao(AppDatabase database) {
        return database.augmentDao();
    }

    /**
     * 提供 StrategyDao —— 攻略数据访问对象
     *
     * @param database AppDatabase 实例
     * @return StrategyDao 实例
     */
    @Provides
    public StrategyDao provideStrategyDao(AppDatabase database) {
        return database.strategyDao();
    }

    /**
     * 提供 BulletinDao —— 公告数据访问对象
     *
     * @param database AppDatabase 实例
     * @return BulletinDao 实例
     */
    @Provides
    public BulletinDao provideBulletinDao(AppDatabase database) {
        return database.bulletinDao();
    }
}
