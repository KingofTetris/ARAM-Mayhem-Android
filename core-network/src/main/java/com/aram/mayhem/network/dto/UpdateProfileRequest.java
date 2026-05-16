package com.aram.mayhem.network.dto;

/**
 * 用户资料更新请求 DTO
 *
 * 数据流向：ProfileFragment → ProfileViewModel → ProfileRepository → ProfileApi
 * 对应后端：UpdateProfileRequest
 * 用途：部分更新用户资料（仅包含需要修改的字段）
 */
public class UpdateProfileRequest {

    /** 用户昵称（1-30位，null 表示不修改） */
    public String nickname;

    /** 用户头像 URL（null 表示不修改） */
    public String avatarUrl;

    /** 显示模式（0=浅色, 1=深色，null 表示不修改） */
    public Integer displayMode;

    /** 是否启用通知（1=启用, 0=关闭，null 表示不修改） */
    public Integer notificationEnabled;

    /**
     * 构造函数
     *
     * @param nickname             用户昵称
     * @param avatarUrl            头像 URL
     * @param displayMode          显示模式
     * @param notificationEnabled  通知开关
     */
    public UpdateProfileRequest(String nickname, String avatarUrl, Integer displayMode, Integer notificationEnabled) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.displayMode = displayMode;
        this.notificationEnabled = notificationEnabled;
    }
}
