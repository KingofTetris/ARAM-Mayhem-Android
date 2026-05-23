package com.aram.mayhem.data.local.converter;

import com.aram.mayhem.data.local.entity.HeroEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * Room 类型转换器 ── 在 Java 对象和 SQLite 原生类型之间进行转换
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、为什么需要类型转换器？
 * ═══════════════════════════════════════════════════════════════════
 *
 * SQLite 只支持 5 种基本数据类型：NULL、INTEGER、REAL、TEXT、BLOB
 * 但 Java 中有很多复杂类型（如 List、Map、自定义对象），
 * 这些类型无法直接存入 SQLite 数据库。
 *
 * Room 的 TypeConverter 就是解决这个问题的桥梁：
 * - 写入数据库时：Java 复杂类型 → SQLite 原生类型（通常是 TEXT/JSON）
 * - 读取数据库时：SQLite 原生类型 → Java 复杂类型
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、转换策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本转换器使用 Gson 库将复杂类型序列化为 JSON 字符串：
 *
 * | Java 类型                    | SQLite 类型 | JSON 示例                                    |
 * |-----------------------------|------------|----------------------------------------------|
 * | List<SkillData>             | TEXT       | [{"key":"Q","name":"斩钢闪","description":...}] |
 * | List<String>                | TEXT       | ["避免近身对拼","利用控制技能打断位移"]            |
 * | List<Long>                  | TEXT       | [101,205,302]                                 |
 * | List<AugmentBriefData>      | TEXT       | [{"id":101,"nameZh":"电刑","quality":"Gold"}]  |
 *
 * 为什么选择 JSON 而不是其他格式？
 * - JSON 是最通用的数据交换格式，可读性好
 * - Gson 是 Android 生态最成熟的 JSON 库
 * - JSON 支持嵌套结构，适合复杂对象
 * - 缺点：JSON 序列化/反序列化有一定性能开销
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、@TypeConverter 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @TypeConverter 标记的方法必须是成对的：
 * - 一个方法负责"写入转换"（Java → SQLite）
 * - 另一个方法负责"读取转换"（SQLite → Java）
 * - 两个方法的参数和返回类型必须互为反向
 *
 * Room 通过方法签名自动匹配转换器：
 * - 当遇到 List<SkillData> 类型的字段时，调用 fromSkillList() 转为 String
 * - 当从数据库读取 String 类型的值时，调用 toSkillList() 转为 List<SkillData>
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、TypeToken 的作用
 * ═══════════════════════════════════════════════════════════════════
 *
 * Java 的泛型在运行时会被"擦除"（Type Erasure）：
 * - List<SkillData> 在运行时变成 List（类型参数丢失）
 * - Gson 无法知道 JSON 应该反序列化为 List<SkillData> 还是 List<String>
 *
 * TypeToken 通过匿名子类保留泛型信息：
 * - new TypeToken<List<SkillData>>() {} 创建了一个匿名子类
 * - 匿名子类在编译时保留了完整的泛型信息
 * - gson.fromJson(data, type) 利用这个信息正确反序列化
 *
 * 关联类：
 * - HeroEntity：使用本转换器的实体类（skills, counterTips, synergies 等字段）
 * - AugmentEntity：使用本转换器的实体类（heroIds 字段）
 * - AppDatabase：注册本转换器的数据库类
 */
public class SkillListConverter {

    /**
     * Gson 实例 ── 全局共享，避免重复创建
     *
     * Gson 是线程安全的，可以多线程共享使用。
     * 创建 Gson 实例有一定开销（解析注解、注册适配器等），
     * 所以用 static final 确保只创建一次。
     */
    private static final Gson gson = new Gson();

    /**
     * List<SkillData> → JSON String ── 写入数据库时调用
     *
     * 将技能列表序列化为 JSON 字符串，存入 SQLite 的 TEXT 列。
     *
     * 示例：
     * 输入：[{key="Q", name="斩钢闪", description="向前方出剑"}]
     * 输出："[{\"key\":\"Q\",\"name\":\"斩钢闪\",\"description\":\"向前方出剑\"}]"
     *
     * @param skills 技能列表，null 时返回 null（Room 会存储 NULL）
     * @return JSON 字符串
     */
    @androidx.room.TypeConverter
    public String fromSkillList(List<HeroEntity.SkillData> skills) {
        if (skills == null) return null;
        return gson.toJson(skills);
    }

    /**
     * JSON String → List<SkillData> ── 读取数据库时调用
     *
     * 将 JSON 字符串反序列化为技能列表。
     *
     * 注意：data 为 null 时返回空列表而非 null，
     * 避免 NullPointerException 和空指针安全问题。
     *
     * @param data JSON 字符串，从数据库 TEXT 列读取
     * @return 技能列表，永不为 null
     */
    @androidx.room.TypeConverter
    public List<HeroEntity.SkillData> toSkillList(String data) {
        if (data == null) return Collections.emptyList();
        Type type = new TypeToken<List<HeroEntity.SkillData>>() {}.getType();
        return gson.fromJson(data, type);
    }

    /**
     * List<String> → JSON String ── 写入数据库时调用
     *
     * 用于 HeroEntity.counterTips 和 HeroEntity.synergies 字段。
     * 也用于 StrategyEntity.augmentIcons 和 StrategyEntity.itemIcons 字段。
     *
     * @param list 字符串列表，null 时返回 null
     * @return JSON 字符串
     */
    @androidx.room.TypeConverter
    public String fromStringList(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    /**
     * JSON String → List<String> ── 读取数据库时调用
     *
     * @param data JSON 字符串
     * @return 字符串列表，永不为 null
     */
    @androidx.room.TypeConverter
    public List<String> toStringList(String data) {
        if (data == null) return Collections.emptyList();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(data, type);
    }

    /**
     * List<Long> → JSON String ── 写入数据库时调用
     *
     * 用于 HeroEntity.recommendedAugmentIds 和 AugmentEntity.heroIds 字段。
     *
     * @param list Long 列表，null 时返回 null
     * @return JSON 字符串
     */
    @androidx.room.TypeConverter
    public String fromLongList(List<Long> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    /**
     * JSON String → List<Long> ── 读取数据库时调用
     *
     * @param data JSON 字符串
     * @return Long 列表，永不为 null
     */
    @androidx.room.TypeConverter
    public List<Long> toLongList(String data) {
        if (data == null) return Collections.emptyList();
        Type type = new TypeToken<List<Long>>() {}.getType();
        return gson.fromJson(data, type);
    }

    /**
     * List<AugmentBriefData> → JSON String ── 写入数据库时调用
     *
     * 用于 HeroEntity.recommendedAugments 字段。
     * 存储推荐符文的简要信息，避免列表页额外查询 augments 表。
     *
     * @param augments 符文简要信息列表，null 时返回 null
     * @return JSON 字符串
     */
    @androidx.room.TypeConverter
    public String fromAugmentBriefList(List<HeroEntity.AugmentBriefData> augments) {
        if (augments == null) return null;
        return gson.toJson(augments);
    }

    /**
     * JSON String → List<AugmentBriefData> ── 读取数据库时调用
     *
     * @param data JSON 字符串
     * @return 符文简要信息列表，永不为 null
     */
    @androidx.room.TypeConverter
    public List<HeroEntity.AugmentBriefData> toAugmentBriefList(String data) {
        if (data == null) return Collections.emptyList();
        Type type = new TypeToken<List<HeroEntity.AugmentBriefData>>() {}.getType();
        return gson.fromJson(data, type);
    }
}
