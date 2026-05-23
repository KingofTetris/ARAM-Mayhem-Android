package com.aram.mayhem.ui.model;

import com.aram.mayhem.ui.R;

import java.util.Locale;

/**
 * 强化符文 UI 模型 ── 符文列表卡片的数据载体
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentUiModel 是符文列表页专用的数据模型，用于 RecyclerView 卡片展示。
 * 由 Repository 层从 AugmentResponse（网络 DTO）或 AugmentEntity（数据库实体）转换而来。
 *
 * 与 HeroUiModel 类似的设计思路：
 * - 不可变对象（所有字段 final）
 * - 自带格式化方法（getWinRateDisplay、getQualityDisplay 等）
 * - 自带颜色映射方法（getQualityColorRes）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端 API ──→ AugmentResponse（网络 DTO）
 *                    │
 *                    ▼  AugmentRepository.toUiModel()
 *              AugmentUiModel（本类）
 *                    │
 *                    ▼  AugmentCardAdapter.bind()
 *           RecyclerView 卡片视图
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、符文品质体系
 * ═══════════════════════════════════════════════════════════════════
 *
 * ARAM 强化符文分三个品质等级：
 *
 * | 品质英文      | 品质中文 | 颜色     | 说明               |
 * |--------------|---------|---------|---------------------|
 * | PRISMATIC    | 棱彩    | 紫色     | 最稀有，效果最强     |
 * | GOLD/LEGENDARY | 金    | 金色     | 稀有，效果较强       |
 * | 其他(SILVER等) | 银     | 灰色     | 普通，效果一般       |
 *
 * 品质影响：
 * - 卡片边框颜色（通过 getQualityColorRes() 获取）
 * - QualityChip 组件的背景色和文字色
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、套装系统说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 强化符文可以属于多个套装（Synergy Set）：
 * - synergySet：主套装名称
 * - synergySet2：副套装名称（可为 null）
 * - synergySet3：第三套装名称（可为 null）
 *
 * 示例：一个符文可能同时属于"暴击套装"和"攻速套装"
 * 当同时装备同一套装的多个符文时，会触发套装加成效果
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、胜率存储格式差异（重要！）
 * ═══════════════════════════════════════════════════════════════════
 *
 * 注意：AugmentUiModel 的 winRate/pickRate 与 HeroUiModel 的存储格式不同！
 *
 * | 模型            | winRate 格式      | getWinRateDisplay() 转换 |
 * |----------------|-------------------|--------------------------|
 * | HeroUiModel    | 0.0~1.0（小数）    | String.format("%.1f%%", winRate) → "52.3%" |
 * | AugmentUiModel | 0.0~1.0（小数）    | String.format("%.1f%%", winRate * 100) → "52.3%" |
 *
 * AugmentUiModel 的格式化方法需要乘以 100，因为后端返回的是小数格式
 * 而 HeroUiModel 的格式化方法不需要乘以 100（直接用小数格式化）
 * 这是一个已知的风格不一致，后续应统一
 */
public class AugmentUiModel {

    /** 符文唯一 ID ── 对应后端数据库的主键 */
    private final long id;

    /** 符文中文名称 ── 显示在卡片主标题，如"暴击强化" */
    private final String nameZh;

    /** 符文英文名称 ── 用于搜索匹配，如"Crit Augment" */
    private final String nameEn;

    /**
     * 符文描述 ── 符文的效果说明文字
     *
     * 示例："你的暴击伤害提高 15%"
     * 显示在符文详情页，列表卡片不显示（太长）
     */
    private final String description;

    /**
     * 符文品质 ── 决定卡片边框颜色和品质标签
     *
     * 可选值：PRISMATIC / GOLD / LEGENDARY / EPIC / 其他
     * 颜色映射见 getQualityColorRes() 和 getQualityDisplay() 方法
     */
    private final String quality;

    /**
     * 主套装名称 ── 符文所属的第一个套装
     *
     * 示例："暴击" "攻速" "法术"
     * 显示在卡片副标题：getSynergyDisplay() → "套装：暴击"
     * 为 null 时表示不属于任何套装
     */
    private final String synergySet;

    /**
     * 副套装名称 ── 符文所属的第二个套装（可为 null）
     *
     * 一个符文可以同时属于多个套装
     * 目前只在详情页显示，列表卡片只显示主套装
     */
    private final String synergySet2;

    /**
     * 第三套装名称 ── 符文所属的第三个套装（可为 null）
     *
     * 极少数符文属于三个套装
     */
    private final String synergySet3;

    /**
     * 符文图标 URL ── Glide 加载符文图标的来源
     *
     * URL 格式：通常指向 Riot CDN 或本地资源
     * 列表卡片使用小尺寸图标，详情页使用大尺寸图标
     */
    private final String iconUrl;

    /**
     * 胜率 ── 该符文在 ARAM 中的胜率
     *
     * 存储格式：0.0~1.0 的小数（如 0.5234 表示 52.34%）
     * 显示格式：getWinRateDisplay() → "52.3%"（注意要乘以 100）
     */
    private final double winRate;

    /**
     * 选取率 ── 该符文在 ARAM 中的被选取频率
     *
     * 存储格式：0.0~1.0 的小数
     * 显示格式：getPickRateDisplay() → "15.2%"
     */
    private final double pickRate;

    /**
     * 平均名次 ── 使用该符文后的平均排名
     *
     * ARAM 中通常不计算名次，此字段可能用于其他模式
     * 范围：1.0~8.0（1.0 = 第一名，8.0 = 第八名）
     * 值越低表示使用该符文后表现越好
     */
    private final double avgPlacement;

    /**
     * 梯级评级字符串 ── 基于胜率的综合评级
     *
     * 与 HeroUiModel.tier（Tier 枚举）不同，这里用 String 类型
     * 可选值："S_PLUS" / "S" / "A" / "B" / "C"
     *
     * 为什么用 String 而不是 Tier 枚举？
     * - 后端返回的是字符串格式
     * - 转换为枚举需要额外映射逻辑
     * - 列表页只需要显示文字，不需要枚举的颜色映射
     */
    private final String tier;

    /**
     * 是否为版本陷阱符文 ── 被近期版本削弱的符文
     *
     * true 时卡片边框变为红色警告
     * 与 HeroUiModel.isTrap 含义相同
     */
    private final boolean isTrap;

    /**
     * 构造函数 ── 创建符文 UI 模型实例
     *
     * @param id            符文 ID
     * @param nameZh        中文名称
     * @param nameEn        英文名称
     * @param description   符文描述
     * @param quality       品质（PRISMATIC/GOLD/SILVER）
     * @param synergySet    主套装名称
     * @param synergySet2   副套装名称（可为 null）
     * @param synergySet3   第三套装名称（可为 null）
     * @param iconUrl       图标 URL
     * @param winRate       胜率（0.0~1.0）
     * @param pickRate      选取率（0.0~1.0）
     * @param avgPlacement  平均名次
     * @param tier          梯级评级字符串
     * @param isTrap        是否为版本陷阱
     */
    public AugmentUiModel(long id, String nameZh, String nameEn, String description,
                          String quality, String synergySet, String synergySet2, String synergySet3,
                          String iconUrl, double winRate, double pickRate, double avgPlacement,
                          String tier, boolean isTrap) {
        this.id = id;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.description = description;
        this.quality = quality;
        this.synergySet = synergySet;
        this.synergySet2 = synergySet2;
        this.synergySet3 = synergySet3;
        this.iconUrl = iconUrl;
        this.winRate = winRate;
        this.pickRate = pickRate;
        this.avgPlacement = avgPlacement;
        this.tier = tier;
        this.isTrap = isTrap;
    }

    /** @return 符文唯一 ID */
    public long getId() {
        return id;
    }

    /** @return 符文中文名称 */
    public String getNameZh() {
        return nameZh;
    }

    /**
     * 获取符文名称 ── getNameZh() 的别名
     *
     * 为什么有两个获取名称的方法？
     * - getNameZh()：语义明确，表示中文名称
     * - getName()：通用名称，AugmentCardAdapter 中使用
     * - 两者返回值相同，只是为了不同场景的调用便利性
     *
     * @return 符文中文名称
     */
    public String getName() {
        return nameZh;
    }

    /** @return 符文英文名称 */
    public String getNameEn() {
        return nameEn;
    }

    /** @return 符文描述 */
    public String getDescription() {
        return description;
    }

    /** @return 符文品质（PRISMATIC/GOLD/SILVER 等） */
    public String getQuality() {
        return quality;
    }

    /** @return 主套装名称，可为 null */
    public String getSynergySet() {
        return synergySet;
    }

    /** @return 副套装名称，可为 null */
    public String getSynergySet2() {
        return synergySet2;
    }

    /** @return 第三套装名称，可为 null */
    public String getSynergySet3() {
        return synergySet3;
    }

    /** @return 符文图标 URL */
    public String getIconUrl() {
        return iconUrl;
    }

    /** @return 胜率（0.0~1.0 小数） */
    public double getWinRate() {
        return winRate;
    }

    /** @return 选取率（0.0~1.0 小数） */
    public double getPickRate() {
        return pickRate;
    }

    /** @return 平均名次（1.0~8.0，越低越好） */
    public double getAvgPlacement() {
        return avgPlacement;
    }

    /** @return 梯级评级字符串（S_PLUS/S/A/B/C） */
    public String getTier() {
        return tier;
    }

    /** @return 是否为版本陷阱符文 */
    public boolean isTrap() {
        return isTrap;
    }

    /**
     * 获取胜率显示文本 ── 将小数胜率转为百分比文字
     *
     * 转换逻辑：
     * - 输入：winRate = 0.5234
     * - 乘以 100：52.34
     * - 格式化保留 1 位小数：52.3
     * - 拼接百分号："52.3%"
     *
     * 注意：这里需要乘以 100，因为 winRate 存储的是 0~1 的小数
     * 与 HeroUiModel.getWinRateDisplay() 不同（那里不需要乘以 100）
     *
     * Locale.getDefault()：使用系统默认区域设置
     * - 某些地区的小数点用逗号（如德语：52,3%）
     * - 中文/英文环境用点号（52.3%）
     *
     * @return 格式化后的胜率百分比，如"52.3%"
     */
    public String getWinRateDisplay() {
        return String.format(Locale.getDefault(), "%.1f%%", winRate * 100);
    }

    /**
     * 获取选取率显示文本 ── 将小数选取率转为百分比文字
     *
     * 转换逻辑与 getWinRateDisplay() 相同
     *
     * @return 格式化后的选取率百分比，如"15.2%"
     */
    public String getPickRateDisplay() {
        return String.format(Locale.getDefault(), "%.1f%%", pickRate * 100);
    }

    /**
     * 获取平均名次显示文本 ── 保留 2 位小数
     *
     * 示例：avgPlacement = 3.4567 → "3.46"
     *
     * @return 格式化后的平均名次，如"3.46"
     */
    public String getAvgPlacementDisplay() {
        return String.format(Locale.getDefault(), "%.2f", avgPlacement);
    }

    /**
     * 获取品质对应的颜色资源 ID ── 用于设置卡片边框颜色
     *
     * 品质与颜色映射：
     * - PRISMATIC → R.color.quality_prismatic（紫色）
     * - GOLD / LEGENDARY → R.color.quality_gold（金色）
     * - 其他（SILVER 等）→ R.color.quality_silver（灰色）
     *
     * 使用方式（在 AugmentCardAdapter.bind() 中）：
     * <pre>
     * int qualityColor = context.getColor(augment.getQualityColorRes());
     * binding.cardAugment.setStrokeColor(qualityColor);
     * </pre>
     *
     * 为什么返回颜色资源 ID 而不是直接返回颜色值？
     * - 资源 ID 支持主题切换（深色/浅色模式）
     * - ContextCompat.getColor() 会自动处理主题
     * - 直接使用硬编码颜色值无法适配主题
     *
     * @return 颜色资源 ID（如 R.color.quality_prismatic）
     */
    public int getQualityColorRes() {
        if (quality == null) return R.color.quality_silver;
        switch (quality.toUpperCase()) {
            case "PRISMATIC":
                return R.color.quality_prismatic;
            case "GOLD":
            case "LEGENDARY":
                return R.color.quality_gold;
            default:
                return R.color.quality_silver;
        }
    }

    /**
     * 获取套装显示文本 ── 用于卡片副标题
     *
     * 转换逻辑：
     * - synergySet 不为 null → "套装：暴击"
     * - synergySet 为 null → ""（空字符串，不显示套装信息）
     *
     * 注意：只显示主套装，副套装和第三套装在详情页展示
     *
     * @return 套装显示文本，如"套装：暴击" 或 ""
     */
    public String getSynergyDisplay() {
        return synergySet != null ? "套装：" + synergySet : "";
    }

    /**
     * 获取品质中文显示文本 ── 用于品质标签
     *
     * 品质与中文映射：
     * - PRISMATIC → "棱彩"
     * - GOLD / LEGENDARY → "金"
     * - EPIC → "紫"
     * - 其他 → "银"
     *
     * 使用方式（在 QualityChip 或 TextView 中）：
     * <pre>
     * chip.setText(augment.getQualityDisplay());
     * </pre>
     *
     * @return 品质中文文本，如"棱彩"、"金"、"银"
     */
    public String getQualityDisplay() {
        if (quality == null) return "银";
        switch (quality.toUpperCase()) {
            case "PRISMATIC":
                return "棱彩";
            case "GOLD":
            case "LEGENDARY":
                return "金";
            case "EPIC":
                return "紫";
            default:
                return "银";
        }
    }
}
