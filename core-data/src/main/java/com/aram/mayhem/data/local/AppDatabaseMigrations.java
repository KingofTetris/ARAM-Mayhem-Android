package com.aram.mayhem.data.local;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room 数据库迁移策略
 *
 * 替代 fallbackToDestructiveMigration()，确保用户数据在版本升级时不丢失
 * 每次版本升级必须添加对应的 Migration，禁止使用破坏性迁移
 *
 * 迁移历史：
 * v1→v2: heroes 新增 title/description/skills/counterTips/synergies/avgKDA/recommendedBuild
 * v2→v3: heroes 新增 isVersionTrap
 * v3→v4: heroes 新增 recommendedAugmentIds/recommendedAugments
 * v4→v5: heroes 新增 banRate；augments 新增 nameEn/descriptionDetail
 */
public final class AppDatabaseMigrations {

    private AppDatabaseMigrations() {
    }

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

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE heroes ADD COLUMN `isVersionTrap` INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE heroes ADD COLUMN `recommendedAugmentIds` TEXT");
            database.execSQL("ALTER TABLE heroes ADD COLUMN `recommendedAugments` TEXT");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE heroes ADD COLUMN `banRate` REAL NOT NULL DEFAULT 0.0");
            database.execSQL("ALTER TABLE augments ADD COLUMN `nameEn` TEXT");
            database.execSQL("ALTER TABLE augments ADD COLUMN `descriptionDetail` TEXT");
        }
    };

    public static Migration[] getAll() {
        return new Migration[]{
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5
        };
    }
}
