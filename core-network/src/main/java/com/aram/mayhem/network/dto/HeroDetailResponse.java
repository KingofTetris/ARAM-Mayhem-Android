package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 英雄详情响应模型 ── 对应后端 HeroDetailVO，用于英雄详情页展示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 HeroApi.getHeroDetail() 获取英雄详情时，
 * 响应中的 data 字段会被 Gson 反序列化为这个类的实例。
 *
 * 数据流向：HeroApi → HeroRepository → HeroDetailViewModel → HeroDetailFragment
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、与 HeroResponse 的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroDetailResponse 包含 HeroResponse 的所有字段（id, nameZh, tier 等），
 * 还额外包含详情数据（技能、克制提示、推荐出装等）。
 *
 * 注意：本类没有继承 HeroResponse，而是独立定义了所有字段。
 * 这是因为 JSON 反序列化时，Gson 需要每个类有完整的字段定义。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、字段分类
 * ═══════════════════════════════════════════════════════════════════
 *
 * 基础信息：id, nameEn, nameZh, title, role, tier, winRate, pickRate, imageUrl, description
 * 技能数据：skills（Q/W/E/R/被动技能的详细信息）
 * 对局数据：avgKills, avgDeaths, avgAssists（平均 KDA）
 * 克制/协同：counterTips（克制提示）, synergies（协同英雄）
 * 推荐数据：recommendedBuild（推荐出装）, recommendedAugments（推荐符文）
 * 陷阱标记：isVersionTrap（是否为版本陷阱英雄）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、版本陷阱（Version Trap）说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * isVersionTrap = true 表示该英雄在当前版本看起来很强（如高选取率），
 * 但实际胜率很低，属于"陷阱"选择。
 * 详情页会用特殊标记提醒玩家，避免盲目选择。
 *
 * 关联类：
 * - HeroResponse：英雄列表项 DTO
 * - SkillResponse：技能数据内部类
 * - AugmentBriefResponse：符文简要信息内部类
 * - HeroApi：英雄 API 接口
 */
public class HeroDetailResponse {

    /** 英雄 ID */
    @SerializedName("id")
    private Long id;

    /** 英文名 */
    @SerializedName("nameEn")
    private String nameEn;

    /** 中文名 */
    @SerializedName("nameZh")
    private String nameZh;

    /** 英雄称号 */
    @SerializedName("title")
    private String title;

    /** 角色定位 */
    @SerializedName("role")
    private String role;

    /** 梯级评级 */
    @SerializedName("tier")
    private String tier;

    /** 胜率（0.0 ~ 1.0） */
    @SerializedName("winRate")
    private Double winRate;

    /** 选取率（0.0 ~ 1.0） */
    @SerializedName("pickRate")
    private Double pickRate;

    /** 头像图片 URL */
    @SerializedName("imageUrl")
    private String imageUrl;

    /** 英雄简介 */
    @SerializedName("description")
    private String description;

    /**
     * 技能列表 ── 包含 Q/W/E/R 和被动技能
     * 每个技能包含：key（Q/W/E/R/passive）、name（技能名）、description（描述）
     */
    @SerializedName("skills")
    private List<SkillResponse> skills;

    /**
     * 克制提示 ── 对抗该英雄时的建议
     * 如："注意躲避他的龙卷风"、"在他没有风墙时进攻"
     */
    @SerializedName("counterTips")
    private List<String> counterTips;

    /**
     * 协同英雄 ── 与该英雄配合良好的英雄列表
     * 如："阿利斯塔"、"蕾欧娜"（与亚索配合好的英雄）
     */
    @SerializedName("synergies")
    private List<String> synergies;

    /** 场均击杀数 */
    @SerializedName("avgKills")
    private Double avgKills;

    /** 场均死亡数 */
    @SerializedName("avgDeaths")
    private Double avgDeaths;

    /** 场均助攻数 */
    @SerializedName("avgAssists")
    private Double avgAssists;

    /**
     * 推荐出装 ── JSON 格式的出装推荐
     * 包含核心装备、可选装备、鞋子选择等
     */
    @SerializedName("recommendedBuild")
    private String recommendedBuild;

    /** 推荐符文 ID 列表（已废弃，使用 recommendedAugments 代替） */
    @SerializedName("recommendedAugmentIds")
    private List<Long> recommendedAugmentIds;

    /** 推荐符文列表 ── 包含符文的简要信息（ID、名称、品质、图标） */
    @SerializedName("recommendedAugments")
    private List<AugmentBriefResponse> recommendedAugments;

    /**
     * 是否为版本陷阱 ── true 表示该英雄看似强势但实际胜率低
     * 详情页会用红色标记提醒玩家
     */
    @SerializedName("isVersionTrap")
    private Boolean isVersionTrap;

    public Long getId() { return id; }
    public String getNameEn() { return nameEn; }
    public String getNameZh() { return nameZh; }
    public String getTitle() { return title; }
    public String getRole() { return role; }
    public String getTier() { return tier; }
    public Double getWinRate() { return winRate; }
    public Double getPickRate() { return pickRate; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public List<SkillResponse> getSkills() { return skills; }
    public List<String> getCounterTips() { return counterTips; }
    public List<String> getSynergies() { return synergies; }
    public Double getAvgKills() { return avgKills; }
    public Double getAvgDeaths() { return avgDeaths; }
    public Double getAvgAssists() { return avgAssists; }
    public String getRecommendedBuild() { return recommendedBuild; }
    public List<Long> getRecommendedAugmentIds() { return recommendedAugmentIds; }
    public List<AugmentBriefResponse> getRecommendedAugments() { return recommendedAugments; }
    public Boolean getIsVersionTrap() { return isVersionTrap; }

    /**
     * 技能响应内部类 ── 描述英雄的一个技能
     *
     * 英雄通常有 5 个技能：
     * - 被动技能（passive）：无需主动释放，满足条件自动触发
     * - Q 技能：主要输出/控制技能
     * - W 技能：辅助/防御技能
     * - E 技能：位移/增益技能
     * - R 技能（大招）：强力终极技能
     */
    public static class SkillResponse {
        /** 技能键名：Q / W / E / R / passive */
        @SerializedName("key")
        private String key;

        /** 技能名称：如"斩钢闪"、"风之障壁" */
        @SerializedName("name")
        private String name;

        /** 技能描述：含数值加成的详细说明 */
        @SerializedName("description")
        private String description;

        public String getKey() { return key; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    /**
     * 符文简要信息内部类 ── 用于英雄详情页的推荐符文展示
     *
     * 只包含符文的基础信息（ID、名称、品质、图标），
     * 不包含完整描述和套装信息，避免数据冗余。
     */
    public static class AugmentBriefResponse {
        /** 符文 ID */
        @SerializedName("id")
        private Long id;

        /** 符文中文名 */
        @SerializedName("nameZh")
        private String nameZh;

        /** 符文品质：MYTHIC / LEGENDARY / EPIC / RARE */
        @SerializedName("quality")
        private String quality;

        /** 符文图标 URL */
        @SerializedName("iconUrl")
        private String iconUrl;

        public Long getId() { return id; }
        public String getNameZh() { return nameZh; }
        public String getQuality() { return quality; }
        public String getIconUrl() { return iconUrl; }
    }
}
