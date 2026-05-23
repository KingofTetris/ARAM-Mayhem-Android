package com.aram.mayhem.data.local.converter;

import com.aram.mayhem.data.local.entity.HeroEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class AugmentBriefListConverter {

    private static final Gson GSON = new Gson();

    @androidx.room.TypeConverter
    public static String fromAugmentBriefList(List<HeroEntity.AugmentBriefData> list) {
        if (list == null) {
            return null;
        }
        return GSON.toJson(list);
    }

    @androidx.room.TypeConverter
    public static List<HeroEntity.AugmentBriefData> toAugmentBriefList(String value) {
        if (value == null) {
            return null;
        }
        Type listType = new TypeToken<List<HeroEntity.AugmentBriefData>>() {}.getType();
        return GSON.fromJson(value, listType);
    }
}