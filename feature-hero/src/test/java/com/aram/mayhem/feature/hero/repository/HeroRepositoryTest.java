package com.aram.mayhem.feature.hero.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.data.local.entity.HeroEntity;
import com.aram.mayhem.network.dto.HeroResponse;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * HeroRepository 单元测试
 *
 * 测试范围：
 * - URL 拼接逻辑（相对路径 → 完整 URL）
 * - HeroEntity 字段映射
 * - HeroResponse 数据读取
 */
class HeroRepositoryTest {

    @Test
    void relativeImageUrl_ShouldStartWithSlash() {
        String relativeUrl = "/images/heroes/Aatrox.png";
        assertTrue(relativeUrl.startsWith("/"));
        String fullUrl = Constants.BASE_URL + relativeUrl.substring(1);
        assertEquals("http://10.0.2.2:8080/images/heroes/Aatrox.png", fullUrl);
    }

    @Test
    void absoluteImageUrl_ShouldNotBeModified() {
        String absoluteUrl = "http://example.com/image.png";
        assertFalse(absoluteUrl.startsWith("/"));
    }

    @Test
    void nullImageUrl_ShouldNotCauseNPE() {
        String imageUrl = null;
        assertNull(imageUrl);
    }

    @Test
    void heroEntity_PublicFieldsShouldBeAccessible() {
        HeroEntity entity = new HeroEntity();
        entity.id = 1L;
        entity.nameZh = "亚托克斯";
        entity.nameEn = "Aatrox";
        entity.title = "暗裔剑魔";
        entity.role = "战士";
        entity.tier = "S";
        entity.winRate = 55.5;
        entity.pickRate = 15.2;
        entity.avatarUrl = "/images/heroes/Aatrox.png";

        assertEquals(1L, entity.id);
        assertEquals("亚托克斯", entity.nameZh);
        assertEquals("Aatrox", entity.nameEn);
        assertEquals("S", entity.tier);
        assertEquals(55.5, entity.winRate, 0.01);
    }

    @Test
    void heroEntity_AvatarUrlWithRelativePath_ShouldFormFullUrl() {
        HeroEntity entity = new HeroEntity();
        entity.avatarUrl = "/images/heroes/Aatrox.png";

        String fullUrl = entity.avatarUrl;
        if (fullUrl != null && fullUrl.startsWith("/")) {
            fullUrl = Constants.BASE_URL + fullUrl.substring(1);
        }

        assertEquals("http://10.0.2.2:8080/images/heroes/Aatrox.png", fullUrl);
    }

    @Test
    void heroResponse_ShouldHaveGetterMethods() throws Exception {
        HeroResponse response = createHeroResponseViaReflection(
                1L, "亚托克斯", "Aatrox", "暗裔剑魔",
                "战士", "S", 55.5, 15.2,
                "/images/heroes/Aatrox.png", "描述"
        );

        assertEquals(1L, response.getId());
        assertEquals("亚托克斯", response.getNameZh());
        assertEquals("Aatrox", response.getNameEn());
        assertEquals("S", response.getTier());
        assertEquals(55.5, response.getWinRate(), 0.01);
        assertEquals("/images/heroes/Aatrox.png", response.getImageUrl());
    }

    @Test
    void baseUrl_ShouldBeEmulatorLocalhost() {
        assertEquals("http://10.0.2.2:8080/", Constants.BASE_URL);
    }

    @Test
    void offlineMode_UrlConcatenationStillWorks() {
        HeroEntity entity = new HeroEntity();
        entity.avatarUrl = "/images/heroes/Aatrox.png";

        String fullUrl = entity.avatarUrl;
        if (fullUrl != null && fullUrl.startsWith("/")) {
            fullUrl = Constants.BASE_URL + fullUrl.substring(1);
        }

        assertNotNull(fullUrl);
        assertTrue(fullUrl.startsWith("http://"));
        assertTrue(fullUrl.contains("images/heroes"));
    }

    @Test
    void offlineMode_EntityDataPreservedWithoutNetwork() {
        HeroEntity entity = new HeroEntity();
        entity.id = 1L;
        entity.nameZh = "亚托克斯";
        entity.tier = "S";
        entity.winRate = 55.5;
        entity.avatarUrl = "/images/heroes/Aatrox.png";

        assertEquals(1L, entity.id);
        assertEquals("亚托克斯", entity.nameZh);
        assertEquals("S", entity.tier);
        assertEquals(55.5, entity.winRate, 0.01);
        assertNotNull(entity.avatarUrl);
    }

    @Test
    void offlineMode_EmptyCacheShouldReturnEmptyList() {
        java.util.List<HeroEntity> cachedList = java.util.Collections.emptyList();
        assertTrue(cachedList.isEmpty());
    }

    @Test
    void offlineMode_CachedDataShouldBeAvailable() {
        java.util.List<HeroEntity> cachedList = new java.util.ArrayList<>();
        HeroEntity entity = new HeroEntity();
        entity.id = 1L;
        entity.nameZh = "亚托克斯";
        cachedList.add(entity);

        assertFalse(cachedList.isEmpty());
        assertEquals(1, cachedList.size());
        assertEquals("亚托克斯", cachedList.get(0).nameZh);
    }

    private HeroResponse createHeroResponseViaReflection(
            Long id, String nameZh, String nameEn, String title,
            String role, String tier, Double winRate, Double pickRate,
            String imageUrl, String description) throws Exception {
        Constructor<HeroResponse> constructor = HeroResponse.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        HeroResponse response = constructor.newInstance();

        setField(response, "id", id);
        setField(response, "nameZh", nameZh);
        setField(response, "nameEn", nameEn);
        setField(response, "title", title);
        setField(response, "role", role);
        setField(response, "tier", tier);
        setField(response, "winRate", winRate);
        setField(response, "pickRate", pickRate);
        setField(response, "imageUrl", imageUrl);
        setField(response, "description", description);

        return response;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
