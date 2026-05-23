package com.aram.mayhem.ui.model;

/**
 * 公告 UI 模型 ── 公告列表和轮播图的数据载体
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * BulletinUiModel 是公告模块的数据模型，用于两种展示场景：
 * 1. 首页轮播图（BulletinCarouselView）── 顶部大图轮播
 * 2. 公告列表页（BulletinListFragment）── 完整公告列表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端 API ──→ BulletinResponse（网络 DTO）
 *                    │
 *                    ▼  BulletinRepository.toUiModel()
 *              BulletinUiModel（本类）
 *                    │
 *          ┌─────────┴─────────┐
 *          ▼                   ▼
 *   BulletinCarouselView   BulletinAdapter
 *   （首页轮播图）          （公告列表）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、公告类型说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * | type 值   | 中文显示  | 说明                     |
 * |-----------|----------|--------------------------|
 * | version   | 版本更新  | 游戏版本更新公告           |
 * | event     | 活动      | 限时活动、赛事公告         |
 * | notice    | 通知      | 系统通知、维护公告         |
 * | 其他       | 原值显示  | 未知类型直接显示英文原文   |
 *
 * getTypeDisplay() 方法负责将英文 type 转为中文显示文本
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、置顶机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * pinned 字段表示该公告是否置顶：
 * - pinned = true：重要公告，显示在列表顶部和轮播图中
 * - pinned = false：普通公告，按发布时间排序
 *
 * 置顶公告通常包括：版本更新、重大活动、系统维护等
 */
public class BulletinUiModel {

    /**
     * 公告唯一 ID ── 对应后端数据库的主键
     *
     * 用于：
     * - 点击公告时传递 ID 给详情页
     * - 列表去重（DiffUtil 比较标识）
     */
    private final long id;

    /**
     * 公告类型 ── 决定公告的分类标签颜色和文字
     *
     * 可选值：version / event / notice
     * 中文映射见 getTypeDisplay() 方法
     */
    private final String type;

    /**
     * 公告标题 ── 显示在列表项和轮播图上的主文字
     *
     * 示例："14.1 版本更新公告"、"ARAM 限时双倍金币活动"
     */
    private final String title;

    /**
     * 公告内容 ── 公告的完整正文
     *
     * 列表页只显示标题，点击进入详情页才显示完整内容
     * 内容可能包含 HTML 格式（需要 WebView 渲染）
     */
    private final String content;

    /**
     * 公告图片 URL ── 轮播图和列表缩略图的图片来源
     *
     * - 轮播图：使用大尺寸图片（CENTER_CROP 填充）
     * - 列表缩略图：使用小尺寸图片
     * - 为 null 时显示默认占位图
     */
    private final String imageUrl;

    /**
     * 是否置顶 ── 重要公告标记
     *
     * true 时：
     * - 在列表中排在最前面
     * - 在首页轮播图中优先展示
     * - 可能显示"置顶"标签
     */
    private final boolean pinned;

    /**
     * 发布时间 ── 公告的正式发布时间字符串
     *
     * 格式：后端返回的 ISO 8601 格式，如"2026-05-01T10:00:00"
     * 显示时需要格式化为用户友好的格式，如"2026-05-01"
     */
    private final String publishedAt;

    /**
     * 创建时间 ── 公告记录在数据库中的创建时间
     *
     * 与 publishedAt 的区别：
     * - createdAt：管理员创建公告的时间（可能早于发布时间）
     * - publishedAt：公告正式对用户可见的时间（定时发布场景）
     */
    private final String createdAt;

    /**
     * 构造函数 ── 创建公告 UI 模型实例
     *
     * @param id          公告 ID
     * @param type        公告类型（version/event/notice）
     * @param title       公告标题
     * @param content     公告内容
     * @param imageUrl    图片 URL（可为 null）
     * @param pinned      是否置顶
     * @param publishedAt 发布时间
     * @param createdAt   创建时间
     */
    public BulletinUiModel(long id, String type, String title, String content,
                           String imageUrl, boolean pinned, String publishedAt, String createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.pinned = pinned;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
    }

    /** @return 公告唯一 ID */
    public long getId() { return id; }
    /** @return 公告类型（version/event/notice） */
    public String getType() { return type; }
    /** @return 公告标题 */
    public String getTitle() { return title; }
    /** @return 公告内容正文 */
    public String getContent() { return content; }
    /** @return 图片 URL，可为 null */
    public String getImageUrl() { return imageUrl; }
    /** @return 是否置顶 */
    public boolean isPinned() { return pinned; }
    /** @return 发布时间字符串 */
    public String getPublishedAt() { return publishedAt; }
    /** @return 创建时间字符串 */
    public String getCreatedAt() { return createdAt; }

    /**
     * 获取公告类型的中文显示文本 ── 将英文 type 转为用户友好的中文标签
     *
     * 映射规则：
     * - "version" → "版本更新"
     * - "event"   → "活动"
     * - "notice"  → "通知"
     * - 其他/null → 原值或空字符串
     *
     * 使用场景：
     * - 公告列表项的类型标签
     * - 公告详情页的类型标识
     *
     * @return 类型的中文显示文本，如"版本更新"、"活动"、"通知"
     */
    public String getTypeDisplay() {
        if (type == null) return "";
        switch (type) {
            case "version": return "版本更新";
            case "event": return "活动";
            case "notice": return "通知";
            default: return type;
        }
    }
}
