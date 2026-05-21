package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** 攻略详情响应模型（与后端 StrategyDetailVO 对应） */
public class StrategyDetailResponse {

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

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("augments")
    private List<AugmentResponse> augments;

    @SerializedName("items")
    private List<ItemResponse> items;

    @SerializedName("userVoteType")
    private String userVoteType;

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

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
    }

    public Integer getDownvotes() {
        return downvotes;
    }

    public void setDownvotes(Integer downvotes) {
        this.downvotes = downvotes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<AugmentResponse> getAugments() {
        return augments;
    }

    public List<ItemResponse> getItems() {
        return items;
    }

    public String getUserVoteType() {
        return userVoteType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public void setHeroId(Long heroId) {
        this.heroId = heroId;
    }

    public void setHeroName(String heroName) {
        this.heroName = heroName;
    }

    public void setHeroIcon(String heroIcon) {
        this.heroIcon = heroIcon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setAugments(List<AugmentResponse> augments) {
        this.augments = augments;
    }

    public void setItems(List<ItemResponse> items) {
        this.items = items;
    }

    public void setUserVoteType(String userVoteType) {
        this.userVoteType = userVoteType;
    }

    public static class AugmentResponse {
        @SerializedName("id")
        private Long id;

        @SerializedName("nameZh")
        private String nameZh;

        @SerializedName("nameEn")
        private String nameEn;

        @SerializedName("icon")
        private String icon;

        @SerializedName("quality")
        private String quality;

        public Long getId() {
            return id;
        }

        public String getNameZh() {
            return nameZh;
        }

        public String getNameEn() {
            return nameEn;
        }

        public String getIcon() {
            return icon;
        }

        public String getQuality() {
            return quality;
        }
    }

    public static class ItemResponse {
        @SerializedName("id")
        private Long id;

        @SerializedName("nameZh")
        private String nameZh;

        @SerializedName("nameEn")
        private String nameEn;

        @SerializedName("icon")
        private String icon;

        public Long getId() {
            return id;
        }

        public String getNameZh() {
            return nameZh;
        }

        public String getNameEn() {
            return nameEn;
        }

        public String getIcon() {
            return icon;
        }
    }
}