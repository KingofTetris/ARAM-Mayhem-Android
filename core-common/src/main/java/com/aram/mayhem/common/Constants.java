package com.aram.mayhem.common;

/**
 * 全局常量定义
 *
 * 包含：后端 API 基础 URL、各模块 API 路径、网络超时配置、分页参数、业务通用常量
 * 注意：BASE_URL 需根据部署环境修改（开发/测试/生产）
 */
public final class Constants {

    private Constants() {
    }

    // ====================== 网络请求基础配置 ======================

    /** 网络请求基础根地址（后端服务地址） */
    public static final String BASE_URL = "http://192.168.1.100:8080/";

    // ====================== 认证相关接口地址 ======================

    /** 用户注册接口地址 */
    public static final String API_AUTH_REGISTER = "api/auth/register";

    /** 用户登录接口地址 */
    public static final String API_AUTH_LOGIN = "api/auth/login";

    /** Token 刷新续期接口地址 */
    public static final String API_AUTH_REFRESH = "api/auth/refresh";

    // ====================== 业务模块接口地址 ======================

    /** 英雄相关接口根地址 */
    public static final String API_HEROES = "api/heroes";

    /** 强化符文相关接口根地址 */
    public static final String API_AUGMENTS = "api/augments";

    /** 游戏攻略相关接口根地址 */
    public static final String API_STRATEGIES = "api/strategies";

    /** 公告/资讯相关接口根地址 */
    public static final String API_BULLETINS = "api/bulletins";

    /** 用户信息相关接口根地址 */
    public static final String API_USERS = "api/users";

    // ====================== Retrofit 网络超时配置（单位：秒） ======================

    /** 网络连接超时时间，单位：秒 */
    public static final int CONNECT_TIMEOUT = 30;

    /** 网络读取超时时间，单位：秒 */
    public static final int READ_TIMEOUT = 30;

    /** 网络写入超时时间，单位：秒 */
    public static final int WRITE_TIMEOUT = 30;

    // ====================== 业务通用配置常量 ======================

    /** 搜索防抖延迟时间，单位：毫秒 */
    public static final int SEARCH_DEBOUNCE_MS = 300;

    /** 列表分页默认每页数据条数 */
    public static final int PAGE_SIZE = 20;
}