package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.chip.Chip;

/**
 * 符文品质标签组件 ── 用颜色区分符文稀有度的小标签
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * QualityChip 是一个"品质标签"，显示在符文卡片上，告诉用户这个符文
 * 属于哪个品质等级。不同品质用不同颜色区分，让用户一眼就能看出稀有度。
 *
 * 在游戏 ARAM 大乱斗中，符文（Augment）有三种品质：
 * - 棱彩（PRISMATIC）：最稀有，紫色标签
 * - 金（GOLD）：较稀有，金色标签
 * - 银（SILVER）：普通，灰色标签
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、它在界面上的样子
 * ═══════════════════════════════════════════════════════════════════
 *
 *  ┌──────────┐
 *  │  ○ 棱彩   │  ← 紫色边框 + 半透明紫色背景 + 紫色文字
 *  └──────────┘
 *
 *  ┌──────────┐
 *  │  ○ 金    │  ← 金色边框 + 半透明金色背景 + 金色文字
 *  └──────────┘
 *
 *  ┌──────────┐
 *  │  ○ 银    │  ← 灰色边框 + 半透明灰色背景 + 灰色文字
 *  └──────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、继承关系与技术选型
 * ═══════════════════════════════════════════════════════════════════
 *
 * QualityChip 继承自 Material Design 的 Chip 组件：
 *
 *   View
 *    └── TextView
 *         └── Button
 *              └── Chip           ← Material Design 芯片组件
 *                   └── QualityChip  ← 我们的自定义品质标签
 *
 * 为什么选 Chip 而不直接用 TextView？
 * - Chip 自带圆角背景、边框、图标等样式，省去手动绘制
 * - Chip 支持 checkable（可勾选）状态，可以表示"选中此品质筛选"
 * - Chip 的视觉风格符合 Material Design 规范，美观统一
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、使用场景
 * ═══════════════════════════════════════════════════════════════════
 *
 * 场景 1：符文列表卡片 ── 显示单个符文的品质
 *   QualityChip chip = findViewById(R.id.chip_quality);
 *   chip.setQuality("PRISMATIC");  // 自动变为紫色"棱彩"标签
 *
 * 场景 2：品质筛选栏 ── 用户点击选择要筛选的品质
 *   QualityChip chipGold = new QualityChip(this);
 *   chipGold.setQuality("GOLD");
 *   chipGold.setOnCheckedChangeListener((btn, checked) -> {
 *       if (checked) filterByQuality("GOLD");
 *   });
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、颜色编码规则
 * ═══════════════════════════════════════════════════════════════════
 *
 * 每种品质有 3 个颜色值（背景色、边框色、文字色）：
 *
 * | 品质       | 背景色          | 边框色          | 文字色          | 标签文字 |
 * |-----------|----------------|----------------|----------------|---------|
 * | PRISMATIC | 0x339C27B0(半透明紫) | 0xFF9C27B0(紫色) | 0xFF9C27B0(紫色) | "棱彩"  |
 * | GOLD      | 0x33FFB300(半透明金) | 0xFFFFB300(金色) | 0xFFFF8F00(深金色) | "金"   |
 * | 其他(银)   | 0x339E9E9E(半透明灰) | 0xFF9E9E9E(灰色) | 0xFF757575(深灰色) | "银"   |
 *
 * 颜色值格式说明：
 * - 0x339C27B0：前两位 0x33 = 20% 透明度（51/255），后面 6 位是 RGB 颜色
 * - 0xFF9C27B0：前两位 0xFF = 100% 不透明，后面 6 位是 RGB 颜色
 * - 背景色用半透明，让底色透出来，视觉更柔和
 * - 边框和文字用不透明，确保清晰可读
 */
public class QualityChip extends Chip {

    /**
     * 当前品质值
     *
     * 存储从外部传入的品质字符串，如 "PRISMATIC"、"GOLD"、"SILVER" 等。
     * 这个值决定了 updateAppearance() 方法中走哪个 switch 分支，
     * 从而决定标签的颜色和文字。
     *
     * 可能为 null（刚创建时还没调用 setQuality()），
     * 此时 updateAppearance() 会直接 return，不做任何更新。
     */
    private String quality;

    /**
     * 构造函数 ── 代码动态创建时调用
     *
     * 当你在 Java/Kotlin 代码中用 new QualityChip(context) 创建时，
     * 系统会调用这个构造函数。
     *
     * 使用场景示例：
     *   QualityChip chip = new QualityChip(this);  // ← 调用此构造函数
     *   chip.setQuality("GOLD");
     *   layout.addView(chip);
     *
     * @param context 上下文对象，用于获取资源、主题等信息
     */
    public QualityChip(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数 ── XML 布局 inflate 时调用
     *
     * 当你在 XML 布局文件中写 <com.aram.mayhem.ui.widget.QualityChip /> 时，
     * 系统在 inflate（填充）布局时会调用这个双参数构造函数。
     *
     * XML 使用示例：
     *   <com.aram.mayhem.ui.widget.QualityChip
     *       android:id="@+id/chip_quality"
     *       android:layout_width="wrap_content"
     *       android:layout_height="wrap_content" />
     *
     * @param context 上下文对象
     * @param attrs   XML 属性集合，包含 android:xxx 和 app:xxx 中定义的属性
     */
    public QualityChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数 ── 带默认样式属性时调用
     *
     * 当组件需要指定默认样式（defStyleAttr）时使用。
     * 例如在主题中定义了 <item name="chipStyle">@style/MyChip</item>，
     * 系统会通过 defStyleAttr 找到这个样式并应用。
     *
     * 一般情况下不会直接调用此构造函数，
     * 它主要由系统框架在 inflate 布局时内部使用。
     *
     * @param context      上下文对象
     * @param attrs        XML 属性集合
     * @param defStyleAttr 默认样式属性的 resource ID
     */
    public QualityChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件样式 ── 所有构造函数的公共初始化逻辑
     *
     * 这个方法在所有三个构造函数中都被调用，确保无论通过哪种方式创建
     * QualityChip，都会执行相同的初始化逻辑。
     *
     * 初始化内容：
     * 1. setCheckable(true) ── 允许勾选，用于品质筛选场景
     * 2. setCheckedIconVisible(false) ── 隐藏勾选图标（用颜色变化代替勾选标记）
     * 3. setChipStrokeWidth(2f) ── 设置 2dp 边框宽度
     * 4. updateAppearance() ── 根据当前品质更新外观（首次调用时 quality 为 null，不会更新）
     *
     * 为什么隐藏勾选图标？
     * - 默认的 Chip 勾选图标是一个 ✓ 对号，放在标签左侧
     * - 对于品质标签，我们不需要对号，而是用"选中时背景色变深"来表示
     * - 这样视觉更简洁，不会和品质文字重叠
     *
     * @param context 上下文对象，用于获取资源
     */
    private void init(Context context) {
        setCheckable(true);
        setCheckedIconVisible(false);
        setChipStrokeWidth(2f);
        updateAppearance();
    }

    /**
     * 设置品质并更新显示 ── 外部调用的核心方法
     *
     * 这是 QualityChip 最重要的公开方法。调用后：
     * 1. 保存品质值到 this.quality 字段
     * 2. 调用 updateAppearance() 根据品质更新颜色和文字
     *
     * 调用示例：
     *   chip.setQuality("PRISMATIC");  // 标签变为紫色"棱彩"
     *   chip.setQuality("GOLD");       // 标签变为金色"金"
     *   chip.setQuality("SILVER");     // 标签变为灰色"银"
     *
     * @param quality 品质值字符串，如 "PRISMATIC"、"GOLD"、"SILVER"、"EPIC" 等
     *                任何非 "PRISMATIC" 和 "GOLD" 的值都会被归为"银"
     */
    public void setQuality(@NonNull String quality) {
        this.quality = quality;
        updateAppearance();
    }

    /**
     * 根据品质更新外观样式 ── 核心渲染逻辑
     *
     * 这个方法根据 quality 字段的值，设置标签的 4 个视觉属性：
     * 1. 背景色（ChipBackgroundColor）── 半透明颜色，柔和
     * 2. 边框色（ChipStrokeColor）── 不透明颜色，清晰
     * 3. 文字色（TextColor）── 不透明颜色，可读
     * 4. 标签文字（Text）── "棱彩"/"金"/"银"
     *
     * 颜色值详解（以 PRISMATIC 为例）：
     * - backgroundColor = 0x339C27B0
     *   ┌────┬──────┐
     *   │ 33 │9C27B0│
     *   │透明度│ RGB  │
     *   │ 20% │ 紫色 │
     *   └────┴──────┘
     *   0x33 = 十进制 51，51/255 ≈ 20% 透明度
     *   9C27B0 = Material Design Purple 500
     *
     * - strokeColor = 0xFF9C27B0
     *   0xFF = 100% 不透明
     *   9C27B0 = 同上的紫色
     *
     * 为什么文字色用深金色(0xFFFF8F00)而边框用亮金色(0xFFFFB300)？
     * - 文字需要和背景有足够对比度才能清晰阅读
     * - 深金色在半透明金色背景上更容易看清
     * - 边框是装饰性的，用亮金色更醒目
     */
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

    /**
     * 获取当前品质值
     *
     * 用于外部查询当前标签显示的是哪个品质。
     * 通常在筛选逻辑中使用，判断用户选中了哪个品质。
     *
     * 使用示例：
     *   String currentQuality = chip.getQuality();
     *   if ("PRISMATIC".equals(currentQuality)) {
     *       // 只显示棱彩符文
     *   }
     *
     * @return 当前品质值字符串，如 "PRISMATIC"、"GOLD" 等；
     *         如果还没调用过 setQuality()，则返回 null
     */
    @Nullable
    public String getQuality() {
        return quality;
    }
}
