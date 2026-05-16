package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.aram.mayhem.common.Tier;

/**
 * 梯级标签组件
 *
 * 功能：显示英雄/符文梯级评级（S+/S/A/B/C），根据梯级自动着色
 * 颜色映射：Tier 枚举的 color 字段
 * 用途：列表卡片和详情页的梯级标签
 */
public class TierBadgeView extends AppCompatTextView {

    private Tier tier;

    public TierBadgeView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public TierBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TierBadgeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setGravity(Gravity.CENTER);
        setAllCaps(true);
        setTextColor(Color.WHITE);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        setTypeface(null, android.graphics.Typeface.BOLD);
        int paddingH = dpToPx(context, 8);
        int paddingV = dpToPx(context, 2);
        setPadding(paddingH, paddingV, paddingH, paddingV);
        setMinWidth(dpToPx(context, 28));
        setBackgroundResource(com.aram.mayhem.ui.R.drawable.bg_tier_badge);
    }

    public void setTier(@NonNull Tier tier) {
        this.tier = tier;
        setText(tier.getLabel());
        int color = tier.getColor();
        setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Nullable
    public Tier getTier() {
        return tier;
    }

    private int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
