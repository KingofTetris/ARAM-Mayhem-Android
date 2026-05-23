package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 符文详情响应模型 ── 继承 AugmentResponse，对应后端 AugmentVO
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 AugmentApi.getAugmentDetail() 获取符文详情时，
 * 响应中的 data 字段会被 Gson 反序列化为这个类的实例。
 *
 * 继承 AugmentResponse，拥有列表项的所有字段（id, nameZh, quality 等），
 * 额外包含详情数据（描述、额外套装、陷阱标记）。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、多套装机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 一个符文可以属于多个套装：
 * - synergySet（父类）：主套装，如"战士"
 * - synergySet2：副套装，如"坦克"
 * - synergySet3：第三套装，如"辅助"
 *
 * 例如"钢铁之心"符文：
 * - 主套装：战士（2件套：+10% 攻击力）
 * - 副套装：坦克（3件套：+15% 最大生命值）
 * - 第三套装：辅助（2件套：+8% 护盾效果）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、陷阱符文（Trap Augment）
 * ═══════════════════════════════════════════════════════════════════
 *
 * isTrap = true 表示该符文看似很强（如高选取率），
 * 但实际胜率很低，属于"陷阱"选择。
 * 详情页会用红色标记提醒玩家。
 *
 * 关联类：
 * - AugmentResponse：符文列表项 DTO（父类）
 * - AugmentApi：符文 API 接口
 */
public class AugmentDetailResponse extends AugmentResponse {

    /** 符文详细描述 ── 包含效果数值和触发条件 */
    @SerializedName("description")
    private String description;

    /**
     * 第二套装 ── 符文可以属于多个套装
     * null 表示该符文不属于第二套装
     */
    @SerializedName("synergySet2")
    private String synergySet2;

    /**
     * 第三套装 ── 符文最多可属于三个套装
     * null 表示该符文不属于第三套装
     */
    @SerializedName("synergySet3")
    private String synergySet3;

    /**
     * 是否为陷阱符文 ── true 表示该符文看似强势但实际胜率低
     * 详情页会用红色标记提醒玩家
     */
    @SerializedName("isTrap")
    private Boolean isTrap;

    public String getDescription() {
        return description;
    }

    public String getSynergySet2() {
        return synergySet2;
    }

    public String getSynergySet3() {
        return synergySet3;
    }

    public Boolean getIsTrap() {
        return isTrap;
    }
}
