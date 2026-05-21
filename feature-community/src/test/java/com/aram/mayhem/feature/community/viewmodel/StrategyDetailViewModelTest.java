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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StrategyDetailViewModelTest {

    @Mock
    private Application mockApplication;

    @Mock
    private StrategyRepository mockRepository;

    private MutableLiveData<StrategyDetailResponse> detailResult;
    private MutableLiveData<Boolean> voteResult;
    private MutableLiveData<Boolean> cancelVoteResult;

    private StrategyDetailViewModel viewModel;

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

        detailResult = new MutableLiveData<>();
        voteResult = new MutableLiveData<>();
        cancelVoteResult = new MutableLiveData<>();

        when(mockRepository.getStrategyDetail(anyLong())).thenReturn(detailResult);
        when(mockRepository.vote(anyLong(), anyString())).thenReturn(voteResult);
        when(mockRepository.cancelVote(anyLong())).thenReturn(cancelVoteResult);

        viewModel = new StrategyDetailViewModel(mockApplication, mockRepository);
    }

    @AfterEach
    void tearDown() {
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @DisplayName("getStrategy 初始值为 null")
    void getStrategy_initialValueIsNull() {
        assertNull(viewModel.getStrategy().getValue());
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
    @DisplayName("loadStrategy 成功：应设置 strategy LiveData")
    void loadStrategy_success_setsStrategy() {
        viewModel.loadStrategy(1L);

        assertTrue(viewModel.getLoading().getValue());

        StrategyDetailResponse detail = createTestDetail(1L, "测试攻略", 10, 2, "UP");
        detailResult.setValue(detail);

        assertFalse(viewModel.getLoading().getValue());
        assertNotNull(viewModel.getStrategy().getValue());
        assertEquals(1L, viewModel.getStrategy().getValue().getId());
        assertEquals("测试攻略", viewModel.getStrategy().getValue().getTitle());
        assertEquals("UP", viewModel.getCurrentVoteType().getValue());
        assertNull(viewModel.getError().getValue());
    }

    @Test
    @DisplayName("loadStrategy 失败：应设置 error")
    void loadStrategy_failure_setsError() {
        viewModel.loadStrategy(1L);

        detailResult.setValue(null);

        assertFalse(viewModel.getLoading().getValue());
        assertNull(viewModel.getStrategy().getValue());
        assertEquals("加载失败，请重试", viewModel.getError().getValue());
    }

    @Test
    @DisplayName("loadStrategy 相同 ID 且已有数据不重复加载")
    void loadStrategy_sameIdWithCachedData_doesNotReload() {
        viewModel.loadStrategy(1L);
        detailResult.setValue(createTestDetail(1L, "攻略", 5, 0, null));
        reset(mockRepository);

        viewModel.loadStrategy(1L);

        verify(mockRepository, never()).getStrategyDetail(anyLong());
    }

    @Test
    @DisplayName("loadStrategy 不同 ID 重新加载")
    void loadStrategy_differentId_reloads() {
        viewModel.loadStrategy(1L);
        detailResult.setValue(createTestDetail(1L, "攻略1", 5, 0, null));

        MutableLiveData<StrategyDetailResponse> newDetailResult = new MutableLiveData<>();
        when(mockRepository.getStrategyDetail(2L)).thenReturn(newDetailResult);

        viewModel.loadStrategy(2L);

        verify(mockRepository).getStrategyDetail(2L);
    }

    @Test
    @DisplayName("vote 成功：应更新 currentVoteType 和投票计数")
    void vote_success_updatesVoteTypeAndCount() {
        viewModel.loadStrategy(1L);
        detailResult.setValue(createTestDetail(1L, "攻略", 10, 2, null));

        viewModel.vote("UP");
        voteResult.setValue(true);

        assertTrue(viewModel.getVoteSuccess().getValue());
        assertEquals("UP", viewModel.getCurrentVoteType().getValue());
        assertEquals(11, viewModel.getStrategy().getValue().getUpvotes());
    }

    @Test
    @DisplayName("vote DOWN 成功：应更新 downvotes")
    void vote_down_success_updatesDownvotes() {
        viewModel.loadStrategy(1L);
        detailResult.setValue(createTestDetail(1L, "攻略", 10, 2, null));

        viewModel.vote("DOWN");
        voteResult.setValue(true);

        assertEquals("DOWN", viewModel.getCurrentVoteType().getValue());
        assertEquals(3, viewModel.getStrategy().getValue().getDownvotes());
    }

    @Test
    @DisplayName("vote 失败：不更新投票状态")
    void vote_failure_doesNotUpdateState() {
        viewModel.loadStrategy(1L);
        detailResult.setValue(createTestDetail(1L, "攻略", 10, 2, null));

        viewModel.vote("UP");
        voteResult.setValue(false);

        assertFalse(viewModel.getVoteSuccess().getValue());
        assertNull(viewModel.getCurrentVoteType().getValue());
        assertEquals(10, viewModel.getStrategy().getValue().getUpvotes());
    }

    @Test
    @DisplayName("cancelVote 成功：应清空 currentVoteType 并减少计数")
    void cancelVote_success_clearsVoteTypeAndDecrementsCount() {
        viewModel.loadStrategy(1L);
        detailResult.setValue(createTestDetail(1L, "攻略", 10, 2, "UP"));

        viewModel.cancelVote();
        cancelVoteResult.setValue(true);

        assertTrue(viewModel.getVoteSuccess().getValue());
        assertNull(viewModel.getCurrentVoteType().getValue());
        assertEquals(9, viewModel.getStrategy().getValue().getUpvotes());
    }

    @Test
    @DisplayName("cancelVote 无当前攻略 ID 不执行")
    void cancelVote_noCurrentStrategy_doesNotCallRepository() {
        viewModel.cancelVote();

        verify(mockRepository, never()).cancelVote(anyLong());
    }

    @Test
    @DisplayName("vote 无当前攻略 ID 不执行")
    void vote_noCurrentStrategy_doesNotCallRepository() {
        viewModel.vote("UP");

        verify(mockRepository, never()).vote(anyLong(), anyString());
    }

    private StrategyDetailResponse createTestDetail(long id, String title, int upvotes, int downvotes, String userVoteType) {
        StrategyDetailResponse detail = new StrategyDetailResponse();
        detail.setId(id);
        detail.setTitle(title);
        detail.setUpvotes(upvotes);
        detail.setDownvotes(downvotes);
        detail.setUserVoteType(userVoteType);
        detail.setAuthorNickname("测试用户");
        detail.setDescription("测试内容");
        return detail;
    }
}
