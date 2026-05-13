package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AugmentRecommendRequest {

    @SerializedName("heroId")
    private long heroId;

    @SerializedName("selectedAugmentIds")
    private List<Long> selectedAugmentIds;

    public AugmentRecommendRequest(long heroId, List<Long> selectedAugmentIds) {
        this.heroId = heroId;
        this.selectedAugmentIds = selectedAugmentIds;
    }

    public long getHeroId() {
        return heroId;
    }

    public List<Long> getSelectedAugmentIds() {
        return selectedAugmentIds;
    }
}