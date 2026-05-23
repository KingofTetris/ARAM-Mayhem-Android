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
 * 版本陷阱横幅组件 ── 警告用户某英雄/符文在当前版本被大幅削弱
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * VersionTrapBanner 是一个"红色警告横幅"，当某个英雄或符文在当前版本
 * 被大幅削弱时，在详情页顶部显示警告信息，提醒用户谨慎选择。
 *
 * 什么是"版本陷阱"？
 * - 游戏每隔几周会发布新版本，调整英雄和符文的数值
 * - 有些英雄/符文在上个版本很强，但新版本被大幅削弱
 * - 如果用户还按旧版本的习惯选择，就会"踩坑"
 * - VersionTrapBanner 就是用来防止用户"踩坑"的警告
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、它在界面上的样子
 * ═══════════════════════════════════════════════════════════════════
 *
 *  ┌─────────────────────────────────────────────────────┐
 *  │  ⚠  提莫在14.8版本被大幅削弱，慎用！                  │
 *  └─────────────────────────────────────────────────────┘
 *   ↑         ↑                              ↑
 *  警告图标   英雄名称                       版本号
 *
 * 视觉特征：
 * - 红色圆角矩形背景（0xFFD32F2F = Material Red 700）
 * - 白色加粗文字，14sp 字号
 * - 左侧白色警告图标（系统自带的 ic_dialog_alert）
 * - 默认隐藏（GONE），只在 setTrapInfo() 调用时显示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、继承关系与技术选型
 * ═══════════════════════════════════════════════════════════════════
 *
 *   View
 *    └── ViewGroup
 *         └── LinearLayout         ← 线性布局（水平排列子视图）
 *              └── VersionTrapBanner  ← 我们的警告横幅
 *
 * 为什么继承 LinearLayout？
 * - 横幅由 2 个子视图组成：图标 + 文字
 * - LinearLayout 的 HORIZONTAL 方向可以简单地把它们水平排列
 * - 不需要复杂的布局规则，LinearLayout 最轻量
 *
 * 为什么不用 ConstraintLayout？
 * - 只有 2 个子视图，ConstraintLayout 太重了
 * - ConstraintLayout 适合复杂布局（3+ 个子视图、多约束关系）
 * - 对于"图标 + 文字"这种简单场景，LinearLayout 更高效
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、显示/隐藏机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * VersionTrapBanner 的可见性由三个方法控制：
 *
 *   初始状态                    调用 setTrapInfo()             调用 hide()
 *   ┌──────────┐               ┌──────────────────┐           ┌──────────┐
 *   │ GONE     │ ──────────→   │ VISIBLE          │ ────────→ │ GONE     │
 *   │ (不占空间) │               │ (显示红色横幅)    │           │ (不占空间) │
 *   └──────────┘               └──────────────────┘           └──────────┘
 *
 * GONE vs INVISIBLE 的区别：
 * - GONE：视图不显示，也不占布局空间（其他视图会填补它的位置）
 * - INVISIBLE：视图不显示，但仍然占布局空间（留下空白区域）
 * - 我们用 GONE，因为横幅隐藏时不应该留下空白
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、使用场景
 * ═══════════════════════════════════════════════════════════════════
 *
 * 场景 1：英雄详情页 ── 英雄被削弱时显示警告
 *   VersionTrapBanner banner = findViewById(R.id.banner_trap);
 *   if (heroDetail.isVersionTrap()) {
 *       banner.setTrapInfo(heroDetail.getNameZh(), "14.8");
 *       // 显示："⚠ 提莫在14.8版本被大幅削弱，慎用！"
 *   }
 *
 * 场景 2：符文详情页 ── 符文被削弱时显示警告
 *   if (augment.isTrap()) {
 *       banner.setTrapInfo("14.8");
 *       // 显示："⚠ 该英雄在14.8版本被大幅削弱，慎用！"
 *   }
 *
 * 场景 3：英雄不是陷阱时 ── 不显示横幅
 *   if (!heroDetail.isVersionTrap()) {
 *       banner.hide();  // 确保隐藏（默认就是 GONE，但保险起见）
 *   }
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、程序化创建视图 vs XML 布局
 * ═══════════════════════════════════════════════════════════════════
 *
 * VersionTrapBanner 的子视图（图标和文字）是在 init() 方法中
 * 通过 Java 代码动态创建的，而不是在 XML 布局文件中定义的。
 *
 * 为什么用代码创建而不用 XML？
 * - 横幅的子视图结构非常简单（只有图标+文字），不值得单独建一个布局文件
 * - 代码创建可以精确控制每个属性，不需要额外的 findViewById
 * - 横幅是一个自包含的组件，外部不需要访问它的子视图
 *
 * 代码创建子视图的步骤：
 * 1. new ImageView(context) ── 创建警告图标
 * 2. new TextView(context)  ── 创建警告文字
 * 3. addView(icon) + addView(text) ── 添加到 LinearLayout
 * 4. 设置 LayoutParams ── 控制子视图的尺寸和间距
 */
public class VersionTrapBanner extends LinearLayout {

    /**
     * 警告文字视图 ── 显示"⚠ XXX在XX版本被大幅削弱，慎用！"
     *
     * 这个 TextView 是在 init() 方法中动态创建的，
     * 不是通过 XML 布局 inflate 的。
     *
     * 为什么保存为成员变量？
     * - setTrapInfo() 方法需要更新文字内容
     * - 如果不保存引用，每次更新都要遍历子视图找到 TextView
     * - 保存引用可以直接调用 setText()，效率更高
     */
    private TextView textWarning;

    /**
     * 构造函数 ── 代码动态创建时调用
     *
     * 使用场景：在代码中动态创建 VersionTrapBanner
     *
     * 示例：
     *   VersionTrapBanner banner = new VersionTrapBanner(context);
     *   banner.setTrapInfo("提莫", "14.8");
     *   parentLayout.addView(banner);
     *
     * @param context 上下文对象
     */
    public VersionTrapBanner(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数 ── XML 布局 inflate 时调用
     *
     * 当你在 XML 布局文件中声明 VersionTrapBanner 时，
     * 系统在 inflate 布局时会调用这个双参数构造函数。
     *
     * XML 使用示例：
     *   <com.aram.mayhem.ui.widget.VersionTrapBanner
     *       android:id="@+id/banner_trap"
     *       android:layout_width="match_parent"
     *       android:layout_height="wrap_content" />
     *
     * @param context 上下文对象
     * @param attrs   XML 属性集合
     */
    public VersionTrapBanner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数 ── 带默认样式属性时调用
     *
     * 当组件需要应用主题中的默认样式时使用。
     *
     * @param context      上下文对象
     * @param attrs        XML 属性集合
     * @param defStyleAttr 默认样式属性的 resource ID
     */
    public VersionTrapBanner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件 ── 构建横幅的所有子视图和样式
     *
     * 这个方法完成以下工作：
     *
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 步骤 1：设置 LinearLayout 自身属性                           │
     * │   setOrientation(HORIZONTAL)  → 子视图水平排列（图标在左，文字在右）│
     * │   setGravity(CENTER_VERTICAL) → 子视图垂直居中对齐              │
     * │   setPadding(24, 16, 24, 16)  → 内边距（px），让内容不贴边     │
     * ├─────────────────────────────────────────────────────────────┤
     * │ 步骤 2：创建红色圆角背景                                      │
     * │   GradientDrawable ── 代码动态创建的圆角矩形                   │
     * │   setColor(0xFFD32F2F) ── Material Red 700 红色              │
     * │   setCornerRadius(8) ── 8px 圆角                              │
     * ├─────────────────────────────────────────────────────────────┤
     * │ 步骤 3：创建警告图标（ImageView）                              │
     * │   使用系统自带的 ic_dialog_alert 图标（三角形感叹号）            │
     * │   setColorFilter(白色) ── 把黄色图标染成白色，配合红色背景       │
     * │   setMarginEnd(12) ── 图标和文字之间 12px 间距                 │
     * ├─────────────────────────────────────────────────────────────┤
     * │ 步骤 4：创建警告文字（TextView）                               │
     * │   白色加粗文字，14sp 字号                                      │
     * │   MATCH_PARENT 宽度 ── 占据图标右侧所有空间                    │
     * ├─────────────────────────────────────────────────────────────┤
     * │ 步骤 5：默认隐藏                                              │
     * │   setVisibility(GONE) ── 初始不显示，等 setTrapInfo() 调用才显示│
     * └─────────────────────────────────────────────────────────────┘
     *
     * GradientDrawable 详解：
     * - GradientDrawable 是 Android 提供的"代码创建形状"类
     * - 等同于 XML 中的 <shape> 标签，但可以在代码中动态创建
     * - 这里创建的是一个纯色圆角矩形，不需要 XML 文件
     * - 也可以用 XML 定义（res/drawable/bg_trap_banner.xml），
     *   但代码创建更直观，所有样式集中在一处
     *
     * setColorFilter 详解：
     * - ic_dialog_alert 原本是黄色的系统图标
     * - setColorFilter(0xFFFFFFFF) 把图标染成白色
     * - 原理：把图标每个像素的颜色替换为指定颜色，保留透明度
     * - 这样一个图标可以适配不同背景色
     *
     * @param context 上下文对象，用于创建子视图和获取资源
     */
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

    /**
     * 设置陷阱信息（无名称版） ── 显示通用警告
     *
     * 当不需要指定具体英雄/符文名称时使用此方法。
     * 文字格式："⚠ 该英雄在{version}版本被大幅削弱，慎用！"
     *
     * 使用场景：
     * - 符文详情页，不需要显示符文名称（页面上已经有名称了）
     * - 简化版警告，只提示版本号
     *
     * 调用示例：
     *   banner.setTrapInfo("14.8");
     *   // 显示："⚠ 该英雄在14.8版本被大幅削弱，慎用！"
     *
     * 注意：调用此方法会自动将横幅设为 VISIBLE（可见）
     *
     * @param version 削弱版本号，如 "14.8"、"14.9"
     */
    public void setTrapInfo(String version) {
        textWarning.setText("⚠ 该英雄在" + version + "版本被大幅削弱，慎用！");
        setVisibility(VISIBLE);
    }

    /**
     * 设置陷阱信息（带名称版） ── 显示包含具体名称的警告
     *
     * 当需要在横幅中显示具体英雄/符文名称时使用此方法。
     * 文字格式："⚠ {name}在{version}版本被大幅削弱，慎用！"
     *
     * 使用场景：
     * - 英雄详情页，显示"⚠ 提莫在14.8版本被大幅削弱，慎用！"
     * - 社区攻略中引用被削弱的英雄
     *
     * 调用示例：
     *   banner.setTrapInfo("提莫", "14.8");
     *   // 显示："⚠ 提莫在14.8版本被大幅削弱，慎用！"
     *
     *   banner.setTrapInfo("不灭之握", "14.7");
     *   // 显示："⚠ 不灭之握在14.7版本被大幅削弱，慎用！"
     *
     * 注意：调用此方法会自动将横幅设为 VISIBLE（可见）
     *
     * @param name    英雄/符文名称，如 "提莫"、"不灭之握"
     * @param version 削弱版本号，如 "14.8"
     */
    public void setTrapInfo(String name, String version) {
        textWarning.setText("⚠ " + name + "在" + version + "版本被大幅削弱，慎用！");
        setVisibility(VISIBLE);
    }

    /**
     * 隐藏横幅 ── 将横幅设为 GONE（不显示且不占空间）
     *
     * 当英雄/符文不是版本陷阱时调用此方法，确保横幅不显示。
     *
     * 使用场景：
     * - 用户切换到另一个英雄详情页，新英雄不是陷阱
     * - 需要重新隐藏横幅（因为上一个英雄可能显示了横幅）
     *
     * 调用示例：
     *   if (hero.isVersionTrap()) {
     *       banner.setTrapInfo(hero.getNameZh(), hero.getTrapVersion());
     *   } else {
     *       banner.hide();  // 不是陷阱，隐藏横幅
     *   }
     *
     * GONE vs INVISIBLE 的选择：
     * - GONE：视图从布局中移除，不占空间 → 适合横幅（隐藏后不应留空白）
     * - INVISIBLE：视图不可见但占空间 → 适合需要保持布局位置的场景
     */
    public void hide() {
        setVisibility(GONE);
    }
}
