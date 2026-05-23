package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 英雄列表项响应模型 ── 对应后端 HeroListVO，用于英雄列表页展示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 HeroApi.getHeroes() 获取英雄列表时，每条英雄数据
 * 会被 Gson 反序列化为这个类的实例。
 *
 * 数据流向：HeroApi → HeroRepository → HeroListViewModel → HeroListFragment
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、@SerializedName 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @SerializedName("nameZh") 告诉 Gson：
 * - JSON 中的 "nameZh" 字段 → 映射到 Java 的 nameZh 属性
 * - 如果 JSON 字段名和 Java 属性名相同，@SerializedName 可以省略
 * - 但加上它更明确，也方便未来重命名 Java 属性而不影响 JSON 解析
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、列表项 vs 详情的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroResponse（本类）是列表项，只包含基础信息：
 * - 名称、角色、梯级、胜率、图标等
 * - 不包含技能、出装、符文推荐等详细数据
 * - 数据量小，适合列表快速加载
 *
 * HeroDetailResponse 是详情，包含完整信息：
 * - 继承了本类的所有字段
 * - 额外包含技能、克制提示、协同英雄、推荐出装等
 * - 数据量大，只在查看详情时加载
 *
 * 为什么分成两个类？
 * - 列表页只需要基础信息，加载详情数据浪费带宽和内存
 * - 分页查询时，每页 20 条，如果每条都带详情数据，响应会很大
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、字段分类
 * ═══════════════════════════════════════════════════════════════════
 *
 * 基础信息：id, nameEn, nameZh, title, role, imageUrl, description
 * 统计数据：tier, winRate, pickRate
 *
 * tier（梯级）说明：
 * - S_PLUS：最强英雄，胜率极高
 * - S：强势英雄，胜率较高
 * - A：均衡英雄，胜率中等偏上
 * - B：一般英雄，胜率中等
 * - C：弱势英雄，胜率较低
 *
 * 关联类：
 * - HeroDetailResponse：英雄详情 DTO
 * - HeroApi：英雄 API 接口
 * - HeroEntity：本地缓存实体（Room）
 */
public class HeroResponse {

    /** 英雄 ID ── 对应 Riot DataDragon 的英雄唯一标识，如 157（亚索） */
    @SerializedName("id")
    private Long id;

    /** 英文名 ── 如 "Yasuo"、"Ahri" */
    @SerializedName("nameEn")
    private String nameEn;

    /** 中文名 ── 如 "亚索"、"阿狸" */
    @SerializedName("nameZh")
    private String nameZh;

    /** 英雄称号 ── 如 "疾风剑豪"、"九尾妖狐" */
    @SerializedName("title")
    private String title;

    /**
     * 角色定位 ── 英雄的主要位置
     * 可选值：战士/法师/刺客/射手/辅助/坦克
     */
    @SerializedName("role")
    private String role;

    /**
     * 梯级评级 ── 基于胜率的综合评级
     * 可选值：S_PLUS / S / A / B / C
     */
    @SerializedName("tier")
    private String tier;

    /**
     * 胜率 ── 该英雄在 ARAM 模式中的胜率
     * 范围：0.0 ~ 1.0（如 0.5234 表示 52.34%）
     */
    @SerializedName("winRate")
    private Double winRate;

    /**
     * 选取率 ── 该英雄在 ARAM 模式中的出场率
     * 范围：0.0 ~ 1.0（如 0.0856 表示 8.56%）
     */
    @SerializedName("pickRate")
    private Double pickRate;

    /**
     * 头像图片 URL ── 用于列表页显示英雄图标
     * 格式：https://ddragon.leagueoflegends.com/cdn/14.1.1/img/champion/Yasuo.png
     */
    @SerializedName("imageUrl")
    private String imageUrl;

    /** 英雄简介 ── 一句话描述英雄特点 */
    @SerializedName("description")
    private String description;

    public Long getId() {
        return id;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getTitle() {
        return title;
    }

    public String getRole() {
        return role;
    }

    public String getTier() {
        return tier;
    }

    public Double getWinRate() {
        return winRate;
    }

    public Double getPickRate() {
        return pickRate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDescription() {
        return description;
    }
}
