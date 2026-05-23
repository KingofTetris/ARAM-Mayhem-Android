package com.aram.mayhem.data.local.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class LongListConverter {

    private static final Gson GSON = new Gson();

    @androidx.room.TypeConverter
    public static String fromLongList(List<Long> list) {
        if (list == null) {
            return null;
        }
        return GSON.toJson(list);
    }

    @androidx.room.TypeConverter
    public static List<Long> toLongList(String value) {
        if (value == null) {
            return null;
        }
        Type listType = new TypeToken<List<Long>>() {}.getType();
        return GSON.fromJson(value, listType);
    }
}