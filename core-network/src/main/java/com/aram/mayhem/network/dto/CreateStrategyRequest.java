package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 创建攻略请求模型 ── 发送给后端的发布攻略参数
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 CommunityApi.publishStrategy() 发布攻略时，
 * 需要将攻略内容封装为此类实例发送给后端。
 *
 * 数据流向：PublishStrategyFragment → CommunityViewModel → CommunityRepository → CommunityApi
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、字段说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * heroId：攻略针对的英雄（必填）
 * title：攻略标题（必填）
 * description：攻略正文（必填）
 * augmentIds：推荐的符文 ID 列表（可选）
 * itemIds：推荐的装备 ID 列表（可选）
 *
 * 作者信息不需要传，后端从 JWT Token 中提取 userId。
 *
 * 关联类：
 * - StrategyDetailResponse：发布成功后返回的攻略详情
 * - CommunityApi：社区 API 接口
 */
public class CreateStrategyRequest {

    /** 攻略针对的英雄 ID */
    @SerializedName("heroId")
    private Long heroId;

    /** 攻略标题 */
    @SerializedName("title")
    private String title;

    /** 攻略正文描述 */
    @SerializedName("description")
    private String description;

    /** 推荐符文 ID 列表（可选，可以为空列表） */
    @SerializedName("augmentIds")
    private List<Long> augmentIds;

    /** 推荐装备 ID 列表（可选，可以为空列表） */
    @SerializedName("itemIds")
    private List<Long> itemIds;

    /**
     * 构造发布攻略请求体
     *
     * @param heroId 英雄 ID
     * @param title 标题
     * @param description 正文
     * @param augmentIds 推荐符文 ID 列表
     * @param itemIds 推荐装备 ID 列表
     */
    public CreateStrategyRequest(Long heroId, String title, String description, List<Long> augmentIds, List<Long> itemIds) {
        this.heroId = heroId;
        this.title = title;
        this.description = description;
        this.augmentIds = augmentIds;
        this.itemIds = itemIds;
    }
}
