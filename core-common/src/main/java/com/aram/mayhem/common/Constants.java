package com.aram.mayhem.common;

/**
 * 全局常量定义 —— 集中管理应用中使用的所有常量值
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 将应用中所有硬编码的常量值集中到一个类中管理，避免"魔法数字"散落在各处。
 * 好处：
 * 1. 修改常量只需改一处，不用到处搜索
 * 2. 常量名有语义，比直接写数字/字符串更容易理解
 * 3. 编译期检查，拼错常量名会报错
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么用 final class + private 构造函数？
 * ═══════════════════════════════════════════════════════════════════
 *
 * - final class：防止被继承，常量类不需要子类
 * - private 构造函数：防止被实例化，常量类只需要静态字段
 * 这是 Java 常量类的标准写法（如 java.util.Collections）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、BASE_URL 说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * 10.0.2.2 是 Android 模拟器访问宿主机（你的电脑）localhost 的特殊 IP。
 * - 模拟器中 10.0.2.2 = 电脑上的 127.0.0.1
 * - 真机调试时需要改为电脑的局域网 IP（如 192.168.1.100）
 * - 生产环境需要改为正式服务器地址
 */
public final class Constants {

    /**
     * 私有构造函数 —— 防止外部实例化此常量类
     *
     * 常量类只包含静态字段，不需要创建对象。
     * 如果有人尝试 new Constants()，会编译报错。
     */
    private Constants() {
    }

    // ====================== 网络请求基础配置 ======================

    /**
     * 网络请求基础根地址 —— 后端 Spring Boot 服务的地址
     *
     * 10.0.2.2 是 Android 模拟器的特殊 IP，映射到宿主机的 localhost。
     * 端口 8080 是 Spring Boot 的默认端口。
     *
     * 不同环境配置：
     * - 模拟器开发：http://10.0.2.2:8080/
     * - 真机调试：http://192.168.x.x:8080/（电脑局域网 IP）
     * - 生产环境：https://api.arammayhem.com/
     */
    public static final String BASE_URL = "http://10.0.2.2:8080/";

    // ====================== 认证相关接口地址 ======================

    /** 用户注册接口地址：POST /api/auth/register */
    public static final String API_AUTH_REGISTER = "api/auth/register";

    /** 用户登录接口地址：POST /api/auth/login */
    public static final String API_AUTH_LOGIN = "api/auth/login";

    /** Token 刷新续期接口地址：POST /api/auth/refresh */
    public static final String API_AUTH_REFRESH = "api/auth/refresh";

    // ====================== 业务模块接口地址 ======================

    /** 英雄相关接口根地址：GET /api/heroes */
    public static final String API_HEROES = "api/heroes";

    /** 强化符文相关接口根地址：GET /api/augments */
    public static final String API_AUGMENTS = "api/augments";

    /** 游戏攻略相关接口根地址：GET /api/strategies */
    public static final String API_STRATEGIES = "api/strategies";

    /** 公告/资讯相关接口根地址：GET /api/bulletins */
    public static final String API_BULLETINS = "api/bulletins";

    /** 用户信息相关接口根地址：GET /api/users */
    public static final String API_USERS = "api/users";

    // ====================== Retrofit 网络超时配置（单位：秒） ======================

    /**
     * 网络连接超时时间 —— 建立 TCP 连接的最大等待时间
     *
     * 30秒足够应对弱网环境，如果服务器在30秒内无法建立连接则报错。
     * 过短：弱网环境下频繁超时
     * 过长：用户等待太久才知道连接失败
     */
    public static final int CONNECT_TIMEOUT = 30;

    /**
     * 网络读取超时时间 —— 等待服务器响应的最大时间
     *
     * 30秒覆盖大多数 API 响应时间，包括数据同步等较慢的接口。
     */
    public static final int READ_TIMEOUT = 30;

    /**
     * 网络写入超时时间 —— 发送请求数据的最大时间
     *
     * 30秒覆盖上传攻略等需要发送较大请求体的场景。
     */
    public static final int WRITE_TIMEOUT = 30;

    // ====================== 业务通用配置常量 ======================

    /**
     * 搜索防抖延迟时间 —— 用户停止输入后等待多久才发起搜索请求
     *
     * 300ms 的选择原因：
     * - 用户快速输入时不会每个字都发请求（减少服务器压力）
     * - 300ms 的延迟用户几乎感知不到
     * - 配合 RxJava debounce 操作符使用
     */
    public static final int SEARCH_DEBOUNCE_MS = 300;

    /**
     * 列表分页默认每页数据条数
     *
     * 20条的选择原因：
     * - 手机屏幕一屏大约显示 5~7 个列表项
     * - 20条约 3~4 屏，滚动体验流畅
     * - 不会一次加载太多数据导致卡顿
     */
    public static final int PAGE_SIZE = 20;
}