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
 * 平衡条组件 ── 自定义绘制的水平进度条，显示胜率/选取率偏差
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * BalanceBar 是英雄/符文详情页中显示数据偏差的自定义进度条。
 * 它不是普通的 0→100% 进度条，而是以中心为零点的双向进度条：
 * - 正值（>0）：向右延伸，绿色（表示高于平均）
 * - 负值（<0）：向左延伸，红色（表示低于平均）
 * - 零值（=0）：中心细线，灰色（表示等于平均）
 *
 * 视觉结构：
 * ┌─────────────────────────────────────────────────────┐
 * │ 胜率        ▓▓▓▓▓▓▓▓▓▓▓▓│           +5.3%          │
 * │ 选取率              │▓▓▓▓▓▓▓       -2.1%            │
 * │ KDA    ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│    +12.7%          │
 * └─────────────────────────────────────────────────────┘
 *  ← 标签(30%) → ← 进度条(50%) → ← 数值(20%) →
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么继承 View 而不是用 ProgressBar？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 系统 ProgressBar 的限制：
 * - 只支持单向（0→max），不支持中心零点双向
 * - 不支持正负值不同颜色
 * - 不支持标签+进度条+数值的三段式布局
 * - 不支持圆角矩形进度条
 *
 * 自定义 View 的优势：
 * - 完全控制绘制逻辑（onDraw）
 * - 支持任意布局和颜色
 * - 性能更好（无额外视图层级）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、自定义 View 绘制流程
 * ═══════════════════════════════════════════════════════════════════
 *
 * Android 自定义 View 的核心方法：
 * 1. onMeasure()：测量视图大小（告诉父容器我需要多大空间）
 * 2. onDraw()：绘制视图内容（用 Canvas 画布绘制图形和文字）
 *
 * 绘制工具：
 * - Paint（画笔）：定义颜色、粗细、样式等
 * - Canvas（画布）：提供绘制方法（drawRect、drawText、drawRoundRect）
 * - RectF（矩形）：定义绘制区域
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数值计算示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 假设：value = 5.3, maxValue = 20
 * - ratio = 5.3 / 20 = 0.265
 * - barWidth = 0.265 * (barAreaWidth / 2) = 0.265 * 50% = 13.25% 总宽度
 * - 从中心点向右延伸 13.25%，颜色为绿色
 *
 * 假设：value = -2.1, maxValue = 20
 * - ratio = -2.1 / 20 = -0.105
 * - barWidth = 0.105 * (barAreaWidth / 2) = 5.25% 总宽度
 * - 从中心点向左延伸 5.25%，颜色为红色
 */
public class BalanceBar extends View {

    // ═══════════════════════════════════════════════════════════════
    // 颜色常量 ── 使用 ARGB 格式（0xAARRGGBB）
    // ═══════════════════════════════════════════════════════════════

    /** 正值颜色（绿色）── Material Design Green 500 */
    private static final int COLOR_POSITIVE = 0xFF4CAF50;

    /** 负值颜色（红色）── 自定义红色 */
    private static final int COLOR_NEGATIVE = 0xFFE53E3E;

    /** 零值颜色（灰色）── Material Design Grey 500 */
    private static final int COLOR_NEUTRAL = 0xFF9E9E9E;

    /** 文字颜色（深灰）── Material Design Grey 900 */
    private static final int COLOR_TEXT = 0xFF212121;

    /** 轨道背景颜色（浅灰）── Material Design Grey 300 */
    private static final int COLOR_TRACK = 0xFFE0E0E0;

    // ═══════════════════════════════════════════════════════════════
    // 数据字段
    // ═══════════════════════════════════════════════════════════════

    /** 标签文字（如"胜率"、"选取率"、"KDA"） */
    private String label = "";

    /** 当前值（百分比偏差，如 +5.3 表示高于平均 5.3%） */
    private float value = 0f;

    /** 最大值（用于计算进度条比例，如 20 表示 ±20% 是最大范围） */
    private float maxValue = 20f;

    // ═══════════════════════════════════════════════════════════════
    // 绘制工具
    // ═══════════════════════════════════════════════════════════════

    /**
     * 轨道绘制画笔 ── 绘制进度条背景（浅灰色圆角矩形）
     * Paint.ANTI_ALIAS_FLAG：开启抗锯齿，让边缘更平滑
     */
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 进度条绘制画笔 ── 绘制彩色进度条（颜色动态设置）
     * Style.FILL：填充模式（画实心矩形）
     */
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 标签文字绘制画笔 ── 绘制左侧标签文字（如"胜率"）
     * 默认左对齐（Paint.Align.LEFT）
     */
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 数值文字绘制画笔 ── 绘制右侧数值文字（如"+5.3%"）
     * 设置为右对齐（Paint.Align.RIGHT），方便从右侧边缘向左绘制
     */
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * 轨道矩形区域 ── 定义进度条背景的绘制范围
     * set(left, top, right, bottom) 设置四个边的坐标
     */
    private final RectF trackRect = new RectF();

    /**
     * 进度条矩形区域 ── 定义彩色进度条的绘制范围
     * 根据正负值动态计算左右坐标
     */
    private final RectF barRect = new RectF();

    // ═══════════════════════════════════════════════════════════════
    // 尺寸参数
    // ═══════════════════════════════════════════════════════════════

    /** 进度条高度（像素）── 8dp 转换后的值 */
    private float barHeight;

    /** 文字高度（像素）── 12sp 转换后的值 */
    private float textHeight;

    /** 内边距（像素）── 4dp 转换后的值 */
    private float padding;

    /**
     * 构造函数（代码调用）
     * @param context 上下文
     */
    public BalanceBar(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     * @param context 上下文
     * @param attrs   XML 属性集合
     */
    public BalanceBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数（带样式）
     * @param context      上下文
     * @param attrs        XML 属性集合
     * @param defStyleAttr 默认样式属性
     */
    public BalanceBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件 ── 配置画笔和尺寸参数
     *
     * 画笔配置说明：
     * - setColor()：设置画笔颜色
     * - setStyle()：设置绘制样式（FILL=填充, STROKE=描边）
     * - setTextSize()：设置文字大小
     * - setTextAlign()：设置文字对齐方式
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 初始化尺寸（dp/sp 转换为像素）
        // dpToPx(8)：进度条高度 8dp
        barHeight = dpToPx(8);
        // spToPx(12)：文字大小 12sp
        textHeight = spToPx(12);
        // dpToPx(4)：内边距 4dp
        padding = dpToPx(4);

        // 配置轨道画笔（进度条背景）
        trackPaint.setColor(COLOR_TRACK);       // 浅灰色
        trackPaint.setStyle(Paint.Style.FILL);   // 填充模式

        // 配置进度条画笔（颜色在 onDraw 中动态设置）
        barPaint.setStyle(Paint.Style.FILL);     // 填充模式

        // 配置标签文字画笔
        textPaint.setColor(COLOR_TEXT);          // 深灰色文字
        textPaint.setTextSize(textHeight);       // 12sp 字号

        // 配置数值文字画笔
        valuePaint.setColor(COLOR_TEXT);         // 深灰色文字
        valuePaint.setTextSize(textHeight);      // 12sp 字号
        valuePaint.setTextAlign(Paint.Align.RIGHT); // 右对齐（从右向左绘制）
    }

    /**
     * 设置数据并刷新视图 ── Fragment 从 ViewModel 获取数据后调用
     *
     * 使用示例：
     * <pre>
     * balanceBar.setData("胜率", 5.3f, 20f);   // 高于平均 5.3%，绿色向右
     * balanceBar.setData("选取率", -2.1f, 20f); // 低于平均 2.1%，红色向左
     * balanceBar.setData("KDA", 0f, 20f);       // 等于平均，灰色中心线
     * </pre>
     *
     * @param label    标签文字（如"胜率"）
     * @param value    当前值（百分比偏差，正=高于平均，负=低于平均）
     * @param maxValue 最大值（用于计算进度条比例，防止溢出）
     */
    public void setData(@NonNull String label, float value, float maxValue) {
        this.label = label;
        this.value = value;
        // 防止除零错误：maxValue 最小为 0.01
        // 如果 maxValue = 0，计算 ratio 时会除零导致 NaN
        this.maxValue = Math.max(maxValue, 0.01f);
        // invalidate()：标记视图需要重绘，系统会在下一帧调用 onDraw()
        invalidate();
    }

    /**
     * 自定义绘制方法 ── 核心方法，在 Canvas 画布上绘制所有视觉元素
     *
     * 绘制顺序（后绘制的覆盖先绘制的）：
     * 1. 标签文字（左侧）
     * 2. 轨道背景（中间灰色圆角矩形）
     * 3. 进度条（中间彩色圆角矩形）
     * 4. 数值文字（右侧）
     *
     * @param canvas 画布对象（系统提供，无需创建）
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // ═══════════════════════════════════════════════════════════
        // 步骤 1：计算布局区域
        // ═══════════════════════════════════════════════════════════

        // 可用宽度 = 总宽度 - 左右内边距
        float width = getWidth() - getPaddingLeft() - getPaddingRight();

        // 三段式布局分配比例：
        // - 标签区域：30%（显示"胜率"等标签）
        // - 进度条区域：50%（显示进度条）
        // - 数值区域：20%（显示"+5.3%"等数值）
        float labelWidth = width * 0.3f;
        float barAreaWidth = width * 0.5f;
        float valueWidth = width * 0.2f;

        // 起始坐标（考虑左内边距）
        float x = getPaddingLeft();
        float y = getPaddingTop();

        // 文字基线位置
        // Android 文字绘制以基线为参考，y 坐标是基线位置而非顶部
        // textY = y + textHeight + padding：文字顶部留 padding 间距
        float textY = y + textHeight + padding;

        // ═══════════════════════════════════════════════════════════
        // 步骤 2：绘制标签文字
        // ═══════════════════════════════════════════════════════════

        // canvas.drawText(text, x, y, paint)
        // - text：要绘制的文字
        // - x：起始 X 坐标（左对齐，从 x 开始）
        // - y：基线 Y 坐标
        // - paint：画笔（定义颜色、字号等）
        canvas.drawText(label, x, textY, textPaint);

        // ═══════════════════════════════════════════════════════════
        // 步骤 3：绘制轨道背景
        // ═══════════════════════════════════════════════════════════

        // 计算进度条区域位置
        float barStartX = x + labelWidth + padding;     // 左边界
        float barEndX = barStartX + barAreaWidth;       // 右边界
        // 进度条垂直居中于文字基线
        float barY = textY - barHeight / 2f - padding / 2f;

        // 设置轨道矩形区域
        // trackRect.set(left, top, right, bottom)
        trackRect.set(barStartX, barY - barHeight / 2f, barEndX, barY + barHeight / 2f);
        // drawRoundRect(rect, rx, ry, paint)：绘制圆角矩形
        // rx/ry = barHeight/2：圆角半径等于进度条高度的一半（形成胶囊形状）
        canvas.drawRoundRect(trackRect, barHeight / 2f, barHeight / 2f, trackPaint);

        // ═══════════════════════════════════════════════════════════
        // 步骤 4：绘制进度条
        // ═══════════════════════════════════════════════════════════

        // 中心点 X 坐标（零值位置）
        float centerBarX = (barStartX + barEndX) / 2f;

        // 比例 = 当前值 / 最大值（-1.0 ~ +1.0）
        float ratio = value / maxValue;

        // 进度条宽度 = |比例| × 单边最大宽度
        // barAreaWidth / 2f：单边最大宽度（从中心到边缘）
        // 例如：ratio = 0.5，barWidth = 0.5 * 25% = 12.5% 总宽度
        float barWidth = Math.abs(ratio) * (barAreaWidth / 2f);

        // 根据正负值设置颜色和位置
        int barColor;
        if (value > 0) {
            // 正值：从中心向右延伸（绿色）
            barColor = COLOR_POSITIVE;
            barRect.set(centerBarX, barY - barHeight / 2f, centerBarX + barWidth, barY + barHeight / 2f);
        } else if (value < 0) {
            // 负值：从中心向左延伸（红色）
            barColor = COLOR_NEGATIVE;
            barRect.set(centerBarX - barWidth, barY - barHeight / 2f, centerBarX, barY + barHeight / 2f);
        } else {
            // 零值：中心细线（灰色，宽度 2px）
            barColor = COLOR_NEUTRAL;
            barRect.set(centerBarX - 1, barY - barHeight / 2f, centerBarX + 1, barY + barHeight / 2f);
        }

        // 绘制进度条（圆角矩形，与轨道相同的圆角）
        barPaint.setColor(barColor);
        canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, barPaint);

        // ═══════════════════════════════════════════════════════════
        // 步骤 5：绘制数值文字
        // ═══════════════════════════════════════════════════════════

        // 格式化数值文字（带正负号）
        // value >= 0 时加 "+" 前缀，负值自带 "-" 号
        // String.format("%.1f%%", value)：保留 1 位小数 + 百分号
        String valueText = (value >= 0 ? "+" : "") + String.format("%.1f%%", value);
        // 数值颜色与进度条颜色一致（绿色/红色/灰色）
        valuePaint.setColor(barColor);
        // 绘制数值文字（右对齐，从最右侧开始）
        canvas.drawText(valueText, x + width, textY, valuePaint);
    }

    /**
     * 测量视图尺寸 ── 告诉父容器这个 View 需要多大空间
     *
     * 为什么需要重写 onMeasure？
     * - 自定义 View 默认不知道自己应该多大
     * - 如果不重写，可能显示为 0x0（不可见）
     * - 需要根据内容计算合适的尺寸
     *
     * MeasureSpec 三种模式：
     * - EXACTLY：精确值（如 layout_width="200dp" 或 match_parent）
     * - AT_MOST：最大值（如 layout_width="wrap_content"）
     * - UNSPECIFIED：无限制（很少见）
     *
     * @param widthMeasureSpec  宽度测量规格
     * @param heightMeasureSpec 高度测量规格
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 计算期望高度：文字高度 + 进度条高度 + 3 倍内边距 + 上下 padding
        int desiredHeight = (int) (textHeight + barHeight + padding * 3 + getPaddingTop() + getPaddingBottom());
        // 宽度使用父容器分配的宽度（通常是 match_parent）
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // setMeasuredDimension()：设置测量结果，必须调用
        setMeasuredDimension(width, desiredHeight);
    }

    /**
     * dp 转 px 工具方法 ── 将密度无关像素(dp)转换为实际像素(px)
     *
     * 为什么需要转换？
     * - Android 使用 dp 作为设计单位，不同屏幕密度下 1dp 对应不同的 px
     * - 例如：1dp = 1px（mdpi）、2px（xhdpi）、3px（xxhdpi）
     * - 代码中设置尺寸必须使用 px，所以需要转换
     *
     * @param dp dp 值
     * @return px 值
     */
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }

    /**
     * sp 转 px 工具方法 ── 将缩放无关像素(sp)转换为实际像素(px)
     *
     * sp vs dp：
     * - dp：不受用户字体大小设置影响（用于布局尺寸）
     * - sp：受用户字体大小设置影响（用于文字大小）
     * - 用户设置"大字体"时，sp 会放大，dp 不会
     *
     * @param sp sp 值
     * @return px 值
     */
    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getContext().getResources().getDisplayMetrics());
    }
}
