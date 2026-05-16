package com.aram.mayhem.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aram.mayhem.data.local.entity.AugmentEntity;

import java.util.List;

/**
 * 强化符文数据访问对象
 *
 * 功能：符文列表查询（按品质/套装/关键词筛选）、单个符文查询、批量插入、清空
 * 返回类型：LiveData（观察式）、List/Entity（同步，供推荐算法使用）
 */
@Dao
public interface AugmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AugmentEntity> augments);

    @Query("SELECT * FROM augments ORDER BY quality ASC, nameZh ASC")
    LiveData<List<AugmentEntity>> getAllAugments();

    @Query("SELECT * FROM augments WHERE quality = :quality ORDER BY nameZh ASC")
    LiveData<List<AugmentEntity>> getAugmentsByQuality(String quality);

    @Query("SELECT * FROM augments WHERE synergySet = :synergySet ORDER BY quality ASC, nameZh ASC")
    LiveData<List<AugmentEntity>> getAugmentsBySynergy(String synergySet);

    @Query("SELECT * FROM augments WHERE nameZh LIKE '%' || :query || '%' ORDER BY quality ASC, nameZh ASC")
    LiveData<List<AugmentEntity>> searchAugments(String query);

    @Query("SELECT * FROM augments WHERE id = :id")
    LiveData<AugmentEntity> getAugmentById(long id);

    @Query("SELECT * FROM augments ORDER BY quality ASC, nameZh ASC")
    List<AugmentEntity> getAllAugmentsSync();

    @Query("SELECT * FROM augments WHERE id = :id")
    AugmentEntity getAugmentByIdSync(long id);

    @Query("DELETE FROM augments")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM augments")
    int getCount();
}