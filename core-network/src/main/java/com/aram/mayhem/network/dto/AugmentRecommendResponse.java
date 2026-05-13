package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

public class AugmentRecommendResponse extends AugmentResponse {

    @SerializedName("score")
    private double score;

    @SerializedName("recommendationReason")
    private String recommendationReason;

    public double getScore() {
        return score;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public int getScorePercent() {
        return (int) Math.min(100, score);
    }
}