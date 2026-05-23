package com.aram.mayhem.common;

import com.google.gson.annotations.SerializedName;

/**
 * 英雄/符文梯级评级枚举 —— 定义从 S+ 到 C 的五个评级等级及对应颜色
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 ARAM 模式中，每个英雄和符文都有一个"强度评级"（Tier），
 * 表示它在当前版本中的胜率和表现。评级从高到低分为 5 个等级：
 *
 * S+（最强）→ S（强）→ A（中上）→ B（中等）→ C（较弱）
 *
 * 每个评级都有一个专属颜色，在列表页上以彩色标签的形式显示：
 * - S+：红色（#E53E3E）—— 非常强，强烈推荐
 * - S ：橙色（#FF6B35）—— 很强，推荐使用
 * - A ：金色（#FFB800）—— 中上水平，可用
 * - B ：绿色（#4CAF50）—— 中等水平，看情况
 * - C ：蓝色（#2196F3）—— 较弱，不推荐
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、评级如何计算？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 评级基于胜率（winRate）划分，由后端数据管线计算：
 *
 * | 评级 | 胜率范围      | 含义                     |
 * |------|-------------|--------------------------|
 * | S+   | ≥ 54%       | 超模级别，几乎必赢        |
 * | S    | 52% ~ 54%   | 很强，明显优势            |
 * | A    | 50% ~ 52%   | 略高于平均水平            |
 * | B    | 48% ~ 50%   | 略低于平均水平            |
 * | C    | < 48%       | 较弱，明显劣势            |
 *
 * 注意：具体阈值可能随版本调整，以上为参考值。
 * 评级由后端 MultiSourceValidatorImpl 计算后存入数据库，
 * Android 端只负责显示，不参与评级计算。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、为什么用枚举而不是字符串？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 如果用字符串表示评级（如 "S+"、"A"），会有以下问题：
 * 1. 拼写错误不会在编译期发现（"S+" 写成 "S＋" 全角符号）
 * 2. 无法关联颜色等属性（需要额外的 Map 来映射颜色）
 * 3. switch 语句无法穷举所有情况（编译器不会警告遗漏）
 *
 * 使用枚举的优势：
 * 1. 类型安全：Tier.S_PLUS 不会拼错
 * 2. 自带属性：每个枚举值自带 label 和 color
 * 3. switch 穷举：编译器会检查是否处理了所有枚举值
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、@SerializedName 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端 JSON 中使用下划线命名（如 "S_PLUS"），
 * 而 Java 枚举值命名规范不允许加号（+），所以用 S_PLUS 代替 S+。
 *
 * @SerializedName("S_PLUS") 告诉 Gson：
 * 当 JSON 中出现 "S_PLUS" 时，映射到枚举值 Tier.S_PLUS。
 * 如果不加这个注解，Gson 默认按枚举名称匹配，也能工作，
 * 但显式声明更安全，防止 Gson 命名策略变更导致映射错误。
 *
 * JSON 示例：
 * { "name": "亚索", "tier": "S_PLUS", "winRate": 55.2 }
 *                                    ↑
 *                    Gson 通过 @SerializedName 映射到 Tier.S_PLUS
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、颜色值说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 颜色值使用 ARGB 格式（0xAARRGGBB），与 Android 的 Color 类一致：
 * - 0xFF：Alpha 通道，表示完全不透明
 * - E53E3E：RGB 颜色值
 *
 * 在 UI 中使用方式：
 *   textView.setTextColor(tier.getColor());  // 直接设置文字颜色
 *   badge.setBackgroundColor(tier.getColor()); // 设置背景颜色
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端数据管线计算评级 → 存入 MySQL → API 返回 JSON（"tier": "S_PLUS"）
 *     → Retrofit 接收 → Gson 反序列化为 Tier.S_PLUS
 *     → HeroUiModel.tier / AugmentUiModel.tier
 *     → HeroAdapter / AugmentAdapter 绑定到列表项
 *     → tierBadge.setText(tier.getLabel())  // 显示 "S+"
 *     → tierBadge.setBackgroundColor(tier.getColor())  // 显示红色
 *
 * 关联类：
 * - HeroUiModel：英雄 UI 模型，包含 tier 字段
 * - AugmentUiModel：符文 UI 模型，包含 tier 字段
 * - HeroAdapter / AugmentAdapter：列表适配器，读取 tier 的 label 和 color
 *
 * ═══════════════════════════════════════════════════════════════════
 * 七、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * // 在 Adapter 中设置梯级标签
 * Tier tier = hero.getTier();
 * tierBadge.setText(tier.getLabel());           // 显示 "S+"
 * tierBadge.setBackgroundColor(tier.getColor()); // 设置红色背景
 *
 * // 根据评级筛选英雄
 * List<HeroUiModel> topHeroes = heroes.stream()
 *     .filter(h -> h.getTier() == Tier.S_PLUS || h.getTier() == Tier.S)
 *     .collect(Collectors.toList());
 *
 * // switch 处理不同评级
 * switch (tier) {
 *     case S_PLUS:
 *     case S:
 *         showRecommendBadge();  // 显示"推荐"标签
 *         break;
 *     case A:
 *     case B:
 *         break;  // 不显示额外标签
 *     case C:
 *         showWeakBadge();  // 显示"较弱"标签
 *         break;
 * }
 */
public enum Tier {

    /**
     * S+ 级 —— 最高评级，红色标签
     *
     * 含义：胜率 ≥ 54%，超模级别，强烈推荐使用。
     * 在英雄列表中显示为红色 "S+" 标签，非常醒目。
     *
     * 典型英雄：当前版本中胜率最高的英雄
     * 典型符文：当前版本中效果最强的符文
     *
     * @SerializedName("S_PLUS") 映射后端 JSON 中的 "S_PLUS" 字符串
     * label = "S+"  → UI 上显示的文本
     * color = 0xFFE53E3E → 红色（Alpha=FF 完全不透明, RGB=E53E3E）
     */
    @SerializedName("S_PLUS")
    S_PLUS("S+", 0xFFE53E3E),

    /**
     * S 级 —— 第二高评级，橙色标签
     *
     * 含义：胜率 52%~54%，很强，推荐使用。
     * 在英雄列表中显示为橙色 "S" 标签。
     *
     * @SerializedName("S") 映射后端 JSON 中的 "S" 字符串
     * label = "S"  → UI 上显示的文本
     * color = 0xFFFF6B35 → 橙色（Alpha=FF, RGB=FF6B35）
     */
    @SerializedName("S")
    S("S", 0xFFFF6B35),

    /**
     * A 级 —— 中上评级，金色标签
     *
     * 含义：胜率 50%~52%，略高于平均水平，可以使用。
     * 在英雄列表中显示为金色 "A" 标签。
     *
     * @SerializedName("A") 映射后端 JSON 中的 "A" 字符串
     * label = "A"  → UI 上显示的文本
     * color = 0xFFFFB800 → 金色（Alpha=FF, RGB=FFB800）
     */
    @SerializedName("A")
    A("A", 0xFFFFB800),

    /**
     * B 级 —— 中等评级，绿色标签
     *
     * 含义：胜率 48%~50%，略低于平均水平，看情况选择。
     * 在英雄列表中显示为绿色 "B" 标签。
     *
     * @SerializedName("B") 映射后端 JSON 中的 "B" 字符串
     * label = "B"  → UI 上显示的文本
     * color = 0xFF4CAF50 → 绿色（Alpha=FF, RGB=4CAF50）
     */
    @SerializedName("B")
    B("B", 0xFF4CAF50),

    /**
     * C 级 —— 最低评级，蓝色标签
     *
     * 含义：胜率 < 48%，较弱，不推荐使用。
     * 在英雄列表中显示为蓝色 "C" 标签。
     *
     * 注意：C 级不代表英雄"没用"，只是当前版本胜率较低。
     * 版本更新后评级可能变化。
     *
     * @SerializedName("C") 映射后端 JSON 中的 "C" 字符串
     * label = "C"  → UI 上显示的文本
     * color = 0xFF2196F3 → 蓝色（Alpha=FF, RGB=2196F3）
     */
    @SerializedName("C")
    C("C", 0xFF2196F3);

    /**
     * 梯级标签显示文本 —— 在 UI 上显示的评级文字
     *
     * 这个值用于设置 TextView 的文本，如：
     *   tierBadge.setText(tier.getLabel());  // 显示 "S+"、"A"、"B" 等
     *
     * 为什么不直接用枚举名称（如 S_PLUS）？
     * 因为枚举名称受 Java 命名规范限制（不能有 + 号），
     * 而 label 可以是任意字符串，更灵活。
     */
    private final String label;

    /**
     * 梯级对应的颜色值 —— ARGB 格式的 32 位整数
     *
     * ARGB 格式说明（从高位到低位）：
     * - A（Alpha）：透明度，0xFF = 完全不透明，0x00 = 完全透明
     * - R（Red）：红色分量，0x00~0xFF
     * - G（Green）：绿色分量，0x00~0xFF
     * - B（Blue）：蓝色分量，0x00~0xFF
     *
     * 示例：0xFFE53E3E
     * - FF = 完全不透明
     * - E5 = 红色分量（229）
     * - 3E = 绿色分量（62）
     * - 3E = 蓝色分量（62）
     * 结果：不透明的红色
     *
     * 在 UI 中使用：
     *   tierBadge.setBackgroundColor(tier.getColor());
     *   tierBadge.setTextColor(tier.getColor());
     *
     * 使用 final 修饰：颜色值在枚举创建后不可修改，保证一致性。
     */
    private final int color;

    /**
     * 枚举构造函数 —— 定义每个评级等级的显示文本和颜色
     *
     * 枚举构造函数的特点：
     * 1. 只能是 private（默认就是，无需显式声明）
     * 2. 在枚举类加载时自动调用，创建所有枚举实例
     * 3. 不能在代码中手动 new 一个枚举实例
     *
     * 调用过程：
     * 当 JVM 加载 Tier 类时，会依次执行：
     *   S_PLUS = new Tier("S+", 0xFFE53E3E);
     *   S      = new Tier("S",  0xFFFF6B35);
     *   A      = new Tier("A",  0xFFFFB800);
     *   B      = new Tier("B",  0xFF4CAF50);
     *   C      = new Tier("C",  0xFF2196F3);
     *
     * @param label 显示标签（如 "S+"、"A"），用于 UI 文本
     * @param color ARGB 颜色值（如 0xFFE53E3E），用于 UI 着色
     */
    Tier(String label, int color) {
        this.label = label;
        this.color = color;
    }

    /**
     * 获取梯级显示标签 —— 用于 UI 文本显示
     *
     * 返回值示例："S+"、"S"、"A"、"B"、"C"
     *
     * 使用场景：
     *   TextView tierBadge = findViewById(R.id.tier_badge);
     *   tierBadge.setText(hero.getTier().getLabel());  // 显示 "S+"
     *
     * @return 评级的显示文本
     */
    public String getLabel() {
        return label;
    }

    /**
     * 获取梯级颜色 —— 用于 UI 标签着色
     *
     * 返回值示例：0xFFE53E3E（红色）、0xFFFF6B35（橙色）等
     *
     * 使用场景：
     *   View tierBadge = findViewById(R.id.tier_badge);
     *   tierBadge.setBackgroundColor(hero.getTier().getColor());  // 设置红色背景
     *
     * 也可以用于文字颜色：
     *   tierBadge.setTextColor(tier.getColor());
     *
     * @return ARGB 格式的颜色值（0xAARRGGBB）
     */
    public int getColor() {
        return color;
    }
}
