package com.aram.mayhem.network.api;

import com.aram.mayhem.common.Result;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("api/auth/register")
    Call<Result<Object>> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<Result<AuthResponse>> login(@Body LoginRequest request);

    @POST("api/auth/refresh")
    Call<Result<AuthResponse>> refresh(@Body RefreshRequest request);

    class RegisterRequest {
        public String email;
        public String password;
        public String nickname;

        public RegisterRequest(String email, String password, String nickname) {
            this.email = email;
            this.password = password;
            this.nickname = nickname;
        }
    }

    class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    class RefreshRequest {
        public String refreshToken;

        public RefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    class AuthResponse {
        public String accessToken;
        public String refreshToken;
        public long expiresIn;
    }
}
