package com.aram.mayhem.network.dto;

/**
 * 用户资料响应 DTO ── 对应后端 UserProfileVO，用于个人中心展示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 ProfileApi.getUserProfile() 获取用户资料时，
 * 响应中的 data 字段会被 Gson 反序列化为这个类的实例。
 *
 * 数据流向：ProfileApi → ProfileRepository → ProfileViewModel → ProfileFragment
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、字段分类
 * ═══════════════════════════════════════════════════════════════════
 *
 * 基础信息：id, email, nickname, avatarUrl
 * 偏好设置：displayMode, notificationEnabled
 * 权限信息：role
 * 统计数据：strategyCount, favoriteCount
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、为什么用 public 字段而非 private + getter？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本类使用 public 字段直接访问，原因：
 * - DTO 类是纯数据容器，没有业务逻辑
 * - 减少样板代码（不需要 getter/setter）
 * - Gson 可以直接访问 public 字段，反序列化更快
 * - 与其他 DTO（如 UpdateProfileRequest）保持一致风格
 *
 * 关联类：
 * - UpdateProfileRequest：更新资料请求 DTO
 * - ProfileApi：用户资料 API 接口
 */
public class UserProfileResponse {

    /** 用户 ID ── 数据库主键 */
    public long id;

    /** 登录邮箱 ── 作为登录账号使用 */
    public String email;

    /** 用户昵称 ── 显示在社区攻略的作者名等位置 */
    public String nickname;

    /** 用户头像 URL ── 显示在个人中心和攻略作者信息中 */
    public String avatarUrl;

    /**
     * 显示模式 ── 控制应用的主题
     * 0 = 浅色模式（Light）
     * 1 = 深色模式（Dark）
     */
    public int displayMode;

    /**
     * 是否启用通知 ── 控制推送通知
     * 1 = 启用通知
     * 0 = 关闭通知
     */
    public int notificationEnabled;

    /**
     * 用户角色 ── 控制权限
     * USER：普通用户
     * ADMIN：管理员（可以管理公告、删除违规攻略等）
     */
    public String role;

    /** 发布的攻略数量 ── 统计数据，显示在个人中心 */
    public int strategyCount;

    /** 收藏数量 ── 统计数据，显示在个人中心 */
    public int favoriteCount;
}
