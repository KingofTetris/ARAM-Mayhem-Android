package com.aram.mayhem.data.local.converter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StringListConverter {

    private static final Gson GSON = new Gson();

    @androidx.room.TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) {
            return null;
        }
        return GSON.toJson(list);
    }

    @androidx.room.TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null) {
            return null;
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return GSON.fromJson(value, listType);
    }
}