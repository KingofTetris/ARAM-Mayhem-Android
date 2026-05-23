package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 通用分页响应模型 ── 对应后端 PageResult，所有分页接口共用
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个类是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 后端所有分页查询接口都返回统一格式的分页数据：
 * {
 *   "code": 200,
 *   "data": {
 *     "total": 167,
 *     "page": 1,
 *     "size": 20,
 *     "records": [...]
 *   }
 * }
 *
 * 本类就是 data 字段的 Java 模型，泛型 T 代表每条记录的类型：
 * - PageResponse<HeroResponse>：英雄分页列表
 * - PageResponse<AugmentResponse>：符文分页列表
 * - PageResponse<StrategyListResponse>：攻略分页列表
 * - PageResponse<BulletinResponse>：公告分页列表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、泛型 T 的工作原理
 * ═══════════════════════════════════════════════════════════════════
 *
 * 泛型 T 让同一个类可以适配不同的数据类型：
 * - PageResponse<HeroResponse> 中，T = HeroResponse
 *   records 就是 List<HeroResponse>
 * - PageResponse<AugmentResponse> 中，T = AugmentResponse
 *   records 就是 List<AugmentResponse>
 *
 * Gson 反序列化时，根据 API 方法的返回类型推断 T 的具体类型，
 * 自动将 JSON 数组中的每个对象反序列化为对应的 Java 对象。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、分页计算示例
 * ═══════════════════════════════════════════════════════════════════
 *
 * 假设 total=167, page=1, size=20：
 * - 总页数 = ceil(167 / 20) = 9 页
 * - 当前页记录数 = records.size() = 20
 * - 最后一页记录数 = 167 - 8*20 = 7
 * - 是否有下一页 = page < ceil(total / size)
 *
 * 关联类：
 * - HeroApi / AugmentApi / CommunityApi / BulletinApi：使用本类的 API 接口
 */
public class PageResponse<T> {

    /**
     * 总记录数 ── 用于计算总页数和判断是否还有更多数据
     * 示例：167 表示数据库中共有 167 条英雄记录
     */
    @SerializedName("total")
    private long total;

    /**
     * 当前页码 ── 从 1 开始（不是 0）
     * 示例：1 表示当前是第 1 页
     */
    @SerializedName("page")
    private int page;

    /**
     * 每页数量 ── 每页返回的最大记录数
     * 示例：20 表示每页最多返回 20 条记录
     */
    @SerializedName("size")
    private int size;

    /**
     * 当前页的数据列表 ── 泛型 T 的列表
     * 列表长度 <= size（最后一页可能不足 size 条）
     */
    @SerializedName("records")
    private List<T> records;

    /** 获取总记录数 */
    public long getTotal() {
        return total;
    }

    /** 获取当前页码（从 1 开始） */
    public int getPage() {
        return page;
    }

    /** 获取每页数量 */
    public int getSize() {
        return size;
    }

    /** 获取当前页的数据列表 */
    public List<T> getRecords() {
        return records;
    }
}
