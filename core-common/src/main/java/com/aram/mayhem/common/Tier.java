package com.aram.mayhem.common;

import com.google.gson.annotations.SerializedName;

/**
 * 英雄/符文梯级评级枚举
 *
 * 评级从高到低：S+(红) > S(橙) > A(金) > B(绿) > C(蓝)
 * 用于列表页梯级标签着色和筛选
 */
public enum Tier {
    /** S+级（最高评级，红色） */
    @SerializedName("S_PLUS")
    S_PLUS("S+", 0xFFE53E3E),

    /** S级（橙色） */
    @SerializedName("S")
    S("S", 0xFFFF6B35),

    /** A级（金色） */
    @SerializedName("A")
    A("A", 0xFFFFB800),

    /** B级（绿色） */
    @SerializedName("B")
    B("B", 0xFF4CAF50),

    /** C级（最低评级，蓝色） */
    @SerializedName("C")
    C("C", 0xFF2196F3);

    /** 梯级标签显示文本（如 "S+"） */
    private final String label;

    /** 梯级对应的颜色值（ARGB格式） */
    private final int color;

    /**
     * 构造函数
     *
     * @param label 显示标签
     * @param color 颜色值（ARGB）
     */
    Tier(String label, int color) {
        this.label = label;
        this.color = color;
    }

    /**
     * 获取梯级显示标签
     *
     * @return 标签文本
     */
    public String getLabel() {
        return label;
    }

    /**
     * 获取梯级颜色
     *
     * @return ARGB颜色值
     */
    public int getColor() {
        return color;
    }
}
