package com.aram.mayhem.common;

/**
 * 全局常量定义
 *
 * 包含：后端 API 基础 URL、各模块 API 路径、网络超时配置、分页参数
 * 注意：BASE_URL 需根据部署环境修改
 */
public final class Constants {

    private Constants() {
    }

    public static final String BASE_URL = "http://192.168.1.100:8080/";

    public static final String API_AUTH_REGISTER = "api/auth/register";
    public static final String API_AUTH_LOGIN = "api/auth/login";
    public static final String API_AUTH_REFRESH = "api/auth/refresh";

    public static final String API_HEROES = "api/heroes";
    public static final String API_AUGMENTS = "api/augments";
    public static final String API_STRATEGIES = "api/strategies";
    public static final String API_BULLETINS = "api/bulletins";
    public static final String API_USERS = "api/users";

    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;

    public static final int SEARCH_DEBOUNCE_MS = 300;
    public static final int PAGE_SIZE = 20;
}
