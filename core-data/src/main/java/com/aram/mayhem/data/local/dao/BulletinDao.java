package com.aram.mayhem.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aram.mayhem.data.local.entity.BulletinEntity;

import java.util.List;

/**
 * 公告数据访问对象 —— 定义对 bulletins 表的所有数据库操作
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义对 bulletins（公告）表的增删改查操作。
 * 公告是管理员发布的信息，包括版本更新、活动通知、维护公告等。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、公告排序规则
 * ═══════════════════════════════════════════════════════════════════
 *
 * ORDER BY isPinned DESC, publishedAt DESC 的含义：
 * 1. isPinned DESC：置顶公告优先显示（1 排在 0 前面）
 * 2. publishedAt DESC：同级别内按发布时间降序（最新的排前面）
 *
 * 这样确保置顶公告始终在列表顶部，非置顶公告按时间排序。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、公告类型
 * ═══════════════════════════════════════════════════════════════════
 *
 * type 字段支持三种公告类型：
 * - "version"：版本更新公告
 * - "event"：活动公告
 * - "notice"：普通通知
 *
 * 用户可以在公告页面按类型筛选。
 *
 * 关联类：
 * - BulletinEntity：公告表实体类
 * - BulletinRepository：调用本 DAO 的 Repository
 * - BulletinListViewModel：通过 Repository 间接使用本 DAO
 */
@Dao
public interface BulletinDao {

    /**
     * 批量插入公告数据 ── 网络数据同步到本地时调用
     *
     * @param bulletins 公告实体列表，从 API 获取后转换而来
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BulletinEntity> bulletins);

    /**
     * 查询所有公告 ── 公告列表页使用
     *
     * 排序规则：置顶优先，然后按发布时间降序
     *
     * @return 所有公告的 LiveData 列表
     */
    @Query("SELECT * FROM bulletins ORDER BY isPinned DESC, publishedAt DESC")
    LiveData<List<BulletinEntity>> getAllBulletins();

    /**
     * 按类型筛选公告 ── 公告页面类型 Tab 筛选使用
     *
     * @param type 公告类型（"version"、"event"、"notice"）
     * @return 符合条件的公告列表，置顶优先+时间降序
     */
    @Query("SELECT * FROM bulletins WHERE type = :type ORDER BY isPinned DESC, publishedAt DESC")
    LiveData<List<BulletinEntity>> getBulletinsByType(String type);

    /**
     * 获取最新 N 条公告 ── 首页轮播/通知栏使用
     *
     * LIMIT :limit 限制返回的记录数，避免加载过多数据。
     * 首页轮播通常只需要 3~5 条最新公告。
     *
     * @param limit 返回的最大记录数（如 5）
     * @return 最新的 N 条公告，按发布时间降序
     */
    @Query("SELECT * FROM bulletins ORDER BY publishedAt DESC LIMIT :limit")
    LiveData<List<BulletinEntity>> getLatestBulletins(int limit);

    /**
     * 清空公告表 ── 数据同步前清空旧数据
     */
    @Query("DELETE FROM bulletins")
    void deleteAll();

    /**
     * 获取公告总数 ── 用于判断本地是否有缓存数据
     *
     * @return 公告总数
     */
    @Query("SELECT COUNT(*) FROM bulletins")
    int getCount();
}
