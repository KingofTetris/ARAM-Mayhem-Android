package com.aram.mayhem.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aram.mayhem.data.local.entity.StrategyEntity;

import java.util.List;

/**
 * 社区攻略数据访问对象 —— 定义对 strategies 表的所有数据库操作
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义对 strategies（社区攻略）表的增删改查操作。
 * 攻略是用户创建的英雄玩法分享内容，包含出装、符文推荐等。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、攻略排序方式
 * ═══════════════════════════════════════════════════════════════════
 *
 * 攻略列表支持两种排序方式：
 * 1. 按热度（score DESC）：得分高的排前面，score = upvotes - downvotes
 * 2. 按最新（createdAt DESC）：发布时间最近的排前面
 *
 * 用户可以在社区页面切换排序方式。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、离线缓存策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 攻略数据采用"网络优先，本地缓存"策略：
 * 1. 有网络时：从 API 获取最新攻略，存入本地数据库
 * 2. 无网络时：从本地数据库读取缓存的攻略数据
 * 3. 网络恢复后：自动刷新数据
 *
 * 关联类：
 * - StrategyEntity：攻略表实体类
 * - StrategyRepository：调用本 DAO 的 Repository
 * - CommunityFeedViewModel：通过 Repository 间接使用本 DAO
 */
@Dao
public interface StrategyDao {

    /**
     * 批量插入攻略数据 ── 网络数据同步到本地时调用
     *
     * @param strategies 攻略实体列表，从 API 获取后转换而来
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<StrategyEntity> strategies);

    /**
     * 查询所有攻略（按得分降序）── 社区页面默认排序
     *
     * score = upvotes - downvotes，得分越高排名越前
     *
     * @return 所有攻略的 LiveData 列表，按得分降序
     */
    @Query("SELECT * FROM strategies ORDER BY score DESC")
    LiveData<List<StrategyEntity>> getAllStrategies();

    /**
     * 查询所有攻略（按发布时间降序）── "最新"排序 Tab 使用
     *
     * @return 所有攻略的 LiveData 列表，按发布时间降序
     */
    @Query("SELECT * FROM strategies ORDER BY createdAt DESC")
    LiveData<List<StrategyEntity>> getStrategiesByLatest();

    /**
     * 查询所有攻略（按热度降序）── "最热"排序 Tab 使用
     *
     * 与 getAllStrategies() 的 SQL 相同，提供语义更明确的方法名
     *
     * @return 所有攻略的 LiveData 列表，按得分降序
     */
    @Query("SELECT * FROM strategies ORDER BY score DESC")
    LiveData<List<StrategyEntity>> getStrategiesByHot();

    /**
     * 按英雄 ID 筛选攻略 ── 英雄详情页"相关攻略"区域使用
     *
     * 当用户查看某个英雄的详情时，底部会显示该英雄的相关攻略列表。
     *
     * @param heroId 英雄 ID
     * @return 该英雄的攻略列表，按得分降序
     */
    @Query("SELECT * FROM strategies WHERE heroId = :heroId ORDER BY score DESC")
    LiveData<List<StrategyEntity>> getStrategiesByHeroId(long heroId);

    /**
     * 清空攻略表 ── 数据同步前清空旧数据
     */
    @Query("DELETE FROM strategies")
    void deleteAll();

    /**
     * 获取攻略总数 ── 用于判断本地是否有缓存数据
     *
     * @return 攻略总数
     */
    @Query("SELECT COUNT(*) FROM strategies")
    int getCount();
}
