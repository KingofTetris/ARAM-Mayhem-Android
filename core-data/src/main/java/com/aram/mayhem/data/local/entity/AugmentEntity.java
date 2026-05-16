package com.aram.mayhem.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 强化符文本地缓存实体
 *
 * 对应表：augments
 * 索引：quality, synergySet
 * 数据流向：Retrofit → AugmentRepository → Room → AugmentViewModel → UI
 * 关联：AugmentDao
 */
@Entity(tableName = "augments", indices = {
        @Index(value = "quality"),
        @Index(value = "synergySet")
})
public class AugmentEntity {

    @PrimaryKey
    public long id;

    public String nameZh;

    public String nameEn;

    public String description;

    public String quality;

    public String synergySet;

    public String synergySet2;

    public String synergySet3;

    public String iconUrl;

    public double winRate;

    public double pickRate;

    public double avgPlacement;

    public String tier;

    public boolean isTrap;

    public long updatedAt;
}