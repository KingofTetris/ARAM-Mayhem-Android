package com.aram.mayhem.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 公告本地缓存实体 ── 对应 SQLite 数据库中的 bulletins 表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义 bulletins 表的结构，存储管理员发布的公告数据。
 * 公告包括版本更新通知、活动公告、维护通知等。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、公告类型
 * ═══════════════════════════════════════════════════════════════════
 *
 * type 字段支持三种类型：
 * - "version"：版本更新公告（如"v2.0 新功能上线"）
 * - "event"：活动公告（如"周年庆活动"）
 * - "notice"：普通通知（如"服务器维护通知"）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、置顶机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * isPinned 字段控制公告是否置顶：
 * - isPinned = 1（true）：置顶公告，始终显示在列表顶部
 * - isPinned = 0（false）：普通公告，按时间排序
 *
 * 管理员可以将重要公告设为置顶，确保用户优先看到。
 *
 * 关联类：
 * - BulletinDao：操作 bulletins 表的数据访问对象
 * - BulletinUiModel：UI 展示用的公告模型
 */
@Entity(tableName = "bulletins", indices = {
        @Index(value = "type"),
        @Index(value = "publishedAt")
})
public class BulletinEntity {

    /**
     * 公告 ID ── 主键，由后端数据库自增生成
     */
    @PrimaryKey
    public long id;

    /**
     * 公告标题 ── 如"v2.0 版本更新通知"
     *
     * 显示在公告列表和详情页的标题位置
     */
    public String title;

    /**
     * 公告内容 ── 公告的详细描述
     *
     * 支持纯文本和简单的换行格式
     */
    public String content;

    /**
     * 公告类型 ── "version"、"event"、"notice"
     *
     * 用于公告页面按类型筛选
     */
    public String type;

    /**
     * 是否置顶 ── 1=置顶，0=普通
     *
     * SQLite 中 boolean 存储为 INTEGER
     */
    public boolean isPinned;

    /**
     * 发布时间 ── ISO 8601 格式的时间字符串
     *
     * 如 "2026-05-20T14:30:00"
     * 用于公告列表按时间排序
     */
    public String publishedAt;

    /**
     * 数据更新时间戳 ── 毫秒级 Unix 时间戳
     *
     * 用于离线缓存过期判断
     */
    public long updatedAt;
}
