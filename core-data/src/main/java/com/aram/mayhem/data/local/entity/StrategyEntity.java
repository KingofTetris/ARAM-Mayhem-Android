package com.aram.mayhem.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.aram.mayhem.data.local.converter.StringListConverter;

import java.util.List;

/**
 * 社区攻略本地缓存实体 ── 对应 SQLite 数据库中的 strategies 表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义 strategies 表的结构，存储用户创建的英雄攻略数据。
 * 攻略包含出装推荐、符文搭配、玩法心得等内容。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、投票计分机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 攻略的排序得分 score = upvotes - downvotes
 * - upvotes：赞成票数（认为攻略有用的人数）
 * - downvotes：反对票数（认为攻略无用的人数）
 * - score 越高，攻略排名越前
 *
 * 这个计分方式简单直观，但存在"早期优势"问题：
 * 早期发布的攻略积累更多投票，可能比新发布的优质攻略排名更高。
 * 未来可以考虑使用 Wilson Score 或贝叶斯平均来优化排序。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、冗余字段说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * authorNickname, authorAvatar, heroName, heroIcon 等字段是冗余的：
 * - 理论上可以通过 userId 关联用户表获取作者信息
 * - 理论上可以通过 heroId 关联英雄表获取英雄信息
 *
 * 但为了减少 JOIN 查询和提高列表页加载速度，这些信息被冗余存储。
 * 这是一种常见的"读优化"策略：用空间换时间。
 *
 * 关联类：
 * - StrategyDao：操作 strategies 表的数据访问对象
 * - StringListConverter：List 类型与 JSON 字符串的转换器
 * - StrategyUiModel：UI 展示用的攻略模型
 */
@Entity(tableName = "strategies", indices = {
        @Index(value = "heroId"),
        @Index(value = "score")
})
@TypeConverters(StringListConverter.class)
public class StrategyEntity {

    /**
     * 攻略 ID ── 主键，由后端数据库自增生成
     */
    @PrimaryKey
    public long id;

    /**
     * 作者用户 ID ── 关联用户表
     *
     * 用于判断当前用户是否是攻略作者（作者可以编辑/删除自己的攻略）
     */
    public long userId;

    /**
     * 作者昵称 ── 冗余字段，避免 JOIN 用户表
     *
     * 显示在攻略卡片的作者区域
     */
    public String authorNickname;

    /**
     * 作者头像 URL ── 冗余字段，避免 JOIN 用户表
     *
     * 由 Glide 加载并显示在攻略卡片的作者头像位置
     */
    public String authorAvatar;

    /**
     * 关联英雄 ID ── 该攻略针对的英雄
     *
     * 用于英雄详情页的"相关攻略"筛选
     */
    public long heroId;

    /**
     * 英雄中文名称 ── 冗余字段，避免 JOIN 英雄表
     */
    public String heroName;

    /**
     * 英雄图标 URL ── 冗余字段，避免 JOIN 英雄表
     */
    public String heroIcon;

    /**
     * 攻略标题 ── 如"亚索 ARAM 暴击流出装攻略"
     *
     * 显示在攻略卡片的主标题位置
     */
    public String title;

    /**
     * 攻略内容 ── 攻略的详细描述
     *
     * 支持纯文本和简单的换行格式
     */
    public String description;

    /**
     * 赞成票数 ── 认为攻略有用的人数
     */
    public int upvotes;

    /**
     * 反对票数 ── 认为攻略无用的人数
     */
    public int downvotes;

    /**
     * 排序得分 ── score = upvotes - downvotes
     *
     * 用于攻略列表按热度排序
     */
    public int score;

    /**
     * 发布时间 ── ISO 8601 格式的时间字符串
     *
     * 如 "2026-05-20T14:30:00"
     * 用于攻略列表按最新排序
     */
    public String createdAt;

    /**
     * 符文图标 URL 列表 ── 攻略推荐的符文图标
     *
     * 显示在攻略卡片底部，快速展示攻略的符文搭配
     * 存储为 JSON 字符串
     */
    public List<String> augmentIcons;

    /**
     * 装备图标 URL 列表 ── 攻略推荐的装备图标
     *
     * 显示在攻略卡片底部，快速展示攻略的出装推荐
     * 存储为 JSON 字符串
     */
    public List<String> itemIcons;

    /**
     * 数据更新时间戳 ── 毫秒级 Unix 时间戳
     *
     * 用于离线缓存过期判断
     */
    public long updatedAt;
}
