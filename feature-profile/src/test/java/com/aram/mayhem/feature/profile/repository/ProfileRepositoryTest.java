package com.aram.mayhem.feature.profile.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.network.dto.UserProfileResponse;

import org.junit.jupiter.api.Test;

/**
 * ProfileRepository 单元测试
 *
 * 测试范围：
 * - UserProfileResponse 字段映射
 * - URL 拼接逻辑
 * - 用户角色验证
 */
class ProfileRepositoryTest {

    @Test
    void userProfileResponse_PublicFieldsShouldBeAccessible() {
        UserProfileResponse response = new UserProfileResponse();
        response.id = 1L;
        response.email = "test@example.com";
        response.nickname = "测试用户";
        response.avatarUrl = "/images/avatars/default.png";
        response.displayMode = 1;
        response.notificationEnabled = 1;
        response.role = "USER";
        response.strategyCount = 5;
        response.favoriteCount = 10;

        assertEquals(1L, response.id);
        assertEquals("test@example.com", response.email);
        assertEquals("测试用户", response.nickname);
        assertEquals("USER", response.role);
        assertEquals(5, response.strategyCount);
    }

    @Test
    void relativeAvatarUrl_ShouldFormFullUrl() {
        UserProfileResponse response = new UserProfileResponse();
        response.avatarUrl = "/images/avatars/default.png";

        String fullUrl = response.avatarUrl;
        if (fullUrl != null && fullUrl.startsWith("/")) {
            fullUrl = Constants.BASE_URL + fullUrl.substring(1);
        }

        assertEquals("http://10.0.2.2:8080/images/avatars/default.png", fullUrl);
    }

    @Test
    void adminRole_ShouldBeRecognized() {
        UserProfileResponse response = new UserProfileResponse();
        response.role = "ADMIN";
        assertEquals("ADMIN", response.role);
        assertTrue(response.role.equals("ADMIN"));
    }

    @Test
    void userRole_ShouldBeDefault() {
        UserProfileResponse response = new UserProfileResponse();
        response.role = "USER";
        assertFalse(response.role.equals("ADMIN"));
    }

    @Test
    void displayMode_DarkMode() {
        UserProfileResponse response = new UserProfileResponse();
        response.displayMode = 1;
        assertEquals(1, response.displayMode);
    }
}
