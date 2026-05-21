package com.aram.mayhem.feature.community.viewmodel;

import android.app.Application;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.community.repository.StrategyRepository;
import com.aram.mayhem.network.dto.StrategyListResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StrategyFeedViewModelTest {

    @Mock
    private Application mockApplication;

    @Mock
    private StrategyRepository mockRepository;

    private MutableLiveData<List<StrategyListResponse>> repositoryResult;

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

        repositoryResult = new MutableLiveData<>();
        when(mockRepository.getStrategies(anyString(), anyInt(), anyInt())).thenReturn(repositoryResult);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @DisplayName("构造时自动加载第一页数据")
    void constructor_autoLoadsFirstPage() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        verify(mockRepository).getStrategies("hot", 1, 10);
        assertTrue(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("加载成功：应更新 strategies 列表")
    void loadStrategies_success_updatesStrategiesList() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        List<StrategyListResponse> strategies = createTestStrategies(10);
        repositoryResult.setValue(strategies);

        assertFalse(viewModel.getLoading().getValue());
        assertFalse(viewModel.getLoadingMore().getValue());
        assertEquals(10, viewModel.getStrategies().getValue().size());
        assertFalse(viewModel.getIsLastPage().getValue());
    }

    @Test
    @DisplayName("加载成功且数据少于 pageSize：应标记为最后一页")
    void loadStrategies_lessThanPageSize_marksLastPage() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        List<StrategyListResponse> strategies = createTestStrategies(5);
        repositoryResult.setValue(strategies);

        assertTrue(viewModel.getIsLastPage().getValue());
    }

    @Test
    @DisplayName("加载返回空列表：应标记为最后一页")
    void loadStrategies_emptyList_marksLastPage() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        repositoryResult.setValue(Collections.emptyList());

        assertTrue(viewModel.getIsLastPage().getValue());
        assertTrue(viewModel.getStrategies().getValue().isEmpty());
    }

    @Test
    @DisplayName("setSort 切换排序：应重新加载第一页")
    void setSort_differentSort_reloadsFirstPage() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        repositoryResult.setValue(createTestStrategies(3));

        reset(mockRepository);
        MutableLiveData<List<StrategyListResponse>> newResult = new MutableLiveData<>();
        when(mockRepository.getStrategies(anyString(), anyInt(), anyInt())).thenReturn(newResult);

        viewModel.setSort("latest");

        verify(mockRepository).getStrategies("latest", 1, 10);
    }

    @Test
    @DisplayName("setSort 相同排序：不重新加载")
    void setSort_sameSort_doesNotReload() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        repositoryResult.setValue(createTestStrategies(3));
        reset(mockRepository);

        viewModel.setSort("hot");

        verify(mockRepository, never()).getStrategies(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("loadMore：应加载下一页并追加数据")
    void loadMore_appendsNextPage() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        List<StrategyListResponse> page1 = createTestStrategies(10);
        repositoryResult.setValue(page1);

        reset(mockRepository);
        MutableLiveData<List<StrategyListResponse>> page2Result = new MutableLiveData<>();
        when(mockRepository.getStrategies(anyString(), anyInt(), anyInt())).thenReturn(page2Result);

        viewModel.loadMore();

        verify(mockRepository).getStrategies("hot", 2, 10);

        List<StrategyListResponse> page2 = createTestStrategies(5);
        for (int i = 0; i < page2.size(); i++) {
            page2.get(i).setId(100L + i);
        }
        page2Result.setValue(page2);

        assertEquals(15, viewModel.getStrategies().getValue().size());
        assertTrue(viewModel.getIsLastPage().getValue());
    }

    @Test
    @DisplayName("loadMore 在最后一页时不加载")
    void loadMore_atLastPage_doesNotLoad() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        repositoryResult.setValue(createTestStrategies(3));
        reset(mockRepository);

        viewModel.loadMore();

        verify(mockRepository, never()).getStrategies(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("retry：应重新加载第一页")
    void retry_reloadsFirstPage() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        repositoryResult.setValue(Collections.emptyList());
        reset(mockRepository);
        MutableLiveData<List<StrategyListResponse>> retryResult = new MutableLiveData<>();
        when(mockRepository.getStrategies(anyString(), anyInt(), anyInt())).thenReturn(retryResult);

        viewModel.retry();

        verify(mockRepository).getStrategies("hot", 1, 10);
    }

    @Test
    @DisplayName("getLoading 初始值在构造后为 true")
    void getLoading_afterConstructor_isTrue() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        assertTrue(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("getError 初始值为 null")
    void getError_initialValueIsNull() {
        StrategyFeedViewModel viewModel = new StrategyFeedViewModel(mockApplication, mockRepository);

        assertNull(viewModel.getError().getValue());
    }

    private List<StrategyListResponse> createTestStrategies(int count) {
        List<StrategyListResponse> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StrategyListResponse s = new StrategyListResponse();
            s.setId((long) i);
            s.setTitle("攻略 " + i);
            s.setAuthorNickname("用户" + i);
            s.setUpvotes(i * 10);
            list.add(s);
        }
        return list;
    }
}
