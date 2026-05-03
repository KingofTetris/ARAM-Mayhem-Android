package com.aram.mayhem.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "augments", indices = {
        @Index(value = "quality"),
        @Index(value = "synergySet")
})
public class AugmentEntity {

    @PrimaryKey
    public long id;

    public String name;

    public String description;

    public String quality;

    public String synergySet;

    public String iconUrl;

    public boolean isTrap;

    public long updatedAt;
}
