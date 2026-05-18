package com.aram.mayhem.feature.hero.viewmodel;

import com.aram.mayhem.common.Tier;
import com.aram.mayhem.ui.model.HeroDetailUiModel;
import com.aram.mayhem.ui.model.HeroDetailUiModel.SkillUiModel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeroDetailUiModelVersionTrapTest {

    @Test
    @DisplayName("HeroDetailUiModel versionTrap=true 时 isVersionTrap 返回 true")
    void heroDetailUiModel_versionTrapTrue() {
        HeroDetailUiModel model = createTestDetail(true);

        assertTrue(model.isVersionTrap());
    }

    @Test
    @DisplayName("HeroDetailUiModel versionTrap=false 时 isVersionTrap 返回 false")
    void heroDetailUiModel_versionTrapFalse() {
        HeroDetailUiModel model = createTestDetail(false);

        assertFalse(model.isVersionTrap());
    }

    @Test
    @DisplayName("HeroDetailUiModel 兼容旧构造函数 versionTrap 默认为 false")
    void heroDetailUiModel_legacyConstructor_versionTrapDefaultFalse() {
        HeroDetailUiModel model = new HeroDetailUiModel(
                1L, "亚索", "Yasuo", "疾风剑豪", "Fighter",
                Tier.S, 52.0, 8.5, "描述",
                List.of(), List.of(), List.of(),
                7.0, 5.5, 9.0,
                "推荐出装", List.of(), List.of(), "/images/yasuo.png", false
        );

        assertFalse(model.isVersionTrap());
    }

    @Test
    @DisplayName("HeroDetailUiModel 新构造函数 versionTrap=true 正确传递")
    void heroDetailUiModel_newConstructor_versionTrapTrue() {
        HeroDetailUiModel model = new HeroDetailUiModel(
                1L, "亚索", "Yasuo", "疾风剑豪", "Fighter",
                Tier.S, 52.0, 8.5, "描述",
                List.of(), List.of(), List.of(),
                7.0, 5.5, 9.0,
                "推荐出装", List.of(), List.of(), "/images/yasuo.png", true
        );

        assertTrue(model.isVersionTrap());
    }

    private HeroDetailUiModel createTestDetail(boolean versionTrap) {
        return new HeroDetailUiModel(
                1L, "亚索", "Yasuo", "疾风剑豪", "Fighter",
                Tier.S, 52.0, 8.5, "描述",
                List.of(), List.of(), List.of(),
                7.0, 5.5, 9.0,
                "推荐出装", List.of(), List.of(), "/images/yasuo.png", versionTrap
        );
    }
}
