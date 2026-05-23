package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 符文推荐请求模型 ── 发送给后端的推荐请求参数
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 AugmentApi.getRecommendations() 获取符文推荐时，
 * 需要告诉后端当前的游戏状态：
 * 1. 正在玩哪个英雄（heroId）
 * 2. 已经选择了哪些符文（selectedAugmentIds）
 *
 * 后端根据这些信息，结合推荐算法，返回最优的符文选择建议。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么需要 selectedAugmentIds？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 推荐算法需要考虑已有符文的协同效应：
 * - 如果已有2个"战士"套装符文，推荐第3个"战士"符文可以完成套装
 * - 如果已有符文与某符文冲突（效果不叠加），则不推荐
 * - 已有符文决定了剩余符文的搭配空间
 *
 * 关联类：
 * - AugmentRecommendResponse：推荐响应 DTO
 * - AugmentApi：符文 API 接口
 */
public class AugmentRecommendRequest {

    /**
     * 英雄 ID ── 当前正在玩的英雄
     * 不同英雄适合不同的符文组合
     * 如 157（亚索）适合暴击和攻速符文
     */
    @SerializedName("heroId")
    private long heroId;

    /**
     * 已选符文 ID 列表 ── 当前游戏中已选择的符文
     * 可以为空列表（第一轮选择时没有任何符文）
     * 后端会根据已有符文推荐最优的下一步选择
     */
    @SerializedName("selectedAugmentIds")
    private List<Long> selectedAugmentIds;

    /**
     * 构造推荐请求体
     *
     * @param heroId 英雄 ID
     * @param selectedAugmentIds 已选符文 ID 列表（可以为空列表）
     */
    public AugmentRecommendRequest(long heroId, List<Long> selectedAugmentIds) {
        this.heroId = heroId;
        this.selectedAugmentIds = selectedAugmentIds;
    }

    public long getHeroId() {
        return heroId;
    }

    public List<Long> getSelectedAugmentIds() {
        return selectedAugmentIds;
    }
}
