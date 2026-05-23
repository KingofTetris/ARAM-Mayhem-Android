package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.BulletinResponse;
import com.aram.mayhem.network.dto.PageResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 公告 API 接口 ── 提供公告列表、最新公告、公告详情功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义了与公告相关的三个 HTTP 端点：
 * 1. GET /api/bulletins         ── 获取公告列表（支持类型筛选、分页）
 * 2. GET /api/bulletins/latest  ── 获取最新公告（首页快速展示）
 * 3. GET /api/bulletins/{id}    ── 获取公告详情
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、公告类型
 * ═══════════════════════════════════════════════════════════════════
 *
 * 公告按类型分类，用户可以按类型筛选：
 * - UPDATE（版本更新）：游戏版本更新公告
 * - EVENT（活动）：限时活动公告
 * - MAINTENANCE（维护）：服务器维护公告
 * - NOTICE（通知）：一般通知公告
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、列表 vs 最新 vs 详情的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * getBulletins（列表）：
 * - 支持分页和类型筛选
 * - 用于公告列表页，展示所有公告
 * - 返回 PageResponse，包含分页信息
 *
 * getLatestBulletins（最新）：
 * - 只返回最近 N 条公告
 * - 用于首页快速展示最新公告
 * - 返回 List，不分页
 * - limit 参数控制返回数量（通常为 3~5 条）
 *
 * getBulletinDetail（详情）：
 * - 返回单条公告的完整内容
 * - 用于公告详情页
 * - 包含公告正文（HTML 格式）
 *
 * 关联类：
 * - BulletinResponse：公告 DTO
 * - PageResponse：通用分页响应 DTO
 * - BulletinRepository：调用本 API 的 Repository
 */
public interface BulletinApi {

    /**
     * 获取公告列表（分页）── 公告列表页使用
     *
     * 请求：GET /api/bulletins?type=UPDATE&page=1&size=20
     *
     * @param type 公告类型筛选（可选，null 时返回所有类型）
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return Call<Result<PageResponse<BulletinResponse>>> 分页公告列表
     */
    @GET("api/bulletins")
    Call<Result<PageResponse<BulletinResponse>>> getBulletins(
            @Query("type") String type,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * 获取最新公告 ── 首页公告轮播使用
     *
     * 请求：GET /api/bulletins/latest?limit=5
     *
     * 返回最近发布的 N 条公告，按发布时间降序排列。
     * 通常用于首页展示最新公告摘要，用户点击后跳转详情。
     *
     * @param limit 返回数量（通常为 3~5 条）
     * @return Call<Result<List<BulletinResponse>>> 最新公告列表
     */
    @GET("api/bulletins/latest")
    Call<Result<List<BulletinResponse>>> getLatestBulletins(
            @Query("limit") int limit
    );

    /**
     * 获取公告详情 ── 公告详情页使用
     *
     * 请求：GET /api/bulletins/7
     *
     * 详情包含公告的完整内容，包括：
     * - 标题、类型、发布时间
     * - 正文内容（可能包含 HTML 格式）
     * - 是否置顶、是否已读
     *
     * @param id 公告 ID
     * @return Call<Result<BulletinResponse>> 公告详情
     */
    @GET("api/bulletins/{id}")
    Call<Result<BulletinResponse>> getBulletinDetail(@Path("id") long id);
}
