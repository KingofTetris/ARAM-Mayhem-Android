package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * 版本陷阱横幅组件
 *
 * 功能：在英雄/符文详情页顶部显示"版本陷阱"警告横幅
 * 触发条件：isVersionTrap = true 时显示
 * 布局：横向警告图标 + 提示文字 + 关闭按钮
 */
public class VersionTrapBanner extends LinearLayout {

    private TextView textWarning;

    public VersionTrapBanner(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VersionTrapBanner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VersionTrapBanner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(24, 16, 24, 16);

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFFD32F2F);
        background.setCornerRadius(8);
        setBackground(background);

        ImageView warningIcon = new ImageView(context);
        warningIcon.setImageResource(android.R.drawable.ic_dialog_alert);
        warningIcon.setColorFilter(0xFFFFFFFF);
        LayoutParams iconParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        iconParams.setMarginEnd(12);
        warningIcon.setLayoutParams(iconParams);
        addView(warningIcon);

        textWarning = new TextView(context);
        textWarning.setTextColor(0xFFFFFFFF);
        textWarning.setTextSize(14);
        textWarning.setTypeface(null, Typeface.BOLD);
        textWarning.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(textWarning);

        setVisibility(GONE);
    }

    public void setTrapInfo(String version) {
        textWarning.setText("⚠ 该英雄在" + version + "版本被大幅削弱，慎用！");
        setVisibility(VISIBLE);
    }

    public void setTrapInfo(String name, String version) {
        textWarning.setText("⚠ " + name + "在" + version + "版本被大幅削弱，慎用！");
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }
}
