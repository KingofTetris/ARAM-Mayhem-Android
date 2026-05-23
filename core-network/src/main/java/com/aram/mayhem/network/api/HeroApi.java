package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.HeroDetailResponse;
import com.aram.mayhem.network.dto.HeroResponse;
import com.aram.mayhem.network.dto.PageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 英雄 API 接口 ── 提供英雄列表查询和详情获取功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义了与英雄数据相关的两个 HTTP 端点：
 * 1. GET /api/heroes     ── 获取英雄列表（支持搜索、筛选、分页）
 * 2. GET /api/heroes/{id} ── 获取英雄详情（单个英雄的完整数据）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、Retrofit 查询参数说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * @Query("keyword") String keyword：
 *   - 将参数值附加到 URL 的查询字符串中
 *   - 如 keyword="亚索"，URL 变为 /api/heroes?keyword=亚索
 *   - 参数为 null 时，该查询参数不会出现在 URL 中
 *
 * @Path("id") long id：
 *   - 将参数值替换 URL 路径中的 {id} 占位符
 *   - 如 id=157，URL 变为 /api/heroes/157
 *   - 参数不能为 null，必须提供有效值
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、分页机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 英雄列表使用分页加载，避免一次性加载所有英雄数据：
 * - page：页码，从 1 开始（不是 0）
 * - size：每页数量，通常为 20
 * - 响应中的 PageResponse 包含 total（总记录数）和 records（当前页数据）
 *
 * 分页加载的好处：
 * - 减少网络传输量（只加载当前页的数据）
 * - 减少内存占用（不需要持有所有英雄数据）
 * - 提升用户体验（快速显示第一页，滚动时加载更多）
 *
 * 关联类：
 * - HeroResponse：英雄列表项 DTO
 * - HeroDetailResponse：英雄详情 DTO
 * - PageResponse：通用分页响应 DTO
 * - HeroRepository：调用本 API 的 Repository
 */
public interface HeroApi {

    /**
     * 获取英雄列表（分页）── 英雄列表页使用
     *
     * 请求：GET /api/heroes?keyword=xxx&tier=S_PLUS&sortBy=winRate&page=1&size=20
     *
     * 所有参数都是可选的（可以为 null）：
     * - keyword：搜索关键词，匹配中文名和英文名
     * - tier：梯级筛选，值为 S_PLUS/S/A/B/C
     * - sortBy：排序字段，值为 winRate/pickRate/name
     * - page：页码，从 1 开始
     * - size：每页数量
     *
     * @param keyword 搜索关键词（可选，null 时不筛选）
     * @param tier 梯级筛选（可选，null 时不筛选）
     * @param sortBy 排序字段（可选，null 时使用默认排序）
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return Call<Result<PageResponse<HeroResponse>>> 分页英雄列表
     */
    @GET("api/heroes")
    Call<Result<PageResponse<HeroResponse>>> getHeroes(
            @Query("keyword") String keyword,
            @Query("tier") String tier,
            @Query("sortBy") String sortBy,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * 获取英雄详情 ── 英雄详情页使用
     *
     * 请求：GET /api/heroes/157
     * 响应：包含英雄的完整数据（技能、出装、符文推荐、克制提示等）
     *
     * 与 HeroResponse 的区别：
     * - HeroResponse：列表项，只包含基础信息（名称、胜率、图标等）
     * - HeroDetailResponse：详情，包含完整信息（技能、出装、符文、克制等）
     *
     * @param id 英雄 ID（对应 Riot DataDragon 的英雄 ID）
     * @return Call<Result<HeroDetailResponse>> 英雄详情
     */
    @GET("api/heroes/{id}")
    Call<Result<HeroDetailResponse>> getHeroDetail(@Path("id") long id);
}
