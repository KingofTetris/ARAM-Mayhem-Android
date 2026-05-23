package com.aram.mayhem.ui.model;

import com.aram.mayhem.common.Tier;

/**
 * 英雄 UI 模型 ── 英雄列表卡片的数据载体
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroUiModel 是"UI 层数据模型"，专门给 RecyclerView 列表卡片用的。
 * 它不直接和后端通信，也不直接操作数据库，而是由 Repository 层
 * 从 HeroResponse（网络 DTO）或 HeroEntity（本地数据库实体）转换而来。
 *
 * 为什么要单独建一个 UiModel，不直接用 HeroResponse？
 * - 解耦：UI 层不依赖网络层的数据结构，后端改字段不影响 UI
 * - 裁剪：列表页只需要部分字段，不需要详情页的技能、出装等数据
 * - 格式化：UiModel 自带格式化方法（如 getWinRateDisplay()），UI 直接调用
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端 API ──→ HeroResponse（网络 DTO）
 *                    │
 *                    ▼  Repository 层转换
 *              HeroUiModel（本类）
 *                    │
 *                    ▼  HeroCardAdapter.bind()
 *           RecyclerView 卡片视图
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、字段说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * | 字段       | 类型    | 说明                                    | 示例              |
 * |------------|---------|-----------------------------------------|-------------------|
 * | id         | long    | 英雄唯一 ID（Riot DataDragon 标识）     | 157（亚索）       |
 * | nameZh     | String  | 中文名称                                 | "亚索"            |
 * | nameEn     | String  | 英文名称                                 | "Yasuo"           |
 * | title      | String  | 英雄称号                                 | "疾风剑豪"        |
 * | role       | String  | 定位/角色                                | "战士"            |
 * | tier       | Tier    | 梯级评级（S+/S/A/B/C 枚举）              | Tier.S_PLUS       |
 * | winRate    | double  | 胜率（0.0~1.0 小数形式）                 | 0.5234            |
 * | pickRate   | double  | 选取率（0.0~1.0 小数形式）               | 0.1523            |
 * | avatarUrl  | String  | 头像图片 URL                              | "https://..."     |
 * | isTrap     | boolean | 是否为版本陷阱英雄（被削弱需警告）        | false             |
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、为什么字段都是 final 的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 所有字段用 final 修饰，表示"不可变对象"（Immutable Object）：
 * - 构造后不能修改，避免数据被意外篡改
 * - 线程安全：多线程访问不需要加锁
 * - DiffUtil 友好：比较两个 UiModel 时，只需比较字段值即可
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、与 HeroDetailUiModel 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * HeroUiModel（本类）── 列表页用的"轻量版"：
 *   - 只有名称、胜率、梯级等基础字段
 *   - 数据量小，列表页一次加载 20 条也很快
 *
 * HeroDetailUiModel ── 详情页用的"完整版"：
 *   - 继承了本类的所有字段
 *   - 额外包含技能、克制提示、协同英雄、推荐出装等
 *   - 数据量大，只在点击进入详情页时加载
 */
public class HeroUiModel {

    /**
     * 英雄唯一 ID ── 对应 Riot DataDragon 的英雄标识
     *
     * 为什么用 long 而不是 Long？
     * - id 一定有值，不会是 null，用基本类型更安全
     * - 基本类型没有自动装箱/拆箱开销，性能更好
     * - 注意：在 DiffUtil 中比较 id 时，用 == 即可（基本类型）
     *
     * 示例值：157（亚索）、1（安妮）、22（沃利贝尔）
     */
    private final long id;

    /** 英雄中文名称 ── 显示在卡片主标题位置，如"亚索"、"安妮" */
    private final String nameZh;

    /** 英雄英文名称 ── 显示在卡片副标题位置，如"Yasuo"、"Annie" */
    private final String nameEn;

    /** 英雄称号 ── 显示在详情页，如"疾风剑豪"、"黑暗之女" */
    private final String title;

    /**
     * 英雄定位/角色 ── 显示在卡片角色标签位置
     *
     * 常见值：战士、法师、刺客、射手、辅助、坦克
     * 注意：一个英雄可能有多个定位，这里取主定位
     */
    private final String role;

    /**
     * 梯级评级 ── 基于胜率的综合评级，决定卡片上梯级标签的颜色
     *
     * Tier 是枚举类型，定义在 core-common 模块中：
     * - S_PLUS（金色）：胜率极高，版本强势英雄
     * - S（橙色）：胜率较高
     * - A（绿色）：胜率中等偏上
     * - B（蓝色）：胜率中等
     * - C（灰色）：胜率较低，不推荐
     *
     * TierBadgeView 组件会根据 tier 自动设置背景色和文字
     */
    private final Tier tier;

    /**
     * 胜率 ── 该英雄在 ARAM 模式中的胜率
     *
     * 存储格式：0.0 ~ 1.0 的小数（如 0.5234 表示 52.34%）
     * 显示格式：通过 getWinRateDisplay() 转为 "胜率 52.3%" 文字
     *
     * 为什么存小数而不是百分比整数？
     * - 后端返回的就是小数格式，避免转换精度丢失
     * - 计算和比较时小数更方便
     */
    private final double winRate;

    /**
     * 选取率 ── 该英雄在 ARAM 模式中的被选取频率
     *
     * 存储格式：0.0 ~ 1.0 的小数（如 0.1523 表示 15.23%）
     * 高选取率 = 热门英雄，低选取率 = 冷门英雄
     */
    private final double pickRate;

    /**
     * 头像图片 URL ── Glide 加载头像的图片来源
     *
     * URL 格式：通常指向 Riot DataDragon CDN
     * 示例："https://ddragon.leagueoflegends.com/cdn/14.1.1/img/champion/Yasuo.png"
     *
     * Glide 加载流程：
     * 1. HeroCardAdapter.bind() 调用 Glide.with().load(avatarUrl)
     * 2. Glide 先检查内存缓存 → 磁盘缓存 → 网络下载
     * 3. 下载后自动裁剪为圆形（circleCrop()）显示
     */
    private final String avatarUrl;

    /**
     * 是否为版本陷阱英雄 ── 被近期版本大幅削弱的英雄
     *
     * 版本陷阱的含义：
     * - 该英雄在之前版本很强，但最近被削弱了
     * - 玩家可能还以为它很强，继续选用导致输掉比赛
     * - 需要在卡片上显示红色警告边框提醒玩家
     *
     * UI 表现：
     * - isTrap = true：卡片边框变为红色（trap_warning 颜色）
     * - isTrap = false：卡片边框无特殊颜色
     *
     * 默认值：false（构造函数中硬编码）
     */
    private final boolean isTrap;

    /**
     * 构造函数 ── 创建英雄 UI 模型实例
     *
     * 使用方式（在 Repository 层调用）：
     * <pre>
     * HeroUiModel model = new HeroUiModel(
     *     157,           // id：亚索的 Riot ID
     *     "亚索",        // nameZh：中文名
     *     "Yasuo",       // nameEn：英文名
     *     "疾风剑豪",    // title：称号
     *     "战士",        // role：定位
     *     Tier.S_PLUS,   // tier：梯级
     *     0.5234,        // winRate：胜率 52.34%
     *     0.1523,        // pickRate：选取率 15.23%
     *     "https://..."  // avatarUrl：头像 URL
     * );
     * </pre>
     *
     * 注意：isTrap 参数未暴露在构造函数中，默认为 false
     * 如果需要设置 isTrap = true，需要扩展构造函数或使用 Builder 模式
     *
     * @param id        英雄唯一 ID
     * @param nameZh    中文名称
     * @param nameEn    英文名称
     * @param title     英雄称号
     * @param role      定位/角色
     * @param tier      梯级评级
     * @param winRate   胜率（0.0~1.0）
     * @param pickRate  选取率（0.0~1.0）
     * @param avatarUrl 头像图片 URL
     */
    public HeroUiModel(long id, String nameZh, String nameEn, String title,
                       String role, Tier tier, double winRate, double pickRate,
                       String avatarUrl) {
        this.id = id;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.title = title;
        this.role = role;
        this.tier = tier;
        this.winRate = winRate;
        this.pickRate = pickRate;
        this.avatarUrl = avatarUrl;
        this.isTrap = false;
    }

    /**
     * 获取英雄 ID
     * @return 英雄唯一标识，如 157（亚索）
     */
    public long getId() { return id; }

    /**
     * 获取英雄中文名称
     * @return 中文名称，如"亚索"
     */
    public String getNameZh() { return nameZh; }

    /**
     * 获取英雄英文名称
     * @return 英文名称，如"Yasuo"
     */
    public String getNameEn() { return nameEn; }

    /**
     * 获取英雄称号
     * @return 英雄称号，如"疾风剑豪"
     */
    public String getTitle() { return title; }

    /**
     * 获取英雄定位
     * @return 定位/角色，如"战士"
     */
    public String getRole() { return role; }

    /**
     * 获取梯级评级
     * @return Tier 枚举值，如 Tier.S_PLUS
     */
    public Tier getTier() { return tier; }

    /**
     * 获取胜率（原始小数值）
     * @return 胜率，0.0~1.0，如 0.5234
     */
    public double getWinRate() { return winRate; }

    /**
     * 获取选取率（原始小数值）
     * @return 选取率，0.0~1.0，如 0.1523
     */
    public double getPickRate() { return pickRate; }

    /**
     * 获取头像图片 URL
     * @return 图片 URL 字符串，供 Glide 加载
     */
    public String getAvatarUrl() { return avatarUrl; }

    /**
     * 判断是否为版本陷阱英雄
     * @return true 表示该英雄被削弱需要警告，false 表示正常
     */
    public boolean isTrap() { return isTrap; }

    /**
     * 获取胜率显示文本 ── 将小数胜率转为用户友好的百分比文字
     *
     * 转换逻辑：
     * - 输入：winRate = 0.5234
     * - 乘以 100：52.34
     * - 格式化保留 1 位小数：52.3
     * - 拼接前缀："胜率 52.3%"
     *
     * 使用场景：
     * - HeroCardAdapter.bind() 中设置卡片胜率文字
     * - binding.textWinRate.setText(hero.getWinRateDisplay())
     *
     * @return 格式化后的胜率文字，如"胜率 52.3%"
     */
    public String getWinRateDisplay() {
        return String.format("胜率 %.1f%%", winRate);
    }
}
