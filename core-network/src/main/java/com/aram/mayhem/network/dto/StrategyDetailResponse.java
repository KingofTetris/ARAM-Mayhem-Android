package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 攻略详情响应模型 ── 对应后端 StrategyDetailVO，用于攻略详情页展示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 CommunityApi.getStrategyDetail() 获取攻略详情时，
 * 响应中的 data 字段会被 Gson 反序列化为这个类的实例。
 *
 * 与 StrategyListResponse 的区别：
 * - 列表：只有摘要和图标 URL
 * - 详情：有完整的符文/装备对象和用户投票状态
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、userVoteType 字段说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * userVoteType 表示当前登录用户对该攻略的投票状态：
 * - null：未投票
 * - "UP"：已点赞
 * - "DOWN"：已点踩
 *
 * 详情页根据此字段显示投票按钮的状态（高亮已选的按钮）。
 * 未登录用户此字段始终为 null。
 *
 * 关联类：
 * - StrategyListResponse：攻略列表项 DTO
 * - CommunityApi：社区 API 接口
 */
public class StrategyDetailResponse {

    /** 攻略 ID */
    @SerializedName("id")
    private Long id;

    /** 作者用户 ID */
    @SerializedName("userId")
    private Long userId;

    /** 作者昵称 */
    @SerializedName("authorNickname")
    private String authorNickname;

    /** 作者头像 URL */
    @SerializedName("authorAvatar")
    private String authorAvatar;

    /** 关联英雄 ID */
    @SerializedName("heroId")
    private Long heroId;

    /** 关联英雄名称 */
    @SerializedName("heroName")
    private String heroName;

    /** 关联英雄图标 URL */
    @SerializedName("heroIcon")
    private String heroIcon;

    /** 攻略标题 */
    @SerializedName("title")
    private String title;

    /** 攻略详细描述 */
    @SerializedName("description")
    private String description;

    /** 点赞数 */
    @SerializedName("upvotes")
    private Integer upvotes;

    /** 点踩数 */
    @SerializedName("downvotes")
    private Integer downvotes;

    /** 发布时间 */
    @SerializedName("createdAt")
    private String createdAt;

    /**
     * 推荐符文列表 ── 包含完整符文信息（ID、名称、品质、图标）
     * 列表接口只返回图标 URL，详情接口返回完整对象
     */
    @SerializedName("augments")
    private List<AugmentResponse> augments;

    /**
     * 推荐装备列表 ── 包含完整装备信息（ID、名称、图标）
     */
    @SerializedName("items")
    private List<ItemResponse> items;

    /**
     * 当前用户的投票状态
     * null：未投票 / "UP"：已点赞 / "DOWN"：已点踩
     */
    @SerializedName("userVoteType")
    private String userVoteType;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorAvatar() { return authorAvatar; }
    public Long getHeroId() { return heroId; }
    public String getHeroName() { return heroName; }
    public String getHeroIcon() { return heroIcon; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getUpvotes() { return upvotes; }
    public void setUpvotes(Integer upvotes) { this.upvotes = upvotes; }
    public Integer getDownvotes() { return downvotes; }
    public void setDownvotes(Integer downvotes) { this.downvotes = downvotes; }
    public String getCreatedAt() { return createdAt; }
    public List<AugmentResponse> getAugments() { return augments; }
    public List<ItemResponse> getItems() { return items; }
    public String getUserVoteType() { return userVoteType; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public void setHeroId(Long heroId) { this.heroId = heroId; }
    public void setHeroName(String heroName) { this.heroName = heroName; }
    public void setHeroIcon(String heroIcon) { this.heroIcon = heroIcon; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setAugments(List<AugmentResponse> augments) { this.augments = augments; }
    public void setItems(List<ItemResponse> items) { this.items = items; }
    public void setUserVoteType(String userVoteType) { this.userVoteType = userVoteType; }

    /**
     * 攻略关联符文内部类 ── 详情页展示推荐符文
     *
     * 与 AugmentResponse（符文模块的 DTO）不同，
     * 这是社区模块的简化版本，只包含攻略中需要展示的字段。
     */
    public static class AugmentResponse {
        /** 符文 ID */
        @SerializedName("id")
        private Long id;

        /** 符文中文名 */
        @SerializedName("nameZh")
        private String nameZh;

        /** 符文英文名 */
        @SerializedName("nameEn")
        private String nameEn;

        /** 符文图标 URL */
        @SerializedName("icon")
        private String icon;

        /** 符文品质 */
        @SerializedName("quality")
        private String quality;

        public Long getId() { return id; }
        public String getNameZh() { return nameZh; }
        public String getNameEn() { return nameEn; }
        public String getIcon() { return icon; }
        public String getQuality() { return quality; }
    }

    /**
     * 攻略关联装备内部类 ── 详情页展示推荐装备
     *
     * 装备数据来自 Riot DataDragon，包含装备的 ID、名称和图标。
     */
    public static class ItemResponse {
        /** 装备 ID */
        @SerializedName("id")
        private Long id;

        /** 装备中文名 */
        @SerializedName("nameZh")
        private String nameZh;

        /** 装备英文名 */
        @SerializedName("nameEn")
        private String nameEn;

        /** 装备图标 URL */
        @SerializedName("icon")
        private String icon;

        public Long getId() { return id; }
        public String getNameZh() { return nameZh; }
        public String getNameEn() { return nameEn; }
        public String getIcon() { return icon; }
    }
}
