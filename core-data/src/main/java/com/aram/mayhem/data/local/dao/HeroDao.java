package com.aram.mayhem.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aram.mayhem.data.local.entity.HeroEntity;

import java.util.List;

/**
 * 英雄数据访问对象 —— 定义对 heroes 表的所有数据库操作
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * DAO（Data Access Object）是 Room 的核心概念之一，
 * 它定义了对数据库表的增删改查操作，让开发者用注解代替手写 SQL。
 *
 * Room 会在编译时检查 @Query 中的 SQL 语法：
 * - 如果表名或列名写错了，编译期就会报错（而不是运行时崩溃）
 * - 如果返回类型与查询结果不匹配，编译期也会报错
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、@Dao 注解说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Dao 标记一个接口为 Room 的数据访问对象。
 * Room 会在编译时自动生成此接口的实现类（HeroDao_Impl.java），
 * 开发者不需要手动实现任何方法。
 *
 * 为什么是接口而不是类？
 * - 接口只定义"做什么"（方法签名），不定义"怎么做"（实现）
 * - Room 通过动态代理在运行时生成实现
 * - 接口更轻量，不需要构造函数和状态管理
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、返回类型说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本 DAO 使用两种返回类型：
 *
 * 1. LiveData<T>：响应式返回类型，数据变化时自动通知 UI 更新
 *    - 适合列表查询（getAllHeroes, searchHeroes 等）
 *    - 当 heroes 表数据变化时，LiveData 自动推送新数据给观察者
 *    - ViewModel 通过 LiveData 观察数据变化，无需手动刷新
 *
 * 2. int：同步返回类型，直接返回查询结果
 *    - 适合简单查询（getCount）
 *    - 在后台线程执行，阻塞调用线程直到查询完成
 *    - 不适合大数据量查询（会阻塞主线程导致 ANR）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、SQL 排序说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * ORDER BY tier ASC, winRate DESC 的含义：
 * 1. 先按 tier 升序排列（S+, S, A, B, C 的字母顺序）
 * 2. 同 tier 内按 winRate 降序排列（胜率高的排前面）
 *
 * 注意：tier 是字符串类型，字母排序 S < A < B < C，
 * 所以 S+ 和 S 会排在最前面，符合"强英雄优先"的展示需求。
 *
 * 关联类：
 * - HeroEntity：英雄表实体类
 * - HeroRepository：调用本 DAO 的 Repository
 * - HeroListViewModel：通过 Repository 间接使用本 DAO
 */
@Dao
public interface HeroDao {

    /**
     * 批量插入英雄数据 —— 网络数据同步到本地时调用
     *
     * @Insert 注解告诉 Room 这是一个插入操作
     * onConflict = OnConflictStrategy.REPLACE 的含义：
     * - 如果插入的数据与已有数据主键冲突（相同 id），则替换旧数据
     * - 这样每次同步时，新数据会覆盖旧数据，保持本地缓存最新
     *
     * 为什么用 REPLACE 而不是 IGNORE 或 ABORT？
     * - REPLACE：更新胜率等变化的数据，保证本地数据与服务器一致
     * - IGNORE：忽略冲突，保留旧数据，可能导致数据过时
     * - ABORT：冲突时抛出异常，导致同步失败
     *
     * @param heroes 英雄实体列表，从 API 获取后转换而来
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HeroEntity> heroes);

    /**
     * 查询所有英雄 —— 英雄列表页使用
     *
     * 排序规则：先按梯级升序（S+在前），再按胜率降序（高胜率在前）
     *
     * 返回 LiveData 的好处：
     * - 当 heroes 表数据变化时（如 insertAll 被调用），LiveData 自动通知观察者
     * - ViewModel 不需要手动调用 refresh()，UI 自动更新
     * - 生命周期感知：Activity 销毁时自动取消观察，避免内存泄漏
     *
     * @return 所有英雄的 LiveData 列表，按梯级和胜率排序
     */
    @Query("SELECT * FROM heroes ORDER BY tier ASC, winRate DESC")
    LiveData<List<HeroEntity>> getAllHeroes();

    /**
     * 按梯级筛选英雄 —— 列表页梯级 Tab 筛选使用
     *
     * 参数 :tier 由 Room 自动绑定，防止 SQL 注入。
     * Room 使用预编译语句（PreparedStatement），参数不会拼入 SQL 字符串。
     *
     * @param tier 梯级标识（如 "S_PLUS"、"A"、"C"）
     * @return 符合条件的英雄列表，按胜率降序
     */
    @Query("SELECT * FROM heroes WHERE tier = :tier ORDER BY winRate DESC")
    LiveData<List<HeroEntity>> getHeroesByTier(String tier);

    /**
     * 按定位筛选英雄 —— 列表页定位 Tab 筛选使用（战士/法师/射手等）
     *
     * @param role 角色定位（如 "战士"、"法师"、"射手"）
     * @return 符合条件的英雄列表，按胜率降序
     */
    @Query("SELECT * FROM heroes WHERE role = :role ORDER BY winRate DESC")
    LiveData<List<HeroEntity>> getHeroesByRole(String role);

    /**
     * 搜索英雄 —— 搜索框输入关键词时使用
     *
     * LIKE '%' || :query || '%' 的含义：
     * - '%' 是 SQL 通配符，匹配任意字符
     * - || 是 SQLite 字符串连接符
     * - 如果 query = "亚索"，SQL 变为 LIKE '%亚索%'
     * - 匹配包含"亚索"的任何字符串
     *
     * 同时搜索中文名和英文名：
     * - nameZh LIKE '%亚索%' → 匹配中文名
     * - nameEn LIKE '%Yasuo%' → 匹配英文名
     * - 用 OR 连接，任一匹配即返回
     *
     * @param query 搜索关键词（中文或英文）
     * @return 匹配的英雄列表，按胜率降序
     */
    @Query("SELECT * FROM heroes WHERE nameZh LIKE '%' || :query || '%' OR nameEn LIKE '%' || :query || '%' ORDER BY winRate DESC")
    LiveData<List<HeroEntity>> searchHeroes(String query);

    /**
     * 按 ID 查询单个英雄 —— 英雄详情页使用
     *
     * @param id 英雄 ID（对应 Riot DataDragon 的英雄 ID）
     * @return 单个英雄的 LiveData，数据变化时自动更新
     */
    @Query("SELECT * FROM heroes WHERE id = :id")
    LiveData<HeroEntity> getHeroById(long id);

    /**
     * 清空英雄表 —— 数据同步前清空旧数据
     *
     * 使用场景：全量同步时，先清空旧数据再插入新数据
     * 注意：配合 insertAll(REPLACE) 使用时，通常不需要先清空
     *
     * 危险操作：会删除所有英雄数据，仅在特定场景使用！
     */
    @Query("DELETE FROM heroes")
    void deleteAll();

    /**
     * 获取英雄总数 —— 用于判断本地是否有缓存数据
     *
     * 返回 int 而非 LiveData，因为只需要一个快照值，不需要观察变化。
     *
     * 使用场景：
     * - 应用启动时判断是否需要从网络加载数据
     * - getCount() > 0 表示有本地缓存，可以离线浏览
     * - getCount() == 0 表示无缓存，必须先联网加载
     *
     * @return 英雄总数
     */
    @Query("SELECT COUNT(*) FROM heroes")
    int getCount();
}
