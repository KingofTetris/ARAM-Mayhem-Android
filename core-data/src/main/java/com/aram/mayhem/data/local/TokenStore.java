package com.aram.mayhem.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * JWT 令牌安全存储 —— 使用加密方式存储用户的登录凭证
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当用户登录成功后，后端返回两个令牌（Token）：
 * 1. Access Token（访问令牌）：用于每次 API 请求的身份验证，有效期短（如 2 小时）
 * 2. Refresh Token（刷新令牌）：用于在 Access Token 过期后获取新的 Access Token，有效期长（如 7 天）
 *
 * TokenStore 负责安全地存储这两个令牌，以及令牌的过期时间。
 * 后续所有需要身份验证的 API 请求，都从这里读取 Access Token 附加到请求头。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么需要加密存储？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 普通 SharedPreferences 以明文 XML 存储在设备上：
 * /data/data/com.aram.mayhem/shared_prefs/auth_prefs.xml
 *
 * 如果手机被 root，任何应用都能读取这个文件，获取用户的 Token。
 * 攻击者拿到 Token 后，可以冒充用户身份调用 API，造成严重安全问题。
 *
 * EncryptedSharedPreferences 使用 Android Jetpack Security 库：
 * - 密钥（Key）使用 AES256-GCM 加密
 * - 值（Value）使用 AES256-GCM 加密
 * - 主密钥存储在 Android Keystore 中（硬件级安全）
 * - 即使文件被读取，也无法解密获取原始 Token
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、加密方案详解
 * ═══════════════════════════════════════════════════════════════════
 *
 * MasterKey（主密钥）：
 * - 使用 AES256-GCM 算法生成
 * - 存储在 Android Keystore 系统中
 * - Keystore 是硬件级安全模块，密钥无法被导出
 * - 即使应用被反编译，也无法获取主密钥
 *
 * PrefKeyEncryptionScheme.AES256_SIV：
 * - SharedPreferences 的键（Key）加密方案
 * - AES256-SIV 是一种确定性加密算法
 * - 确定性 = 相同的键名加密后结果相同，用于查找
 * - 256 位密钥长度，安全性极高
 *
 * PrefValueEncryptionScheme.AES256_GCM：
 * - SharedPreferences 的值（Value）加密方案
 * - AES256-GCM 是一种认证加密算法
 * - GCM 模式提供加密 + 完整性校验
 * - 防止密文被篡改（如果密文被修改，解密时会检测到）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、存储的数据项
 * ═══════════════════════════════════════════════════════════════════
 *
 * | 键名               | 类型   | 说明                                    |
 * |-------------------|--------|----------------------------------------|
 * | access_token      | String | 访问令牌，每次 API 请求携带              |
 * | refresh_token     | String | 刷新令牌，用于获取新的 access_token      |
 * | token_expires_at  | long   | 访问令牌过期时间（毫秒级时间戳）          |
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 登录流程：
 * LoginActivity → AuthApi.login() → 后端返回 Token
 *     → TokenStore.saveTokens() → EncryptedSharedPreferences 加密存储
 *
 * 请求认证：
 * AuthInterceptor → TokenStore.getAccessToken() → 附加到请求头 Authorization: Bearer xxx
 *
 * Token 刷新：
 * TokenRefreshInterceptor → TokenStore.isTokenExpired() → 检测过期
 *     → AuthApi.refreshToken() → 后端返回新 Token
 *     → TokenStore.saveTokens() → 更新存储
 *
 * 退出登录：
 * ProfileFragment → TokenStore.clear() → 清除所有 Token
 *     → 跳转到 LoginActivity
 *
 * 关联类：
 * - AuthInterceptor：网络请求拦截器，自动附加 Access Token
 * - TokenRefreshInterceptor：Token 过期自动刷新拦截器
 * - AuthApi：认证相关的 Retrofit API 接口
 */
@Singleton
public class TokenStore {

    /**
     * 加密存储文件名 —— SharedPreferences XML 文件名
     *
     * 实际文件路径：/data/data/com.aram.mayhem/shared_prefs/auth_prefs.xml
     * 但由于使用了 EncryptedSharedPreferences，文件内容是加密的，无法直接读取。
     */
    private static final String FILE_NAME = "auth_prefs";

    /**
     * Access Token 的存储键名
     *
     * 格式示例：eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.xxx
     * 这是 JWT（JSON Web Token）格式，由三段 Base64 编码的字符串用 . 连接：
     * 1. Header（头部）：算法信息
     * 2. Payload（载荷）：用户 ID 等信息
     * 3. Signature（签名）：防篡改签名
     */
    private static final String KEY_ACCESS_TOKEN = "access_token";

    /**
     * Refresh Token 的存储键名
     *
     * 与 Access Token 格式相同，但用途不同：
     * - Access Token：短期有效，用于 API 请求认证
     * - Refresh Token：长期有效，仅用于获取新的 Access Token
     */
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    /**
     * Token 过期时间的存储键名
     *
     * 存储的是绝对过期时间（毫秒级 Unix 时间戳），不是相对有效期。
     * 例如：1715000000000 表示 2024-05-06 某个时刻过期。
     *
     * 使用绝对时间而非相对时间的原因：
     * - 避免设备时钟调整导致计算错误
     * - 直接与 System.currentTimeMillis() 比较，逻辑更简单
     */
    private static final String KEY_TOKEN_EXPIRES_AT = "token_expires_at";

    /**
     * 加密的 SharedPreferences 实例
     *
     * 与普通 SharedPreferences 的区别：
     * - 普通 SP：数据明文存储，任何人都能读取
     * - 加密 SP：数据加密存储，只有本应用能解密
     *
     * 使用方式与普通 SP 完全相同：
     * prefs.getString("key", null)
     * prefs.edit().putString("key", "value").apply()
     * 区别仅在于底层自动加解密。
     */
    private final SharedPreferences prefs;

    /**
     * 构造函数 —— 初始化加密的 SharedPreferences
     *
     * 由 Hilt 自动注入，@Singleton 保证全局只有一个实例。
     *
     * 初始化流程：
     * 1. 创建 MasterKey：使用 AES256-GCM 算法，存储在 Android Keystore
     * 2. 创建 EncryptedSharedPreferences：用 MasterKey 加密键和值
     * 3. 如果创建失败（如设备不支持），抛出 RuntimeException
     *
     * 可能的失败原因：
     * - 设备太旧，不支持 Android Keystore
     * - Keystore 被锁定或损坏
     * - 加密库初始化异常
     *
     * @param context 应用上下文，由 Hilt @ApplicationContext 注入
     */
    @Inject
    public TokenStore(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create EncryptedSharedPreferences", e);
        }
    }

    /**
     * 保存登录令牌 —— 登录成功或 Token 刷新成功后调用
     *
     * 同时保存三个值：
     * 1. accessToken：访问令牌，用于 API 请求认证
     * 2. refreshToken：刷新令牌，用于获取新的 accessToken
     * 3. tokenExpiresAt：accessToken 的过期时间（当前时间 + expiresInMs）
     *
     * 使用 apply() 而非 commit() 的原因：
     * - apply()：异步写入磁盘，不阻塞调用线程，适合 UI 场景
     * - commit()：同步写入磁盘，阻塞调用线程直到写入完成
     * 对于 Token 保存，异步即可，不需要等待磁盘写入完成。
     *
     * @param accessToken  访问令牌（JWT 格式字符串）
     * @param refreshToken 刷新令牌（JWT 格式字符串）
     * @param expiresInMs  访问令牌的有效期（毫秒），如 7200000 表示 2 小时
     */
    public void saveTokens(String accessToken, String refreshToken, long expiresInMs) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_TOKEN_EXPIRES_AT, System.currentTimeMillis() + expiresInMs)
                .apply();
    }

    /**
     * 获取访问令牌 —— AuthInterceptor 在每次 API 请求时调用
     *
     * 返回值直接附加到 HTTP 请求头：
     * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     *
     * @return Access Token 字符串，未登录时返回 null
     */
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * 获取刷新令牌 —— TokenRefreshInterceptor 在 Token 过期时调用
     *
     * 用于调用后端 /api/auth/refresh 接口获取新的 Access Token。
     *
     * @return Refresh Token 字符串，未登录时返回 null
     */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * 获取令牌过期时间 —— 用于判断 Access Token 是否过期
     *
     * 返回的是绝对时间戳（毫秒），不是相对有效期。
     * 例如返回 1715000000000，表示 2024-05-06 某个时刻过期。
     *
     * @return 过期时间的毫秒级时间戳，未登录时返回 0
     */
    public long getTokenExpiresAt() {
        return prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0);
    }

    /**
     * 判断 Access Token 是否已过期
     *
     * 判断逻辑：当前时间 >= 过期时间 → 已过期
     *
     * 使用场景：
     * - TokenRefreshInterceptor 在每次请求前检查
     * - 如果过期，先调用 refreshToken() 获取新 Token
     * - 如果未过期，直接使用当前 Token
     *
     * 预留缓冲时间的建议：
     * 实际项目中建议提前 30 秒~1 分钟判定过期，
     * 避免请求发出时 Token 刚好过期的边界情况。
     * 当前实现未预留缓冲，后续可优化为：
     * return System.currentTimeMillis() >= getTokenExpiresAt() - 30_000;
     *
     * @return true=已过期（需要刷新），false=未过期（可继续使用）
     */
    public boolean isTokenExpired() {
        return System.currentTimeMillis() >= getTokenExpiresAt();
    }

    /**
     * 判断用户是否已登录 —— 检查是否存在 Access Token
     *
     * 使用场景：
     * - 应用启动时判断是否需要显示登录页
     * - 社区发帖/投票前判断是否需要跳转登录
     *
     * 注意：此方法只检查 Token 是否存在，不检查是否过期。
     * 即使 Token 过期，hasToken() 仍返回 true。
     * 需要同时检查 isTokenExpired() 才能确定 Token 是否真正可用。
     *
     * @return true=已登录（Token 存在），false=未登录（Token 不存在）
     */
    public boolean hasToken() {
        return getAccessToken() != null;
    }

    /**
     * 清除所有令牌 —— 用户退出登录时调用
     *
     * 清除后：
     * - getAccessToken() 返回 null
     * - getRefreshToken() 返回 null
     * - getTokenExpiresAt() 返回 0
     * - hasToken() 返回 false
     *
     * 调用场景：
     * 1. 用户主动退出登录
     * 2. Token 刷新失败（Refresh Token 也过期）
     * 3. 后端返回 401（Token 无效或被撤销）
     *
     * 清除后需要跳转到登录页面，让用户重新登录。
     */
    public void clear() {
        prefs.edit().clear().apply();
    }
}
