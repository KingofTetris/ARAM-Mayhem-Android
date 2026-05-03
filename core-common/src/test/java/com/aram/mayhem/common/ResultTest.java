package com.aram.mayhem.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    @DisplayName("success() 应返回 code=200 的成功结果")
    void success_returnsCode200() {
        Result<String> result = Result.success("test data");
        assertTrue(result.isSuccess());
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("test data", result.getData());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    @DisplayName("error() 应返回错误结果")
    void error_returnsErrorResult() {
        Result<Object> result = Result.error(401, "Unauthorized");
        assertFalse(result.isSuccess());
        assertEquals(401, result.getCode());
        assertEquals("Unauthorized", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("isSuccess() 仅在 code=200 时返回 true")
    void isSuccess_onlyTrueFor200() {
        assertTrue(Result.success(null).isSuccess());
        assertFalse(Result.error(400, "Bad Request").isSuccess());
        assertFalse(Result.error(500, "Server Error").isSuccess());
    }

    @Test
    @DisplayName("data 为 null 时 success 仍返回 true")
    void success_withNullData_stillSuccess() {
        Result<Object> result = Result.success(null);
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }
}
