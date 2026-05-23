package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 符文列表项响应模型 ── 对应后端 AugmentListVO，用于符文图鉴页展示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 AugmentApi.getAugments() 获取符文列表时，每条符文数据
 * 会被 Gson 反序列化为这个类的实例。
 *
 * 数据流向：AugmentApi → AugmentRepository → AugmentListViewModel → AugmentListFragment
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、字段分类
 * ═══════════════════════════════════════════════════════════════════
 *
 * 基础信息：id, nameZh, nameEn, quality, synergySet, iconUrl
 * 统计数据：winRate, pickRate, avgPlacement, tier
 *
 * quality（品质）说明：
 * - MYTHIC（神话）：最稀有，效果最强，每局只能选一个
 * - LEGENDARY（传说）：较稀有，效果显著
 * - EPIC（史诗）：中等稀有，效果适中
 * - RARE（稀有）：最常见，效果基础
 *
 * avgPlacement（平均名次）说明：
 * - ARAM 模式中选择该符文后的平均排名
 * - 范围 1.0 ~ 5.0，越低越好（1.0 = 总是第一名）
 * - 用于评估符文的实际效果
 *
 * 关联类：
 * - AugmentDetailResponse：符文详情 DTO（继承本类）
 * - AugmentRecommendResponse：推荐符文 DTO（继承本类）
 * - AugmentApi：符文 API 接口
 */
public class AugmentResponse {

    /** 符文 ID */
    @SerializedName("id")
    private Long id;

    /** 符文中文名 */
    @SerializedName("nameZh")
    private String nameZh;

    /** 符文英文名 */
    @SerializedName("nameEn")
    private String nameEn;

    /**
     * 符文品质 ── 决定符文的稀有度和强度
     * 可选值：MYTHIC / LEGENDARY / EPIC / RARE
     */
    @SerializedName("quality")
    private String quality;

    /**
     * 所属套装 ── 符文可以属于一个或多个套装
     * 如"战士"、"法师"、"刺客"等
     * 集齐同一套装的符文可获得额外加成
     */
    @SerializedName("synergySet")
    private String synergySet;

    /** 符文图标 URL */
    @SerializedName("iconUrl")
    private String iconUrl;

    /** 胜率（0.0 ~ 1.0） */
    @SerializedName("winRate")
    private Double winRate;

    /** 选取率（0.0 ~ 1.0） */
    @SerializedName("pickRate")
    private Double pickRate;

    /**
     * 平均名次 ── 选择该符文后的平均排名
     * 范围 1.0 ~ 5.0，越低越好
     */
    @SerializedName("avgPlacement")
    private Double avgPlacement;

    /** 梯级评级：S_PLUS / S / A / B / C */
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
