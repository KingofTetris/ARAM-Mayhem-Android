package com.aram.mayhem.ui.model;

import com.aram.mayhem.common.Tier;
import java.util.List;

/**
 * 英雄详情 UI 模型 ── 英雄详情页的完整数据载体
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroDetailUiModel 是英雄详情页专用的数据模型，包含英雄的所有信息：
 * 基础信息（名称、梯级、胜率）+ 详细数据（技能、克制、协同、出装、符文推荐）。
 *
 * 与 HeroUiModel 的关系：
 * - HeroUiModel = 列表页的"摘要版"，只有名称、胜率等基础字段
 * - HeroDetailUiModel = 详情页的"完整版"，额外包含技能、出装等详细数据
 * - 两者没有继承关系，是独立的数据类（因为字段差异太大）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端 API ──→ HeroDetailResponse（网络 DTO）
 *                    │
 *                    ▼  HeroRepository.toDetailUiModel()
 *              HeroDetailUiModel（本类）
 *                    │
 *                    ▼  HeroDetailFragment 观察 LiveData
 *              详情页各区域视图
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、内部类说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类包含两个静态内部类，用于组织嵌套数据：
 *
 * 1. SkillUiModel ── 英雄技能模型
 *    - key：技能按键（Q/W/E/R/P），用于显示技能图标
 *    - name：技能名称，如"斩钢闪"
 *    - description：技能描述，如"向前出剑..."
 *
 * 2. AugmentBriefUiModel ── 推荐符文简要模型
 *    - id：符文 ID
 *    - nameZh：符文中文名
 *    - quality：品质（PRISMATIC/GOLD/SILVER）
 *    - iconUrl：图标 URL
 *    - "Brief"表示只包含摘要信息，不含完整描述和套装信息
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、详情页各区域与字段对应关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * | 详情页区域           | 使用的字段                                    |
 * |---------------------|-----------------------------------------------|
 * | 顶部英雄信息         | nameZh, nameEn, title, role, tier, imageUrl  |
 * | 胜率/选取率统计       | winRate, pickRate → getWinRateDisplay() 等    |
 * | KDA 统计             | avgKills, avgDeaths, avgAssists → getKdaDisplay() |
 * | 版本陷阱警告         | versionTrap → VersionTrapBanner               |
 * | 英雄描述             | description                                   |
 * | 技能列表             | skills (List<SkillUiModel>)                   |
 * | 克制提示             | counterTips (List<String>)                    |
 * | 协同英雄             | synergies (List<String>)                      |
 * | 推荐出装             | recommendedBuild                              |
 * | 推荐符文             | recommendedAugments (List<AugmentBriefUiModel>) |
 */
public class HeroDetailUiModel {

    /**
     * 英雄唯一 ID ── 与 HeroUiModel.id 相同，用于标识英雄
     * 示例：157（亚索）
     */
    private final long id;

    /** 英雄中文名称 ── 详情页主标题，如"亚索" */
    private final String nameZh;

    /** 英雄英文名称 ── 详情页副标题，如"Yasuo" */
    private final String nameEn;

    /** 英雄称号 ── 显示在名称下方，如"疾风剑豪" */
    private final String title;

    /** 英雄定位/角色 ── 如"战士"、"法师" */
    private final String role;

    /** 梯级评级 ── TierBadgeView 显示的梯级标签 */
    private final Tier tier;

    /** 胜率 ── 0.0~1.0 小数，如 0.5234 */
    private final double winRate;

    /** 选取率 ── 0.0~1.0 小数，如 0.1523 */
    private final double pickRate;

    /**
     * 英雄描述 ── 英雄的背景故事/简介文字
     *
     * 来源：Riot DataDragon 的英雄 lore 字段
     * 示例："一个艾欧尼亚人，也是一名出色的剑客..."
     */
    private final String description;

    /**
     * 技能列表 ── 英雄的 Q/W/E/R 四个技能 + 被动技能
     *
     * 每个技能包含：
     * - key：技能按键标识（Q/W/E/R/P），P = Passive（被动）
     * - name：技能名称
     * - description：技能描述
     *
     * 列表通常包含 5 个元素：[被动, Q, W, E, R]
     */
    private final List<SkillUiModel> skills;

    /**
     * 克制提示列表 ── 对抗该英雄的建议
     *
     * 示例：
     * - "避免与他在狭窄通道交战"
     * - "他的风墙有较长冷却时间，抓住空窗期进攻"
     * - "购买护甲穿透装备来对抗他的护盾"
     */
    private final List<String> counterTips;

    /**
     * 协同英雄列表 ── 与该英雄配合良好的英雄名称
     *
     * 示例：["娑娜", "璐璐", "迦娜"]
     * 这些英雄的技能与该英雄有良好的配合效果
     */
    private final List<String> synergies;

    /**
     * 场均击杀数 ── 该英雄在 ARAM 中的平均击杀数
     *
     * 示例：6.5 表示平均每局击杀 6.5 次
     * 用于 KDA 显示：getKdaDisplay() → "6.5 / 5.2 / 8.3"
     */
    private final double avgKills;

    /**
     * 场均死亡数 ── 该英雄在 ARAM 中的平均死亡数
     *
     * 示例：5.2 表示平均每局死亡 5.2 次
     */
    private final double avgDeaths;

    /**
     * 场均助攻数 ── 该英雄在 ARAM 中的平均助攻数
     *
     * 示例：8.3 表示平均每局助攻 8.3 次
     */
    private final double avgAssists;

    /**
     * 推荐出装 ── 该英雄在 ARAM 中的推荐出装方案
     *
     * 格式：通常是出装名称的逗号分隔字符串
     * 示例："无尽之刃,幻影之舞,死亡之舞"
     *
     * 注意：这是简化的出装推荐，不是完整的出装路径
     */
    private final String recommendedBuild;

    /**
     * 推荐符文 ID 列表 ── 推荐的强化符文 ID
     *
     * 示例：[101, 205, 308]
     * 用于从本地数据库查询符文详情
     *
     * 为什么同时有 ID 列表和 UiModel 列表？
     * - recommendedAugmentIds：用于数据库查询和去重
     * - recommendedAugments：用于直接显示在 UI 上
     * - 两者数据来源相同，只是表示形式不同
     */
    private final List<Long> recommendedAugmentIds;

    /**
     * 推荐符文简要列表 ── 包含名称和图标的符文摘要
     *
     * 用于详情页底部的"推荐符文"区域展示
     * 每个元素只包含 id、nameZh、quality、iconUrl
     * 不包含完整描述和套装信息（避免数据量过大）
     */
    private final List<AugmentBriefUiModel> recommendedAugments;

    /**
     * 英雄大图 URL ── 详情页顶部的英雄大图（非头像）
     *
     * 与 HeroUiModel.avatarUrl 的区别：
     * - avatarUrl：小尺寸圆形头像，用于列表卡片
     * - imageUrl：大尺寸矩形图片，用于详情页顶部
     *
     * URL 示例："https://ddragon.leagueoflegends.com/cdn/14.1.1/img/champion/Yasuo.png"
     * （实际使用 splash 图片更清晰）
     */
    private final String imageUrl;

    /**
     * 是否为版本陷阱英雄 ── 详情页顶部显示红色警告横幅
     *
     * true 时 VersionTrapBanner 组件会显示：
     * "⚠ 该英雄在 XX 版本被大幅削弱，慎用！"
     */
    private final boolean versionTrap;

    /**
     * 构造函数 ── 创建英雄详情 UI 模型实例
     *
     * 参数较多（20个），通常由 Repository 层的转换方法调用：
     * <pre>
     * HeroDetailUiModel model = new HeroDetailUiModel(
     *     hero.getId(),
     *     hero.getNameZh(),
     *     hero.getNameEn(),
     *     hero.getTitle(),
     *     hero.getRole(),
     *     hero.getTier(),
     *     hero.getWinRate(),
     *     hero.getPickRate(),
     *     hero.getDescription(),
     *     skillList,
     *     counterTips,
     *     synergies,
     *     hero.getAvgKills(),
     *     hero.getAvgDeaths(),
     *     hero.getAvgAssists(),
     *     hero.getRecommendedBuild(),
     *     augmentIds,
     *     augmentBriefs,
     *     hero.getImageUrl(),
     *     hero.isVersionTrap()
     * );
     * </pre>
     */
    public HeroDetailUiModel(long id, String nameZh, String nameEn, String title, String role,
                             Tier tier, double winRate, double pickRate, String description,
                             List<SkillUiModel> skills, List<String> counterTips, List<String> synergies,
                             double avgKills, double avgDeaths, double avgAssists,
                             String recommendedBuild, List<Long> recommendedAugmentIds,
                             List<AugmentBriefUiModel> recommendedAugments,
                             String imageUrl, boolean versionTrap) {
        this.id = id;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.title = title;
        this.role = role;
        this.tier = tier;
        this.winRate = winRate;
        this.pickRate = pickRate;
        this.description = description;
        this.skills = skills;
        this.counterTips = counterTips;
        this.synergies = synergies;
        this.avgKills = avgKills;
        this.avgDeaths = avgDeaths;
        this.avgAssists = avgAssists;
        this.recommendedBuild = recommendedBuild;
        this.recommendedAugmentIds = recommendedAugmentIds;
        this.recommendedAugments = recommendedAugments;
        this.imageUrl = imageUrl;
        this.versionTrap = versionTrap;
    }

    /** @return 英雄唯一 ID */
    public long getId() { return id; }
    /** @return 中文名称 */
    public String getNameZh() { return nameZh; }
    /** @return 英文名称 */
    public String getNameEn() { return nameEn; }
    /** @return 英雄称号 */
    public String getTitle() { return title; }
    /** @return 定位/角色 */
    public String getRole() { return role; }
    /** @return 梯级评级 */
    public Tier getTier() { return tier; }
    /** @return 胜率（0.0~1.0） */
    public double getWinRate() { return winRate; }
    /** @return 选取率（0.0~1.0） */
    public double getPickRate() { return pickRate; }
    /** @return 英雄描述文字 */
    public String getDescription() { return description; }
    /** @return 技能列表（含被动+Q/W/E/R） */
    public List<SkillUiModel> getSkills() { return skills; }
    /** @return 克制提示列表 */
    public List<String> getCounterTips() { return counterTips; }
    /** @return 协同英雄名称列表 */
    public List<String> getSynergies() { return synergies; }
    /** @return 场均击杀数 */
    public double getAvgKills() { return avgKills; }
    /** @return 场均死亡数 */
    public double getAvgDeaths() { return avgDeaths; }
    /** @return 场均助攻数 */
    public double getAvgAssists() { return avgAssists; }
    /** @return 推荐出装方案 */
    public String getRecommendedBuild() { return recommendedBuild; }
    /** @return 推荐符文 ID 列表 */
    public List<Long> getRecommendedAugmentIds() { return recommendedAugmentIds; }
    /** @return 推荐符文简要列表（含名称和图标） */
    public List<AugmentBriefUiModel> getRecommendedAugments() { return recommendedAugments; }
    /** @return 英雄大图 URL */
    public String getImageUrl() { return imageUrl; }
    /** @return 是否为版本陷阱英雄 */
    public boolean isVersionTrap() { return versionTrap; }

    /**
     * 获取胜率显示文本 ── 将小数胜率转为百分比文字
     *
     * 转换逻辑：winRate 0.5234 → "52.3%"
     * 注意：与 HeroUiModel.getWinRateDisplay() 不同，这里没有"胜率"前缀
     * 因为详情页的胜率标签是单独的 TextView，只需要数值部分
     *
     * @return 格式化后的胜率百分比，如"52.3%"
     */
    public String getWinRateDisplay() {
        return String.format("%.1f%%", winRate);
    }

    /**
     * 获取选取率显示文本 ── 将小数选取率转为百分比文字
     *
     * 转换逻辑：pickRate 0.1523 → "15.2%"
     *
     * @return 格式化后的选取率百分比，如"15.2%"
     */
    public String getPickRateDisplay() {
        return String.format("%.1f%%", pickRate);
    }

    /**
     * 获取 KDA 显示文本 ── 将击杀/死亡/助攻合并为一行文字
     *
     * 转换逻辑：
     * - avgKills=6.5, avgDeaths=5.2, avgAssists=8.3
     * - → "6.5 / 5.2 / 8.3"
     *
     * KDA 的含义：
     * - K = Kill（击杀）：该英雄平均每局击杀数
     * - D = Death（死亡）：该英雄平均每局死亡数
     * - A = Assist（助攻）：该英雄平均每局助攻数
     *
     * KDA 比值 = (K + A) / D，比值越高表示表现越好
     *
     * @return 格式化后的 KDA 文字，如"6.5 / 5.2 / 8.3"
     */
    public String getKdaDisplay() {
        return String.format("%.1f / %.1f / %.1f", avgKills, avgDeaths, avgAssists);
    }

    /**
     * 技能 UI 模型 ── 英雄单个技能的数据载体
     *
     * 每个英雄有 5 个技能：
     * - 被动技能（Passive）：key = "P"
     * - Q 技能：key = "Q"
     * - W 技能：key = "W"
     * - E 技能：key = "E"
     * - R 技能（大招）：key = "R"
     *
     * key 的用途：
     * - 根据按键标识加载对应的技能图标（如 ic_skill_q.png）
     * - 显示技能按键标签（如 "Q"、"W"）
     */
    public static class SkillUiModel {
        /** 技能按键标识 ── P/Q/W/E/R，用于加载对应图标 */
        private final String key;
        /** 技能名称 ── 如"斩钢闪"、"风之障壁" */
        private final String name;
        /** 技能描述 ── 如"向前出剑，造成物理伤害" */
        private final String description;

        /**
         * @param key         技能按键标识（P/Q/W/E/R）
         * @param name        技能名称
         * @param description 技能描述
         */
        public SkillUiModel(String key, String name, String description) {
            this.key = key;
            this.name = name;
            this.description = description;
        }

        /** @return 技能按键标识 */
        public String getKey() { return key; }
        /** @return 技能名称 */
        public String getName() { return name; }
        /** @return 技能描述 */
        public String getDescription() { return description; }
    }

    /**
     * 推荐符文简要 UI 模型 ── 详情页推荐符文区域的数据载体
     *
     * "Brief"（简要）的含义：
     * - 只包含 id、名称、品质、图标四个字段
     * - 不包含完整描述、套装信息、胜率等详细数据
     * - 因为详情页的推荐区域只需要展示图标和名称
     *
     * 如果用户点击某个推荐符文，再跳转到符文详情页查看完整信息
     */
    public static class AugmentBriefUiModel {
        /** 符文 ID ── 用于点击跳转时查询符文详情 */
        private final long id;
        /** 符文中文名称 ── 显示在推荐符文列表中 */
        private final String nameZh;
        /**
         * 符文品质 ── 决定符文卡片的边框颜色
         * 可选值：PRISMATIC（棱彩）、GOLD（金）、SILVER（银）
         */
        private final String quality;
        /** 符文图标 URL ── Glide 加载符文图标的来源 */
        private final String iconUrl;

        /**
         * @param id      符文 ID
         * @param nameZh  符文中文名称
         * @param quality 符文品质
         * @param iconUrl 符文图标 URL
         */
        public AugmentBriefUiModel(long id, String nameZh, String quality, String iconUrl) {
            this.id = id;
            this.nameZh = nameZh;
            this.quality = quality;
            this.iconUrl = iconUrl;
        }

        /** @return 符文 ID */
        public long getId() { return id; }
        /** @return 符文中文名称 */
        public String getNameZh() { return nameZh; }
        /** @return 符文品质（PRISMATIC/GOLD/SILVER） */
        public String getQuality() { return quality; }
        /** @return 符文图标 URL */
        public String getIconUrl() { return iconUrl; }
    }
}
