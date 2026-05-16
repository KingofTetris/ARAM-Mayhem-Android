package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.chip.Chip;

/**
 * 符文品质标签组件（核心UI模块）
 *
 * 功能：显示符文品质（银色/金色/棱彩），根据品质自动着色
 * 品质映射：PRISMATIC-棱彩（紫色）、GOLD-金（金色）、其他-银（灰色）
 * 用途：符文列表卡片、符文详情页的品质标签展示
 * 特性：支持勾选状态切换，选中后显示对应品质颜色
 */
public class QualityChip extends Chip {

    /** 当前品质值 */
    private String quality;

    /**
     * 构造函数（代码调用）
     *
     * @param context 上下文
     */
    public QualityChip(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     *
     * @param context 上下文
     * @param attrs   属性集合
     */
    public QualityChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数（带样式）
     *
     * @param context      上下文
     * @param attrs        属性集合
     * @param defStyleAttr 默认样式属性
     */
    public QualityChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件样式
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 设置为可勾选状态
        setCheckable(true);
        // 隐藏勾选图标（使用颜色变化表示选中状态）
        setCheckedIconVisible(false);
        // 设置边框宽度
        setChipStrokeWidth(2f);
        // 更新外观
        updateAppearance();
    }

    /**
     * 设置品质并更新显示
     *
     * @param quality 品质值（PRISMATIC/GOLD/EPIC/COMMON）
     */
    public void setQuality(@NonNull String quality) {
        this.quality = quality;
        updateAppearance();
    }

    /**
     * 根据品质更新外观样式
     */
    private void updateAppearance() {
        // 如果品质为空，不进行更新
        if (quality == null) return;

        // 根据品质定义颜色和标签
        int backgroundColor;
        int strokeColor;
        int textColor;
        String label;

        switch (quality) {
            case "PRISMATIC":
                // 棱彩品质：紫色系
                backgroundColor = 0x339C27B0;  // 半透明紫色背景
                strokeColor = 0xFF9C27B0;      // 紫色边框
                textColor = 0xFF9C27B0;        // 紫色文字
                label = "棱彩";
                break;
            case "GOLD":
                // 金色品质：金色系
                backgroundColor = 0x33FFB300;  // 半透明金色背景
                strokeColor = 0xFFFFB300;      // 金色边框
                textColor = 0xFFFF8F00;        // 金色文字
                label = "金";
                break;
            default:
                // 银色品质：灰色系（默认）
                backgroundColor = 0x339E9E9E;  // 半透明灰色背景
                strokeColor = 0xFF9E9E9E;      // 灰色边框
                textColor = 0xFF757575;        // 灰色文字
                label = "银";
                break;
        }

        // 应用样式
        setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        setChipStrokeColor(ColorStateList.valueOf(strokeColor));
        setTextColor(textColor);
        setText(label);
    }

    /**
     * 获取当前品质
     *
     * @return 当前品质值，未设置时返回 null
     */
    @Nullable
    public String getQuality() {
        return quality;
    }
}
