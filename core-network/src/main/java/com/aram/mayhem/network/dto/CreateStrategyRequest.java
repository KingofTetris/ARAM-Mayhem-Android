package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CreateStrategyRequest {

    @SerializedName("heroId")
    private Long heroId;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("augmentIds")
    private List<Long> augmentIds;

    @SerializedName("itemIds")
    private List<Long> itemIds;

    public CreateStrategyRequest(Long heroId, String title, String description, List<Long> augmentIds, List<Long> itemIds) {
        this.heroId = heroId;
        this.title = title;
        this.description = description;
        this.augmentIds = augmentIds;
        this.itemIds = itemIds;
    }
}