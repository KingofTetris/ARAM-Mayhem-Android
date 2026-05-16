package com.aram.mayhem.network.interceptor;

import androidx.annotation.NonNull;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.data.local.TokenStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 令牌刷新拦截器
 *
 * 功能：当收到 401 响应时，自动使用 refreshToken 换取新的 accessToken
 * 流程：401 → 同步刷新请求 → 更新 TokenStore → 用新 Token 重试原请求
 * 刷新失败：清除 TokenStore（用户需重新登录）
 * 线程安全：synchronized 防止并发刷新
 * 关联：TokenStore, Constants
 */
@Singleton
public class TokenRefreshInterceptor implements Interceptor {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final TokenStore tokenStore;
    private final Gson gson;

    @Inject
    public TokenRefreshInterceptor(TokenStore tokenStore, Gson gson) {
        this.tokenStore = tokenStore;
        this.gson = gson;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());

        if (response.code() == 401) {
            synchronized (this) {
                String currentAccessToken = tokenStore.getAccessToken();
                String refreshToken = tokenStore.getRefreshToken();

                if (refreshToken != null && !refreshToken.isEmpty()) {
                    Response refreshResponse = performRefresh(refreshToken);
                    if (refreshResponse != null && refreshResponse.isSuccessful()) {
                        String responseBody = refreshResponse.body() != null
                                ? refreshResponse.body().string() : "";
                        refreshResponse.close();

                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        if (json.has("data")) {
                            JsonObject data = json.getAsJsonObject("data");
                            String newAccessToken = data.get("accessToken").getAsString();
                            String newRefreshToken = data.get("refreshToken").getAsString();
                            long expiresIn = data.has("expiresIn")
                                    ? data.get("expiresIn").getAsLong() * 1000
                                    : 900000L;

                            tokenStore.saveTokens(newAccessToken, newRefreshToken, expiresIn);

                            Request newRequest = chain.request().newBuilder()
                                    .header("Authorization", "Bearer " + newAccessToken)
                                    .build();

                            return chain.proceed(newRequest);
                        }
                    }
                }

                tokenStore.clear();
            }
        }

        return response;
    }

    private Response performRefresh(String refreshToken) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("refreshToken", refreshToken);

        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
                .url(Constants.BASE_URL + Constants.API_AUTH_REFRESH)
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA_TYPE))
                .build();

        return client.newCall(request).execute();
    }
}
