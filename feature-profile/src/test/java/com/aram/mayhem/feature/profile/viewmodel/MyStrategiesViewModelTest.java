package com.aram.mayhem.feature.profile.viewmodel;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.api.CommunityApi;
import com.aram.mayhem.network.dto.StrategyListResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyStrategiesViewModelTest {

    @Mock
    private CommunityApi mockCommunityApi;

    @Mock
    private Call<Result<List<StrategyListResponse>>> mockMyStrategiesCall;

    @Mock
    private Call<Result<Void>> mockDeleteCall;

    private MyStrategiesViewModel viewModel;

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
        viewModel = new MyStrategiesViewModel(mockCommunityApi);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @DisplayName("getMyStrategies 初始值为 null")
    void getMyStrategies_initialValueIsNull() {
        assertNull(viewModel.getMyStrategies().getValue());
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
    @DisplayName("loadMyStrategies 成功：应设置 myStrategies 列表")
    void loadMyStrategies_success_setsMyStrategies() {
        when(mockCommunityApi.getMyStrategies()).thenReturn(mockMyStrategiesCall);

        viewModel.loadMyStrategies();

        assertTrue(viewModel.getLoading().getValue());

        ArgumentCaptor<Callback<Result<List<StrategyListResponse>>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockMyStrategiesCall).enqueue(captor.capture());

        List<StrategyListResponse> strategies = createTestStrategies(3);
        Result<List<StrategyListResponse>> result = Result.success(strategies);

        captor.getValue().onResponse(mockMyStrategiesCall, Response.success(result));

        assertFalse(viewModel.getLoading().getValue());
        assertNotNull(viewModel.getMyStrategies().getValue());
        assertEquals(3, viewModel.getMyStrategies().getValue().size());
        assertNull(viewModel.getError().getValue());
    }

    @Test
    @DisplayName("loadMyStrategies 服务器返回错误：应设置 error")
    void loadMyStrategies_serverError_setsError() {
        when(mockCommunityApi.getMyStrategies()).thenReturn(mockMyStrategiesCall);

        viewModel.loadMyStrategies();

        ArgumentCaptor<Callback<Result<List<StrategyListResponse>>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockMyStrategiesCall).enqueue(captor.capture());

        captor.getValue().onResponse(mockMyStrategiesCall, Response.success(Result.error(500, "Server Error")));

        assertFalse(viewModel.getLoading().getValue());
        assertTrue(viewModel.getMyStrategies().getValue().isEmpty());
        assertEquals("加载我的攻略失败", viewModel.getError().getValue());
    }

    @Test
    @DisplayName("loadMyStrategies 网络失败：应设置 error")
    void loadMyStrategies_networkFailure_setsError() {
        when(mockCommunityApi.getMyStrategies()).thenReturn(mockMyStrategiesCall);

        viewModel.loadMyStrategies();

        ArgumentCaptor<Callback<Result<List<StrategyListResponse>>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockMyStrategiesCall).enqueue(captor.capture());

        captor.getValue().onFailure(mockMyStrategiesCall, new RuntimeException("Connection refused"));

        assertFalse(viewModel.getLoading().getValue());
        assertTrue(viewModel.getMyStrategies().getValue().isEmpty());
        assertEquals("网络错误：Connection refused", viewModel.getError().getValue());
    }

    @Test
    @DisplayName("loadMyStrategies 加载中不重复请求")
    void loadMyStrategies_whileLoading_doesNotDuplicateRequest() {
        when(mockCommunityApi.getMyStrategies()).thenReturn(mockMyStrategiesCall);

        viewModel.loadMyStrategies();
        viewModel.loadMyStrategies();

        verify(mockCommunityApi, times(1)).getMyStrategies();
    }

    @Test
    @DisplayName("deleteStrategy 成功：应设置 deleteSuccess 为 true")
    void deleteStrategy_success_setsDeleteSuccessTrue() {
        when(mockCommunityApi.deleteStrategy(anyLong())).thenReturn(mockDeleteCall);

        viewModel.deleteStrategy(1L, 0);

        ArgumentCaptor<Callback<Result<Void>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockDeleteCall).enqueue(captor.capture());

        captor.getValue().onResponse(mockDeleteCall, Response.success(Result.success(null)));

        assertTrue(viewModel.getDeleteSuccess().getValue());
    }

    @Test
    @DisplayName("deleteStrategy 失败：应设置 error")
    void deleteStrategy_failure_setsError() {
        when(mockCommunityApi.deleteStrategy(anyLong())).thenReturn(mockDeleteCall);

        viewModel.deleteStrategy(1L, 0);

        ArgumentCaptor<Callback<Result<Void>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockDeleteCall).enqueue(captor.capture());

        captor.getValue().onResponse(mockDeleteCall, Response.success(Result.error(403, "Forbidden")));

        assertFalse(viewModel.getDeleteSuccess().getValue());
        assertEquals("删除失败", viewModel.getError().getValue());
    }

    @Test
    @DisplayName("deleteStrategy 网络失败：应设置 error")
    void deleteStrategy_networkFailure_setsError() {
        when(mockCommunityApi.deleteStrategy(anyLong())).thenReturn(mockDeleteCall);

        viewModel.deleteStrategy(1L, 0);

        ArgumentCaptor<Callback<Result<Void>>> captor = ArgumentCaptor.forClass(Callback.class);
        verify(mockDeleteCall).enqueue(captor.capture());

        captor.getValue().onFailure(mockDeleteCall, new RuntimeException("Timeout"));

        assertFalse(viewModel.getDeleteSuccess().getValue());
        assertEquals("网络错误：Timeout", viewModel.getError().getValue());
    }

    private List<StrategyListResponse> createTestStrategies(int count) {
        List<StrategyListResponse> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StrategyListResponse s = new StrategyListResponse();
            s.setId((long) i);
            s.setTitle("我的攻略 " + i);
            s.setUpvotes(i * 5);
            list.add(s);
        }
        return list;
    }
}
