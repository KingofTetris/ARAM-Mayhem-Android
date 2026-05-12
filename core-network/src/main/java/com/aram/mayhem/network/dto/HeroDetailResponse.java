package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HeroDetailResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("nameEn")
    private String nameEn;

    @SerializedName("nameZh")
    private String nameZh;

    @SerializedName("title")
    private String title;

    @SerializedName("role")
    private String role;

    @SerializedName("tier")
    private String tier;

    @SerializedName("winRate")
    private Double winRate;

    @SerializedName("pickRate")
    private Double pickRate;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("description")
    private String description;

    @SerializedName("skills")
    private List<SkillResponse> skills;

    @SerializedName("counterTips")
    private List<String> counterTips;

    @SerializedName("synergies")
    private List<String> synergies;

    @SerializedName("avgKills")
    private Double avgKills;

    @SerializedName("avgDeaths")
    private Double avgDeaths;

    @SerializedName("avgAssists")
    private Double avgAssists;

    @SerializedName("recommendedBuild")
    private String recommendedBuild;

    public Long getId() { return id; }
    public String getNameEn() { return nameEn; }
    public String getNameZh() { return nameZh; }
    public String getTitle() { return title; }
    public String getRole() { return role; }
    public String getTier() { return tier; }
    public Double getWinRate() { return winRate; }
    public Double getPickRate() { return pickRate; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public List<SkillResponse> getSkills() { return skills; }
    public List<String> getCounterTips() { return counterTips; }
    public List<String> getSynergies() { return synergies; }
    public Double getAvgKills() { return avgKills; }
    public Double getAvgDeaths() { return avgDeaths; }
    public Double getAvgAssists() { return avgAssists; }
    public String getRecommendedBuild() { return recommendedBuild; }

    public static class SkillResponse {
        @SerializedName("key")
        private String key;

        @SerializedName("name")
        private String name;

        @SerializedName("description")
        private String description;

        public String getKey() { return key; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
}
