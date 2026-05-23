package com.aram.mayhem.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.aram.mayhem.data.local.converter.SkillListConverter;

import java.util.List;

/**
 * 英雄本地缓存实体 ── 对应 SQLite 数据库中的 heroes 表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 这个类定义了 heroes 表的结构：有哪些列、每列的类型、主键和索引。
 * Room 会根据这个类自动创建 SQLite 表，并生成增删改查的实现代码。
 *
 * 类的字段 = 表的列，字段的类型 = 列的类型：
 * - long id → INTEGER（整数）
 * - String nameZh → TEXT（文本）
 * - double winRate → REAL（浮点数）
 * - boolean isTrap → INTEGER（0/1）
 * - List<SkillData> skills → TEXT（JSON 字符串，通过 TypeConverter 转换）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、Room 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Entity(tableName = "heroes")：
 *   - 标记这个类是一个 Room 数据表实体
 *   - tableName 指定表名（如果不指定，默认使用类名的小写）
 *
 * @PrimaryKey：
 *   - 标记主键字段，每条记录的唯一标识
 *   - heroes 表的主键是 id（对应 Riot DataDragon 的英雄 ID）
 *
 * @Index：
 *   - 为指定列创建索引，加速查询
 *   - 索引就像书的目录，让数据库不用扫描全表就能找到数据
 *   - tier 索引：加速按梯级筛选（WHERE tier = 'S_PLUS'）
 *   - role 索引：加速按定位筛选（WHERE role = '法师'）
 *   - nameZh 索引：加速中文名搜索（WHERE nameZh LIKE '%亚索%'）
 *
 * @TypeConverters(SkillListConverter.class)：
 *   - 告诉 Room 如何处理无法直接映射到 SQLite 类型的字段
 *   - List<SkillData> 无法直接存入 SQLite，需要转换为 JSON 字符串
 *   - SkillListConverter 负责在 List ↔ JSON 之间转换
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、字段分类
 * ═══════════════════════════════════════════════════════════════════
 *
 * 基础信息：id, nameZh, nameEn, title, role, avatarUrl, description
 * 统计数据：tier, winRate, pickRate, banRate, avgKills, avgDeaths, avgAssists
 * 技能信息：skills（技能列表）
 * 对局建议：counterTips（克制提示）, synergies（配合建议）
 * 推荐配置：recommendedBuild（推荐出装）, recommendedAugmentIds/RecommendedAugments（推荐符文）
 * 标记字段：isTrap（陷阱标记）, isVersionTrap（版本陷阱标记）
 * 时间戳：updatedAt（数据更新时间，用于缓存过期判断）
 *
 * 关联类：
 * - HeroDao：操作 heroes 表的数据访问对象
 * - SkillListConverter：List 类型与 JSON 字符串的转换器
 * - HeroUiModel：UI 展示用的英雄模型（从 Entity 转换而来）
 */
@Entity(tableName = "heroes", indices = {
        @Index(value = "tier"),
        @Index(value = "role"),
        @Index(value = "nameZh")
})
@TypeConverters(SkillListConverter.class)
public class HeroEntity {

    /**
     * 英雄 ID ── 主键，对应 Riot DataDragon 的英雄唯一标识
     *
     * 示例值：1（艾希）、22（奈德丽）、157（亚索）
     * 这个 ID 在全球范围内唯一，与 Riot 官方数据一致。
     */
    @PrimaryKey
    public long id;

    /**
     * 中文名称 ── 如"亚索"、"艾希"
     *
     * 用于列表显示和中文搜索
     */
    public String nameZh;

    /**
     * 英文名称 ── 如"Yasuo"、"Ashe"
     *
     * 用于英文搜索和国际化
     */
    public String nameEn;

    /**
     * 英雄称号 ── 如"疾风剑豪"、"寒冰射手"
     *
     * 显示在英雄详情页标题下方
     */
    public String title;

    /**
     * 角色定位 ── 如"战士"、"法师"、"射手"、"辅助"
     *
     * 用于列表页按定位筛选
     */
    public String role;

    /**
     * 梯级评级 ── 如"S_PLUS"、"S"、"A"、"B"、"C"
     *
     * 对应 Tier 枚举的 name()，用于列表页按梯级筛选和着色
     */
    public String tier;

    /**
     * 胜率 ── 如 52.3 表示 52.3%
     *
     * 由后端数据管线从多源数据聚合计算
     */
    public double winRate;

    /**
     * 选取率 ── 如 15.2 表示 15.2%
     *
     * 表示该英雄在所有对局中被选择的频率
     */
    public double pickRate;

    /**
     * 头像 URL ── 英雄图标图片地址
     *
     * 格式：https://ddragon.leagueoflegends.com/cdn/14.10.1/img/champion/Yasuo.png
     * 由 Glide 加载并显示在列表和详情页
     */
    public String avatarUrl;

    /**
     * 英雄描述 ── 英雄的背景故事简介
     */
    public String description;

    /**
     * 技能列表 ── 包含 Q/W/E/R 四个技能的详细信息
     *
     * 存储为 JSON 字符串，通过 SkillListConverter 转换为 List<SkillData>
     * SQLite 中实际存储类型为 TEXT
     */
    public List<SkillData> skills;

    /**
     * 克制提示列表 ── 对抗该英雄的建议
     *
     * 如 ["避免近身对拼", "利用控制技能打断位移"]
     * 存储为 JSON 字符串
     */
    public List<String> counterTips;

    /**
     * 配合建议列表 ── 与该英雄搭配的英雄/策略建议
     *
     * 如 ["配合硬控英雄效果更佳", "与坦克前排搭配更好"]
     * 存储为 JSON 字符串
     */
    public List<String> synergies;

    /**
     * 场均击杀数 ── 如 8.5
     */
    public double avgKills;

    /**
     * 场均死亡数 ── 如 5.2
     */
    public double avgDeaths;

    /**
     * 场均助攻数 ── 如 6.8
     */
    public double avgAssists;

    /**
     * 推荐出装 ── JSON 格式的装备列表
     *
     * 包含核心装备、可选装备等分类
     */
    public String recommendedBuild;

    /**
     * 推荐符文 ID 列表 ── 该英雄推荐搭配的符文 ID
     *
     * 如 [101, 205, 302]
     * 存储为 JSON 字符串
     */
    public List<Long> recommendedAugmentIds;

    /**
     * 推荐符文简要信息列表 ── 包含符文的 id、名称、品质、图标
     *
     * 存储为 JSON 字符串，避免额外查询符文表
     */
    public List<AugmentBriefData> recommendedAugments;

    /**
     * 陷阱标记 ── 该英雄是否为"陷阱"（看似很强但实际效果差）
     *
     * SQLite 中存储为 INTEGER（0=false, 1=true）
     */
    public boolean isTrap;

    /**
     * 版本陷阱标记 ── 该英雄是否为"版本陷阱"（当前版本看似很强但胜率低）
     *
     * 与 isTrap 的区别：
     * - isTrap：长期标记，该英雄本质上容易误导
     * - isVersionTrap：版本相关，下个版本可能就不再是陷阱
     */
    public boolean isVersionTrap;

    /**
     * 被禁用率 ── 如 12.5 表示 12.5%
     *
     * 表示该英雄在选人阶段被禁用的频率
     */
    public double banRate;

    /**
     * 数据更新时间戳 ── 毫秒级 Unix 时间戳
     *
     * 用于缓存过期判断：
     * - 如果 updatedAt 距今超过 1 小时，认为数据过期，需要重新从网络获取
     * - 如果 updatedAt 距今不足 1 小时，使用本地缓存
     */
    public long updatedAt;

    /**
     * 技能数据内部类 ── 描述英雄的一个技能
     *
     * 存储在 skills 列表中，通过 SkillListConverter 序列化为 JSON
     */
    public static class SkillData {
        /** 技能键名：Q/W/E/R（被动技能为 "passive"） */
        public String key;
        /** 技能名称：如"斩钢闪"、"风之障壁" */
        public String name;
        /** 技能描述：含数值加成的详细说明 */
        public String description;
    }

    /**
     * 符文简要信息内部类 ── 用于推荐符文的简要展示
     *
     * 存储在 recommendedAugments 列表中，避免额外查询 augments 表
     */
    public static class AugmentBriefData {
        /** 符文 ID */
        public long id;
        /** 符文中文名称 */
        public String nameZh;
        /** 符文品质：Prismatic/Gold/Silver */
        public String quality;
        /** 符文图标 URL */
        public String iconUrl;
    }
}
