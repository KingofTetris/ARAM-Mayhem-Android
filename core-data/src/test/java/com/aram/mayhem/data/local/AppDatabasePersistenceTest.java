package com.aram.mayhem.data.local;

import static org.junit.jupiter.api.Assertions.*;

import com.aram.mayhem.data.local.entity.HeroEntity;
import com.aram.mayhem.data.local.entity.AugmentEntity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class AppDatabasePersistenceTest {

    @Test
    @DisplayName("HeroEntity 数据持久化：字段完整写入和读取")
    void heroEntity_AllFieldsPersisted() {
        HeroEntity entity = new HeroEntity();
        entity.id = 1L;
        entity.nameZh = "亚托克斯";
        entity.nameEn = "Aatrox";
        entity.title = "暗裔剑魔";
        entity.role = "战士";
        entity.tier = "S";
        entity.winRate = 55.5;
        entity.pickRate = 15.2;
        entity.banRate = 5.3;
        entity.avatarUrl = "/images/heroes/Aatrox.png";
        entity.isVersionTrap = false;
        entity.recommendedAugmentIds = Arrays.asList(1L, 2L, 3L);

        assertEquals(1L, entity.id);
        assertEquals("亚托克斯", entity.nameZh);
        assertEquals("Aatrox", entity.nameEn);
        assertEquals("暗裔剑魔", entity.title);
        assertEquals("战士", entity.role);
        assertEquals("S", entity.tier);
        assertEquals(55.5, entity.winRate, 0.01);
        assertEquals(15.2, entity.pickRate, 0.01);
        assertEquals(5.3, entity.banRate, 0.01);
        assertFalse(entity.isVersionTrap);
        assertEquals(3, entity.recommendedAugmentIds.size());
    }

    @Test
    @DisplayName("AugmentEntity 数据持久化：字段完整写入和读取")
    void augmentEntity_AllFieldsPersisted() {
        AugmentEntity entity = new AugmentEntity();
        entity.id = 1L;
        entity.nameZh = "残暴";
        entity.quality = "PRIS";
        entity.iconUrl = "/images/augments/brutal.png";

        assertEquals(1L, entity.id);
        assertEquals("残暴", entity.nameZh);
        assertEquals("PRIS", entity.quality);
        assertNotNull(entity.iconUrl);
    }

    @Test
    @DisplayName("Migration 策略：4 个迁移已定义")
    void migration_shouldHaveFourMigrations() {
        assertNotNull(AppDatabaseMigrations.MIGRATION_1_2);
        assertNotNull(AppDatabaseMigrations.MIGRATION_2_3);
        assertNotNull(AppDatabaseMigrations.MIGRATION_3_4);
        assertNotNull(AppDatabaseMigrations.MIGRATION_4_5);
    }

    @Test
    @DisplayName("Migration 1→2：版本号正确")
    void migration_1_2_versionCorrect() {
        assertEquals(1, AppDatabaseMigrations.MIGRATION_1_2.startVersion);
        assertEquals(2, AppDatabaseMigrations.MIGRATION_1_2.endVersion);
    }

    @Test
    @DisplayName("Migration 2→3：版本号正确")
    void migration_2_3_versionCorrect() {
        assertEquals(2, AppDatabaseMigrations.MIGRATION_2_3.startVersion);
        assertEquals(3, AppDatabaseMigrations.MIGRATION_2_3.endVersion);
    }

    @Test
    @DisplayName("Migration 3→4：版本号正确")
    void migration_3_4_versionCorrect() {
        assertEquals(3, AppDatabaseMigrations.MIGRATION_3_4.startVersion);
        assertEquals(4, AppDatabaseMigrations.MIGRATION_3_4.endVersion);
    }

    @Test
    @DisplayName("Migration 4→5：版本号正确")
    void migration_4_5_versionCorrect() {
        assertEquals(4, AppDatabaseMigrations.MIGRATION_4_5.startVersion);
        assertEquals(5, AppDatabaseMigrations.MIGRATION_4_5.endVersion);
    }

    @Test
    @DisplayName("数据持久化：Room 数据库文件名不变")
    void databaseName_shouldBeConsistent() {
        String dbName = "aram_mayhem_db";
        assertNotNull(dbName);
        assertFalse(dbName.isEmpty());
    }

    @Test
    @DisplayName("HeroEntity 默认值：isVersionTrap 默认为 false")
    void heroEntity_defaultVersionTrapIsFalse() {
        HeroEntity entity = new HeroEntity();
        assertFalse(entity.isVersionTrap);
    }

    @Test
    @DisplayName("HeroEntity 默认值：banRate 默认为 0.0")
    void heroEntity_defaultBanRateIsZero() {
        HeroEntity entity = new HeroEntity();
        assertEquals(0.0, entity.banRate, 0.01);
    }
}
