package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Tier;
import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.aram.mayhem.network.dto.SynergyProgressResponse;
import com.aram.mayhem.ui.model.HeroUiModel;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AugmentRecommendViewModelTest {

    @Mock
    private Application mockApplication;

    @Mock
    private AugmentRepository mockRepository;

    private MutableLiveData<List<SynergyProgressResponse>> progressResult;
    private MutableLiveData<List<AugmentRecommendResponse>> recommendResult;

    private AugmentRecommendViewModel viewModel;

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
        recommendResult = new MutableLiveData<>();
        when(mockRepository.getSynergyProgress(anyList())).thenReturn(progressResult);
        when(mockRepository.getRecommendations(anyLong(), anyList())).thenReturn(recommendResult);

        viewModel = new AugmentRecommendViewModel(mockApplication, mockRepository);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @DisplayName("getSelectedHero 初始值为 null")
    void getSelectedHero_initialValueIsNull() {
        assertNull(viewModel.getSelectedHero().getValue());
    }

    @Test
    @DisplayName("getLoading 初始值为 false")
    void getLoading_initialValueIsFalse() {
        assertFalse(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("getSelectedAugmentIds 初始值为空列表")
    void getSelectedAugmentIds_initialValueIsEmpty() {
        assertTrue(viewModel.getSelectedAugmentIds().getValue().isEmpty());
    }

    @Test
    @DisplayName("setSelectedHero 设置英雄")
    void setSelectedHero_setsHero() {
        HeroUiModel hero = createTestHero(1L, "艾希");
        viewModel.setSelectedHero(hero);

        assertNotNull(viewModel.getSelectedHero().getValue());
        assertEquals(1L, viewModel.getSelectedHero().getValue().getId());
    }

    @Test
    @DisplayName("addSelectedAugment 新 ID：应添加到列表")
    void addSelectedAugment_newId_addsToList() {
        viewModel.addSelectedAugment(1L);

        assertEquals(1, viewModel.getSelectedAugmentIds().getValue().size());
        assertTrue(viewModel.getSelectedAugmentIds().getValue().contains(1L));
    }

    @Test
    @DisplayName("addSelectedAugment 重复 ID：不添加")
    void addSelectedAugment_duplicateId_notAdded() {
        viewModel.addSelectedAugment(1L);
        viewModel.addSelectedAugment(1L);

        assertEquals(1, viewModel.getSelectedAugmentIds().getValue().size());
    }

    @Test
    @DisplayName("removeSelectedAugment：应从列表移除")
    void removeSelectedAugment_removesFromList() {
        viewModel.addSelectedAugment(1L);
        viewModel.addSelectedAugment(2L);
        viewModel.removeSelectedAugment(1L);

        assertEquals(1, viewModel.getSelectedAugmentIds().getValue().size());
        assertFalse(viewModel.getSelectedAugmentIds().getValue().contains(1L));
    }

    @Test
    @DisplayName("setSelectedHero 无符文：不调用 Repository")
    void setSelectedHero_noAugments_doesNotCallRepository() {
        HeroUiModel hero = createTestHero(1L, "艾希");
        viewModel.setSelectedHero(hero);

        verify(mockRepository, never()).getSynergyProgress(anyList());
        verify(mockRepository, never()).getRecommendations(anyLong(), anyList());
    }

    @Test
    @DisplayName("addSelectedAugment 有英雄时：应触发数据刷新")
    void addSelectedAugment_withHero_triggersDataRefresh() {
        HeroUiModel hero = createTestHero(1L, "艾希");
        viewModel.setSelectedHero(hero);

        viewModel.addSelectedAugment(10L);

        verify(mockRepository).getSynergyProgress(anyList());
        verify(mockRepository).getRecommendations(eq(1L), anyList());
        assertTrue(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("数据刷新成功：应更新 synergyProgress 和 recommendations")
    void refreshData_success_updatesProgressAndRecommendations() {
        HeroUiModel hero = createTestHero(1L, "艾希");
        viewModel.setSelectedHero(hero);
        viewModel.addSelectedAugment(10L);

        List<SynergyProgressResponse> progress = createTestProgress();
        progressResult.setValue(progress);

        List<AugmentRecommendResponse> recs = createTestRecommendations();
        recommendResult.setValue(recs);

        assertFalse(viewModel.getLoading().getValue());
        assertNotNull(viewModel.getSynergyProgress().getValue());
        assertNotNull(viewModel.getRecommendations().getValue());
        assertEquals(2, viewModel.getSynergyProgress().getValue().size());
        assertEquals(3, viewModel.getRecommendations().getValue().size());
    }

    @Test
    @DisplayName("无英雄有符文：不调用推荐接口")
    void noHeroWithAugments_doesNotCallRecommendations() {
        viewModel.addSelectedAugment(10L);

        verify(mockRepository, never()).getRecommendations(anyLong(), anyList());
    }

    private HeroUiModel createTestHero(long id, String name) {
        return new HeroUiModel(id, name, "Ashe", "Frost Archer",
                "Marksman", Tier.A, 52.5, 8.3, "/icons/ashe.png");
    }

    private List<SynergyProgressResponse> createTestProgress() {
        List<SynergyProgressResponse> list = new ArrayList<>();
        SynergyProgressResponse p1 = new SynergyProgressResponse();
        SynergyProgressResponse p2 = new SynergyProgressResponse();
        list.add(p1);
        list.add(p2);
        return list;
    }

    private List<AugmentRecommendResponse> createTestRecommendations() {
        List<AugmentRecommendResponse> list = new ArrayList<>();
        AugmentRecommendResponse r1 = new AugmentRecommendResponse();
        AugmentRecommendResponse r2 = new AugmentRecommendResponse();
        AugmentRecommendResponse r3 = new AugmentRecommendResponse();
        list.add(r1);
        list.add(r2);
        list.add(r3);
        return list;
    }
}
