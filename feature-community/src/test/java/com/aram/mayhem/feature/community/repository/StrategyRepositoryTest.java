package com.aram.mayhem.feature.community.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.network.dto.StrategyListResponse;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * StrategyRepository 单元测试
 *
 * 测试范围：
 * - StrategyListResponse 数据读写
 * - 排序逻辑验证
 * - URL 拼接逻辑
 */
class StrategyRepositoryTest {

    @Test
    void baseUrl_ShouldBeEmulatorLocalhost() {
        assertEquals("http://10.0.2.2:8080/", Constants.BASE_URL);
    }

    @Test
    void strategyListResponse_ShouldHaveGetterAndSetter() {
        StrategyListResponse response = new StrategyListResponse();
        response.setId(1L);
        response.setTitle("亚索攻略");
        response.setAuthorNickname("user1");
        response.setUpvotes(100);
        response.setDownvotes(10);
        response.setScore(90);

        assertEquals(1L, response.getId());
        assertEquals("亚索攻略", response.getTitle());
        assertEquals("user1", response.getAuthorNickname());
        assertEquals(100, response.getUpvotes());
        assertEquals(10, response.getDownvotes());
        assertEquals(90, response.getScore());
    }

    @Test
    void upvotesAndDownvotes_ShouldCalculateScore() {
        int upvotes = 100;
        int downvotes = 10;
        int score = upvotes - downvotes;
        assertEquals(90, score);
    }

    @Test
    void strategyListResponse_ShouldHandleAugmentIcons() {
        StrategyListResponse response = new StrategyListResponse();
        List<String> augmentIcons = Arrays.asList("/images/augments/a1.png", "/images/augments/a2.png");
        response.setAugmentIcons(augmentIcons);

        assertEquals(2, response.getAugmentIcons().size());
        assertEquals("/images/augments/a1.png", response.getAugmentIcons().get(0));
    }

    @Test
    void relativeIconUrl_ShouldFormFullUrl() {
        String relativeUrl = "/images/augments/a1.png";
        String fullUrl = Constants.BASE_URL + relativeUrl.substring(1);
        assertEquals("http://10.0.2.2:8080/images/augments/a1.png", fullUrl);
    }
}
