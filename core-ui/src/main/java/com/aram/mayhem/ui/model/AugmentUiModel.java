package com.aram.mayhem.ui.model;

import com.aram.mayhem.ui.R;

import java.util.Locale;

public class AugmentUiModel {

    private final long id;
    private final String nameZh;
    private final String nameEn;
    private final String description;
    private final String quality;
    private final String synergySet;
    private final String synergySet2;
    private final String synergySet3;
    private final String iconUrl;
    private final double winRate;
    private final double pickRate;
    private final double avgPlacement;
    private final String tier;
    private final boolean isTrap;

    public AugmentUiModel(long id, String nameZh, String nameEn, String description,
                          String quality, String synergySet, String synergySet2, String synergySet3,
                          String iconUrl, double winRate, double pickRate, double avgPlacement,
                          String tier, boolean isTrap) {
        this.id = id;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.description = description;
        this.quality = quality;
        this.synergySet = synergySet;
        this.synergySet2 = synergySet2;
        this.synergySet3 = synergySet3;
        this.iconUrl = iconUrl;
        this.winRate = winRate;
        this.pickRate = pickRate;
        this.avgPlacement = avgPlacement;
        this.tier = tier;
        this.isTrap = isTrap;
    }

    public long getId() {
        return id;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getName() {
        return nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getDescription() {
        return description;
    }

    public String getQuality() {
        return quality;
    }

    public String getSynergySet() {
        return synergySet;
    }

    public String getSynergySet2() {
        return synergySet2;
    }

    public String getSynergySet3() {
        return synergySet3;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public double getWinRate() {
        return winRate;
    }

    public double getPickRate() {
        return pickRate;
    }

    public double getAvgPlacement() {
        return avgPlacement;
    }

    public String getTier() {
        return tier;
    }

    public boolean isTrap() {
        return isTrap;
    }

    public String getWinRateDisplay() {
        return String.format(Locale.getDefault(), "%.1f%%", winRate * 100);
    }

    public String getPickRateDisplay() {
        return String.format(Locale.getDefault(), "%.1f%%", pickRate * 100);
    }

    public String getAvgPlacementDisplay() {
        return String.format(Locale.getDefault(), "%.2f", avgPlacement);
    }

    public int getQualityColorRes() {
        if (quality == null) return R.color.quality_silver;
        switch (quality.toUpperCase()) {
            case "PRISMATIC":
                return R.color.quality_prismatic;
            case "GOLD":
            case "LEGENDARY":
                return R.color.quality_gold;
            default:
                return R.color.quality_silver;
        }
    }

    public String getSynergyDisplay() {
        return synergySet != null ? "套装：" + synergySet : "";
    }

    public String getQualityDisplay() {
        if (quality == null) return "银";
        switch (quality.toUpperCase()) {
            case "PRISMATIC":
                return "棱彩";
            case "GOLD":
            case "LEGENDARY":
                return "金";
            case "EPIC":
                return "紫";
            default:
                return "银";
        }
    }
}