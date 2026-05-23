package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.dto.CreateStrategyRequest;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.network.dto.StrategyDetailResponse;
import com.aram.mayhem.network.dto.StrategyListResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 社区攻略 API 接口 ── 提供攻略的列表、详情、发布、删除、投票功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 定义了与社区攻略相关的七个 HTTP 端点：
 * 1. GET    /api/strategies          ── 获取攻略列表（支持排序、分页）
 * 2. GET    /api/strategies/{id}     ── 获取攻略详情
 * 3. POST   /api/strategies          ── 发布攻略（需要登录）
 * 4. GET    /api/strategies/my       ── 获取我的攻略列表（需要登录）
 * 5. DELETE /api/strategies/{id}     ── 删除攻略（只能删自己的）
 * 6. POST   /api/strategies/{id}/vote ── 投票（点赞/点踩）
 * 7. DELETE /api/strategies/{id}/vote ── 取消投票
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、社区攻略的业务逻辑
 * ═══════════════════════════════════════════════════════════════════
 *
 * 社区攻略是用户生成内容（UGC）模块，允许玩家分享自己的游戏心得：
 * - 玩家可以发布针对特定英雄/符文的攻略
 * - 其他玩家可以浏览、投票（点赞/点踩）
 * - 攻略按"热门"或"最新"排序
 * - 作者只能删除自己发布的攻略
 * - 每个用户对每篇攻略只能投一次票，可以取消
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、权限控制
 * ═══════════════════════════════════════════════════════════════════
 *
 * 不同端点的权限要求：
 * - 浏览攻略列表/详情：无需登录（公开接口）
 * - 发布攻略：需要登录（Bearer Token）
 * - 获取我的攻略：需要登录
 * - 删除攻略：需要登录 + 只能删自己的
 * - 投票/取消投票：需要登录
 *
 * AuthInterceptor 会自动为需要认证的请求添加 Token，
 * 后端通过 JWT 中的 userId 验证身份和权限。
 *
 * 关联类：
 * - StrategyListResponse：攻略列表项 DTO
 * - StrategyDetailResponse：攻略详情 DTO
 * - CreateStrategyRequest：发布攻略请求体
 * - CommunityRepository：调用本 API 的 Repository
 */
public interface CommunityApi {

    /**
     * 获取攻略列表（分页）── 社区首页使用
     *
     * 请求：GET /api/strategies?sort=hot&page=1&size=20
     *
     * 排序方式说明：
     * - hot（热门）：按投票数（点赞-点踩）降序，优质攻略排在前面
     * - latest（最新）：按发布时间降序，新攻略排在前面
     *
     * @param sort 排序方式（hot-热门 / latest-最新）
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return Call<Result<PageResponse<StrategyListResponse>>> 分页攻略列表
     */
    @GET("api/strategies")
    Call<Result<PageResponse<StrategyListResponse>>> getStrategies(
            @Query("sort") String sort,
            @Query("page") int page,
            @Query("size") int size
    );

    /**
     * 获取攻略详情 ── 攻略详情页使用
     *
     * 请求：GET /api/strategies/42
     *
     * 详情包含比列表更多的信息：
     * - 完整内容（列表只显示摘要）
     * - 作者信息（昵称、头像）
     * - 投票统计（点赞数、点踩数）
     * - 当前用户的投票状态（是否已投票、投了什么）
     *
     * @param id 攻略 ID
     * @return Call<Result<StrategyDetailResponse>> 攻略详情
     */
    @GET("api/strategies/{id}")
    Call<Result<StrategyDetailResponse>> getStrategyDetail(@Path("id") long id);

    /**
     * 发布攻略 ── 发布攻略页面使用
     *
     * 请求：POST /api/strategies
     * 请求体：{"title": "xxx", "content": "xxx", "heroId": 157, "augmentIds": [1,5]}
     *
     * 发布成功后返回攻略详情（包含服务端生成的 ID 和时间戳）。
     * 需要登录，后端从 JWT 中提取 userId 作为作者 ID。
     *
     * @param request 发布攻略请求体，包含标题、内容、关联英雄、关联符文
     * @return Call<Result<StrategyDetailResponse>> 发布后的攻略详情
     */
    @POST("api/strategies")
    Call<Result<StrategyDetailResponse>> publishStrategy(@Body CreateStrategyRequest request);

    /**
     * 获取当前用户发布的攻略列表 ── 个人中心"我的攻略"使用
     *
     * 请求：GET /api/strategies/my
     *
     * 不需要分页，因为单个用户发布的攻略数量有限。
     * 需要登录，后端从 JWT 中提取 userId 查询该用户的攻略。
     *
     * @return Call<Result<List<StrategyListResponse>>> 用户攻略列表
     */
    @GET("api/strategies/my")
    Call<Result<java.util.List<StrategyListResponse>>> getMyStrategies();

    /**
     * 删除攻略 ── 攻略详情页/个人中心使用
     *
     * 请求：DELETE /api/strategies/42
     *
     * 权限控制：
     * - 需要登录
     * - 只能删除自己发布的攻略
     * - 尝试删除他人攻略会返回 403 Forbidden
     *
     * @param id 攻略 ID
     * @return Call<Result<Void>> 删除结果
     */
    @DELETE("api/strategies/{id}")
    Call<Result<Void>> deleteStrategy(@Path("id") long id);

    /**
     * 投票（点赞/点踩）── 攻略详情页使用
     *
     * 请求：POST /api/strategies/42/vote
     * 请求体：{"voteType": "UP"}
     *
     * 投票规则：
     * - 每个用户对每篇攻略只能投一次票
     * - 已投 UP 的可以改为 DOWN（切换），反之亦然
     * - voteType 为 UP（点赞）或 DOWN（点踩）
     *
     * @param strategyId 攻略 ID
     * @param request 投票请求体，包含 voteType
     * @return Call<Result<Void>> 投票结果
     */
    @POST("api/strategies/{id}/vote")
    Call<Result<Void>> vote(@Path("id") long strategyId, @Body VoteRequest request);

    /**
     * 取消投票 ── 攻略详情页使用
     *
     * 请求：DELETE /api/strategies/42/vote
     *
     * 取消后该攻略的投票数会相应减少。
     * 如果用户没有投过票，调用此接口不会报错（幂等性）。
     *
     * @param strategyId 攻略 ID
     * @return Call<Result<Void>> 取消投票结果
     */
    @DELETE("api/strategies/{id}/vote")
    Call<Result<Void>> cancelVote(@Path("id") long strategyId);

    /**
     * 投票请求体 ── 发送投票时携带的数据
     *
     * voteType 取值：
     * - UP：点赞，增加攻略的评分
     * - DOWN：点踩，降低攻略的评分
     */
    class VoteRequest {
        /** 投票类型（UP-点赞 / DOWN-点踩） */
        public String voteType;

        /**
         * 构造投票请求体
         *
         * @param voteType 投票类型
         */
        public VoteRequest(String voteType) {
            this.voteType = voteType;
        }
    }
}
