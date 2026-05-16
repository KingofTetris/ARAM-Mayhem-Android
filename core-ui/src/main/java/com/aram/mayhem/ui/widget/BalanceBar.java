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
 * 平衡条组件
 *
 * 功能：显示胜率/选取率的进度条，支持渐变色
 * 用途：英雄/符文列表卡片中的胜率条
 */
public class BalanceBar extends View {

    private static final int COLOR_POSITIVE = 0xFF4CAF50;
    private static final int COLOR_NEGATIVE = 0xFFE53E3E;
    private static final int COLOR_NEUTRAL = 0xFF9E9E9E;
    private static final int COLOR_TEXT = 0xFF212121;
    private static final int COLOR_TRACK = 0xFFE0E0E0;

    private String label = "";
    private float value = 0f;
    private float maxValue = 20f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF barRect = new RectF();

    private float barHeight;
    private float textHeight;
    private float padding;

    public BalanceBar(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BalanceBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BalanceBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        barHeight = dpToPx(8);
        textHeight = spToPx(12);
        padding = dpToPx(4);

        trackPaint.setColor(COLOR_TRACK);
        trackPaint.setStyle(Paint.Style.FILL);

        barPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(COLOR_TEXT);
        textPaint.setTextSize(textHeight);

        valuePaint.setColor(COLOR_TEXT);
        valuePaint.setTextSize(textHeight);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
    }

    public void setData(@NonNull String label, float value, float maxValue) {
        this.label = label;
        this.value = value;
        this.maxValue = Math.max(maxValue, 0.01f);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float labelWidth = width * 0.3f;
        float barAreaWidth = width * 0.5f;
        float valueWidth = width * 0.2f;

        float x = getPaddingLeft();
        float y = getPaddingTop();

        float textY = y + textHeight + padding;

        canvas.drawText(label, x, textY, textPaint);

        float barStartX = x + labelWidth + padding;
        float barEndX = barStartX + barAreaWidth;
        float barY = textY - barHeight / 2f - padding / 2f;

        trackRect.set(barStartX, barY - barHeight / 2f, barEndX, barY + barHeight / 2f);
        canvas.drawRoundRect(trackRect, barHeight / 2f, barHeight / 2f, trackPaint);

        float centerBarX = (barStartX + barEndX) / 2f;
        float ratio = value / maxValue;
        float barWidth = Math.abs(ratio) * (barAreaWidth / 2f);

        int barColor;
        if (value > 0) {
            barColor = COLOR_POSITIVE;
            barRect.set(centerBarX, barY - barHeight / 2f, centerBarX + barWidth, barY + barHeight / 2f);
        } else if (value < 0) {
            barColor = COLOR_NEGATIVE;
            barRect.set(centerBarX - barWidth, barY - barHeight / 2f, centerBarX, barY + barHeight / 2f);
        } else {
            barColor = COLOR_NEUTRAL;
            barRect.set(centerBarX - 1, barY - barHeight / 2f, centerBarX + 1, barY + barHeight / 2f);
        }

        barPaint.setColor(barColor);
        canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, barPaint);

        String valueText = (value >= 0 ? "+" : "") + String.format("%.1f%%", value);
        valuePaint.setColor(barColor);
        canvas.drawText(valueText, x + width, textY, valuePaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) (textHeight + barHeight + padding * 3 + getPaddingTop() + getPaddingBottom());
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, desiredHeight);
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getContext().getResources().getDisplayMetrics());
    }
}
