package com.aram.mayhem.network.dto;

/**
 * 用户资料响应 DTO
 *
 * 数据流向：ProfileApi → ProfileRepository → ProfileViewModel → ProfileFragment
 * 对应后端：UserProfileVO
 * 用途：展示用户个人资料信息
 */
public class UserProfileResponse {

    /** 用户 ID */
    public long id;

    /** 登录邮箱 */
    public String email;

    /** 用户昵称 */
    public String nickname;

    /** 用户头像 URL */
    public String avatarUrl;

    /** 显示模式（0=浅色, 1=深色） */
    public int displayMode;

    /** 是否启用通知（1=启用, 0=关闭） */
    public int notificationEnabled;

    /** 用户角色（USER / ADMIN） */
    public String role;

    /** 发布的攻略数量 */
    public int strategyCount;

    /** 收藏数量 */
    public int favoriteCount;
}
