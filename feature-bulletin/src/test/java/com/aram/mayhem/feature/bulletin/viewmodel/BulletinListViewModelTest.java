package com.aram.mayhem.feature.bulletin.viewmodel;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.data.local.dao.BulletinDao;
import com.aram.mayhem.network.api.BulletinApi;
import com.aram.mayhem.network.dto.BulletinResponse;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.ui.model.BulletinUiModel;

import android.app.Application;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BulletinListViewModelTest {

    @Mock
    private BulletinApi mockBulletinApi;

    @Mock
    private Application mockApplication;

    @Mock
    private BulletinDao mockBulletinDao;

    @Mock
    private Call<Result<PageResponse<BulletinResponse>>> mockBulletinsCall;

    @Mock
    private Call<Result<List<BulletinResponse>>> mockLatestCall;

    private BulletinListViewModel viewModel;

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
        when(mockBulletinDao.getAllBulletins()).thenReturn(new MutableLiveData<>());
        when(mockBulletinDao.getLatestBulletins(anyInt())).thenReturn(new MutableLiveData<>());
        when(mockBulletinDao.getBulletinsByType(anyString())).thenReturn(new MutableLiveData<>());
        viewModel = new BulletinListViewModel(mockApplication, mockBulletinApi, mockBulletinDao);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
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
    @DisplayName("getBulletins 初始值为 null")
    void getBulletins_initialValueIsNull() {
        assertNull(viewModel.getBulletins().getValue());
    }

    @Test
    @DisplayName("getCarouselBulletins 初始值为 null")
    void getCarouselBulletins_initialValueIsNull() {
        assertNull(viewModel.getCarouselBulletins().getValue());
    }

    @Test
    @DisplayName("hasMore 初始值为 true")
    void hasMore_initialValueIsTrue() {
        assertTrue(viewModel.hasMore());
    }

    @Test
    @DisplayName("loadBulletins 调用 BulletinApi.getBulletins 传入正确参数")
    void loadBulletins_callsApiWithCorrectParams() {
        when(mockBulletinApi.getBulletins(anyString(), anyInt(), anyInt())).thenReturn(mockBulletinsCall);

        viewModel.loadBulletins("version", true);

        verify(mockBulletinApi).getBulletins("version", 1, 10);
    }

    @Test
    @DisplayName("loadCarouselBulletins 调用 BulletinApi.getLatestBulletins 传入 limit=3")
    void loadCarouselBulletins_callsApiWithLimit3() {
        when(mockBulletinApi.getLatestBulletins(anyInt())).thenReturn(mockLatestCall);

        viewModel.loadCarouselBulletins();

        verify(mockBulletinApi).getLatestBulletins(3);
    }

    @Test
    @DisplayName("loadBulletins 加载中不重复请求")
    void loadBulletins_whileLoading_doesNotDuplicateRequest() {
        when(mockBulletinApi.getBulletins(any(), anyInt(), anyInt())).thenReturn(mockBulletinsCall);

        viewModel.loadBulletins(null, true);
        viewModel.loadBulletins(null, true);

        verify(mockBulletinApi, times(1)).getBulletins(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("loadBulletins refresh 重置分页状态")
    void loadBulletins_refresh_resetsPagination() {
        when(mockBulletinApi.getBulletins(any(), anyInt(), anyInt())).thenReturn(mockBulletinsCall);

        viewModel.loadBulletins(null, true);
        viewModel.loadBulletins(null, true);

        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mockBulletinApi, times(1)).getBulletins(any(), pageCaptor.capture(), anyInt());
        assertEquals(1, pageCaptor.getValue());
    }

    @Test
    @DisplayName("BulletinUiModel 字段完整性验证")
    void bulletinUiModel_allFieldsCorrect() {
        BulletinUiModel model = new BulletinUiModel(
                1L, "version", "版本15.10更新", "详细内容...",
                "/images/bulletin1.png", true, "2026-05-15", "2026-05-15"
        );

        assertEquals(1L, model.getId());
        assertEquals("version", model.getType());
        assertEquals("版本15.10更新", model.getTitle());
        assertEquals("详细内容...", model.getContent());
        assertEquals("/images/bulletin1.png", model.getImageUrl());
        assertTrue(model.isPinned());
        assertEquals("2026-05-15", model.getPublishedAt());
        assertEquals("2026-05-15", model.getCreatedAt());
    }

    @Test
    @DisplayName("BulletinUiModel getTypeDisplay 返回正确中文")
    void bulletinUiModel_getTypeDisplay_returnsCorrectChinese() {
        BulletinUiModel versionType = new BulletinUiModel(1L, "version", "t", "c", null, false, null, null);
        BulletinUiModel eventType = new BulletinUiModel(2L, "event", "t", "c", null, false, null, null);
        BulletinUiModel noticeType = new BulletinUiModel(3L, "notice", "t", "c", null, false, null, null);
        BulletinUiModel nullType = new BulletinUiModel(4L, null, "t", "c", null, false, null, null);
        BulletinUiModel unknownType = new BulletinUiModel(5L, "other", "t", "c", null, false, null, null);

        assertEquals("版本更新", versionType.getTypeDisplay());
        assertEquals("活动", eventType.getTypeDisplay());
        assertEquals("通知", noticeType.getTypeDisplay());
        assertEquals("", nullType.getTypeDisplay());
        assertEquals("other", unknownType.getTypeDisplay());
    }

    @Test
    @DisplayName("BulletinUiModel isPinned 为 false 时返回 false")
    void bulletinUiModel_notPinned_returnsFalse() {
        BulletinUiModel model = new BulletinUiModel(1L, "version", "t", "c", null, false, null, null);
        assertFalse(model.isPinned());
    }
}
