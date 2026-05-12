package com.aram.mayhem.feature.hero.viewmodel;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.common.Tier;
import com.aram.mayhem.network.api.HeroApi;
import com.aram.mayhem.network.dto.HeroResponse;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.ui.model.HeroUiModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * HeroListViewModel 单元测试
 * 
 * 测试覆盖：
 * 1. 加载英雄列表 - 成功
 * 2. 加载英雄列表 - 空结果
 * 3. 加载英雄列表 - 网络失败
 * 4. 分页加载 - 加载更多
 * 5. 分页加载 - 最后一页
 * 6. 搜索功能
 * 7. 梯级筛选
 * 8. 重试功能
 * 
 * 技术说明：
 * - 使用 TestHeroListViewModel 子类绕过 Android Application 上下文依赖
 * - 使用反射访问 ViewModel 内部状态（currentPage）以验证分页逻辑
 * - 使用 CountDownLatch 等待 Retrofit 异步回调完成
 * - 使用自定义子类（HeroResponseWithValues/PageResponseWithValues）构造测试数据
 */
@ExtendWith(MockitoExtension.class)
class HeroListViewModelTest {

    @Mock
    private HeroApi mockHeroApi;

    @Mock
    private Call<Result<PageResponse<HeroResponse>>> mockCall;

    private TestHeroListViewModel viewModel;

    // 测试辅助工具：用于等待异步回调完成
    private CountDownLatch latch;

    @BeforeEach
    void setUp() {
        viewModel = new TestHeroListViewModel(mockHeroApi);
        latch = new CountDownLatch(1);
    }

    // ============================================================
    // 测试场景 1：加载英雄列表 - 成功
    // ============================================================

    @Test
    @DisplayName("加载英雄列表成功：应返回 20 个英雄，loading 为 false")
    void loadHeroes_success_returns20Heroes() throws InterruptedException {
        // Given：模拟网络返回 20 个英雄
        List<HeroResponse> heroResponses = createHeroResponses(20);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(heroResponses, 100);

        // Mock API 调用
        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        // 重置初始加载（构造函数已自动加载，这里重新触发）
        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        // When：获取 LiveData 值
        List<HeroUiModel> heroes = viewModel.getHeroes().getValue();
        Boolean loading = viewModel.getLoading().getValue();
        Boolean lastPage = viewModel.getIsLastPage().getValue();

        // Then：验证结果
        assertNotNull(heroes);
        assertEquals(20, heroes.size());
        assertFalse(loading);
        // 总共有 100 个，当前 20 个，未到最后一页
        assertFalse(lastPage);

        // 验证 API 调用参数
        verify(mockHeroApi).getHeroes(eq(""), eq(""), eq("winRate"), eq(1), eq(20));
    }

    // ============================================================
    // 测试场景 2：加载英雄列表 - 空结果
    // ============================================================

    @Test
    @DisplayName("加载英雄列表空结果：应返回空列表")
    void loadHeroes_emptyResult_returnsEmptyList() throws InterruptedException {
        // Given：模拟网络返回空列表
        List<HeroResponse> emptyResponses = new ArrayList<>();
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(emptyResponses, 0);

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        // When
        List<HeroUiModel> heroes = viewModel.getHeroes().getValue();
        Boolean loading = viewModel.getLoading().getValue();

        // Then
        assertNotNull(heroes);
        assertTrue(heroes.isEmpty());
        assertFalse(loading);
    }

    // ============================================================
    // 测试场景 3：加载英雄列表 - 网络失败
    // ============================================================

    @Test
    @DisplayName("加载英雄列表网络失败：error LiveData 应包含错误信息")
    void loadHeroes_networkFailure_setsError() throws InterruptedException {
        // Given：模拟网络失败
        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onFailure(mockCall, new IOException("Connection timeout"));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        // When
        String error = viewModel.getError().getValue();
        Boolean loading = viewModel.getLoading().getValue();

        // Then
        assertNotNull(error);
        assertEquals("Connection timeout", error);
        assertFalse(loading);
    }

    // ============================================================
    // 测试场景 3b：加载英雄列表 - 服务端错误
    // ============================================================

    @Test
    @DisplayName("加载英雄列表服务端错误：error LiveData 应包含服务端返回的错误信息")
    void loadHeroes_serverError_setsErrorMessage() throws InterruptedException {
        // Given：模拟服务端返回错误响应（Result.code != 200）
        Result<PageResponse<HeroResponse>> errorResult = Result.error(500, "Internal Server Error");

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(errorResult));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        // When
        String error = viewModel.getError().getValue();
        Boolean loading = viewModel.getLoading().getValue();

        // Then
        assertNotNull(error);
        assertEquals("Internal Server Error", error);
        assertFalse(loading);
    }

    // ============================================================
    // 测试场景 4：分页加载 - 加载更多
    // ============================================================

    @Test
    @DisplayName("分页加载更多：初始 20 个 + 再加载 20 个 = 40 个")
    void loadMore_appendsNextPage() throws InterruptedException {
        // Given：初始加载 20 个
        List<HeroResponse> firstPage = createHeroResponses(20);
        Result<PageResponse<HeroResponse>> firstResult = createSuccessPageResult(firstPage, 100);

        CountDownLatch initialLatch = new CountDownLatch(1);
        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(firstResult));
            initialLatch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        initialLatch.await(1, TimeUnit.SECONDS);

        // 验证初始状态
        assertEquals(20, viewModel.getHeroes().getValue().size());
        assertEquals(2, viewModel.getCurrentPageViaReflection()); // 初始加载后 currentPage 已自增

        // When：调用 loadMore() 加载第二页
        CountDownLatch moreLatch = new CountDownLatch(1);
        List<HeroResponse> secondPage = createHeroResponses(20);
        Result<PageResponse<HeroResponse>> secondResult = createSuccessPageResult(secondPage, 100);

        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(secondResult));
            moreLatch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.loadMore();
        moreLatch.await(1, TimeUnit.SECONDS);

        // Then：验证合并结果
        List<HeroUiModel> heroes = viewModel.getHeroes().getValue();
        Boolean loadingMore = viewModel.getLoadingMore().getValue();

        assertNotNull(heroes);
        assertEquals(40, heroes.size());
        assertFalse(loadingMore);

        // 验证第二页请求参数
        verify(mockHeroApi, times(2)).getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt());
        verify(mockHeroApi, times(1)).getHeroes(eq(""), eq(""), eq("winRate"), eq(2), eq(20));
    }

    // ============================================================
    // 测试场景 5：分页加载 - 最后一页
    // ============================================================

    @Test
    @DisplayName("分页加载最后一页：返回数据少于 pageSize 时 isLastPage 为 true")
    void loadMore_lastPage_setsIsLastPage() throws InterruptedException {
        // Given：初始加载 20 个
        List<HeroResponse> firstPage = createHeroResponses(20);
        Result<PageResponse<HeroResponse>> firstResult = createSuccessPageResult(firstPage, 100);

        CountDownLatch initialLatch = new CountDownLatch(1);
        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(firstResult));
            initialLatch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        initialLatch.await(1, TimeUnit.SECONDS);

        // When：加载第二页，但只返回 5 条（少于 pageSize）
        CountDownLatch lastLatch = new CountDownLatch(1);
        List<HeroResponse> lastPage = createHeroResponses(5);
        Result<PageResponse<HeroResponse>> lastResult = createSuccessPageResult(lastPage, 25);

        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(lastResult));
            lastLatch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.loadMore();
        lastLatch.await(1, TimeUnit.SECONDS);

        // Then：验证 isLastPage 为 true
        assertTrue(viewModel.getIsLastPage().getValue());

        // 再次调用 loadMore() 不应发起新请求
        viewModel.loadMore();
        verify(mockHeroApi, times(2)).getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("最后一页时调用 loadMore 不应发起新请求")
    void loadMore_whenLastPage_doesNotMakeRequest() throws InterruptedException {
        // Given：初始加载后标记为最后一页（返回 20 个，total 也是 20）
        List<HeroResponse> firstPage = createHeroResponses(20);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(firstPage, 20);

        CountDownLatch initialLatch = new CountDownLatch(1);
        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            initialLatch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        initialLatch.await(1, TimeUnit.SECONDS);

        // 验证初始加载后 isLastPage 为 true
        assertTrue(viewModel.getIsLastPage().getValue());

        // When：调用 loadMore
        viewModel.loadMore();

        // Then：不应有新的 API 调用（总共只有初始 1 次）
        verify(mockHeroApi, times(1)).getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt());
    }

    // ============================================================
    // 测试场景 6：搜索功能
    // ============================================================

    @Test
    @DisplayName("搜索英雄：keyword 参数正确传递，currentPage 重置为 1")
    void searchHeroes_passesCorrectKeyword() throws InterruptedException {
        // Given
        List<HeroResponse> searchResults = createHeroResponses(5);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(searchResults, 5);

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        // When：搜索 "Aatrox"
        viewModel.searchHeroes("Aatrox");
        latch.await(1, TimeUnit.SECONDS);

        // Then：验证 keyword 参数
        verify(mockHeroApi).getHeroes(
                eq("Aatrox"), eq(""), eq("winRate"), eq(1), eq(20)
        );
        assertEquals(5, viewModel.getHeroes().getValue().size());
    }

    @Test
    @DisplayName("搜索时 currentPage 应重置为 1")
    void searchHeroes_resetsCurrentPage() throws InterruptedException {
        // Given：先加载几页，让 currentPage 变大
        simulatePageLoads(3, 20, 100);

        // 此时 currentPage 应该已经是 4
        assertEquals(4, viewModel.getCurrentPageViaReflection());

        // When：执行搜索
        CountDownLatch searchLatch = new CountDownLatch(1);
        List<HeroResponse> searchResults = createHeroResponses(3);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(searchResults, 3);

        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            searchLatch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.searchHeroes("Garen");
        searchLatch.await(1, TimeUnit.SECONDS);

        // Then：currentPage 应重置为 2（因为搜索后又自增了一次）
        assertEquals(2, viewModel.getCurrentPageViaReflection());
    }

    // ============================================================
    // 测试场景 7：梯级筛选
    // ============================================================

    @Test
    @DisplayName("筛选梯级：tier 参数正确传递，currentPage 重置为 1")
    void filterByTier_passesCorrectTier() throws InterruptedException {
        // Given
        List<HeroResponse> tierResults = createHeroResponses(10);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(tierResults, 10);

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        // When：筛选 S 级英雄
        viewModel.filterByTier(Tier.S);
        latch.await(1, TimeUnit.SECONDS);

        // Then：验证 tier 参数
        verify(mockHeroApi).getHeroes(
                eq(""), eq("S"), eq("winRate"), eq(1), eq(20)
        );
        assertEquals(10, viewModel.getHeroes().getValue().size());
    }

    @Test
    @DisplayName("筛选 S+ 梯级：tier 参数应为 S+")
    void filterByTier_SPlus_passesCorrectTierLabel() throws InterruptedException {
        List<HeroResponse> results = createHeroResponses(8);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(results, 8);

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.filterByTier(Tier.S_PLUS);
        latch.await(1, TimeUnit.SECONDS);

        verify(mockHeroApi).getHeroes(
                eq(""), eq("S+"), eq("winRate"), eq(1), eq(20)
        );
    }

    @Test
    @DisplayName("筛选 null 梯级：应清空筛选条件")
    void filterByTier_null_clearsFilter() throws InterruptedException {
        List<HeroResponse> results = createHeroResponses(15);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(results, 15);

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.filterByTier(null);
        latch.await(1, TimeUnit.SECONDS);

        verify(mockHeroApi).getHeroes(
                eq(""), eq(""), eq("winRate"), eq(1), eq(20)
        );
    }

    @Test
    @DisplayName("筛选后列表应被清空后重新加载")
    void filterByTier_clearsListBeforeReload() throws InterruptedException {
        // Given：先加载 20 个英雄
        simulatePageLoads(1, 20, 100);
        assertEquals(20, viewModel.getHeroes().getValue().size());

        // When：筛选（返回 5 个结果）
        CountDownLatch filterLatch = new CountDownLatch(1);
        List<HeroResponse> filterResults = createHeroResponses(5);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(filterResults, 5);

        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            filterLatch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.filterByTier(Tier.A);
        filterLatch.await(1, TimeUnit.SECONDS);

        // Then：列表应为筛选结果（5 个），而非原来的 20 + 5 = 25
        assertEquals(5, viewModel.getHeroes().getValue().size());
    }

    // ============================================================
    // 测试场景 8：重试功能
    // ============================================================

    @Test
    @DisplayName("网络失败后重试：应重新发起网络请求")
    void retry_afterFailure_makesNewRequest() throws InterruptedException {
        // Given：第一次请求失败
        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onFailure(mockCall, new IOException("Timeout"));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        assertNotNull(viewModel.getError().getValue());

        // When：调用 retry
        CountDownLatch retryLatch = new CountDownLatch(1);
        List<HeroResponse> retryResults = createHeroResponses(10);
        Result<PageResponse<HeroResponse>> retryResult = createSuccessPageResult(retryResults, 10);

        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(retryResult));
            retryLatch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.retry();
        retryLatch.await(1, TimeUnit.SECONDS);

        // Then：应发起第二次请求，且成功
        verify(mockHeroApi, times(2)).getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt());
        assertNotNull(viewModel.getHeroes().getValue());
        assertEquals(10, viewModel.getHeroes().getValue().size());
        // 注意：error LiveData 不会被清空（ViewModel 实现中没有清空逻辑），只验证 heroes 有值
    }

    // ============================================================
    // 额外测试场景：边界条件
    // ============================================================

    @Test
    @DisplayName("搜索空字符串：应等同于重置搜索")
    void searchHeroes_emptyString_resetsSearch() throws InterruptedException {
        List<HeroResponse> results = createHeroResponses(20);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(results, 100);

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.searchHeroes("");
        latch.await(1, TimeUnit.SECONDS);

        verify(mockHeroApi).getHeroes(
                eq(""), eq(""), eq("winRate"), eq(1), eq(20)
        );
    }

    @Test
    @DisplayName("loadingMore 为 true 时调用 loadMore 不应发起新请求")
    void loadMore_whenAlreadyLoadingMore_doesNotRequest() throws InterruptedException {
        // Given：初始加载
        List<HeroResponse> firstPage = createHeroResponses(20);
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(firstPage, 100);

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            // 不立即回调，模拟加载中
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();

        // When：在 loadingMore 状态下再次调用 loadMore
        viewModel.loadMore();

        // Then：只有一次 API 调用
        verify(mockHeroApi, times(1)).getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("HeroUiModel 转换：应正确转换所有字段")
    void convertToUiModel_correctlyConvertsFields() throws InterruptedException {
        // Given：创建包含完整字段的 HeroResponse
        Result<PageResponse<HeroResponse>> result = createSuccessPageResult(createHeroResponses(1), 1);

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        // When
        List<HeroUiModel> heroes = viewModel.getHeroes().getValue();
        HeroUiModel heroUi = heroes.get(0);

        // Then
        assertEquals("测试英雄 1", heroUi.getNameZh());
        assertEquals("TestHero1", heroUi.getNameEn());
        assertEquals("Mage", heroUi.getRole());
        assertEquals(55.5, heroUi.getWinRate());
        assertEquals(10.0, heroUi.getPickRate());
        assertFalse(heroUi.isTrap());
    }

    @Test
    @DisplayName("HeroResponse 为 null 字段：应使用默认值")
    void convertToUiModel_nullFields_useDefaults() throws InterruptedException {
        // Given：创建一个部分字段为 null 的 HeroResponse
        Result<PageResponse<HeroResponse>> result = Result.success(
                new PageResponseWithValues<>(List.of(createHeroResponseWithNulls()), 1, 1, 20));

        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(result));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        // When
        List<HeroUiModel> heroes = viewModel.getHeroes().getValue();
        HeroUiModel heroUi = heroes.get(0);

        // Then：null 字段应使用默认值
        assertEquals(0.0, heroUi.getWinRate());
        assertEquals(0.0, heroUi.getPickRate());
        assertEquals(Tier.C, heroUi.getTier()); // null tier 默认为 C
    }

    @Test
    @DisplayName("Response body 为 null：应视为请求失败")
    void loadHeroes_nullResponseBody_treatedAsFailure() throws InterruptedException {
        // Given：模拟 response body 为 null
        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.success(null));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        // When
        String error = viewModel.getError().getValue();
        Boolean loading = viewModel.getLoading().getValue();

        // Then
        assertFalse(loading);
        // 错误信息应为默认值
        assertNotNull(error);
        assertEquals("网络请求失败", error);
    }

    @Test
    @DisplayName("HTTP 响应失败（非 2xx）：应视为请求失败")
    void loadHeroes_httpError_treatedAsFailure() throws InterruptedException {
        // Given：模拟 HTTP 500 响应
        when(mockHeroApi.getHeroes(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
            callback.onResponse(mockCall, Response.error(500, okhttp3.ResponseBody.create("", null)));
            latch.countDown();
            return null;
        }).when(mockCall).enqueue(any());

        viewModel.resetAndLoad();
        latch.await(1, TimeUnit.SECONDS);

        // When
        String error = viewModel.getError().getValue();
        Boolean loading = viewModel.getLoading().getValue();

        // Then
        assertFalse(loading);
        assertNotNull(error);
        assertEquals("网络请求失败", error);
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 创建指定数量的 HeroResponse 列表
     */
    private List<HeroResponse> createHeroResponses(int count) {
        List<HeroResponse> responses = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            responses.add(createHeroResponse(i));
        }
        return responses;
    }

    /**
     * 创建单个 HeroResponse（使用可构造的子类）
     */
    private HeroResponse createHeroResponse(int index) {
        return new HeroResponseWithValues(
                (long) index,
                "测试英雄 " + index,
                "TestHero" + index,
                index % 2 == 0 ? "Mage" : "Fighter",
                index <= 5 ? "S+" : (index <= 10 ? "S" : (index <= 15 ? "A" : (index <= 20 ? "B" : "C"))),
                50.0 + (index % 10),
                5.0 + (index % 5),
                "https://example.com/hero" + index + ".png",
                "Test hero " + index
        );
    }

    /**
     * 创建一个包含 null 值的 HeroResponse（用于边界测试）
     */
    private HeroResponse createHeroResponseWithNulls() {
        return new HeroResponseWithValues(
                1L,
                "Null Hero",
                "NullHero",
                null, // role 为 null
                null, // tier 为 null
                null, // winRate 为 null
                null, // pickRate 为 null
                null, // imageUrl 为 null
                null  // description 为 null
        );
    }

    /**
     * 创建成功分页结果
     */
    private Result<PageResponse<HeroResponse>> createSuccessPageResult(
            List<HeroResponse> records, long total) {
        return Result.success(new PageResponseWithValues<>(records, total, 1, 20));
    }

    /**
     * 模拟加载指定页数的数据
     */
    private void simulatePageLoads(int pageCount, int pageSize, long total) throws InterruptedException {
        for (int page = 0; page < pageCount; page++) {
            CountDownLatch pageLatch = new CountDownLatch(1);
            List<HeroResponse> pageData = createHeroResponses(pageSize);
            Result<PageResponse<HeroResponse>> result = createSuccessPageResult(pageData, total);

            doAnswer(invocation -> {
                Callback<Result<PageResponse<HeroResponse>>> callback = invocation.getArgument(0);
                callback.onResponse(mockCall, Response.success(result));
                pageLatch.countDown();
                return null;
            }).when(mockCall).enqueue(any());

            if (page == 0) {
                viewModel.resetAndLoad();
            } else {
                viewModel.loadMore();
            }
            pageLatch.await(1, TimeUnit.SECONDS);
        }
    }

    // ============================================================
    // 测试辅助类
    // ============================================================

    /**
     * 可测试的 ViewModel 子类，不依赖 Android Application 上下文。
     * 
     * 通过传入 null 作为 Application 参数来绕过 Android 依赖，
     * 因为 HeroListViewModel 的实际业务逻辑并未使用 getApplication()。
     * 
     * 提供反射方法来访问内部状态（currentPage）以便验证分页逻辑。
     */
    static class TestHeroListViewModel extends HeroListViewModel {

        public TestHeroListViewModel(HeroApi heroApi) {
            super(null, null, heroApi);
        }

        /**
         * 通过反射获取 currentPage 字段值。
         * ViewModel 内部使用 currentPage 跟踪分页状态，但未公开 getter。
         */
        public int getCurrentPageViaReflection() {
            try {
                Field field = HeroListViewModel.class.getDeclaredField("currentPage");
                field.setAccessible(true);
                return field.getInt(this);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to access currentPage via reflection", e);
            }
        }

        /**
         * 重置并重新加载英雄列表。
         * 调用 retry() 等效于 loadHeroes(true)，会重置 currentPage=1。
         */
        public void resetAndLoad() {
            super.retry();
        }
    }

    /**
     * 可构造的 HeroResponse 实现。
     * 
     * 原始 HeroResponse 类只有 getter 没有 setter，且字段为 private final，
     * 无法直接实例化设置测试数据。通过继承并重写 getter 来解决此问题。
     */
    static class HeroResponseWithValues extends HeroResponse {
        private final Long id;
        private final String nameEn;
        private final String nameZh;
        private final String title;
        private final String role;
        private final String tier;
        private final Double winRate;
        private final Double pickRate;
        private final String imageUrl;
        private final String description;

        public HeroResponseWithValues(Long id, String nameZh, String nameEn, String role,
                                      String tier, Double winRate, Double pickRate,
                                      String imageUrl, String description) {
            this.id = id;
            this.nameZh = nameZh;
            this.nameEn = nameEn;
            this.title = title;
            this.role = role;
            this.tier = tier;
            this.winRate = winRate;
            this.pickRate = pickRate;
            this.imageUrl = imageUrl;
            this.description = description;
        }

        @Override
        public Long getId() { return id; }

        @Override
        public String getNameEn() { return nameEn; }

        @Override
        public String getNameZh() { return nameZh; }

        @Override
        public String getTitle() { return title; }

        @Override
        public String getRole() { return role; }

        @Override
        public String getTier() { return tier; }

        @Override
        public Double getWinRate() { return winRate; }

        @Override
        public Double getPickRate() { return pickRate; }

        @Override
        public String getImageUrl() { return imageUrl; }

        @Override
        public String getDescription() { return description; }
    }

    /**
     * 可构造的 PageResponse 实现。
     * 
     * 原始 PageResponse 类同样只有 getter 没有 setter，
     * 通过继承并重写 getter 来构造测试数据。
     */
    static class PageResponseWithValues<T> extends PageResponse<T> {
        private final List<T> records;
        private final long total;
        private final int page;
        private final int size;

        public PageResponseWithValues(List<T> records, long total, int page, int size) {
            this.records = records;
            this.total = total;
            this.page = page;
            this.size = size;
        }

        @Override
        public List<T> getRecords() { return records; }

        @Override
        public long getTotal() { return total; }

        @Override
        public int getPage() { return page; }

        @Override
        public int getSize() { return size; }
    }
}

