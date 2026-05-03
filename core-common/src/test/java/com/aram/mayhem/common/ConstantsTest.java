package com.aram.mayhem.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    @DisplayName("BASE_URL 应以斜杠结尾")
    void baseUrl_endsWithSlash() {
        assertTrue(Constants.BASE_URL.endsWith("/"),
                "BASE_URL must end with '/' for Retrofit compatibility");
    }

    @Test
    @DisplayName("BASE_URL 应使用 http 协议")
    void baseUrl_usesHttpProtocol() {
        assertTrue(Constants.BASE_URL.startsWith("http://"),
                "BASE_URL must use http:// for local development");
    }

    @Test
    @DisplayName("API 路径不应以斜杠开头")
    void apiPaths_noLeadingSlash() {
        assertFalse(Constants.API_AUTH_REGISTER.startsWith("/"));
        assertFalse(Constants.API_AUTH_LOGIN.startsWith("/"));
        assertFalse(Constants.API_AUTH_REFRESH.startsWith("/"));
        assertFalse(Constants.API_HEROES.startsWith("/"));
        assertFalse(Constants.API_AUGMENTS.startsWith("/"));
        assertFalse(Constants.API_STRATEGIES.startsWith("/"));
        assertFalse(Constants.API_BULLETINS.startsWith("/"));
        assertFalse(Constants.API_USERS.startsWith("/"));
    }

    @Test
    @DisplayName("超时值应为正数")
    void timeoutValues_positive() {
        assertTrue(Constants.CONNECT_TIMEOUT > 0);
        assertTrue(Constants.READ_TIMEOUT > 0);
        assertTrue(Constants.WRITE_TIMEOUT > 0);
    }

    @Test
    @DisplayName("PAGE_SIZE 应为正数")
    void pageSize_positive() {
        assertTrue(Constants.PAGE_SIZE > 0);
    }

    @Test
    @DisplayName("SEARCH_DEBOUNCE_MS 应为正数")
    void searchDebounce_positive() {
        assertTrue(Constants.SEARCH_DEBOUNCE_MS > 0);
    }
}
