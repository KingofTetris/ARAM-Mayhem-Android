package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

public class BulletinResponse {

    @SerializedName("id")
    private long id;

    @SerializedName("type")
    private String type;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("isPinned")
    private Integer isPinned;

    @SerializedName("publishedAt")
    private String publishedAt;

    @SerializedName("createdAt")
    private String createdAt;

    public long getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getImageUrl() { return imageUrl; }
    public Integer getIsPinned() { return isPinned; }
    public String getPublishedAt() { return publishedAt; }
    public String getCreatedAt() { return createdAt; }
}
