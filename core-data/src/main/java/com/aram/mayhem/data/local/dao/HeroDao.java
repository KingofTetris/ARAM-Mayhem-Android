package com.aram.mayhem.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aram.mayhem.data.local.entity.HeroEntity;

import java.util.List;

/**
 * 英雄数据访问对象
 *
 * 功能：英雄列表查询（按梯级/定位/关键词筛选）、单个英雄查询、批量插入、清空
 * 返回类型：LiveData（观察式）、int（同步）
 */
@Dao
public interface HeroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HeroEntity> heroes);

    @Query("SELECT * FROM heroes ORDER BY tier ASC, winRate DESC")
    LiveData<List<HeroEntity>> getAllHeroes();

    @Query("SELECT * FROM heroes WHERE tier = :tier ORDER BY winRate DESC")
    LiveData<List<HeroEntity>> getHeroesByTier(String tier);

    @Query("SELECT * FROM heroes WHERE role = :role ORDER BY winRate DESC")
    LiveData<List<HeroEntity>> getHeroesByRole(String role);

    @Query("SELECT * FROM heroes WHERE nameZh LIKE '%' || :query || '%' OR nameEn LIKE '%' || :query || '%' ORDER BY winRate DESC")
    LiveData<List<HeroEntity>> searchHeroes(String query);

    @Query("SELECT * FROM heroes WHERE id = :id")
    LiveData<HeroEntity> getHeroById(long id);

    @Query("DELETE FROM heroes")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM heroes")
    int getCount();
}
