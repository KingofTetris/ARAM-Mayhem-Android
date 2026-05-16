package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.chip.Chip;

/**
 * 符文品质标签组件
 *
 * 功能：显示符文品质（银色/金色/棱彩），根据品质自动着色
 * 用途：符文列表卡片和详情页
 */
public class QualityChip extends Chip {

    private String quality;

    public QualityChip(@NonNull Context context) {
        super(context);
        init(context);
    }

    public QualityChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public QualityChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setCheckable(true);
        setCheckedIconVisible(false);
        setChipStrokeWidth(2f);
        updateAppearance();
    }

    public void setQuality(@NonNull String quality) {
        this.quality = quality;
        updateAppearance();
    }

    private void updateAppearance() {
        if (quality == null) return;

        int backgroundColor;
        int strokeColor;
        int textColor;
        String label;

        switch (quality) {
            case "PRISMATIC":
                backgroundColor = 0x339C27B0;
                strokeColor = 0xFF9C27B0;
                textColor = 0xFF9C27B0;
                label = "棱彩";
                break;
            case "GOLD":
                backgroundColor = 0x33FFB300;
                strokeColor = 0xFFFFB300;
                textColor = 0xFFFF8F00;
                label = "金";
                break;
            default:
                backgroundColor = 0x339E9E9E;
                strokeColor = 0xFF9E9E9E;
                textColor = 0xFF757575;
                label = "银";
                break;
        }

        setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        setChipStrokeColor(ColorStateList.valueOf(strokeColor));
        setTextColor(textColor);
        setText(label);
    }

    @Nullable
    public String getQuality() {
        return quality;
    }
}
