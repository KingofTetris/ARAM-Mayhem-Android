package com.aram.mayhem.common;

import com.google.gson.annotations.SerializedName;

/**
 * 统一 API 响应模型 —— 前后端数据交互的"信封"
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 想象你寄快递：快递盒（Result）里面装着物品（data），盒子上贴着标签：
 * - code：快递状态码（200=签收成功，其他=异常）
 * - message：快递备注（"success" 或 "收件人不在"）
 * - data：快递里的物品（你真正要的东西）
 * - timestamp：寄出时间（什么时候发出的）
 *
 * 在本项目中，后端 Spring Boot 的每个 API 接口返回值都遵循这个格式：
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... 实际业务数据 ... },
 *   "timestamp": 1715000000000
 * }
 *
 * Android 端用 Retrofit + Gson 把这个 JSON 自动解析成 Result<T> 对象。
 * T 是泛型参数，代表 data 字段的具体类型，比如：
 * - Result<HeroDetailVO>  → data 是英雄详情
 * - Result<List<HeroUiModel>>  → data 是英雄列表
 * - Result<PageResult<BulletinListVO>>  → data 是分页公告列表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、为什么需要统一响应格式？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 如果每个接口返回格式都不一样，前端解析会很混乱：
 * - /api/heroes 返回 [{...}, {...}]
 * - /api/bulletins 返回 {"list": [...], "total": 10}
 * - /api/error 返回 {"error": "not found"}
 *
 * 统一格式后，前端只需要一套解析逻辑：
 * 1. 先检查 result.isSuccess() 判断成功/失败
 * 2. 成功则取 result.getData() 获取业务数据
 * 3. 失败则取 result.getMessage() 显示错误提示
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、状态码约定
 * ═══════════════════════════════════════════════════════════════════
 *
 * code 字段不是 HTTP 状态码，而是业务状态码（由后端自定义）：
 *
 * | code | 含义           | 说明                         |
 * |------|---------------|------------------------------|
 * | 200  | 成功           | 请求处理成功，data 有值       |
 * | 400  | 参数错误       | 请求参数校验失败              |
 * | 401  | 未认证         | Token 缺失或过期，需重新登录  |
 * | 403  | 无权限         | Token 有效但权限不足          |
 * | 404  | 资源不存在     | 请求的英雄/攻略等不存在       |
 * | 500  | 服务器内部错误 | 后端代码异常                  |
 *
 * 注意：HTTP 状态码和业务状态码是两套体系：
 * - HTTP 状态码：网络层，由 Spring Boot 框架自动设置（如 200 OK, 404 Not Found）
 * - 业务状态码：应用层，由后端代码手动设置（如 code=200 表示业务成功）
 * 本类的 code 字段是业务状态码。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端 Controller 返回 Result<T>
 *     → Spring Boot 序列化为 JSON
 *     → HTTP 响应传输
 *     → Retrofit 接收 JSON
 *     → Gson 反序列化为 Result<T>
 *     → Repository/ViewModel 调用 isSuccess() 判断
 *     → 成功则提取 data，失败则提示 message
 *
 * 关联类：
 * - 后端 com.aram.mayhem.dto.Result：后端版本，字段完全对应
 * - PageResult：分页数据包装类，常作为 Result<T> 的 T 使用
 * - AuthInterceptor：处理 401 状态码，自动跳转登录页
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、Gson 反序列化说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @SerializedName 注解告诉 Gson：JSON 中的字段名和 Java 变量名的映射关系。
 * 比如 JSON 中是 "code"，Java 变量名也是 code，所以 @SerializedName("code") 可以省略。
 * 但显式写出来更清晰，防止 Gson 混淆策略（如 LOWER_CASE_WITH_UNDERSCORES）导致映射错误。
 *
 * 空构造函数 Result() 是 Gson 反序列化必需的：
 * Gson 通过反射创建对象时，需要无参构造函数。
 * 如果没有无参构造函数，Gson 会报错：Missing no-args constructor
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、使用示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * // 在 Repository 中处理 API 响应
 * Result<HeroDetailVO> result = heroApi.getHeroDetail(heroId);
 * if (result.isSuccess()) {
 *     HeroDetailVO hero = result.getData();  // 获取英雄详情数据
 *     // 保存到数据库、更新 UI 等
 * } else {
 *     String errorMsg = result.getMessage();  // 获取错误消息
 *     // 显示错误提示给用户
 * }
 *
 * // 在 ViewModel 中观察 LiveData
 * heroDetail.observe(this, result -> {
 *     if (result.isSuccess()) {
 *         showHeroDetail(result.getData());
 *     } else {
 *         showError(result.getMessage());
 *     }
 * });
 *
 * @param <T> 响应数据类型 —— data 字段的具体类型，由调用方指定
 */
public class Result<T> {

    /**
     * 业务状态码 —— 表示后端处理请求的结果状态
     *
     * 这不是 HTTP 状态码，而是后端自定义的业务状态码。
     * 200 表示成功，其他值表示各种错误情况。
     *
     * 示例 JSON：
     * { "code": 200, "message": "success", "data": {...} }
     * { "code": 404, "message": "英雄不存在", "data": null }
     *
     * @SerializedName("code") 告诉 Gson：JSON 中的 "code" 字段映射到这个 Java 变量
     */
    @SerializedName("code")
    private int code;

    /**
     * 响应消息 —— 对状态码的文字说明
     *
     * 成功时通常为 "success"，失败时为具体的错误描述。
     * 这个消息可以直接显示给用户，也可以用于日志记录。
     *
     * 示例：
     * - 成功："success"
     * - 参数错误："英雄名称不能为空"
     * - 未认证："Token 已过期，请重新登录"
     * - 资源不存在："该英雄不存在"
     *
     * @SerializedName("message") 告诉 Gson：JSON 中的 "message" 字段映射到这个 Java 变量
     */
    @SerializedName("message")
    private String message;

    /**
     * 响应数据 —— 请求成功时携带的业务数据
     *
     * 这是整个响应中最核心的部分，包含前端真正需要的数据。
     * T 是泛型参数，在编译时确定具体类型：
     * - Result<HeroDetailVO> 中，data 的类型是 HeroDetailVO
     * - Result<List<HeroUiModel>> 中，data 的类型是 List<HeroUiModel>
     *
     * 当 isSuccess() 返回 false 时，data 通常为 null。
     * 使用前务必先调用 isSuccess() 检查，避免 NullPointerException。
     *
     * @SerializedName("data") 告诉 Gson：JSON 中的 "data" 字段映射到这个 Java 变量
     */
    @SerializedName("data")
    private T data;

    /**
     * 响应时间戳 —— 后端生成响应的时间（毫秒级 Unix 时间戳）
     *
     * 用于：
     * 1. 判断响应是否过期（如缓存策略中对比时间戳）
     * 2. 调试时追踪请求-响应时序
     * 3. 日志记录和问题排查
     *
     * 示例值：1715000000000（对应 2024-05-06 某个时刻）
     *
     * @SerializedName("timestamp") 告诉 Gson：JSON 中的 "timestamp" 字段映射到这个 Java 变量
     */
    @SerializedName("timestamp")
    private long timestamp;

    /**
     * 空构造函数 —— Gson 反序列化必需
     *
     * Gson 在将 JSON 字符串转换为 Java 对象时，通过反射机制：
     * 1. 调用无参构造函数创建对象实例
     * 2. 通过反射设置各个字段的值
     *
     * 如果没有无参构造函数，Gson 会抛出异常：
     * "Unsafe allocation is not supported" 或 "Missing no-args constructor"
     *
     * 这个构造函数通常不需要手动调用，仅由 Gson 内部使用。
     */
    public Result() {
    }

    /**
     * 全参数构造函数 —— 手动创建 Result 对象时使用
     *
     * 通常在以下场景使用：
     * 1. 单元测试中构造模拟响应数据
     * 2. 本地缓存数据转换为 Result 格式
     * 3. 错误处理中构造失败响应
     *
     * timestamp 在构造时自动设置为当前系统时间，无需手动传入。
     *
     * @param code    业务状态码（200=成功，其他=失败）
     * @param message 响应消息（成功时为 "success"，失败时为错误描述）
     * @param data    响应数据（成功时为业务数据，失败时为 null）
     */
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 静态工厂方法：创建成功响应
     *
     * 这是最常用的创建成功 Result 的方式，比 new Result<>(200, "success", data) 更简洁。
     * 自动设置 code=200、message="success"，只需传入业务数据。
     *
     * 使用示例：
     * Result<HeroDetailVO> result = Result.success(heroDetail);
     * Result<List<HeroUiModel>> result = Result.success(heroList);
     *
     * @param data 成功时返回的业务数据（不能为 null，否则调用方可能 NPE）
     * @param <T>  数据类型，由传入的 data 自动推断
     * @return 包含成功状态和业务数据的 Result 对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 静态工厂方法：创建失败响应
     *
     * 创建失败响应时，data 自动设为 null，只需传入错误码和错误消息。
     *
     * 使用示例：
     * Result<HeroDetailVO> result = Result.error(404, "英雄不存在");
     * Result<Void> result = Result.error(401, "Token 已过期");
     *
     * @param code    错误状态码（非 200 的值，如 400/401/403/404/500）
     * @param message 错误消息，描述失败原因，可展示给用户
     * @param <T>     数据类型（失败时通常不关心具体类型，可用 Void 或 Object）
     * @return 包含错误状态码和错误消息的 Result 对象，data 为 null
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断请求是否成功 —— 最核心的判断方法
     *
     * 每次收到 API 响应后，必须先调用此方法判断成功/失败，
     * 然后再决定是取 data 还是取 message。
     *
     * 判断逻辑：code == 200 即为成功。
     * 为什么用 200？因为 200 是最通用的成功状态码，
     * 与 HTTP 200 OK 含义一致，便于理解和记忆。
     *
     * 正确使用方式：
     * if (result.isSuccess()) {
     *     T data = result.getData();     // 安全：成功时 data 不为 null
     * } else {
     *     String msg = result.getMessage(); // 获取错误信息
     * }
     *
     * 错误使用方式（会导致 NullPointerException）：
     * T data = result.getData();  // 危险！失败时 data 为 null
     *
     * @return true 表示成功（code=200），false 表示失败（code≠200）
     */
    public boolean isSuccess() {
        return code == 200;
    }

    /**
     * 获取业务状态码
     *
     * @return 状态码（200=成功，400=参数错误，401=未认证，403=无权限，404=不存在，500=服务器错误）
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置业务状态码
     *
     * 通常由 Gson 反序列化时调用，不建议手动设置。
     * 如果需要手动创建 Result，建议使用 success() 或 error() 工厂方法。
     *
     * @param code 状态码
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获取响应消息
     *
     * @return 成功时为 "success"，失败时为错误描述
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置响应消息
     *
     * 通常由 Gson 反序列化时调用，不建议手动设置。
     *
     * @param message 响应消息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取响应数据 —— 请求成功时的核心数据
     *
     * 重要：调用此方法前务必先调用 isSuccess() 检查！
     * 失败时 data 为 null，直接使用会导致 NullPointerException。
     *
     * @return 业务数据对象，失败时为 null
     */
    public T getData() {
        return data;
    }

    /**
     * 设置响应数据
     *
     * 通常由 Gson 反序列化时调用，不建议手动设置。
     *
     * @param data 业务数据
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * 获取响应时间戳
     *
     * @return 毫秒级 Unix 时间戳（如 1715000000000）
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 设置响应时间戳
     *
     * 通常由 Gson 反序列化时调用，不建议手动设置。
     *
     * @param timestamp 毫秒级 Unix 时间戳
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
