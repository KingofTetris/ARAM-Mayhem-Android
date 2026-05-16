package com.aram.mayhem.common;

import com.google.gson.annotations.SerializedName;

/**
 * 统一 API 响应模型（与后端 Result 对应）
 *
 * 格式：{ code, message, data, timestamp }
 * 用途：Retrofit 响应体解析，isSuccess() 判断请求是否成功
 *
 * @param <T> 响应数据类型
 */
public class Result<T> {

    /** 业务状态码（200=成功，其他=失败） */
    @SerializedName("code")
    private int code;

    /** 响应消息（成功时为"success"，失败时为错误描述） */
    @SerializedName("message")
    private String message;

    /** 响应数据（成功时携带业务数据，失败时为null） */
    @SerializedName("data")
    private T data;

    /** 响应时间戳（毫秒） */
    @SerializedName("timestamp")
    private long timestamp;

    /** 空构造函数（Gson 反序列化必需） */
    public Result() {
    }

    /**
     * 构造函数
     *
     * @param code    业务状态码
     * @param message 响应消息
     * @param data    响应数据
     */
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 创建失败响应
     *
     * @param code    错误状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断请求是否成功
     *
     * @return true=成功（code=200），false=失败
     */
    public boolean isSuccess() {
        return code == 200;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
