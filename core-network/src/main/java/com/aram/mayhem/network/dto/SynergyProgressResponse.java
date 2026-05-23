package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 套装进度响应模型 ── 对应后端 SynergyProgressResponse，展示套装完成度
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 AugmentApi.getSynergyProgress() 查看套装进度时，
 * 后端返回的每个套装进度会被 Gson 反序列化为这个类的实例。
 *
 * 数据流向：AugmentApi → AugmentRepository → AugmentViewModel → SynergyProgressSection
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、套装进度示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 假设玩家已拥有 2 个"战士"套装符文，套装需要 3 个符文完成：
 * - synergyName = "shield"（护盾套装）
 * - currentCount = 2（已有 2 个）
 * - totalCount = 3（需要 3 个）
 * - progress = 0.6667（66.67%）
 * - status = "IN_PROGRESS"（进行中）
 * - avgWinRate = 0.5434（完成该套装的平均胜率 54.34%）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、status 状态说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * - NOT_STARTED：未开始，当前没有该套装的任何符文
 * - IN_PROGRESS：进行中，已有部分符文但未完成
 * - COMPLETED：已完成，集齐了套装所需的所有符文
 *
 * 关联类：
 * - AugmentApi：符文 API 接口
 */
public class SynergyProgressResponse {

    /** 套装英文名 ── 如 "shield"、"regeneration"、"attack-speed" */
    @SerializedName("synergyName")
    private String synergyName;

    /** 当前已拥有的该套装符文数量 */
    @SerializedName("currentCount")
    private int currentCount;

    /** 完成该套装所需的符文总数 */
    @SerializedName("totalCount")
    private int totalCount;

    /**
     * 完成进度 ── 范围 0.0 ~ 1.0
     * progress = currentCount / totalCount
     * 如 2/3 = 0.6667
     */
    @SerializedName("progress")
    private double progress;

    /**
     * 套装状态
     * NOT_STARTED：未开始（0个符文）
     * IN_PROGRESS：进行中（部分符文）
     * COMPLETED：已完成（全部符文）
     */
    @SerializedName("status")
    private String status;

    /**
     * 该套装的平均胜率 ── 完成该套装后的历史胜率
     * 范围 0.0 ~ 1.0，null 表示数据不足
     */
    @SerializedName("avgWinRate")
    private Double avgWinRate;

    public String getSynergyName() {
        return synergyName;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public double getProgress() {
        return progress;
    }

    public String getStatus() {
        return status;
    }

    public Double getAvgWinRate() {
        return avgWinRate;
    }

    /**
     * 获取进度百分比 ── 用于 UI 展示进度条
     *
     * 将 progress（0.0~1.0）转换为整数百分比（0~100）
     *
     * @return 进度百分比（0~100）
     */
    public int getProgressPercent() {
        return (int) (progress * 100);
    }

    /**
     * 获取套装中文显示名 ── 将英文套装名转为中文
     *
     * 后端存储的是英文标识符（如 "shield"），
     * 前端需要显示中文名称（如 "护盾"）。
     * 如果是未知套装名，直接返回英文名。
     *
     * @return 套装中文显示名
     */
    public String getDisplayName() {
        if (synergyName == null) return "";
        switch (synergyName) {
            case "shield": return "护盾";
            case "regeneration": return "回复";
            case "shield-break": return "破盾";
            case "attack-speed": return "攻速";
            case "ability-power": return "法强";
            case "omnivamp": return "吸血";
            case "armor-penetration": return "护甲穿透";
            case "critical-strike": return "暴击";
            case "tenacity": return "韧性";
            default: return synergyName;
        }
    }
}
