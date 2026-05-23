package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 公告响应模型 ── 对应后端 BulletinListVO/BulletinDetailVO
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 BulletinApi 的接口获取公告数据时，
 * 每条公告会被 Gson 反序列化为这个类的实例。
 *
 * 列表和详情共用同一个 DTO，区别在于：
 * - 列表：content 可能为空（只显示标题和摘要）
 * - 详情：content 包含完整正文
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、字段分类
 * ═══════════════════════════════════════════════════════════════════
 *
 * 基础信息：id, type, title, content, imageUrl
 * 状态信息：isPinned（是否置顶）
 * 时间信息：publishedAt, createdAt
 *
 * type（公告类型）说明：
 * - UPDATE：版本更新公告
 * - EVENT：限时活动公告
 * - MAINTENANCE：服务器维护公告
 * - NOTICE：一般通知公告
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、为什么有 setter 方法？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 大部分 DTO 只有 getter（不可变），但 BulletinResponse 同时有 setter，
 * 因为公告数据需要写入 Room 数据库（BulletinEntity），
 * Repository 从网络获取后需要设置字段值再存入数据库。
 *
 * 关联类：
 * - BulletinApi：公告 API 接口
 * - BulletinEntity：本地缓存实体（Room）
 */
public class BulletinResponse {

    /** 公告 ID */
    @SerializedName("id")
    private long id;

    /**
     * 公告类型
     * UPDATE：版本更新 / EVENT：活动 / MAINTENANCE：维护 / NOTICE：通知
     */
    @SerializedName("type")
    private String type;

    /** 公告标题 */
    @SerializedName("title")
    private String title;

    /**
     * 公告正文 ── 列表接口可能为空，详情接口包含完整内容
     * 可能包含 HTML 格式
     */
    @SerializedName("content")
    private String content;

    /** 公告封面图片 URL */
    @SerializedName("imageUrl")
    private String imageUrl;

    /**
     * 是否置顶 ── 1=置顶，0=普通
     * 置顶公告显示在列表最前面
     */
    @SerializedName("isPinned")
    private Integer isPinned;

    /** 发布时间 ── ISO 8601 格式，如 "2026-05-20T10:30:00" */
    @SerializedName("publishedAt")
    private String publishedAt;

    /** 创建时间 ── ISO 8601 格式 */
    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getIsPinned() { return isPinned; }
    public void setIsPinned(Integer isPinned) { this.isPinned = isPinned; }
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
