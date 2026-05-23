package com.aram.mayhem.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.aram.mayhem.data.local.dao.HeroDao;
import com.aram.mayhem.data.local.dao.AugmentDao;
import com.aram.mayhem.data.local.dao.StrategyDao;
import com.aram.mayhem.data.local.dao.BulletinDao;
import com.aram.mayhem.data.local.entity.HeroEntity;
import com.aram.mayhem.data.local.entity.AugmentEntity;
import com.aram.mayhem.data.local.entity.StrategyEntity;
import com.aram.mayhem.data.local.entity.BulletinEntity;

/**
 * Room 本地数据库 —— 应用的离线数据存储核心
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这是整个 Android 应用的本地数据库，基于 Google 的 Room 持久化库构建。
 * Room 是 SQLite 的封装层，让开发者用面向对象的方式操作数据库，
 * 而不需要手写 SQL 建表语句和繁琐的 Cursor 操作。
 *
 * 本数据库存储 4 类数据（对应 4 张表）：
 * 1. heroes    → 英雄数据（胜率、出装、技能等）
 * 2. augments  → 符文数据（品质、套装、效果等）
 * 3. strategies → 社区攻略数据（标题、内容、投票等）
 * 4. bulletins → 公告数据（版本更新、活动通知等）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、Room 三大组件
 * ═══════════════════════════════════════════════════════════════════
 *
 * Room 由三个核心组件构成，本类是第一个：
 *
 * 1. Database（本类 AppDatabase）：
 *    - 数据库入口，定义表和版本号
 *    - 提供 DAO 对象的获取方法
 *    - 管理数据库连接和迁移
 *
 * 2. Entity（HeroEntity, AugmentEntity 等）：
 *    - 数据表的结构定义，一个 Entity 类 = 一张表
 *    - 类的字段 = 表的列
 *    - 用注解标记主键、索引、表名等
 *
 * 3. DAO（HeroDao, AugmentDao 等）：
 *    - 数据访问对象，定义对表的增删改查操作
 *    - 用 @Query 注解写 SQL，用 @Insert/@Delete 注解做简单操作
 *    - Room 在编译期检查 SQL 语法，避免运行时错误
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、@Database 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Database 注解告诉 Room：这个抽象类是数据库定义。
 *
 * - entities：声明数据库包含哪些表（对应哪些 Entity 类）
 *   这里声明了 4 张表：HeroEntity, AugmentEntity, StrategyEntity, BulletinEntity
 *
 * - version：数据库版本号，每次修改表结构必须 +1
 *   当前版本 = 6，表示经历了 6 次表结构变更
 *   版本号用于数据库迁移（Migration），详见 AppDatabaseMigrations
 *
 * - exportSchema：是否导出数据库 schema JSON 文件
 *   设为 true 时，Room 会在编译时生成 schema 文件，
 *   用于版本对比和迁移测试。建议保持 true。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、单例模式说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * getInstance() 使用"双重检查锁定"（Double-Checked Locking）实现单例：
 *
 * 1. 第一次检查（不加锁）：如果实例已存在，直接返回，避免不必要的同步开销
 * 2. 加锁：多线程同时通过第一次检查时，只有一个线程能创建实例
 * 3. 第二次检查（加锁后）：确保只有一个实例被创建
 *
 * volatile 关键字保证 INSTANCE 的可见性：
 * 一个线程修改 INSTANCE 后，其他线程能立即看到最新值，
 * 避免其他线程使用未初始化完成的"半成品"对象。
 *
 * 为什么用单例？
 * - RoomDatabase 实例创建开销大（打开数据库连接、初始化缓存等）
 * - 整个应用只需要一个数据库连接，避免内存浪费和锁竞争
 * - 多个实例可能导致数据不一致
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据库文件位置
 * ═══════════════════════════════════════════════════════════════════
 *
 * Room 创建的 SQLite 数据库文件位于：
 * /data/data/com.aram.mayhem/databases/aram_mayhem_db
 *
 * 数据库名称 "aram_mayhem_db" 在 Room.databaseBuilder() 中指定。
 * 使用 context.getApplicationContext() 确保：
 * - 即使 Activity 重建，数据库实例也不会丢失
 * - 避免内存泄漏（不持有 Activity 的 Context）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 写入方向（网络 → 本地）：
 * Retrofit API → Repository → DAO.insertAll() → Room → SQLite 文件
 *
 * 读取方向（本地 → UI）：
 * SQLite 文件 → Room → DAO.getAllXxx() → LiveData → ViewModel → UI
 *
 * 离线场景：
 * 无网络时，Repository 直接从 DAO 读取本地缓存数据，
 * 保证用户在离线状态下也能浏览英雄/符文/公告等信息。
 *
 * 关联类：
 * - AppDatabaseMigrations：数据库版本迁移策略
 * - DataModule：Hilt 依赖注入，提供 AppDatabase 单例
 * - HeroDao / AugmentDao / StrategyDao / BulletinDao：数据访问对象
 * - HeroEntity / AugmentEntity / StrategyEntity / BulletinEntity：数据表实体
 */
@Database(
        entities = {HeroEntity.class, AugmentEntity.class, StrategyEntity.class, BulletinEntity.class},
        version = 6,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * 单例实例 —— volatile 保证多线程可见性
     *
     * volatile 的作用：
     * 没有 volatile 时，线程 A 创建了 INSTANCE 但还没完全初始化，
     * 线程 B 可能读到非 null 但未初始化完成的 INSTANCE，导致崩溃。
     * volatile 确保写入 INSTANCE 的操作对其他线程立即可见。
     */
    private static volatile AppDatabase INSTANCE;

    /**
     * 获取英雄数据访问对象
     *
     * Room 会在编译时自动生成此抽象方法的实现（AppDatabase_Impl.java），
     * 开发者不需要手动实现。Room 通过反射创建 DAO 的代理对象。
     *
     * @return HeroDao 实例，用于英雄表的增删改查
     */
    public abstract HeroDao heroDao();

    /**
     * 获取符文数据访问对象
     *
     * @return AugmentDao 实例，用于符文表的增删改查
     */
    public abstract AugmentDao augmentDao();

    /**
     * 获取攻略数据访问对象
     *
     * @return StrategyDao 实例，用于攻略表的增删改查
     */
    public abstract StrategyDao strategyDao();

    /**
     * 获取公告数据访问对象
     *
     * @return BulletinDao 实例，用于公告表的增删改查
     */
    public abstract BulletinDao bulletinDao();

    /**
     * 获取数据库单例 —— 双重检查锁定（DCL）实现
     *
     * 执行流程：
     * 1. 检查 INSTANCE 是否为 null（快速路径，不加锁）
     * 2. 如果为 null，进入 synchronized 块
     * 3. 再次检查 INSTANCE 是否为 null（防止多线程重复创建）
     * 4. 使用 Room.databaseBuilder() 创建数据库实例
     * 5. 添加所有迁移策略（AppDatabaseMigrations.getAll()）
     *
     * Room.databaseBuilder() 参数说明：
     * - context.getApplicationContext()：使用应用级 Context，避免 Activity 泄漏
     * - AppDatabase.class：数据库类的 Class 对象，Room 用反射创建实例
     * - "aram_mayhem_db"：数据库文件名
     *
     * addMigrations() 的作用：
     * 告诉 Room 当数据库版本升级时，使用自定义的迁移策略，
     * 而不是默认的 fallbackToDestructiveMigration()（会删除所有数据）。
     *
     * @param context 应用上下文（推荐使用 ApplicationContext）
     * @return AppDatabase 单例实例
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "aram_mayhem_db"
                    )
                            .addMigrations(AppDatabaseMigrations.getAll())
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
