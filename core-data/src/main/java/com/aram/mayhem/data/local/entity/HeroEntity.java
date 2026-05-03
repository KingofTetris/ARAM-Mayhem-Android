package com.aram.mayhem.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "heroes", indices = {
        @Index(value = "tier"),
        @Index(value = "role"),
        @Index(value = "nameZh")
})
public class HeroEntity {

    @PrimaryKey
    public long id;

    public String nameZh;

    public String nameEn;

    public String role;

    public String tier;

    public double winRate;

    public double pickRate;

    public String avatarUrl;

    public boolean isTrap;

    public long updatedAt;
}
