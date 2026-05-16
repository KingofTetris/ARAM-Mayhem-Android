package com.aram.mayhem.ui.model;

/**
 * 公告 UI 模型
 *
 * 数据流向：BulletinResponse → BulletinUiModel → BulletinAdapter/BulletinCarouselView
 * 用途：公告列表和轮播图展示
 */
public class BulletinUiModel {

    private final long id;
    private final String type;
    private final String title;
    private final String content;
    private final String imageUrl;
    private final boolean pinned;
    private final String publishedAt;
    private final String createdAt;

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

    public long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public boolean isPinned() { return pinned; }
    public String getPublishedAt() { return publishedAt; }
    public String getCreatedAt() { return createdAt; }

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
