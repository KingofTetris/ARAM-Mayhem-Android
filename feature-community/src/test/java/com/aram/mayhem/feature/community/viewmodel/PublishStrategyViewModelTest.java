package com.aram.mayhem.feature.community.viewmodel;

import android.app.Application;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.community.repository.StrategyRepository;
import com.aram.mayhem.network.dto.StrategyDetailResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublishStrategyViewModelTest {

    @Mock
    private Application mockApplication;

    @Mock
    private StrategyRepository mockRepository;

    private MutableLiveData<StrategyDetailResponse> publishResult;

    private PublishStrategyViewModel viewModel;

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

        publishResult = new MutableLiveData<>();
        when(mockRepository.publishStrategy(anyLong(), anyString(), anyString(), anyList(), anyList()))
                .thenReturn(publishResult);

        viewModel = new PublishStrategyViewModel(mockApplication, mockRepository);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @DisplayName("getPublishing 初始值为 false")
    void getPublishing_initialValueIsFalse() {
        assertFalse(viewModel.getPublishing().getValue());
    }

    @Test
    @DisplayName("getError 初始值为 null")
    void getError_initialValueIsNull() {
        assertNull(viewModel.getError().getValue());
    }

    @Test
    @DisplayName("getIsFormValid 初始值为 false")
    void getIsFormValid_initialValueIsFalse() {
        assertFalse(viewModel.getIsFormValid().getValue());
    }

    @Test
    @DisplayName("表单验证：所有字段正确时 isFormValid 为 true")
    void validateForm_allFieldsValid_isFormValidTrue() {
        viewModel.setSelectedHeroId(1L);
        viewModel.setTitle("测试攻略标题");
        viewModel.setDescription("这是一段超过十个字的攻略描述内容");

        assertTrue(viewModel.getIsFormValid().getValue());
    }

    @Test
    @DisplayName("表单验证：缺少英雄ID时 isFormValid 为 false")
    void validateForm_noHeroId_isFormValidFalse() {
        viewModel.setTitle("测试攻略标题");
        viewModel.setDescription("这是一段超过十个字的攻略描述内容");

        assertFalse(viewModel.getIsFormValid().getValue());
    }

    @Test
    @DisplayName("表单验证：标题为空时 isFormValid 为 false")
    void validateForm_emptyTitle_isFormValidFalse() {
        viewModel.setSelectedHeroId(1L);
        viewModel.setTitle("");
        viewModel.setDescription("这是一段超过十个字的攻略描述内容");

        assertFalse(viewModel.getIsFormValid().getValue());
    }

    @Test
    @DisplayName("表单验证：描述不足10字时 isFormValid 为 false")
    void validateForm_shortDescription_isFormValidFalse() {
        viewModel.setSelectedHeroId(1L);
        viewModel.setTitle("测试攻略标题");
        viewModel.setDescription("太短了");

        assertFalse(viewModel.getIsFormValid().getValue());
    }

    @Test
    @DisplayName("publish 成功：应设置 publishedStrategy")
    void publish_success_setsPublishedStrategy() {
        viewModel.setSelectedHeroId(1L);
        viewModel.setTitle("测试攻略标题");
        viewModel.setDescription("这是一段超过十个字的攻略描述内容");

        viewModel.publish();

        assertTrue(viewModel.getPublishing().getValue());

        StrategyDetailResponse detail = new StrategyDetailResponse();
        detail.setId(100L);
        detail.setTitle("测试攻略标题");
        publishResult.setValue(detail);

        assertFalse(viewModel.getPublishing().getValue());
        assertNotNull(viewModel.getPublishedStrategy().getValue());
        assertEquals(100L, viewModel.getPublishedStrategy().getValue().getId());
        assertNull(viewModel.getError().getValue());
    }

    @Test
    @DisplayName("publish 失败：应设置 error")
    void publish_failure_setsError() {
        viewModel.setSelectedHeroId(1L);
        viewModel.setTitle("测试攻略标题");
        viewModel.setDescription("这是一段超过十个字的攻略描述内容");

        viewModel.publish();

        publishResult.setValue(null);

        assertFalse(viewModel.getPublishing().getValue());
        assertEquals("发布失败，请重试", viewModel.getError().getValue());
    }

    @Test
    @DisplayName("publish 表单不完整：应设置 error 且不调用 Repository")
    void publish_incompleteForm_setsErrorAndDoesNotCallRepository() {
        viewModel.setTitle("测试攻略标题");

        viewModel.publish();

        assertEquals("请填写完整信息，描述至少10个字", viewModel.getError().getValue());
        verify(mockRepository, never()).publishStrategy(anyLong(), anyString(), anyString(), anyList(), anyList());
    }

    @Test
    @DisplayName("addAugment：应添加到选中列表")
    void addAugment_addsToSelectedList() {
        viewModel.addAugment(1L);
        viewModel.addAugment(2L);

        assertEquals(2, viewModel.getSelectedAugmentIds().getValue().size());
        assertTrue(viewModel.getSelectedAugmentIds().getValue().contains(1L));
        assertTrue(viewModel.getSelectedAugmentIds().getValue().contains(2L));
    }

    @Test
    @DisplayName("addAugment：重复添加不增加")
    void addAugment_duplicateNotAdded() {
        viewModel.addAugment(1L);
        viewModel.addAugment(1L);

        assertEquals(1, viewModel.getSelectedAugmentIds().getValue().size());
    }

    @Test
    @DisplayName("removeAugment：应从选中列表移除")
    void removeAugment_removesFromSelectedList() {
        viewModel.addAugment(1L);
        viewModel.addAugment(2L);
        viewModel.removeAugment(1L);

        assertEquals(1, viewModel.getSelectedAugmentIds().getValue().size());
        assertFalse(viewModel.getSelectedAugmentIds().getValue().contains(1L));
    }

    @Test
    @DisplayName("addItem：应添加到选中列表")
    void addItem_addsToSelectedList() {
        viewModel.addItem(10L);
        viewModel.addItem(20L);

        assertEquals(2, viewModel.getSelectedItemIds().getValue().size());
    }

    @Test
    @DisplayName("removeItem：应从选中列表移除")
    void removeItem_removesFromSelectedList() {
        viewModel.addItem(10L);
        viewModel.removeItem(10L);

        assertTrue(viewModel.getSelectedItemIds().getValue().isEmpty());
    }

    @Test
    @DisplayName("reset：应清空所有表单数据")
    void reset_clearsAllFormData() {
        viewModel.setSelectedHeroId(1L);
        viewModel.setTitle("标题");
        viewModel.setDescription("描述内容超过十个字");
        viewModel.addAugment(1L);
        viewModel.addItem(10L);

        viewModel.reset();

        assertNull(viewModel.getSelectedHeroId().getValue());
        assertNull(viewModel.getTitle().getValue());
        assertNull(viewModel.getDescription().getValue());
        assertTrue(viewModel.getSelectedAugmentIds().getValue().isEmpty());
        assertTrue(viewModel.getSelectedItemIds().getValue().isEmpty());
        assertNull(viewModel.getError().getValue());
        assertNull(viewModel.getPublishedStrategy().getValue());
        assertFalse(viewModel.getIsFormValid().getValue());
    }
}
