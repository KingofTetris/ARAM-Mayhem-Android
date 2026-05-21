package com.aram.mayhem.feature.bulletin.viewmodel;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.api.BulletinApi;
import com.aram.mayhem.network.dto.BulletinResponse;
import com.aram.mayhem.ui.model.BulletinUiModel;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulletinDetailViewModelTest {

    @Mock
    private BulletinApi mockBulletinApi;

    @Mock
    private Call<Result<BulletinResponse>> mockCall;

    private BulletinDetailViewModel viewModel;

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
        viewModel = new BulletinDetailViewModel(mockBulletinApi);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @DisplayName("getBulletinDetail 初始值为 null")
    void getBulletinDetail_initialValueIsNull() {
        assertNull(viewModel.getBulletinDetail().getValue());
    }

    @Test
    @DisplayName("getLoading 初始值为 false")
    void getLoading_initialValueIsFalse() {
        assertFalse(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("getError 初始值为 null")
    void getError_initialValueIsNull() {
        assertNull(viewModel.getError().getValue());
    }

    @Test
    @DisplayName("loadBulletinDetail 成功：应设置 bulletinDetail LiveData")
    void loadBulletinDetail_success_setsBulletinDetail() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(1L);

        ArgumentCaptor<Callback<Result<BulletinResponse>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockCall).enqueue(captor.capture());

        BulletinResponse response = createTestBulletinResponse(1L);
        Result<BulletinResponse> result = Result.success(response);

        captor.getValue().onResponse(mockCall, Response.success(result));

        BulletinUiModel detail = viewModel.getBulletinDetail().getValue();
        assertNotNull(detail);
        assertEquals(1L, detail.getId());
        assertEquals("版本15.10更新", detail.getTitle());
        assertEquals("version", detail.getType());
        assertTrue(detail.isPinned());
        assertFalse(viewModel.getLoading().getValue());
        assertNull(viewModel.getError().getValue());
    }

    @Test
    @DisplayName("loadBulletinDetail 开始时 loading 为 true")
    void loadBulletinDetail_initialState_loadingIsTrue() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(1L);

        assertTrue(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("loadBulletinDetail 服务器返回错误：应设置 error")
    void loadBulletinDetail_serverError_setsError() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(1L);

        ArgumentCaptor<Callback<Result<BulletinResponse>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockCall).enqueue(captor.capture());

        captor.getValue().onResponse(mockCall, Response.success(Result.error(404, "Not Found")));

        assertNull(viewModel.getBulletinDetail().getValue());
        assertEquals("公告不存在", viewModel.getError().getValue());
        assertFalse(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("loadBulletinDetail HTTP 错误响应：应设置 error")
    void loadBulletinDetail_httpError_setsError() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(1L);

        ArgumentCaptor<Callback<Result<BulletinResponse>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockCall).enqueue(captor.capture());

        captor.getValue().onResponse(mockCall, Response.error(500, ResponseBody.create("", MediaType.parse("text/plain"))));

        assertEquals("公告不存在", viewModel.getError().getValue());
        assertFalse(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("loadBulletinDetail 网络失败：应设置 error")
    void loadBulletinDetail_networkFailure_setsError() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(1L);

        ArgumentCaptor<Callback<Result<BulletinResponse>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockCall).enqueue(captor.capture());

        captor.getValue().onFailure(mockCall, new RuntimeException("Connection refused"));

        assertEquals("Connection refused", viewModel.getError().getValue());
        assertFalse(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("loadBulletinDetail 加载中不重复请求")
    void loadBulletinDetail_whileLoading_doesNotDuplicateRequest() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(1L);
        viewModel.loadBulletinDetail(1L);

        verify(mockBulletinApi, times(1)).getBulletinDetail(anyLong());
    }

    @Test
    @DisplayName("loadBulletinDetail 调用 BulletinApi 传入正确 id")
    void loadBulletinDetail_callsApiWithCorrectId() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(42L);

        verify(mockBulletinApi).getBulletinDetail(42L);
    }

    @Test
    @DisplayName("isPinned 为 0 时应转换为 false")
    void loadBulletinDetail_isPinnedZero_convertsToFalse() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(2L);

        ArgumentCaptor<Callback<Result<BulletinResponse>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockCall).enqueue(captor.capture());

        BulletinResponse response = createTestBulletinResponse(2L);
        response.setIsPinned(0);
        Result<BulletinResponse> result = Result.success(response);

        captor.getValue().onResponse(mockCall, Response.success(result));

        BulletinUiModel detail = viewModel.getBulletinDetail().getValue();
        assertNotNull(detail);
        assertFalse(detail.isPinned());
    }

    @Test
    @DisplayName("isPinned 为 null 时应转换为 false")
    void loadBulletinDetail_isPinnedNull_convertsToFalse() {
        when(mockBulletinApi.getBulletinDetail(anyLong())).thenReturn(mockCall);

        viewModel.loadBulletinDetail(3L);

        ArgumentCaptor<Callback<Result<BulletinResponse>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockCall).enqueue(captor.capture());

        BulletinResponse response = createTestBulletinResponse(3L);
        response.setIsPinned(null);
        Result<BulletinResponse> result = Result.success(response);

        captor.getValue().onResponse(mockCall, Response.success(result));

        BulletinUiModel detail = viewModel.getBulletinDetail().getValue();
        assertNotNull(detail);
        assertFalse(detail.isPinned());
    }

    private BulletinResponse createTestBulletinResponse(long id) {
        BulletinResponse response = new BulletinResponse();
        response.setId(id);
        response.setType("version");
        response.setTitle("版本15.10更新");
        response.setContent("详细内容...");
        response.setImageUrl("/images/bulletin1.png");
        response.setIsPinned(1);
        response.setPublishedAt("2026-05-15");
        response.setCreatedAt("2026-05-15");
        return response;
    }
}
