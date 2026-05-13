package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

public class AugmentResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("nameZh")
    private String nameZh;

    @SerializedName("nameEn")
    private String nameEn;

    @SerializedName("quality")
    private String quality;

    @SerializedName("synergySet")
    private String synergySet;

    @SerializedName("iconUrl")
    private String iconUrl;

    @SerializedName("winRate")
    private Double winRate;

    @SerializedName("pickRate")
    private Double pickRate;

    @SerializedName("avgPlacement")
    private Double avgPlacement;

    @SerializedName("tier")
    private String tier;

    public Long getId() {
        return id;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getQuality() {
        return quality;
    }

    public String getSynergySet() {
        return synergySet;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public Double getWinRate() {
        return winRate;
    }

    public Double getPickRate() {
        return pickRate;
    }

    public Double getAvgPlacement() {
        return avgPlacement;
    }

    public String getTier() {
        return tier;
    }
}