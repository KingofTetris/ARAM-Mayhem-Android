package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 符文推荐响应模型 ── 继承 AugmentResponse，含推荐评分和理由
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 AugmentApi.getRecommendations() 获取符文推荐时，
 * 后端返回的每个推荐符文会被 Gson 反序列化为这个类的实例。
 *
 * 继承 AugmentResponse，拥有符文的基础信息（id, nameZh, quality 等），
 * 额外包含推荐相关数据（评分、推荐理由）。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、推荐评分机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * score（评分）范围 0.0 ~ 100.0：
 * - 90+：强烈推荐，最优选择
 * - 70~89：推荐，不错的选择
 * - 50~69：一般，可以考虑
 * - <50：不推荐，有更好的选择
 *
 * 评分由后端推荐算法综合计算，考虑因素：
 * - 英雄适配度：该符文对当前英雄的增益效果
 * - 套装完成度：选择后能完成套装的优先级更高
 * - 协同效应：与已有符文的搭配效果
 * - 胜率数据：选择该符文后的历史胜率
 *
 * 关联类：
 * - AugmentResponse：符文列表项 DTO（父类）
 * - AugmentRecommendRequest：推荐请求 DTO
 * - AugmentApi：符文 API 接口
 */
public class AugmentRecommendResponse extends AugmentResponse {

    /**
     * 推荐评分 ── 范围 0.0 ~ 100.0
     * 越高表示越推荐选择该符文
     */
    @SerializedName("score")
    private double score;

    /**
     * 推荐理由 ── 解释为什么推荐该符文
     * 如："与已选符文组成战士3件套，攻击力+20%"
     * 或："该符文对亚索的适配度极高，Q技能冷却缩减+30%"
     */
    @SerializedName("recommendationReason")
    private String recommendationReason;

    public double getScore() {
        return score;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    /**
     * 获取评分百分比 ── 用于 UI 展示进度条
     *
     * 将 score 转换为 0~100 的整数百分比，
     * 最大值限制为 100（防止 UI 溢出）。
     *
     * @return 评分百分比（0~100）
     */
    public int getScorePercent() {
        return (int) Math.min(100, score);
    }
}
