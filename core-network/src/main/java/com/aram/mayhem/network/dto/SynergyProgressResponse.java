package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/** 套装进度响应模型（与后端 SynergyProgressResponse 对应） */
public class SynergyProgressResponse {

    @SerializedName("synergyName")
    private String synergyName;

    @SerializedName("currentCount")
    private int currentCount;

    @SerializedName("totalCount")
    private int totalCount;

    @SerializedName("progress")
    private double progress;

    @SerializedName("status")
    private String status;

    @SerializedName("avgWinRate")
    private Double avgWinRate;

    public String getSynergyName() {
        return synergyName;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public double getProgress() {
        return progress;
    }

    public String getStatus() {
        return status;
    }

    public Double getAvgWinRate() {
        return avgWinRate;
    }

    public int getProgressPercent() {
        return (int) (progress * 100);
    }

    public String getDisplayName() {
        if (synergyName == null) return "";
        switch (synergyName) {
            case "shield": return "护盾";
            case "regeneration": return "回复";
            case "shield-break": return "破盾";
            case "attack-speed": return "攻速";
            case "ability-power": return "法强";
            case "omnivamp": return "吸血";
            case "armor-penetration": return "护甲穿透";
            case "critical-strike": return "暴击";
            case "tenacity": return "韧性";
            default: return synergyName;
        }
    }
}