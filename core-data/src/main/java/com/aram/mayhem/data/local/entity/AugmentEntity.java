package com.aram.mayhem.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.aram.mayhem.data.local.converter.LongListConverter;

import java.util.List;

/**
 * 强化符文本地缓存实体 ── 对应 SQLite 数据库中的 augments 表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义 augments 表的结构，存储 ARAM 模式中的强化符文数据。
 * 符文是每局游戏中可以选择的强化效果，影响英雄的能力和玩法。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、符文品质体系
 * ═══════════════════════════════════════════════════════════════════
 *
 * ARAM 符文按品质分为三级：
 * - Prismatic（棱彩）：最强品质，效果显著，每局可选 1~2 个
 * - Gold（金色）：中等品质，效果适中，每局可选 2~3 个
 * - Silver（银色）：基础品质，效果温和，每局可选 2~3 个
 *
 * 品质越高，出现概率越低，效果越强。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、符文套装系统
 * ═══════════════════════════════════════════════════════════════════
 *
 * 符文可以属于一个"套装"（synergySet），同一套装的符文同时装备时
 * 会触发额外的套装效果。例如"刺客"套装的符文同时装备 3 个时，
 * 会获得额外的暴击伤害加成。
 *
 * synergySet 字段存储套装名称，如"刺客"、"法师"、"战士"等。
 * 不属于任何套装的符文此字段为 null。
 *
 * 关联类：
 * - AugmentDao：操作 augments 表的数据访问对象
 * - StringListConverter：List 类型与 JSON 字符串的转换器
 * - AugmentUiModel：UI 展示用的符文模型
 */
@Entity(tableName = "augments", indices = {
        @Index(value = "quality"),
        @Index(value = "synergySet")
})
@TypeConverters(LongListConverter.class)
public class AugmentEntity {

    /**
     * 符文 ID ── 主键，符文的唯一标识
     *
     * 由后端数据管线分配，全局唯一
     */
    @PrimaryKey
    public long id;

    /**
     * 中文名称 ── 如"电刑"、"暗影收割者"
     */
    public String nameZh;

    /**
     * 英文名称 ── 如"Electrocute"、"Dark Harvest"
     */
    public String nameEn;

    /**
     * 符文品质 ── "Prismatic"、"Gold"、"Silver"
     *
     * 用于列表页按品质筛选和着色显示
     */
    public String quality;

    /**
     * 套装名称 ── 如"刺客"、"法师"、"战士"
     *
     * null 表示该符文不属于任何套装
     * 用于列表页按套装筛选和套装进度计算
     */
    public String synergySet;

    /**
     * 第二套装名称 ── 符文可以属于多个套装
     *
     * null 表示该符文不属于第二套装
     */
    public String synergySet2;

    /**
     * 第三套装名称 ── 符文最多可属于三个套装
     *
     * null 表示该符文不属于第三套装
     */
    public String synergySet3;

    /**
     * 符文效果描述 ── 简短的效果说明
     *
     * 如"对敌方英雄造成伤害时，触发额外魔法伤害"
     */
    public String description;

    /**
     * 详细效果描述 ── 含数值的效果说明
     *
     * 如"对敌方英雄造成伤害时，触发 30~60 (+0.2 AP) 额外魔法伤害（冷却时间：20秒）"
     */
    public String descriptionDetail;

    /**
     * 符文图标 URL ── 符文图片地址
     *
     * 由 Glide 加载并显示在列表和推荐卡片中
     */
    public String iconUrl;

    /**
     * 适配英雄 ID 列表 ── 该符文推荐的英雄 ID
     *
     * 用于推荐算法：当用户选择了某个英雄时，
     * 优先推荐适配该英雄的符文
     */
    public List<Long> heroIds;

    /**
     * 数据更新时间戳 ── 毫秒级 Unix 时间戳
     *
     * 用于缓存过期判断
     */
    public long updatedAt;

    /**
     * 胜率 ── 如 0.523 表示 52.3%
     */
    public Double winRate;

    /**
     * 选取率 ── 如 0.153 表示 15.3%
     */
    public Double pickRate;

    /**
     * 平均名次 ── 范围 1.0~5.0，越低越好
     */
    public Double avgPlacement;

    /**
     * 梯级评级 ── S_PLUS/S/A/B/C
     */
    public String tier;

    /**
     * 是否为陷阱符文 ── true 表示看似强但实际胜率低
     */
    public boolean isTrap;
}
