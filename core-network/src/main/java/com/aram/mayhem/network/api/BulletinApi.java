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
 * 公告 API 接口
 *
 * 端点：GET /api/bulletins（列表）、GET /api/bulletins/latest（最新）、GET /api/bulletins/{id}（详情）
 * 关联：BulletinResponse, PageResponse
 */
public interface BulletinApi {

    @GET("api/bulletins")
    Call<Result<PageResponse<BulletinResponse>>> getBulletins(
            @Query("type") String type,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("api/bulletins/latest")
    Call<Result<List<BulletinResponse>>> getLatestBulletins(
            @Query("limit") int limit
    );

    @GET("api/bulletins/{id}")
    Call<Result<BulletinResponse>> getBulletinDetail(@Path("id") long id);
}
