package com.aram.mayhem.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.aram.mayhem.data.local.converter.SkillListConverter;

import java.util.List;

/**
 * 英雄本地缓存实体
 *
 * 对应表：heroes
 * 索引：tier, role, nameZh
 * 数据流向：Retrofit → HeroRepository → Room → HeroListViewModel → UI
 * 关联：HeroDao, SkillListConverter
 */
@Entity(tableName = "heroes", indices = {
        @Index(value = "tier"),
        @Index(value = "role"),
        @Index(value = "nameZh")
})
@TypeConverters(SkillListConverter.class)
public class HeroEntity {

    @PrimaryKey
    public long id;

    public String nameZh;

    public String nameEn;

    public String title;

    public String role;

    public String tier;

    public double winRate;

    public double pickRate;

    public String avatarUrl;

    public String description;

    public List<SkillData> skills;

    public List<String> counterTips;

    public List<String> synergies;

    public double avgKills;

    public double avgDeaths;

    public double avgAssists;

    public String recommendedBuild;

    public List<Long> recommendedAugmentIds;

    public List<AugmentBriefData> recommendedAugments;

    public boolean isTrap;

    public boolean isVersionTrap;

    public long updatedAt;

    public static class SkillData {
        public String key;
        public String name;
        public String description;
    }

    public static class AugmentBriefData {
        public long id;
        public String nameZh;
        public String quality;
        public String iconUrl;
    }
}
