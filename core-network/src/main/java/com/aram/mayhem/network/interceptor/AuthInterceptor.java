package com.aram.mayhem.network.interceptor;

import androidx.annotation.NonNull;

import com.aram.mayhem.data.local.TokenStore;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 认证拦截器
 *
 * 功能：从 TokenStore 读取 accessToken，自动附加 Authorization: Bearer 头
 * 无 Token 时：直接放行（公开接口无需认证）
 * 关联：TokenStore
 */
@Singleton
public class AuthInterceptor implements Interceptor {

    private final TokenStore tokenStore;

    @Inject
    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();

        String accessToken = tokenStore.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return chain.proceed(original);
        }

        Request.Builder builder = original.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .method(original.method(), original.body());

        return chain.proceed(builder.build());
    }
}
