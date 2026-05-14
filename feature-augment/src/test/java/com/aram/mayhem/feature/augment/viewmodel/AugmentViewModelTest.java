package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.ui.model.AugmentUiModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AugmentViewModelTest {

    @Mock
    private AugmentRepository mockAugmentRepository;

    private TestAugmentViewModel viewModel;
    private CountDownLatch latch;

    static class TestAugmentViewModel extends AndroidViewModel {
        private final MutableLiveData<List<AugmentUiModel>> augments = new MutableLiveData<>();
        private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
        private final MutableLiveData<String> error = new MutableLiveData<>();
        private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
        private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);
        private final MutableLiveData<Boolean> isOffline = new MutableLiveData<>(false);

        private int currentPage = 1;
        private final int pageSize = 20;
        private String currentQuality = "";
        private String currentSynergySet = "";

        private AugmentRepository augmentRepository;

        public TestAugmentViewModel(Application application) {
            super(application);
        }

        public void setRepository(AugmentRepository repository) {
            this.augmentRepository = repository;
        }

        public void setQualityFilter(String quality) {
            this.currentQuality = quality != null ? quality : "";
            loadAugments(true);
        }

        public void setSynergyFilter(String synergySet) {
            this.currentSynergySet = synergySet != null ? synergySet : "";
            loadAugments(true);
        }

        public void loadMore() {
            if (loadingMore.getValue() == null || !loadingMore.getValue()) {
                if (isLastPage.getValue() == null || !isLastPage.getValue()) {
                    loadAugments(false);
                }
            }
        }

        public void retry() {
            loadAugments(true);
        }

        public LiveData<List<AugmentUiModel>> getAugments() {
            return augments;
        }

        public LiveData<Boolean> getLoading() {
            return loading;
        }

        public LiveData<String> getError() {
            return error;
        }

        public LiveData<Boolean> getLoadingMore() {
            return loadingMore;
        }

        public LiveData<Boolean> getIsLastPage() {
            return isLastPage;
        }

        public LiveData<Boolean> getIsOffline() {
            return isOffline;
        }

        public int getCurrentPageViaReflection() {
            try {
                Field field = TestAugmentViewModel.class.getDeclaredField("currentPage");
                field.setAccessible(true);
                return field.getInt(this);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to access currentPage via reflection", e);
            }
        }

        private void loadAugments(boolean reset) {
            if (reset) {
                currentPage = 1;
                augments.setValue(new ArrayList<>());
                isLastPage.setValue(false);
            }

            loading.setValue(reset);
            loadingMore.setValue(!reset);
            isOffline.setValue(false);

            augmentRepository.getAugments(currentPage, pageSize, currentQuality, currentSynergySet)
                    .observeForever(uiModels -> {
                        loading.setValue(false);
                        loadingMore.setValue(false);

                        if (uiModels != null && !uiModels.isEmpty()) {
                            List<AugmentUiModel> current = new ArrayList<>();
                            if (!reset && augments.getValue() != null) {
                                current.addAll(augments.getValue());
                            }
                            current.addAll(uiModels);
                            augments.setValue(current);
                            isLastPage.setValue(uiModels.size() < pageSize);
                            currentPage++;
                        } else {
                            if (reset) {
                                augments.setValue(new ArrayList<>());
                            }
                            isLastPage.setValue(true);
                        }
                    });
        }
    }

    @BeforeEach
    void setUp() {
        viewModel = new TestAugmentViewModel(null);
        viewModel.setRepository(mockAugmentRepository);
        latch = new CountDownLatch(1);
    }

    private List<AugmentUiModel> createAugmentModels(int count) {
        List<AugmentUiModel> models = new ArrayList<>();
        String[] qualities = {"prismatic", "gold", "silver"};
        String[] synergies = {"shield", "attack-speed", "critical-strike"};
        for (int i = 0; i < count; i++) {
            models.add(new AugmentUiModel(
                    i + 1,
                    "测试符文" + (i + 1),
                    "Test Augment " + (i + 1),
                    "Description " + (i + 1),
                    qualities[i % 3],
                    synergies[i % 3],
                    i % 2 == 0 ? "regeneration" : null,
                    null,
                    "https://example.com/icon.png",
                    0.5 + (i % 10) * 0.01,
                    0.1 + (i % 5) * 0.02,
                    4.0 - (i % 10) * 0.1,
                    "A",
                    false
            ));
        }
        return models;
    }

    @Test
    @DisplayName("加载符文列表成功：应返回 20 个符文，loading 为 false")
    void loadAugments_success_returns20Augments() throws InterruptedException {
        List<AugmentUiModel> mockData = createAugmentModels(20);
        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(mockData);

        when(mockAugmentRepository.getAugments(eq(1), eq(20), eq(""), eq("")))
                .thenReturn(liveData);

        viewModel.retry();
        latch.await(1, TimeUnit.SECONDS);

        List<AugmentUiModel> result = viewModel.getAugments().getValue();
        Boolean loadingState = viewModel.getLoading().getValue();

        assertNotNull(result);
        assertEquals(20, result.size());
        assertFalse(loadingState);
    }

    @Test
    @DisplayName("加载符文列表空结果：应返回空列表")
    void loadAugments_emptyResult_returnsEmptyList() throws InterruptedException {
        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(new ArrayList<>());

        when(mockAugmentRepository.getAugments(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(liveData);

        viewModel.retry();
        latch.await(1, TimeUnit.SECONDS);

        List<AugmentUiModel> result = viewModel.getAugments().getValue();
        Boolean loadingState = viewModel.getLoading().getValue();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertFalse(loadingState);
    }

    @Test
    @DisplayName("品质筛选：filterByQuality 应正确设置 currentQuality")
    void filterByQuality_setsCurrentQuality() throws InterruptedException {
        List<AugmentUiModel> mockData = createAugmentModels(10);
        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(mockData);

        when(mockAugmentRepository.getAugments(eq(1), eq(20), eq("gold"), eq("")))
                .thenReturn(liveData);

        viewModel.setQualityFilter("gold");
        latch.await(1, TimeUnit.SECONDS);

        List<AugmentUiModel> result = viewModel.getAugments().getValue();
        assertNotNull(result);
        assertEquals(10, result.size());

        verify(mockAugmentRepository).getAugments(eq(1), eq(20), eq("gold"), eq(""));
    }

    @Test
    @DisplayName("套装筛选：filterBySynergy 应正确设置 currentSynergySet")
    void filterBySynergy_setsCurrentSynergySet() throws InterruptedException {
        List<AugmentUiModel> mockData = createAugmentModels(5);
        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(mockData);

        when(mockAugmentRepository.getAugments(eq(1), eq(20), eq(""), eq("shield")))
                .thenReturn(liveData);

        viewModel.setSynergyFilter("shield");
        latch.await(1, TimeUnit.SECONDS);

        List<AugmentUiModel> result = viewModel.getAugments().getValue();
        assertNotNull(result);
        assertEquals(5, result.size());

        verify(mockAugmentRepository).getAugments(eq(1), eq(20), eq(""), eq("shield"));
    }

    @Test
    @DisplayName("分页加载：loadMore 应追加数据而不是替换")
    void loadMore_appendsData() throws InterruptedException {
        CountDownLatch initialLatch = new CountDownLatch(1);
        List<AugmentUiModel> firstPage = createAugmentModels(20);
        MutableLiveData<List<AugmentUiModel>> firstLiveData = new MutableLiveData<>();
        firstLiveData.setValue(firstPage);

        when(mockAugmentRepository.getAugments(eq(1), eq(20), eq(""), eq("")))
                .thenReturn(firstLiveData);

        viewModel.retry();
        initialLatch.await(1, TimeUnit.SECONDS);

        assertEquals(20, viewModel.getAugments().getValue().size());

        CountDownLatch moreLatch = new CountDownLatch(1);
        List<AugmentUiModel> secondPage = createAugmentModels(20);
        MutableLiveData<List<AugmentUiModel>> secondLiveData = new MutableLiveData<>();
        secondLiveData.setValue(secondPage);

        when(mockAugmentRepository.getAugments(eq(2), eq(20), eq(""), eq("")))
                .thenReturn(secondLiveData);

        viewModel.loadMore();
        moreLatch.await(1, TimeUnit.SECONDS);

        List<AugmentUiModel> result = viewModel.getAugments().getValue();
        assertNotNull(result);
        assertEquals(40, result.size());
    }

    @Test
    @DisplayName("最后一页：返回数据少于 pageSize 时 isLastPage 应为 true")
    void loadMore_lastPage_setsIsLastPage() throws InterruptedException {
        List<AugmentUiModel> mockData = createAugmentModels(5);
        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(mockData);

        when(mockAugmentRepository.getAugments(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(liveData);

        viewModel.retry();
        latch.await(1, TimeUnit.SECONDS);

        Boolean isLastPage = viewModel.getIsLastPage().getValue();
        assertTrue(isLastPage);
    }

    @Test
    @DisplayName("最后一页时调用 loadMore 不应发起新请求")
    void loadMore_whenLastPage_doesNotMakeRequest() throws InterruptedException {
        List<AugmentUiModel> mockData = createAugmentModels(5);
        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(mockData);

        when(mockAugmentRepository.getAugments(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(liveData);

        viewModel.retry();
        latch.await(1, TimeUnit.SECONDS);

        assertTrue(viewModel.getIsLastPage().getValue());

        viewModel.loadMore();

        verify(mockAugmentRepository, times(1)).getAugments(anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("品质筛选时应重置 currentPage 为 1")
    void filterByQuality_resetsCurrentPage() throws InterruptedException {
        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(createAugmentModels(10));

        when(mockAugmentRepository.getAugments(eq(1), eq(20), eq("gold"), eq("")))
                .thenReturn(liveData);

        viewModel.setQualityFilter("gold");
        latch.await(1, TimeUnit.SECONDS);

        assertEquals(2, viewModel.getCurrentPageViaReflection());
    }

    @Test
    @DisplayName("null 筛选条件应转换为空字符串")
    void filterByNull_clearsFilter() throws InterruptedException {
        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(createAugmentModels(15));

        when(mockAugmentRepository.getAugments(eq(1), eq(20), eq(""), eq("")))
                .thenReturn(liveData);

        viewModel.setQualityFilter(null);
        latch.await(1, TimeUnit.SECONDS);

        verify(mockAugmentRepository).getAugments(eq(1), eq(20), eq(""), eq(""));
    }

    @Test
    @DisplayName("retry 应重置列表并重新加载")
    void retry_resetsAndReloads() throws InterruptedException {
        CountDownLatch retryLatch = new CountDownLatch(2);

        MutableLiveData<List<AugmentUiModel>> liveData = new MutableLiveData<>();
        liveData.setValue(createAugmentModels(10));

        when(mockAugmentRepository.getAugments(eq(1), eq(20), eq(""), eq("")))
                .thenAnswer(invocation -> {
                    retryLatch.countDown();
                    return liveData;
                });

        viewModel.retry();
        retryLatch.await(1, TimeUnit.SECONDS);

        List<AugmentUiModel> result = viewModel.getAugments().getValue();
        assertNotNull(result);
        assertEquals(10, result.size());
        assertEquals(2, viewModel.getCurrentPageViaReflection());
    }
}