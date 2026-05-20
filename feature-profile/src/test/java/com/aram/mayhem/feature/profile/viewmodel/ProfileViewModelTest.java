package com.aram.mayhem.feature.profile.viewmodel;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.DefaultTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.data.local.TokenStore;
import com.aram.mayhem.feature.profile.repository.ProfileRepository;
import com.aram.mayhem.network.api.AuthApi;
import com.aram.mayhem.network.dto.UpdateProfileRequest;
import com.aram.mayhem.network.dto.UserProfileResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileViewModelTest {

    @Mock
    private ProfileRepository mockProfileRepository;

    @Mock
    private TokenStore mockTokenStore;

    @Mock
    private AuthApi mockAuthApi;

    @Mock
    private Call<Result<AuthApi.AuthResponse>> mockAuthCall;

    @Mock
    private Call<Result<Object>> mockRegisterCall;

    private ProfileViewModel viewModel;

    @BeforeEach
    void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable runnable) {
                runnable.run();
            }

            @Override
            public void postToMainThread(Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });
        viewModel = new ProfileViewModel(mockProfileRepository, mockTokenStore, mockAuthApi);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    private UserProfileResponse createProfile(long id, String nickname) {
        UserProfileResponse profile = new UserProfileResponse();
        profile.id = id;
        profile.email = "user" + id + "@test.com";
        profile.nickname = nickname;
        profile.avatarUrl = "https://avatar.url/" + id;
        profile.displayMode = 0;
        profile.notificationEnabled = 1;
        profile.role = "USER";
        profile.strategyCount = 5;
        profile.favoriteCount = 3;
        return profile;
    }

    @Nested
    @DisplayName("isLoggedIn 测试")
    class IsLoggedInTest {

        @Test
        @DisplayName("TokenStore 有 token 时返回 true")
        void isLoggedIn_hasToken_returnsTrue() {
            when(mockTokenStore.hasToken()).thenReturn(true);
            assertTrue(viewModel.isLoggedIn());
        }

        @Test
        @DisplayName("TokenStore 无 token 时返回 false")
        void isLoggedIn_noToken_returnsFalse() {
            when(mockTokenStore.hasToken()).thenReturn(false);
            assertFalse(viewModel.isLoggedIn());
        }
    }

    @Nested
    @DisplayName("loadUserProfile 测试")
    class LoadUserProfileTest {

        @Test
        @DisplayName("加载成功：应更新 userProfile LiveData")
        void loadUserProfile_success_updatesUserProfile() {
            UserProfileResponse profile = createProfile(1L, "TestUser");
            MutableLiveData<UserProfileResponse> liveData = new MutableLiveData<>();
            liveData.setValue(profile);
            when(mockProfileRepository.getUserProfile()).thenReturn(liveData);

            viewModel.loadUserProfile();

            assertEquals(profile, viewModel.getUserProfile().getValue());
        }

        @Test
        @DisplayName("加载失败：应设置 error LiveData")
        void loadUserProfile_failure_setsError() {
            MutableLiveData<UserProfileResponse> liveData = new MutableLiveData<>();
            liveData.setValue(null);
            when(mockProfileRepository.getUserProfile()).thenReturn(liveData);

            viewModel.loadUserProfile();

            assertEquals("加载用户资料失败", viewModel.getError().getValue());
        }

        @Test
        @DisplayName("加载中：loading 应为 true，完成后应为 false")
        void loadUserProfile_loadingState() {
            MutableLiveData<UserProfileResponse> liveData = new MutableLiveData<>();
            when(mockProfileRepository.getUserProfile()).thenReturn(liveData);

            viewModel.loadUserProfile();
            assertTrue(viewModel.getLoading().getValue());

            liveData.setValue(createProfile(1L, "TestUser"));
            assertFalse(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("正在加载时重复调用应被忽略")
        void loadUserProfile_alreadyLoading_ignored() {
            MutableLiveData<UserProfileResponse> liveData = new MutableLiveData<>();
            when(mockProfileRepository.getUserProfile()).thenReturn(liveData);

            viewModel.loadUserProfile();
            verify(mockProfileRepository, times(1)).getUserProfile();

            viewModel.loadUserProfile();
            verify(mockProfileRepository, times(1)).getUserProfile();
        }
    }

    @Nested
    @DisplayName("updateProfile 测试")
    class UpdateProfileTest {

        @Test
        @DisplayName("更新成功：应更新 userProfile 和 updateSuccess")
        void updateProfile_success_updatesProfileAndSuccess() {
            UserProfileResponse profile = createProfile(1L, "NewNick");
            MutableLiveData<UserProfileResponse> liveData = new MutableLiveData<>();
            liveData.setValue(profile);
            when(mockProfileRepository.updateProfile(any(UpdateProfileRequest.class))).thenReturn(liveData);

            viewModel.updateProfile("NewNick", "https://avatar.url", 1, 0);

            assertEquals(profile, viewModel.getUserProfile().getValue());
            assertTrue(viewModel.getUpdateSuccess().getValue());
        }

        @Test
        @DisplayName("更新失败：应设置 error 和 updateSuccess=false")
        void updateProfile_failure_setsErrorAndSuccessFalse() {
            MutableLiveData<UserProfileResponse> liveData = new MutableLiveData<>();
            liveData.setValue(null);
            when(mockProfileRepository.updateProfile(any(UpdateProfileRequest.class))).thenReturn(liveData);

            viewModel.updateProfile("NewNick", null, null, null);

            assertEquals("更新资料失败", viewModel.getError().getValue());
            assertFalse(viewModel.getUpdateSuccess().getValue());
        }

        @Test
        @DisplayName("更新资料：应传递正确的请求参数")
        void updateProfile_passesCorrectRequest() {
            MutableLiveData<UserProfileResponse> liveData = new MutableLiveData<>();
            liveData.setValue(createProfile(1L, "Nick"));
            when(mockProfileRepository.updateProfile(any(UpdateProfileRequest.class))).thenReturn(liveData);

            viewModel.updateProfile("Nick", "https://avatar.url", 1, 0);

            ArgumentCaptor<UpdateProfileRequest> captor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
            verify(mockProfileRepository).updateProfile(captor.capture());
            UpdateProfileRequest request = captor.getValue();
            assertEquals("Nick", request.nickname);
            assertEquals("https://avatar.url", request.avatarUrl);
            assertEquals(1, request.displayMode);
            assertEquals(0, request.notificationEnabled);
        }
    }

    @Nested
    @DisplayName("logout 测试")
    class LogoutTest {

        @Test
        @DisplayName("退出登录：应清除 Token 并触发 logoutEvent")
        void logout_clearsTokenAndTriggersEvent() {
            viewModel.logout();

            verify(mockTokenStore).clear();
            assertTrue(viewModel.getLogoutEvent().getValue());
        }
    }

    @Nested
    @DisplayName("login 测试")
    class LoginTest {

        @Test
        @DisplayName("登录成功：应保存 Token 并触发 loginSuccess")
        void login_success_savesTokenAndTriggersLoginSuccess() {
            when(mockAuthApi.login(any(AuthApi.LoginRequest.class))).thenReturn(mockAuthCall);

            viewModel.login("test@example.com", "password123");

            ArgumentCaptor<Callback<Result<AuthApi.AuthResponse>>> callbackCaptor =
                    ArgumentCaptor.forClass(Callback.class);
            verify(mockAuthCall).enqueue(callbackCaptor.capture());

            AuthApi.AuthResponse authResponse = new AuthApi.AuthResponse();
            authResponse.accessToken = "access-token-123";
            authResponse.refreshToken = "refresh-token-456";
            authResponse.expiresIn = 3600;

            Result<AuthApi.AuthResponse> result = Result.success(authResponse);
            Response<Result<AuthApi.AuthResponse>> response = Response.success(result);

            callbackCaptor.getValue().onResponse(mockAuthCall, response);

            verify(mockTokenStore).saveTokens("access-token-123", "refresh-token-456", 3600_000L);
            assertTrue(viewModel.getLoginSuccess().getValue());
            assertFalse(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("登录失败-密码错误：应设置 error")
        void login_invalidCredential_setsError() {
            when(mockAuthApi.login(any(AuthApi.LoginRequest.class))).thenReturn(mockAuthCall);

            viewModel.login("test@example.com", "wrong");

            ArgumentCaptor<Callback<Result<AuthApi.AuthResponse>>> callbackCaptor =
                    ArgumentCaptor.forClass(Callback.class);
            verify(mockAuthCall).enqueue(callbackCaptor.capture());

            Result<AuthApi.AuthResponse> result = Result.error(401, "Invalid credentials");
            Response<Result<AuthApi.AuthResponse>> response = Response.success(result);

            callbackCaptor.getValue().onResponse(mockAuthCall, response);

            assertEquals("邮箱或密码错误", viewModel.getError().getValue());
            assertFalse(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("登录失败-响应体为空：应设置 error")
        void login_nullResponseBody_setsError() {
            when(mockAuthApi.login(any(AuthApi.LoginRequest.class))).thenReturn(mockAuthCall);

            viewModel.login("test@example.com", "password");

            ArgumentCaptor<Callback<Result<AuthApi.AuthResponse>>> callbackCaptor =
                    ArgumentCaptor.forClass(Callback.class);
            verify(mockAuthCall).enqueue(callbackCaptor.capture());

            Response<Result<AuthApi.AuthResponse>> response = Response.success(null);

            callbackCaptor.getValue().onResponse(mockAuthCall, response);

            assertEquals("邮箱或密码错误", viewModel.getError().getValue());
        }

        @Test
        @DisplayName("登录失败-AuthResponse 为空：应设置 error")
        void login_nullAuthResponse_setsError() {
            when(mockAuthApi.login(any(AuthApi.LoginRequest.class))).thenReturn(mockAuthCall);

            viewModel.login("test@example.com", "password");

            ArgumentCaptor<Callback<Result<AuthApi.AuthResponse>>> callbackCaptor =
                    ArgumentCaptor.forClass(Callback.class);
            verify(mockAuthCall).enqueue(callbackCaptor.capture());

            Result<AuthApi.AuthResponse> result = Result.success(null);
            Response<Result<AuthApi.AuthResponse>> response = Response.success(result);

            callbackCaptor.getValue().onResponse(mockAuthCall, response);

            assertEquals("登录失败：服务器返回为空", viewModel.getError().getValue());
        }

        @Test
        @DisplayName("登录网络错误：应设置 error 含网络错误信息")
        void login_networkError_setsError() {
            when(mockAuthApi.login(any(AuthApi.LoginRequest.class))).thenReturn(mockAuthCall);

            viewModel.login("test@example.com", "password");

            ArgumentCaptor<Callback<Result<AuthApi.AuthResponse>>> callbackCaptor =
                    ArgumentCaptor.forClass(Callback.class);
            verify(mockAuthCall).enqueue(callbackCaptor.capture());

            callbackCaptor.getValue().onFailure(mockAuthCall, new RuntimeException("Connection refused"));

            assertTrue(viewModel.getError().getValue().startsWith("网络错误："));
            assertTrue(viewModel.getError().getValue().contains("Connection refused"));
            assertFalse(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("登录开始时 loading 应为 true")
        void login_setsLoadingTrue() {
            when(mockAuthApi.login(any(AuthApi.LoginRequest.class))).thenReturn(mockAuthCall);

            viewModel.login("test@example.com", "password");

            assertTrue(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("登录：应传递正确的 email 和 password")
        void login_passesCorrectCredentials() {
            when(mockAuthApi.login(any(AuthApi.LoginRequest.class))).thenReturn(mockAuthCall);

            viewModel.login("user@test.com", "pass123");

            ArgumentCaptor<AuthApi.LoginRequest> captor = ArgumentCaptor.forClass(AuthApi.LoginRequest.class);
            verify(mockAuthApi).login(captor.capture());
            assertEquals("user@test.com", captor.getValue().email);
            assertEquals("pass123", captor.getValue().password);
        }
    }

    @Nested
    @DisplayName("register 测试")
    class RegisterTest {

        @Test
        @DisplayName("注册成功：应触发 registerSuccess")
        void register_success_triggersRegisterSuccess() {
            when(mockAuthApi.register(any(AuthApi.RegisterRequest.class))).thenReturn(mockRegisterCall);

            viewModel.register("new@test.com", "password", "NewUser");

            ArgumentCaptor<Callback<Result<Object>>> callbackCaptor =
                    ArgumentCaptor.forClass(Callback.class);
            verify(mockRegisterCall).enqueue(callbackCaptor.capture());

            Result<Object> result = Result.success(new Object());
            Response<Result<Object>> response = Response.success(result);

            callbackCaptor.getValue().onResponse(mockRegisterCall, response);

            assertTrue(viewModel.getRegisterSuccess().getValue());
            assertFalse(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("注册失败-邮箱已存在：应设置 error")
        void register_duplicateEmail_setsError() {
            when(mockAuthApi.register(any(AuthApi.RegisterRequest.class))).thenReturn(mockRegisterCall);

            viewModel.register("exists@test.com", "password", "Exists");

            ArgumentCaptor<Callback<Result<Object>>> callbackCaptor =
                    ArgumentCaptor.forClass(Callback.class);
            verify(mockRegisterCall).enqueue(callbackCaptor.capture());

            Result<Object> result = Result.error(409, "Email already registered");
            Response<Result<Object>> response = Response.success(result);

            callbackCaptor.getValue().onResponse(mockRegisterCall, response);

            assertEquals("注册失败：邮箱可能已被注册", viewModel.getError().getValue());
            assertFalse(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("注册网络错误：应设置 error 含网络错误信息")
        void register_networkError_setsError() {
            when(mockAuthApi.register(any(AuthApi.RegisterRequest.class))).thenReturn(mockRegisterCall);

            viewModel.register("new@test.com", "password", "NewUser");

            ArgumentCaptor<Callback<Result<Object>>> callbackCaptor =
                    ArgumentCaptor.forClass(Callback.class);
            verify(mockRegisterCall).enqueue(callbackCaptor.capture());

            callbackCaptor.getValue().onFailure(mockRegisterCall, new RuntimeException("Timeout"));

            assertTrue(viewModel.getError().getValue().startsWith("网络错误："));
            assertTrue(viewModel.getError().getValue().contains("Timeout"));
            assertFalse(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("注册：应传递正确的 email、password 和 nickname")
        void register_passesCorrectParams() {
            when(mockAuthApi.register(any(AuthApi.RegisterRequest.class))).thenReturn(mockRegisterCall);

            viewModel.register("new@test.com", "pass123", "Nick");

            ArgumentCaptor<AuthApi.RegisterRequest> captor = ArgumentCaptor.forClass(AuthApi.RegisterRequest.class);
            verify(mockAuthApi).register(captor.capture());
            assertEquals("new@test.com", captor.getValue().email);
            assertEquals("pass123", captor.getValue().password);
            assertEquals("Nick", captor.getValue().nickname);
        }

        @Test
        @DisplayName("注册开始时 loading 应为 true")
        void register_setsLoadingTrue() {
            when(mockAuthApi.register(any(AuthApi.RegisterRequest.class))).thenReturn(mockRegisterCall);

            viewModel.register("new@test.com", "password", "NewUser");

            assertTrue(viewModel.getLoading().getValue());
        }
    }

    @Nested
    @DisplayName("初始状态测试")
    class InitialStateTest {

        @Test
        @DisplayName("userProfile 初始值为 null")
        void userProfile_initialNull() {
            assertNull(viewModel.getUserProfile().getValue());
        }

        @Test
        @DisplayName("loading 初始值为 false")
        void loading_initialFalse() {
            assertFalse(viewModel.getLoading().getValue());
        }

        @Test
        @DisplayName("error 初始值为 null")
        void error_initialNull() {
            assertNull(viewModel.getError().getValue());
        }

        @Test
        @DisplayName("updateSuccess 初始值为 null")
        void updateSuccess_initialNull() {
            assertNull(viewModel.getUpdateSuccess().getValue());
        }

        @Test
        @DisplayName("logoutEvent 初始值为 null")
        void logoutEvent_initialNull() {
            assertNull(viewModel.getLogoutEvent().getValue());
        }

        @Test
        @DisplayName("loginSuccess 初始值为 null")
        void loginSuccess_initialNull() {
            assertNull(viewModel.getLoginSuccess().getValue());
        }

        @Test
        @DisplayName("registerSuccess 初始值为 null")
        void registerSuccess_initialNull() {
            assertNull(viewModel.getRegisterSuccess().getValue());
        }
    }
}
