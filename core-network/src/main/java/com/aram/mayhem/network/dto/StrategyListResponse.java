package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 攻略列表项响应模型 ── 对应后端 StrategyListVO，用于社区攻略列表展示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 CommunityApi.getStrategies() 获取攻略列表时，每条攻略数据
 * 会被 Gson 反序列化为这个类的实例。
 *
 * 数据流向：CommunityApi → CommunityRepository → CommunityViewModel → CommunityFeedFragment
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、字段分类
 * ═══════════════════════════════════════════════════════════════════
 *
 * 作者信息：userId, authorNickname, authorAvatar
 * 英雄信息：heroId, heroName, heroIcon
 * 攻略内容：id, title, description
 * 投票统计：upvotes, downvotes, score
 * 时间信息：createdAt
 * 关联图标：augmentIcons, itemIcons（用于列表页快速展示）
 *
 * score = upvotes - downvotes（净投票数），用于热门排序
 *
 * 关联类：
 * - StrategyDetailResponse：攻略详情 DTO
 * - CreateStrategyRequest：发布攻略请求 DTO
 * - CommunityApi：社区 API 接口
 */
public class StrategyListResponse {

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

    /** 攻略摘要描述 */
    @SerializedName("description")
    private String description;

    /** 点赞数 */
    @SerializedName("upvotes")
    private Integer upvotes;

    /** 点踩数 */
    @SerializedName("downvotes")
    private Integer downvotes;

    /** 净投票数 = upvotes - downvotes，用于热门排序 */
    @SerializedName("score")
    private Integer score;

    /** 发布时间 ── ISO 8601 格式 */
    @SerializedName("createdAt")
    private String createdAt;

    /** 推荐符文图标 URL 列表 ── 列表页快速展示 */
    @SerializedName("augmentIcons")
    private List<String> augmentIcons;

    /** 推荐装备图标 URL 列表 ── 列表页快速展示 */
    @SerializedName("itemIcons")
    private List<String> itemIcons;

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
    public Integer getDownvotes() { return downvotes; }
    public Integer getScore() { return score; }
    public String getCreatedAt() { return createdAt; }
    public List<String> getAugmentIcons() { return augmentIcons; }
    public List<String> getItemIcons() { return itemIcons; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setAuthorNickname(String authorNickname) { this.authorNickname = authorNickname; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public void setHeroId(Long heroId) { this.heroId = heroId; }
    public void setHeroName(String heroName) { this.heroName = heroName; }
    public void setHeroIcon(String heroIcon) { this.heroIcon = heroIcon; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setUpvotes(Integer upvotes) { this.upvotes = upvotes; }
    public void setDownvotes(Integer downvotes) { this.downvotes = downvotes; }
    public void setScore(Integer score) { this.score = score; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setAugmentIcons(List<String> augmentIcons) { this.augmentIcons = augmentIcons; }
    public void setItemIcons(List<String> itemIcons) { this.itemIcons = itemIcons; }
}
