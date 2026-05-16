package com.aram.mayhem.common;

import com.google.gson.annotations.SerializedName;

/**
 * 英雄/符文梯级评级枚举
 *
 * 评级从高到低：S+(红) > S(橙) > A(金) > B(绿) > C(蓝)
 * 用于列表页梯级标签着色和筛选
 */
public enum Tier {
    @SerializedName("S_PLUS")
    S_PLUS("S+", 0xFFE53E3E),

    @SerializedName("S")
    S("S", 0xFFFF6B35),

    @SerializedName("A")
    A("A", 0xFFFFB800),

    @SerializedName("B")
    B("B", 0xFF4CAF50),

    @SerializedName("C")
    C("C", 0xFF2196F3);

    private final String label;
    private final int color;

    Tier(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }
}
