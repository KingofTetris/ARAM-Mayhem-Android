package com.aram.mayhem.data.local.converter;

import com.aram.mayhem.data.local.entity.HeroEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class SkillListConverter {

    private static final Gson gson = new Gson();

    @androidx.room.TypeConverter
    public String fromSkillList(List<HeroEntity.SkillData> skills) {
        if (skills == null) return null;
        return gson.toJson(skills);
    }

    @androidx.room.TypeConverter
    public List<HeroEntity.SkillData> toSkillList(String data) {
        if (data == null) return Collections.emptyList();
        Type type = new TypeToken<List<HeroEntity.SkillData>>() {}.getType();
        return gson.fromJson(data, type);
    }

    @androidx.room.TypeConverter
    public String fromStringList(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    @androidx.room.TypeConverter
    public List<String> toStringList(String data) {
        if (data == null) return Collections.emptyList();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(data, type);
    }
}
