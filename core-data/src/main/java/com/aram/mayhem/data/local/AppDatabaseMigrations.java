package com.aram.mayhem.data.local;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room 数据库迁移策略 —— 确保版本升级时用户数据不丢失
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当 AppDatabase 的版本号增加时（如 v1→v2→v3...），
 * 已安装旧版本应用的用户手机上的数据库还是旧表结构。
 * 如果不处理，Room 会抛出 IllegalStateException 导致应用崩溃。
 *
 * Migration 的作用就是：告诉 Room 如何从旧版本升级到新版本，
 * 类似于给房子装修时"在墙上开个门"，而不是"把房子拆了重建"。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么不使用 fallbackToDestructiveMigration()？
 * ═══════════════════════════════════════════════════════════════════
 *
 * Room 提供了一个简单的方法：fallbackToDestructiveMigration()
 * 它的做法是：版本不匹配时，直接删除旧数据库，重新创建新数据库。
 *
 * 这就像"房子格局不对，直接炸掉重建"——简单粗暴，但所有家具（数据）都没了！
 *
 * 对于本应用来说，这是不可接受的：
 * - 用户离线时依赖本地缓存浏览英雄/符文数据
 * - 删除数据库意味着用户下次打开应用看到空白页面
 * - 必须用 Migration 保留数据，只修改表结构
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、Migration 工作原理
 * ═══════════════════════════════════════════════════════════════════
 *
 * 每个 Migration 对象包含：
 * - startVersion：迁移的起始版本号
 * - endVersion：迁移的目标版本号
 * - migrate()：执行迁移的 SQL 语句
 *
 * Room 会按版本号顺序执行迁移链：
 * 例如用户手机上是 v1，当前版本是 v6，Room 会依次执行：
 * MIGRATION_1_2 → MIGRATION_2_3 → MIGRATION_3_4 → MIGRATION_4_5 → MIGRATION_5_6
 *
 * 如果中间缺少某个版本的 Migration，Room 会抛出异常。
 * 所以每个版本升级都必须有对应的 Migration。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、ALTER TABLE 语句说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 大部分迁移使用 ALTER TABLE ADD COLUMN 语句添加新列：
 * - ALTER TABLE heroes ADD COLUMN `title` TEXT
 *   → 在 heroes 表中新增 title 列，类型为 TEXT
 *
 * 注意事项：
 * 1. 新增列不能有 NOT NULL 约束（除非指定 DEFAULT 值）
 *    因为已有行的该列值为 NULL，如果声明 NOT NULL 会报错
 * 2. SQLite 的 ALTER TABLE 只支持 ADD COLUMN，不支持 DROP COLUMN
 *    如果需要删除列，只能创建新表 + 数据迁移 + 删除旧表 + 重命名
 * 3. 字段名用反引号 `` 包裹，避免与 SQLite 保留字冲突
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、迁移历史
 * ═══════════════════════════════════════════════════════════════════
 *
 * v1→v2: heroes 新增 title/description/skills/counterTips/synergies/avgKDA/recommendedBuild
 * v2→v3: heroes 新增 isVersionTrap
 * v3→v4: heroes 新增 recommendedAugmentIds/recommendedAugments
 * v4→v5: heroes 新增 banRate；augments 新增 nameEn/descriptionDetail
 * v5→v6: 新增 strategies 表和 bulletins 表（社区+公告功能上线）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、新增版本的步骤
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当需要修改表结构时，必须按以下步骤操作：
 * 1. 在 Entity 类中添加新字段
 * 2. 将 AppDatabase.version + 1
 * 3. 在本类中新增 MIGRATION_X_Y（X=旧版本，Y=新版本）
 * 4. 在 getAll() 数组中添加新的 Migration
 * 5. 在 AppDatabase.getInstance() 中确保 addMigrations() 包含新 Migration
 * 6. 运行数据库迁移测试验证
 *
 * 关联类：
 * - AppDatabase：数据库定义，version 字段和 addMigrations() 调用
 * - HeroEntity / AugmentEntity / StrategyEntity / BulletinEntity：表结构定义
 */
public final class AppDatabaseMigrations {

    /**
     * 私有构造函数 —— 工具类不允许实例化
     *
     * 所有 Migration 都是静态常量，通过类名直接访问：
     * AppDatabaseMigrations.MIGRATION_1_2
     * AppDatabaseMigrations.getAll()
     *
     * 如果允许 new AppDatabaseMigrations()，会让人误以为需要创建实例才能使用，
     * 但实际上所有方法都是静态的，实例化没有意义。
     */
    private AppDatabaseMigrations() {
    }

    /**
     * 迁移 v1 → v2：heroes 表新增英雄详情字段
     *
     * 新增字段说明：
     * - title：英雄称号（如"疾风剑豪"）
     * - description：英雄描述文本
     * - skills：技能列表（JSON 格式，通过 SkillListConverter 转换）
     * - counterTips：克制提示列表（JSON 格式）
     * - synergies：配合建议列表（JSON 格式）
     * - avgKills/avgDeaths/avgAssists：平均 KDA 数据
     * - recommendedBuild：推荐出装（JSON 格式）
     *
     * 注意：REAL NOT NULL DEFAULT 0.0 表示：
     * - REAL = 浮点数类型（SQLite 中对应 double）
     * - NOT NULL = 不允许为空
     * - DEFAULT 0.0 = 旧行中该列默认值为 0.0（必须指定，否则 NOT NULL 会报错）
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE heroes ADD COLUMN `title` TEXT");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `description` TEXT");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `skills` TEXT");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `counterTips` TEXT");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `synergies` TEXT");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `avgKills` REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `avgDeaths` REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `avgAssists` REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `recommendedBuild` TEXT");
        }
    };

    /**
     * 迁移 v2 → v3：heroes 表新增版本陷阱标记
     *
     * isVersionTrap 字段说明：
     * - 标记该英雄是否为"版本陷阱"（看似很强但实际胜率低）
     * - INTEGER NOT NULL DEFAULT 0：SQLite 中用 0/1 表示布尔值
     *   0 = false（不是版本陷阱），1 = true（是版本陷阱）
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE heroes ADD COLUMN `isVersionTrap` INTEGER NOT NULL DEFAULT 0");
        }
    };

    /**
     * 迁移 v3 → v4：heroes 表新增推荐符文字段
     *
     * 新增字段说明：
     * - recommendedAugmentIds：推荐符文 ID 列表（JSON 格式，如 [1,2,3]）
     * - recommendedAugments：推荐符文简要信息列表（JSON 格式，含 id/name/quality/iconUrl）
     *
     * 这两个字段用于英雄详情页的"推荐符文"区域展示。
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE heroes ADD COLUMN `recommendedAugmentIds` TEXT");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `recommendedAugments` TEXT");
        }
    };

    /**
     * 迁移 v4 → v5：heroes 表新增 banRate，augments 表新增英文名和详细描述
     *
     * 新增字段说明：
     * - heroes.banRate：英雄被禁用率（如 12.5%）
     * - augments.nameEn：符文英文名称（如 "Pristine"）
     * - augments.descriptionDetail：符文详细描述（含数值加成等）
     */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE heroes ADD COLUMN `banRate` REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE augments ADD COLUMN `nameEn` TEXT");
            database.execSQL("ALTER TABLE augments ADD COLUMN `descriptionDetail` TEXT");
        }
    };

    /**
     * 迁移 v5 → v6：新增 strategies 表和 bulletins 表
     *
     * 这是最大的一次迁移，因为社区和公告功能上线，需要两张新表。
     *
     * strategies 表（社区攻略）：
     * - id：攻略 ID（主键）
     * - userId/authorNickname/authorAvatar：作者信息
     * - heroId/heroName/heroIcon：关联英雄信息
     * - title/description：攻略标题和内容
     * - upvotes/downvotes/score：投票数据
     * - createdAt/updatedAt：时间戳
     * - augmentIcons/itemIcons：符文和装备图标（JSON 格式）
     * - 索引：heroId（按英雄筛选）、score（按热度排序）
     *
     * bulletins 表（公告）：
     * - id：公告 ID（主键）
     * - type：公告类型（version/event/notice）
     * - title/content：标题和正文
     * - imageUrl：封面图
     * - isPinned：是否置顶（0=否，1=是）
     * - publishedAt/createdAt/updatedAt：时间戳
     * - 索引：type（按类型筛选）、isPinned（置顶排序）
     *
     * CREATE TABLE IF NOT EXISTS 的作用：
     * 如果表已存在则跳过，避免重复创建报错。
     * 这在迁移场景中更安全，虽然正常迁移时表不应该已存在。
     */
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `strategies` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`userId` INTEGER NOT NULL, " +
                    "`authorNickname` TEXT, " +
                    "`authorAvatar` TEXT, " +
                    "`heroId` INTEGER NOT NULL, " +
                    "`heroName` TEXT, " +
                    "`heroIcon` TEXT, " +
                    "`title` TEXT, " +
                    "`description` TEXT, " +
                    "`upvotes` INTEGER NOT NULL DEFAULT 0, " +
                    "`downvotes` INTEGER NOT NULL DEFAULT 0, " +
                    "`score` INTEGER NOT NULL DEFAULT 0, " +
                    "`createdAt` TEXT, " +
                    "`augmentIcons` TEXT, " +
                    "`itemIcons` TEXT, " +
                    "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_strategies_heroId` ON `strategies` (`heroId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_strategies_score` ON `strategies` (`score`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `bulletins` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`type` TEXT, " +
                    "`title` TEXT, " +
                    "`content` TEXT, " +
                    "`imageUrl` TEXT, " +
                    "`isPinned` INTEGER NOT NULL DEFAULT 0, " +
                    "`publishedAt` TEXT, " +
                    "`createdAt` TEXT, " +
                    "`updatedAt` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_bulletins_type` ON `bulletins` (`type`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_bulletins_isPinned` ON `bulletins` (`isPinned`)");
        }
    };

    /**
     * 获取所有迁移策略 —— 供 AppDatabase.addMigrations() 使用
     *
     * Room 会根据用户手机上的旧版本号，自动选择需要执行的迁移链。
     * 例如用户从 v2 升级到 v6，Room 会执行：
     * MIGRATION_2_3 → MIGRATION_3_4 → MIGRATION_4_5 → MIGRATION_5_6
     *
     * 新增版本时，必须在此数组中追加新的 Migration 对象。
     *
     * @return 所有迁移策略的数组，按版本号顺序排列
     */
    public static Migration[] getAll() {
        return new Migration[]{
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6
        };
    }
}
