package com.aram.mayhem.network.dto;

/**
 * 用户资料更新请求 DTO ── 发送给后端的部分更新参数
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当调用 ProfileApi.updateProfile() 更新用户资料时，
 * 需要将修改的字段封装为此类实例发送给后端。
 *
 * 数据流向：ProfileFragment → ProfileViewModel → ProfileRepository → ProfileApi
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、部分更新（PATCH）机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端使用 PATCH 语义：只更新非 null 的字段。
 *
 * 示例 1：只修改昵称
 *   new UpdateProfileRequest("新昵称", null, null, null)
 *   → 后端只更新 nickname，其他字段保持不变
 *
 * 示例 2：只关闭通知
 *   new UpdateProfileRequest(null, null, null, 0)
 *   → 后端只更新 notificationEnabled，其他字段保持不变
 *
 * 示例 3：同时修改昵称和头像
 *   new UpdateProfileRequest("新昵称", "https://xxx/avatar.png", null, null)
 *   → 后端更新 nickname 和 avatarUrl
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、为什么 displayMode 和 notificationEnabled 用 Integer 而非 int？
 * ═══════════════════════════════════════════════════════════════════
 *
 * int 是基本类型，默认值为 0，无法区分"不修改"和"修改为 0"。
 * Integer 是包装类型，可以为 null，表示"不修改该字段"。
 *
 * 如果用 int：
 * - displayMode 默认为 0，后端无法知道用户是想设为浅色模式还是不修改
 * - 用 Integer：null 表示不修改，0 表示设为浅色模式
 *
 * 关联类：
 * - UserProfileResponse：用户资料响应 DTO
 * - ProfileApi：用户资料 API 接口
 */
public class UpdateProfileRequest {

    /** 用户昵称（null 表示不修改，1-30位） */
    public String nickname;

    /** 用户头像 URL（null 表示不修改） */
    public String avatarUrl;

    /** 显示模式（null 表示不修改，0=浅色, 1=深色） */
    public Integer displayMode;

    /** 是否启用通知（null 表示不修改，1=启用, 0=关闭） */
    public Integer notificationEnabled;

    /**
     * 构造更新请求体
     *
     * 不需要修改的字段传 null，后端会跳过 null 字段的更新。
     *
     * @param nickname             用户昵称（null 表示不修改）
     * @param avatarUrl            头像 URL（null 表示不修改）
     * @param displayMode          显示模式（null 表示不修改）
     * @param notificationEnabled  通知开关（null 表示不修改）
     */
    public UpdateProfileRequest(String nickname, String avatarUrl, Integer displayMode, Integer notificationEnabled) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.displayMode = displayMode;
        this.notificationEnabled = notificationEnabled;
    }
}
