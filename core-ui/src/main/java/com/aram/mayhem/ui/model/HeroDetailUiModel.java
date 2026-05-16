package com.aram.mayhem.ui.model;

import com.aram.mayhem.common.Tier;
import java.util.List;

/**
 * 英雄详情 UI 模型
 *
 * 数据流向：HeroDetailResponse → HeroDetailUiModel → HeroDetailFragment
 * 用途：英雄详情页展示（含技能、克制、协同、推荐出装、推荐强化符文）
 */
public class HeroDetailUiModel {

    private final long id;
    private final String nameZh;
    private final String nameEn;
    private final String title;
    private final String role;
    private final Tier tier;
    private final double winRate;
    private final double pickRate;
    private final String description;
    private final List<SkillUiModel> skills;
    private final List<String> counterTips;
    private final List<String> synergies;
    private final double avgKills;
    private final double avgDeaths;
    private final double avgAssists;
    private final String recommendedBuild;
    private final List<Long> recommendedAugmentIds;
    private final String imageUrl;
    private final boolean versionTrap;

    public HeroDetailUiModel(long id, String nameZh, String nameEn, String title, String role,
                             Tier tier, double winRate, double pickRate, String description,
                             List<SkillUiModel> skills, List<String> counterTips, List<String> synergies,
                             double avgKills, double avgDeaths, double avgAssists,
                             String recommendedBuild, List<Long> recommendedAugmentIds,
                             String imageUrl, boolean versionTrap) {
        this.id = id;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.title = title;
        this.role = role;
        this.tier = tier;
        this.winRate = winRate;
        this.pickRate = pickRate;
        this.description = description;
        this.skills = skills;
        this.counterTips = counterTips;
        this.synergies = synergies;
        this.avgKills = avgKills;
        this.avgDeaths = avgDeaths;
        this.avgAssists = avgAssists;
        this.recommendedBuild = recommendedBuild;
        this.recommendedAugmentIds = recommendedAugmentIds;
        this.imageUrl = imageUrl;
        this.versionTrap = versionTrap;
    }

    public long getId() { return id; }
    public String getNameZh() { return nameZh; }
    public String getNameEn() { return nameEn; }
    public String getTitle() { return title; }
    public String getRole() { return role; }
    public Tier getTier() { return tier; }
    public double getWinRate() { return winRate; }
    public double getPickRate() { return pickRate; }
    public String getDescription() { return description; }
    public List<SkillUiModel> getSkills() { return skills; }
    public List<String> getCounterTips() { return counterTips; }
    public List<String> getSynergies() { return synergies; }
    public double getAvgKills() { return avgKills; }
    public double getAvgDeaths() { return avgDeaths; }
    public double getAvgAssists() { return avgAssists; }
    public String getRecommendedBuild() { return recommendedBuild; }
    public List<Long> getRecommendedAugmentIds() { return recommendedAugmentIds; }
    public String getImageUrl() { return imageUrl; }
    public boolean isVersionTrap() { return versionTrap; }

    public String getWinRateDisplay() {
        return String.format("%.1f%%", winRate);
    }

    public String getPickRateDisplay() {
        return String.format("%.1f%%", pickRate);
    }

    public String getKdaDisplay() {
        return String.format("%.1f / %.1f / %.1f", avgKills, avgDeaths, avgAssists);
    }

    public static class SkillUiModel {
        private final String key;
        private final String name;
        private final String description;

        public SkillUiModel(String key, String name, String description) {
            this.key = key;
            this.name = name;
            this.description = description;
        }

        public String getKey() { return key; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
}
