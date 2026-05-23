package com.aram.mayhem.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aram.mayhem.data.local.entity.AugmentEntity;

import java.util.List;

/**
 * 强化符文数据访问对象 —— 定义对 augments 表的所有数据库操作
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义对 augments（强化符文）表的增删改查操作。
 * 符文是 ARAM 模式中的核心机制，每局游戏可以选择符文强化英雄能力。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、同步方法 vs 异步方法
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本 DAO 提供两种返回类型的方法：
 *
 * 异步方法（返回 LiveData）：
 * - getAllAugments(), getAugmentsByQuality(), searchAugments() 等
 * - 数据变化时自动通知 UI 更新
 * - 适合列表展示场景
 *
 * 同步方法（直接返回 List/Entity）：
 * - getAllAugmentsSync(), getAugmentByIdSync()
 * - 直接返回查询结果，不观察数据变化
 * - 适合推荐算法等需要一次性获取全部数据的场景
 * - 必须在后台线程调用，否则会抛出 IllegalStateException
 *
 * 为什么需要同步方法？
 * - 推荐算法（AugmentRecommendService）需要一次性获取所有符文数据
 * - 推荐算法不需要观察数据变化（只在用户请求时计算一次）
 * - 同步方法比 LiveData 更轻量，没有观察者机制的开销
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、符文筛选维度
 * ═══════════════════════════════════════════════════════════════════
 *
 * 符文可以从三个维度筛选：
 * 1. quality（品质）：Prismatic（棱彩）、Gold（金色）、Silver（银色）
 * 2. synergySet（套装）：符文所属的套装名称（如"刺客"、"法师"）
 * 3. nameZh（名称）：按中文名搜索
 *
 * 关联类：
 * - AugmentEntity：符文表实体类
 * - AugmentRepository：调用本 DAO 的 Repository
 * - AugmentListViewModel：通过 Repository 间接使用本 DAO
 */
@Dao
public interface AugmentDao {

    /**
     * 批量插入符文数据 —— 网络数据同步到本地时调用
     *
     * @param augments 符文实体列表，从 API 获取后转换而来
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AugmentEntity> augments);

    /**
     * 查询所有符文（异步）── 符文列表页使用
     *
     * 排序规则：先按品质升序（Prismatic→Gold→Silver），再按中文名升序
     *
     * @return 所有符文的 LiveData 列表
     */
    @Query("SELECT * FROM augments ORDER BY quality ASC, nameZh ASC")
    LiveData<List<AugmentEntity>> getAllAugments();

    /**
     * 按品质筛选符文（异步）── 列表页品质 Tab 筛选使用
     *
     * @param quality 品质标识（如 "Prismatic"、"Gold"、"Silver"）
     * @return 符合条件的符文列表，按中文名升序
     */
    @Query("SELECT * FROM augments WHERE quality = :quality ORDER BY nameZh ASC")
    LiveData<List<AugmentEntity>> getAugmentsByQuality(String quality);

    /**
     * 按套装筛选符文（异步）── 列表页套装 Tab 筛选使用
     *
     * @param synergySet 套装名称（如 "刺客"、"法师"）
     * @return 符合条件的符文列表，按品质和中文名排序
     */
    @Query("SELECT * FROM augments WHERE synergySet = :synergySet ORDER BY quality ASC, nameZh ASC")
    LiveData<List<AugmentEntity>> getAugmentsBySynergy(String synergySet);

    /**
     * 搜索符文（异步）── 搜索框输入关键词时使用
     *
     * @param query 搜索关键词（中文）
     * @return 匹配的符文列表，按品质和中文名排序
     */
    @Query("SELECT * FROM augments WHERE nameZh LIKE '%' || :query || '%' ORDER BY quality ASC, nameZh ASC")
    LiveData<List<AugmentEntity>> searchAugments(String query);

    /**
     * 按 ID 查询单个符文（异步）── 符文详情页使用
     *
     * @param id 符文 ID
     * @return 单个符文的 LiveData
     */
    @Query("SELECT * FROM augments WHERE id = :id")
    LiveData<AugmentEntity> getAugmentById(long id);

    /**
     * 查询所有符文（同步）── 推荐算法使用
     *
     * 与 getAllAugments() 的区别：
     * - 返回 List 而非 LiveData，不观察数据变化
     * - 直接返回查询结果，适合一次性数据处理
     * - 必须在后台线程调用（Room 不允许在主线程执行同步查询）
     *
     * 使用场景：
     * - AugmentRecommendService 需要获取所有符文来计算推荐
     * - 符文套装进度计算需要遍历所有符文
     *
     * @return 所有符文的列表，按品质和中文名排序
     */
    @Query("SELECT * FROM augments ORDER BY quality ASC, nameZh ASC")
    List<AugmentEntity> getAllAugmentsSync();

    /**
     * 按 ID 查询单个符文（同步）── 推荐算法使用
     *
     * @param id 符文 ID
     * @return 符文实体，不存在时返回 null
     */
    @Query("SELECT * FROM augments WHERE id = :id")
    AugmentEntity getAugmentByIdSync(long id);

    /**
     * 清空符文表 ── 数据同步前清空旧数据
     */
    @Query("DELETE FROM augments")
    void deleteAll();

    /**
     * 获取符文总数 ── 用于判断本地是否有缓存数据
     *
     * @return 符文总数
     */
    @Query("SELECT COUNT(*) FROM augments")
    int getCount();
}
