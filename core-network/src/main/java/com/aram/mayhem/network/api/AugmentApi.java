package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.AugmentDetailResponse;
import com.aram.mayhem.network.dto.AugmentRecommendRequest;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.aram.mayhem.network.dto.AugmentResponse;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.network.dto.SynergyProgressResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 强化符文 API 接口 ── 提供符文列表、详情、套装进度、智能推荐功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义了与强化符文（Augment）相关的四个 HTTP 端点：
 * 1. GET  /api/augments                ── 获取符文列表（支持品质/套装筛选、分页）
 * 2. GET  /api/augments/{id}           ── 获取符文详情
 * 3. GET  /api/augments/synergy-progress ── 获取套装进度（已拥有符文的套装完成度）
 * 4. POST /api/augments/recommend      ── 智能推荐（根据英雄和已有符文推荐最优选择）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、ARAM 模式中的强化符文机制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在英雄联盟 ARAM 模式中，强化符文是核心玩法之一：
 * - 每局游戏开始时，玩家可以从随机出现的符文中选择
 * - 符文分为四个品质：神话(MYTHIC) > 传说(LEGENDARY) > 史诗(EPIC) > 稀有(RARE)
 * - 符文可以组成套装(Synergy Set)，集齐套装获得额外加成
 * - 不同英雄适合不同的符文组合，智能推荐帮助玩家做出最优选择
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、套装进度与智能推荐的关系
 * ═══════════════════════════════════════════════════════════════════
 *
 * 游戏中的典型使用流程：
 * 1. 玩家选择英雄后，查看该英雄的推荐符文
 * 2. 游戏中每轮选择符文时，调用 getSynergyProgress 查看当前套装进度
 * 3. 调用 getRecommendations 获取基于当前局势的智能推荐
 * 4. 推荐算法考虑：英雄适配度、套装完成度、已有符文协同效应
 *
 * 关联类：
 * - AugmentResponse：符文列表项 DTO
 * - AugmentDetailResponse：符文详情 DTO
 * - SynergyProgressResponse：套装进度 DTO
 * - AugmentRecommendRequest/Response：推荐请求/响应 DTO
 * - AugmentRepository：调用本 API 的 Repository
 */
public interface AugmentApi {

    /**
     * 获取符文列表（分页）── 符文图鉴页使用
     *
     * 请求：GET /api/augments?quality=MYTHIC&synergySet=战士&page=1&size=20
     *
     * 筛选参数说明：
     * - quality：符文品质，可选值 MYTHIC/LEGENDARY/EPIC/RARE
     *   MYTHIC（神话）─ 最稀有，效果最强，每局只能选一个
     *   LEGENDARY（传说）─ 较稀有，效果显著
     *   EPIC（史诗）─ 中等稀有，效果适中
     *   RARE（稀有）─ 最常见，效果基础
     * - synergySet：套装名称，如"战士"、"法师"、"刺客"等
     *   集齐同一套装的符文可获得额外加成
     *
     * @param quality 品质筛选（可选，null 时不筛选）
     * @param synergySet 套装筛选（可选，null 时不筛选）
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return Call<Result<PageResponse<AugmentResponse>>> 分页符文列表
     */
    @GET("api/augments")
    Call<Result<PageResponse<AugmentResponse>>> getAugments(
            @Query("quality") String quality,
            @Query("synergySet") String synergySet,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * 获取符文详情 ── 符文详情页使用
     *
     * 请求：GET /api/augments/42
     *
     * 详情包含比列表更多的信息：
     * - 基础属性（名称、描述、品质、图标）
     * - 套装信息（所属套装、套装效果）
     * - 适配英雄（哪些英雄适合选这个符文）
     * - 协同符文（与哪些符文搭配效果更好）
     *
     * @param id 符文 ID
     * @return Call<Result<AugmentDetailResponse>> 符文详情
     */
    @GET("api/augments/{id}")
    Call<Result<AugmentDetailResponse>> getAugmentDetail(@Path("id") long id);

    /**
     * 获取套装进度 ── 游戏中查看当前套装完成度
     *
     * 请求：GET /api/augments/synergy-progress?augmentIds=1,5,12,33
     *
     * augmentIds 参数格式：逗号分隔的符文 ID 列表
     * 如 "1,5,12,33" 表示玩家已拥有 ID 为 1、5、12、33 的符文
     *
     * 响应包含所有相关套装的进度信息：
     * - 套装名称和效果描述
     * - 已拥有符文数 / 套装所需符文数
     * - 完成百分比
     * - 距离下一级加成还差几个符文
     *
     * @param augmentIds 已拥有的符文 ID 列表（逗号分隔，如 "1,5,12,33"）
     * @return Call<Result<List<SynergyProgressResponse>>> 套装进度列表
     */
    @GET("api/augments/synergy-progress")
    Call<Result<List<SynergyProgressResponse>>> getSynergyProgress(@Query("augmentIds") String augmentIds);

    /**
     * 获取符文智能推荐 ── 游戏中选择符文时使用
     *
     * 请求：POST /api/augments/recommend
     * 请求体：{"heroId": 157, "currentAugmentIds": [1, 5, 12]}
     *
     * 推荐算法考虑因素：
     * 1. 英雄适配度 ── 该符文对当前英雄的增益效果
     * 2. 套装完成度 ── 优先推荐能完成套装的符文
     * 3. 协同效应 ── 与已有符文的搭配效果
     * 4. 品质权重 ── 同等条件下优先推荐高品质符文
     *
     * 为什么用 POST 而不是 GET？
     * - 请求体包含 currentAugmentIds 列表，可能很长
     * - GET 请求的 URL 长度有限制（约 2000 字符）
     * - POST 请求体没有长度限制，更适合传递复杂数据
     *
     * @param request 推荐请求体，包含英雄 ID 和已选符文列表
     * @return Call<Result<List<AugmentRecommendResponse>>> 推荐方案列表（按推荐度排序）
     */
    @POST("api/augments/recommend")
    Call<Result<List<AugmentRecommendResponse>>> getRecommendations(@Body AugmentRecommendRequest request);
}
