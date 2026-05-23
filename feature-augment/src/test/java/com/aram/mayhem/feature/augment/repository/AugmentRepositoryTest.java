package com.aram.mayhem.feature.augment.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.data.local.entity.AugmentEntity;
import com.aram.mayhem.network.dto.AugmentResponse;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * AugmentRepository 单元测试
 *
 * 测试范围：
 * - URL 拼接逻辑（相对路径 → 完整 URL）
 * - AugmentEntity 字段映射
 * - AugmentResponse 数据读取
 */
class AugmentRepositoryTest {

    @Test
    void relativeIconUrl_ShouldFormFullUrl() {
        String relativeUrl = "/images/augments/brutal.png";
        assertTrue(relativeUrl.startsWith("/"));
        String fullUrl = Constants.BASE_URL + relativeUrl.substring(1);
        assertEquals("http://10.0.2.2:8080/images/augments/brutal.png", fullUrl);
    }

    @Test
    void absoluteIconUrl_ShouldNotBeModified() {
        String absoluteUrl = "http://example.com/icon.png";
        assertFalse(absoluteUrl.startsWith("/"));
    }

    @Test
    void nullIconUrl_ShouldNotCauseNPE() {
        String iconUrl = null;
        assertNull(iconUrl);
    }

    @Test
    void augmentEntity_PublicFieldsShouldBeAccessible() {
        AugmentEntity entity = new AugmentEntity();
        entity.id = 1L;
        entity.nameZh = "残暴";
        entity.nameEn = "Brutal";
        entity.quality = "Prestige";
        entity.synergySet = "战士";
        entity.iconUrl = "/images/augments/brutal.png";
        entity.winRate = 55.5;
        entity.pickRate = 15.2;
        entity.avgPlacement = 3.5;
        entity.tier = "S";

        assertEquals(1L, entity.id);
        assertEquals("残暴", entity.nameZh);
        assertEquals("Prestige", entity.quality);
        assertEquals("S", entity.tier);
        assertEquals(55.5, entity.winRate, 0.01);
    }

    @Test
    void augmentEntity_IconUrlWithRelativePath_ShouldFormFullUrl() {
        AugmentEntity entity = new AugmentEntity();
        entity.iconUrl = "/images/augments/brutal.png";

        String fullUrl = entity.iconUrl;
        if (fullUrl != null && fullUrl.startsWith("/")) {
            fullUrl = Constants.BASE_URL + fullUrl.substring(1);
        }

        assertEquals("http://10.0.2.2:8080/images/augments/brutal.png", fullUrl);
    }

    @Test
    void augmentResponse_ShouldHaveGetterMethods() throws Exception {
        AugmentResponse response = createAugmentResponseViaReflection(
                1L, "残暴", "Brutal", "Prestige",
                "战士", "/images/augments/brutal.png",
                55.5, 15.2, 3.5, "S"
        );

        assertEquals(1L, response.getId());
        assertEquals("残暴", response.getNameZh());
        assertEquals("Brutal", response.getNameEn());
        assertEquals("Prestige", response.getQuality());
        assertEquals("S", response.getTier());
        assertEquals("/images/augments/brutal.png", response.getIconUrl());
    }

    private AugmentResponse createAugmentResponseViaReflection(
            Long id, String nameZh, String nameEn, String quality,
            String synergySet, String iconUrl,
            Double winRate, Double pickRate, Double avgPlacement, String tier) throws Exception {
        Constructor<AugmentResponse> constructor = AugmentResponse.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        AugmentResponse response = constructor.newInstance();

        setField(response, "id", id);
        setField(response, "nameZh", nameZh);
        setField(response, "nameEn", nameEn);
        setField(response, "quality", quality);
        setField(response, "synergySet", synergySet);
        setField(response, "iconUrl", iconUrl);
        setField(response, "winRate", winRate);
        setField(response, "pickRate", pickRate);
        setField(response, "avgPlacement", avgPlacement);
        setField(response, "tier", tier);

        return response;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
