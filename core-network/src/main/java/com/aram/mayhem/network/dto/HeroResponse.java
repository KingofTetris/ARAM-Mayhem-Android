package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/** 英雄列表项响应模型（与后端 HeroListVO 对应） */
public class HeroResponse {

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

    public Long getId() {
        return id;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getTitle() {
        return title;
    }

    public String getRole() {
        return role;
    }

    public String getTier() {
        return tier;
    }

    public Double getWinRate() {
        return winRate;
    }

    public Double getPickRate() {
        return pickRate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDescription() {
        return description;
    }
}
