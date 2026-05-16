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
 * 梯级标签组件（核心UI模块）
 *
 * 功能：显示英雄/符文梯级评级（S+/S/A/B/C），根据梯级自动着色
 * 颜色映射：Tier 枚举的 color 字段（S+/金色、S/橙色、A/绿色、B/蓝色、C/灰色）
 * 用途：英雄列表卡片、符文列表卡片、英雄详情页、符文详情页的梯级标签展示
 * 布局：圆角矩形背景 + 居中文字
 */
public class TierBadgeView extends AppCompatTextView {

    /** 当前梯级 */
    private Tier tier;

    /**
     * 构造函数（代码调用）
     *
     * @param context 上下文
     */
    public TierBadgeView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    /**
     * 构造函数（XML 布局调用）
     *
     * @param context 上下文
     * @param attrs   属性集合
     */
    public TierBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * 构造函数（带样式）
     *
     * @param context      上下文
     * @param attrs        属性集合
     * @param defStyleAttr 默认样式属性
     */
    public TierBadgeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * 初始化组件样式
     *
     * @param context 上下文
     * @param attrs   属性集合（预留扩展）
     */
    private void init(Context context, AttributeSet attrs) {
        // 设置文字居中
        setGravity(Gravity.CENTER);
        // 文字大写
        setAllCaps(true);
        // 白色文字
        setTextColor(Color.WHITE);
        // 字号 11sp
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        // 加粗字体
        setTypeface(null, android.graphics.Typeface.BOLD);
        
        // 设置内边距（水平8dp，垂直2dp）
        int paddingH = dpToPx(context, 8);
        int paddingV = dpToPx(context, 2);
        setPadding(paddingH, paddingV, paddingH, paddingV);
        
        // 设置最小宽度（确保标签不会太窄）
        setMinWidth(dpToPx(context, 28));
        
        // 设置圆角背景
        setBackgroundResource(com.aram.mayhem.ui.R.drawable.bg_tier_badge);
    }

    /**
     * 设置梯级并更新显示
     *
     * @param tier 梯级枚举值（S+/S/A/B/C）
     */
    public void setTier(@NonNull Tier tier) {
        this.tier = tier;
        // 设置梯级标签文字（如 "S+"）
        setText(tier.getLabel());
        // 获取梯级对应的颜色
        int color = tier.getColor();
        // 设置背景色
        setBackgroundTintList(ColorStateList.valueOf(color));
    }

    /**
     * 获取当前梯级
     *
     * @return 当前梯级，未设置时返回 null
     */
    @Nullable
    public Tier getTier() {
        return tier;
    }

    /**
     * dp 转 px 工具方法
     *
     * @param context 上下文
     * @param dp      dp 值
     * @return px 值
     */
    private int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
