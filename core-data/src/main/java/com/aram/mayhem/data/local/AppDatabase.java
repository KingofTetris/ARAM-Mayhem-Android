package com.aram.mayhem.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.aram.mayhem.data.local.dao.HeroDao;
import com.aram.mayhem.data.local.dao.AugmentDao;
import com.aram.mayhem.data.local.entity.HeroEntity;
import com.aram.mayhem.data.local.entity.AugmentEntity;

/**
 * Room 本地数据库
 *
 * 表：heroes, augments
 * 版本：5（使用正式 Migration 策略，禁止 fallbackToDestructiveMigration）
 * 单例模式：双重检查锁
 * 关联：HeroDao, AugmentDao, HeroEntity, AugmentEntity, AppDatabaseMigrations
 */
@Database(
        entities = {HeroEntity.class, AugmentEntity.class},
        version = 5,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract HeroDao heroDao();

    public abstract AugmentDao augmentDao();

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
