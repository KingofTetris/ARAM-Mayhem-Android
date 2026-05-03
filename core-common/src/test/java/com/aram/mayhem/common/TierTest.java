package com.aram.mayhem.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TierTest {

    @Test
    @DisplayName("S_PLUS 应有正确的 label 和 color")
    void sPlus_hasCorrectLabelAndColor() {
        assertEquals("S+", Tier.S_PLUS.getLabel());
        assertEquals(0xFFE53E3E, Tier.S_PLUS.getColor());
    }

    @Test
    @DisplayName("S 应有正确的 label 和 color")
    void s_hasCorrectLabelAndColor() {
        assertEquals("S", Tier.S.getLabel());
        assertEquals(0xFFFF6B35, Tier.S.getColor());
    }

    @Test
    @DisplayName("A 应有正确的 label 和 color")
    void a_hasCorrectLabelAndColor() {
        assertEquals("A", Tier.A.getLabel());
        assertEquals(0xFFFFB800, Tier.A.getColor());
    }

    @Test
    @DisplayName("B 应有正确的 label 和 color")
    void b_hasCorrectLabelAndColor() {
        assertEquals("B", Tier.B.getLabel());
        assertEquals(0xFF4CAF50, Tier.B.getColor());
    }

    @Test
    @DisplayName("C 应有正确的 label 和 color")
    void c_hasCorrectLabelAndColor() {
        assertEquals("C", Tier.C.getLabel());
        assertEquals(0xFF2196F3, Tier.C.getColor());
    }

    @Test
    @DisplayName("Tier 枚举应有 5 个值")
    void tier_hasFiveValues() {
        assertEquals(5, Tier.values().length);
    }

    @Test
    @DisplayName("Tier 颜色应各不相同")
    void tier_colorsAreDistinct() {
        int[] colors = new int[5];
        Tier[] tiers = Tier.values();
        for (int i = 0; i < tiers.length; i++) {
            colors[i] = tiers[i].getColor();
        }
        for (int i = 0; i < colors.length; i++) {
            for (int j = i + 1; j < colors.length; j++) {
                assertNotEquals(colors[i], colors[j],
                        "Tier " + tiers[i] + " and " + tiers[j] + " should have different colors");
            }
        }
    }
}
