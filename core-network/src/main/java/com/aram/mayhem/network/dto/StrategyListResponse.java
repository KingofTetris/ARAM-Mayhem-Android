package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StrategyListResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("userId")
    private Long userId;

    @SerializedName("authorNickname")
    private String authorNickname;

    @SerializedName("authorAvatar")
    private String authorAvatar;

    @SerializedName("heroId")
    private Long heroId;

    @SerializedName("heroName")
    private String heroName;

    @SerializedName("heroIcon")
    private String heroIcon;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("upvotes")
    private Integer upvotes;

    @SerializedName("downvotes")
    private Integer downvotes;

    @SerializedName("score")
    private Integer score;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("augmentIcons")
    private List<String> augmentIcons;

    @SerializedName("itemIcons")
    private List<String> itemIcons;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public Long getHeroId() {
        return heroId;
    }

    public String getHeroName() {
        return heroName;
    }

    public String getHeroIcon() {
        return heroIcon;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getUpvotes() {
        return upvotes;
    }

    public Integer getDownvotes() {
        return downvotes;
    }

    public Integer getScore() {
        return score;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<String> getAugmentIcons() {
        return augmentIcons;
    }

    public List<String> getItemIcons() {
        return itemIcons;
    }
}