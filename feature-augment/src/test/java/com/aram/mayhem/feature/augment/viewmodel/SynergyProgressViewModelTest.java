package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.network.dto.SynergyProgressResponse;

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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SynergyProgressViewModelTest {

    @Mock
    private Application mockApplication;

    @Mock
    private AugmentRepository mockRepository;

    private MutableLiveData<List<SynergyProgressResponse>> progressResult;

    private SynergyProgressViewModel viewModel;

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

        progressResult = new MutableLiveData<>();
        when(mockRepository.getSynergyProgress(anyList())).thenReturn(progressResult);

        viewModel = new SynergyProgressViewModel(mockApplication, mockRepository);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @DisplayName("getSynergyProgress 初始值为 null")
    void getSynergyProgress_initialValueIsNull() {
        assertNull(viewModel.getSynergyProgress().getValue());
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
    @DisplayName("setSelectedAugmentIds 空列表：应设置空进度且不调用 Repository")
    void setSelectedAugmentIds_emptyList_setsEmptyProgress() {
        viewModel.setSelectedAugmentIds(Collections.emptyList());

        assertTrue(viewModel.getSynergyProgress().getValue().isEmpty());
        verify(mockRepository, never()).getSynergyProgress(anyList());
    }

    @Test
    @DisplayName("setSelectedAugmentIds 非空列表：应调用 Repository 加载进度")
    void setSelectedAugmentIds_nonEmptyList_callsRepository() {
        viewModel.setSelectedAugmentIds(new ArrayList<>(Arrays.asList(1L, 2L, 3L)));

        verify(mockRepository).getSynergyProgress(new ArrayList<>(Arrays.asList(1L, 2L, 3L)));
        assertTrue(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("loadSynergyProgress 成功：应更新 synergyProgress")
    void loadSynergyProgress_success_updatesSynergyProgress() {
        viewModel.setSelectedAugmentIds(new ArrayList<>(Arrays.asList(1L, 2L)));

        List<SynergyProgressResponse> progress = createTestProgress();
        progressResult.setValue(progress);

        assertFalse(viewModel.getLoading().getValue());
        assertNotNull(viewModel.getSynergyProgress().getValue());
        assertEquals(2, viewModel.getSynergyProgress().getValue().size());
    }

    @Test
    @DisplayName("loadSynergyProgress 返回 null：应设置空列表")
    void loadSynergyProgress_nullResult_setsEmptyList() {
        viewModel.setSelectedAugmentIds(new ArrayList<>(Arrays.asList(1L)));

        progressResult.setValue(null);

        assertFalse(viewModel.getLoading().getValue());
        assertTrue(viewModel.getSynergyProgress().getValue().isEmpty());
    }

    @Test
    @DisplayName("addAugment 新 ID：应添加并重新加载")
    void addAugment_newId_addsAndReloads() {
        viewModel.setSelectedAugmentIds(new ArrayList<>(Arrays.asList(1L)));
        progressResult.setValue(createTestProgress());
        reset(mockRepository);
        when(mockRepository.getSynergyProgress(anyList())).thenReturn(progressResult);

        viewModel.addAugment(2L);

        assertTrue(viewModel.getSelectedAugmentIds().contains(2L));
        verify(mockRepository).getSynergyProgress(anyList());
    }

    @Test
    @DisplayName("addAugment 重复 ID：不重新加载")
    void addAugment_duplicateId_doesNotReload() {
        viewModel.setSelectedAugmentIds(new ArrayList<>(Arrays.asList(1L)));
        progressResult.setValue(createTestProgress());
        reset(mockRepository);

        viewModel.addAugment(1L);

        verify(mockRepository, never()).getSynergyProgress(anyList());
    }

    @Test
    @DisplayName("removeAugment：应移除并重新加载")
    void removeAugment_removesAndReloads() {
        viewModel.setSelectedAugmentIds(new ArrayList<>(Arrays.asList(1L, 2L)));
        progressResult.setValue(createTestProgress());
        reset(mockRepository);
        when(mockRepository.getSynergyProgress(anyList())).thenReturn(progressResult);

        viewModel.removeAugment(1L);

        assertFalse(viewModel.getSelectedAugmentIds().contains(1L));
        verify(mockRepository).getSynergyProgress(anyList());
    }

    @Test
    @DisplayName("getSelectedAugmentIds 返回副本而非原始列表")
    void getSelectedAugmentIds_returnsDefensiveCopy() {
        viewModel.setSelectedAugmentIds(new ArrayList<>(Arrays.asList(1L, 2L)));

        List<Long> ids = viewModel.getSelectedAugmentIds();
        ids.add(999L);

        assertEquals(2, viewModel.getSelectedAugmentIds().size());
    }

    private List<SynergyProgressResponse> createTestProgress() {
        List<SynergyProgressResponse> list = new ArrayList<>();
        SynergyProgressResponse p1 = new SynergyProgressResponse();
        SynergyProgressResponse p2 = new SynergyProgressResponse();
        list.add(p1);
        list.add(p2);
        return list;
    }
}
