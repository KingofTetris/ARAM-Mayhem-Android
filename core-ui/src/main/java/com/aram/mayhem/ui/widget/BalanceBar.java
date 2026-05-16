package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 平衡条组件（核心UI模块）
 *
 * 功能：显示胜率/选取率的水平进度条，支持正值/负值的差异化颜色显示
 * 颜色规则：正值-绿色(0xFF4CAF50)、负值-红色(0xFFE53E3E)、零值-灰色(0xFF9E9E9E)
 * 布局结构：左侧标签(30%) + 中间进度条(50%) + 右侧数值(20%)
 * 用途：英雄/符文详情页的胜率、选取率等数据展示
 * 自定义绘制：继承 View 重写 onDraw 实现自定义绘制逻辑
 */
public class BalanceBar extends View {

    // 颜色常量定义
    /** 正值颜色（绿色） */
    private static final int COLOR_POSITIVE = 0xFF4CAF50;
    /** 负值颜色（红色） */
    private static final int COLOR_NEGATIVE = 0xFFE53E3E;
    /** 零值颜色（灰色） */
    private static final int COLOR_NEUTRAL = 0xFF9E9E9E;
    /** 文字颜色 */
    private static final int COLOR_TEXT = 0xFF212121;
    /** 轨道背景颜色 */
    private static final int COLOR_TRACK = 0xFFE0E0E0;

    // 数据字段
    /** 标签文字（如"胜率"、"选取率"） */
    private String label = "";
    /** 当前值（百分比形式，如 52.5 表示 52.5%） */
    private float value = 0f;
    /** 最大值（用于计算比例） */
    private float maxValue = 20f;

    // 绘制工具
    /** 轨道绘制画笔 */
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** 进度条绘制画笔 */
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** 标签文字绘制画笔 */
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** 数值文字绘制画笔 */
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** 轨道矩形区域 */
    private final RectF trackRect = new RectF();
    /** 进度条矩形区域 */
    private final RectF barRect = new RectF();

    // 尺寸参数
    /** 进度条高度 */
    private float barHeight;
    /** 文字高度 */
    private float textHeight;
    /** 内边距 */
    private float padding;

    /**
     * 构造函数（代码调用）
     *
     * @param context 上下文
     */
    public BalanceBar(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     *
     * @param context 上下文
     * @param attrs   属性集合
     */
    public BalanceBar(@NonNull Context context, @Nullable AttributeSet attrs) {
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
    public BalanceBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 初始化尺寸（转换为像素）
        barHeight = dpToPx(8);
        textHeight = spToPx(12);
        padding = dpToPx(4);

        // 配置轨道画笔
        trackPaint.setColor(COLOR_TRACK);
        trackPaint.setStyle(Paint.Style.FILL);

        // 配置进度条画笔
        barPaint.setStyle(Paint.Style.FILL);

        // 配置标签文字画笔
        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextSize(textHeight);

        // 配置数值文字画笔（右对齐）
        valuePaint.setColor(COLOR_TEXT);
        valuePaint.setTextSize(textHeight);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
    }

    /**
     * 设置数据并刷新视图
     *
     * @param label    标签文字（如"胜率"）
     * @param value    当前值（百分比，如 52.5）
     * @param maxValue 最大值（用于计算进度条比例）
     */
    public void setData(@NonNull String label, float value, float maxValue) {
        this.label = label;
        this.value = value;
        // 防止除零错误，最小值设为 0.01
        this.maxValue = Math.max(maxValue, 0.01f);
        // 触发重绘
        invalidate();
    }

    /**
     * 自定义绘制方法
     *
     * @param canvas 画布对象
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 计算可用宽度
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        
        // 分配区域比例：标签30% + 进度条50% + 数值20%
        float labelWidth = width * 0.3f;
        float barAreaWidth = width * 0.5f;
        float valueWidth = width * 0.2f;

        // 起始坐标
        float x = getPaddingLeft();
        float y = getPaddingTop();

        // 文字基线位置
        float textY = y + textHeight + padding;

        // 绘制标签文字（如"胜率"）
        canvas.drawText(label, x, textY, textPaint);

        // 计算进度条位置
        float barStartX = x + labelWidth + padding;
        float barEndX = barStartX + barAreaWidth;
        float barY = textY - barHeight / 2f - padding / 2f;

        // 绘制轨道背景
        trackRect.set(barStartX, barY - barHeight / 2f, barEndX, barY + barHeight / 2f);
        canvas.drawRoundRect(trackRect, barHeight / 2f, barHeight / 2f, trackPaint);

        // 计算进度条宽度和方向
        float centerBarX = (barStartX + barEndX) / 2f;  // 中心点（零值位置）
        float ratio = value / maxValue;                    // 比例
        float barWidth = Math.abs(ratio) * (barAreaWidth / 2f);  // 进度条宽度（单边最大占50%）

        // 根据正负值设置颜色和位置
        int barColor;
        if (value > 0) {
            // 正值：向右延伸（绿色）
            barColor = COLOR_POSITIVE;
            barRect.set(centerBarX, barY - barHeight / 2f, centerBarX + barWidth, barY + barHeight / 2f);
        } else if (value < 0) {
            // 负值：向左延伸（红色）
            barColor = COLOR_NEGATIVE;
            barRect.set(centerBarX - barWidth, barY - barHeight / 2f, centerBarX, barY + barHeight / 2f);
        } else {
            // 零值：中心点细线（灰色）
            barColor = COLOR_NEUTRAL;
            barRect.set(centerBarX - 1, barY - barHeight / 2f, centerBarX + 1, barY + barHeight / 2f);
        }

        // 绘制进度条
        barPaint.setColor(barColor);
        canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, barPaint);

        // 绘制数值文字（带正负号）
        String valueText = (value >= 0 ? "+" : "") + String.format("%.1f%%", value);
        valuePaint.setColor(barColor);
        canvas.drawText(valueText, x + width, textY, valuePaint);
    }

    /**
     * 测量尺寸
     *
     * @param widthMeasureSpec  宽度测量规格
     * @param heightMeasureSpec 高度测量规格
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 计算期望高度：文字高度 + 进度条高度 + 内边距
        int desiredHeight = (int) (textHeight + barHeight + padding * 3 + getPaddingTop() + getPaddingBottom());
        // 宽度使用父容器分配的宽度
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, desiredHeight);
    }

    /**
     * dp 转 px 工具方法
     *
     * @param dp dp 值
     * @return px 值
     */
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    /**
     * sp 转 px 工具方法
     *
     * @param sp sp 值
     * @return px 值
     */
    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getContext().getResources().getDisplayMetrics());
    }
}
