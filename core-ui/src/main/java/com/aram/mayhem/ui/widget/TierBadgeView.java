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
 * 梯级徽章组件 ── 显示英雄/符文强度评级的小标签
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * TierBadgeView 是一个"梯级徽章"，用颜色和文字告诉用户某个英雄或符文
 * 在当前版本的强度评级。评级从高到低分为 S+、S、A、B、C 五个等级，
 * 每个等级有对应的颜色，让用户一眼就能判断强弱。
 *
 * "梯级"（Tier）是游戏社区中常用的评级体系：
 * - S+ 级 = 最强（T0 级别，必选）
 * - S 级  = 很强（T1 级别，优先选）
 * - A 级  = 中等偏上（T2 级别，可选）
 * - B 级  = 中等偏下（T3 级别，特定情况选）
 * - C 级  = 较弱（T4 级别，不推荐）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、它在界面上的样子
 * ═══════════════════════════════════════════════════════════════════
 *
 *  ┌────┐   ┌───┐   ┌───┐   ┌───┐   ┌───┐
 *  │ S+ │   │ S │   │ A │   │ B │   │ C │
 *  └────┘   └───┘   └───┘   └───┘   └───┘
 *  金色背景  橙色背景  绿色背景  蓝色背景  灰色背景
 *  白色文字  白色文字  白色文字  白色文字  白色文字
 *  加粗大写  加粗大写  加粗大写  加粗大写  加粗大写
 *
 * 外观特征：
 * - 圆角矩形背景（由 bg_tier_badge.xml 定义形状）
 * - 背景颜色由 Tier 枚举的 color 字段决定
 * - 白色加粗文字，11sp 字号
 * - 文字全部大写（setAllCaps）
 * - 最小宽度 28dp，确保标签不会太窄
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、继承关系与技术选型
 * ═══════════════════════════════════════════════════════════════════
 *
 *   View
 *    └── TextView
 *         └── AppCompatTextView  ← AndroidX 兼容版 TextView
 *              └── TierBadgeView  ← 我们的梯级徽章
 *
 * 为什么继承 AppCompatTextView 而不是 TextView？
 * - AppCompatTextView 是 AndroidX 提供的兼容版本
 * - 它确保在所有 API 级别上文字渲染行为一致
 * - 支持自动使用 AppCompat 主题中定义的文字外观
 *
 * 为什么不像 QualityChip 那样用 Chip？
 * - TierBadgeView 不需要勾选功能，只是一个纯展示标签
 * - Chip 的样式（圆角+图标+边框）过于复杂，不适合简洁的评级标签
 * - TextView + 圆角背景更轻量，渲染更快
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、与 Tier 枚举的协作关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * TierBadgeView 的核心数据来源是 com.aram.mayhem.common.Tier 枚举：
 *
 *   Tier 枚举（数据源）          TierBadgeView（展示层）
 *   ┌──────────────┐            ┌──────────────┐
 *   │ S_PLUS       │            │   "S+"       │
 *   │  label="S+"  │ ────────→  │   金色背景    │
 *   │  color=金色   │            │   白色加粗    │
 *   └──────────────┘            └──────────────┘
 *
 * Tier 枚举定义了每个梯级的 label（显示文字）和 color（颜色值），
 * TierBadgeView 只负责把这两个值"贴"到 UI 上。
 * 这种分离让 Tier 的定义和展示解耦：
 * - 修改颜色只需改 Tier 枚举，不用动 View 代码
 * - 同一个 Tier 枚举可以被多种 View 复用
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、使用场景
 * ═══════════════════════════════════════════════════════════════════
 *
 * 场景 1：英雄列表卡片 ── 显示英雄的强度评级
 *   TierBadgeView badge = findViewById(R.id.badge_tier);
 *   badge.setTier(hero.getTier());  // 自动显示对应颜色和文字
 *
 * 场景 2：英雄详情页 ── 顶部显示评级
 *   <com.aram.mayhem.ui.widget.TierBadgeView
 *       android:id="@+id/badge_tier"
 *       android:layout_width="wrap_content"
 *       android:layout_height="wrap_content" />
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、dpToPx 转换说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * Android 中尺寸单位有两种：
 * - dp（密度无关像素）：与屏幕密度无关，1dp 在所有屏幕上物理大小相同
 * - px（像素）：屏幕上的实际像素点，不同密度屏幕上 1px 的物理大小不同
 *
 * 代码中设置 padding/margin 等需要用 px，但我们设计时用 dp。
 * 所以需要 dpToPx() 把 dp 值转换为 px 值。
 *
 * 转换公式：px = dp × density
 * 其中 density = 屏幕密度（mdpi=1, hdpi=1.5, xhdpi=2, xxhdpi=3）
 *
 * 示例：8dp 在不同屏幕上的 px 值
 * - mdpi (density=1):   8px
 * - xhdpi (density=2):  16px
 * - xxhdpi (density=3): 24px
 */
public class TierBadgeView extends AppCompatTextView {

    /**
     * 当前梯级枚举值
     *
     * 存储从外部传入的 Tier 枚举对象，如 Tier.S_PLUS、Tier.A 等。
     * Tier 枚举内部包含两个关键信息：
     * - label：显示文字（如 "S+"、"A"）
     * - color：背景颜色（如金色、绿色）
     *
     * 可能为 null（刚创建时还没调用 setTier()），
     * 此时徽章只显示默认样式（白色文字、圆角背景），没有梯级文字和颜色。
     */
    private Tier tier;

    /**
     * 构造函数 ── 代码动态创建时调用
     *
     * 使用场景：在代码中动态创建 TierBadgeView 并添加到布局中
     *
     * 示例：
     *   TierBadgeView badge = new TierBadgeView(context);
     *   badge.setTier(Tier.S_PLUS);
     *   parentLayout.addView(badge);
     *
     * @param context 上下文对象，用于获取资源和显示信息
     */
    public TierBadgeView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    /**
     * 构造函数 ── XML 布局 inflate 时调用
     *
     * 当你在 XML 布局文件中声明 TierBadgeView 时，
     * 系统在 inflate 布局时会调用这个双参数构造函数。
     *
     * XML 使用示例：
     *   <com.aram.mayhem.ui.widget.TierBadgeView
     *       android:id="@+id/badge_tier"
     *       android:layout_width="wrap_content"
     *       android:layout_height="wrap_content" />
     *
     * @param context 上下文对象
     * @param attrs   XML 属性集合，包含 XML 中定义的属性
     */
    public TierBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * 构造函数 ── 带默认样式属性时调用
     *
     * 当组件需要应用主题中的默认样式时使用。
     * 系统框架在 inflate 布局时内部调用此构造函数。
     *
     * @param context      上下文对象
     * @param attrs        XML 属性集合
     * @param defStyleAttr 默认样式属性的 resource ID
     */
    public TierBadgeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * 初始化组件样式 ── 所有构造函数的公共初始化逻辑
     *
     * 这个方法设置 TierBadgeView 的所有默认视觉属性，
     * 确保无论通过哪种方式创建，外观都一致。
     *
     * 初始化内容详解：
     *
     * 1. setGravity(Gravity.CENTER) ── 文字居中
     *    因为徽章是圆角矩形，文字居中看起来最协调
     *
     * 2. setAllCaps(true) ── 文字全部大写
     *    "s+" → "S+"，"a" → "A"，评级字母大写更醒目
     *
     * 3. setTextColor(Color.WHITE) ── 白色文字
     *    因为背景色都比较深（金/橙/绿/蓝/灰），白色文字对比度最高
     *
     * 4. setTextSize(COMPLEX_UNIT_SP, 11) ── 11sp 字号
     *    sp 和 dp 类似，但会随用户设置的字体缩放而变化
     *    11sp 在徽章这个尺寸下刚好合适，不会太大也不会太小
     *
     * 5. setTypeface(null, BOLD) ── 加粗字体
     *    评级文字加粗更醒目，用户滑动列表时能快速识别
     *
     * 6. setPadding(8dp, 2dp, 8dp, 2dp) ── 内边距
     *    水平 8dp：文字左右有足够空间，不贴边
     *    垂直 2dp：徽章高度紧凑，不占太多纵向空间
     *
     * 7. setMinWidth(28dp) ── 最小宽度
     *    确保 "C" 这种单字符的徽章也有足够宽度
     *    否则 "C" 会比 "S+" 窄很多，列表中不整齐
     *
     * 8. setBackgroundResource(R.drawable.bg_tier_badge) ── 圆角背景
     *    bg_tier_badge.xml 是一个 <shape> 定义：
     *    - 矩形 + 圆角（如 4dp）
     *    - 默认填充色透明，通过 setBackgroundTintList 设置实际颜色
     *
     * @param context 上下文对象，用于获取资源和显示信息
     * @param attrs   XML 属性集合（当前未使用，预留未来扩展）
     */
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

    /**
     * 设置梯级并更新显示 ── 外部调用的核心方法
     *
     * 这是 TierBadgeView 最重要的公开方法。调用后：
     * 1. 保存梯级枚举到 this.tier 字段
     * 2. 从 Tier 枚举获取 label，设置为显示文字
     * 3. 从 Tier 枚举获取 color，设置为背景色
     *
     * 执行流程：
     *   setTier(Tier.S_PLUS)
     *     ↓
     *   this.tier = Tier.S_PLUS
     *     ↓
     *   setText("S+")           ← tier.getLabel() 返回 "S+"
     *     ↓
     *   setBackgroundTintList(金色) ← tier.getColor() 返回金色值
     *     ↓
     *   最终效果：金色背景 + 白色 "S+" 文字
     *
     * setBackgroundTintList 的工作原理：
     * - 它不会替换背景 drawable，而是给现有背景"染色"
     * - bg_tier_badge.xml 定义了形状（圆角矩形）
     * - setBackgroundTintList 把这个形状填充为指定颜色
     * - 这样只需一个 drawable，就能显示 5 种不同颜色的徽章
     *
     * ColorStateList.valueOf(color) 的作用：
     * - setBackgroundTintList 需要接收 ColorStateList 类型参数
     * - ColorStateList 可以为不同状态（按下、禁用等）设置不同颜色
     * - valueOf(color) 创建一个"所有状态都用同一颜色"的简单 ColorStateList
     * - 对于徽章这种纯展示组件，不需要状态变色
     *
     * @param tier 梯级枚举值，如 Tier.S_PLUS、Tier.S、Tier.A、Tier.B、Tier.C
     */
    public void setTier(@NonNull Tier tier) {
        this.tier = tier;
        setText(tier.getLabel());
        int color = tier.getColor();
        setBackgroundTintList(ColorStateList.valueOf(color));
    }

    /**
     * 获取当前梯级枚举值
     *
     * 用于外部查询当前徽章显示的是哪个梯级。
     * 通常在列表点击事件中使用，获取被点击英雄的梯级。
     *
     * 使用示例：
     *   Tier currentTier = badge.getTier();
     *   if (currentTier == Tier.S_PLUS || currentTier == Tier.S) {
     *       // 高梯级英雄，优先推荐
     *   }
     *
     * @return 当前梯级枚举值，如 Tier.A；
     *         如果还没调用过 setTier()，则返回 null
     */
    @Nullable
    public Tier getTier() {
        return tier;
    }

    /**
     * dp 转 px 工具方法 ── 将密度无关像素转换为实际像素
     *
     * Android 系统中，代码设置尺寸（padding、margin、width 等）需要用 px，
     * 但设计稿中的尺寸通常用 dp。这个方法负责把 dp 转为 px。
     *
     * 转换原理：
     * - 每个屏幕有一个 density（密度）值
     * - 1dp = density 个 px
     * - mdpi 屏幕 density=1，所以 1dp = 1px
     * - xxhdpi 屏幕 density=3，所以 1dp = 3px
     *
     * TypedValue.applyDimension 的工作：
     * - 内部就是做 dp × density 的乘法
     * - 但它还处理了四舍五入，确保像素对齐
     * - COMPLEX_UNIT_DIP 表示输入单位是 dp
     * -getDisplayMetrics() 返回当前屏幕的显示信息（包含 density）
     *
     * 为什么不用简单的 (int)(dp * density)？
     * - applyDimension 处理了浮点精度问题
     * - 它还支持 sp（会考虑用户字体缩放设置）
     * - 使用系统 API 更可靠，不会出现精度丢失
     *
     * @param context 上下文对象，用于获取屏幕显示信息
     * @param dp      要转换的 dp 值
     * @return 转换后的 px 值（整数）
     */
    private int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
