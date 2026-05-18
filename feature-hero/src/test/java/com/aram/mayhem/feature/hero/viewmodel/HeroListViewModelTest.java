package com.aram.mayhem.feature.hero.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Tier;
import com.aram.mayhem.feature.hero.repository.HeroRepository;
import com.aram.mayhem.ui.model.HeroUiModel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeroListViewModelTest {

    @Mock
    private HeroRepository mockHeroRepository;

    private TestHeroListViewModel viewModel;

    @BeforeEach
    void setUp() {
        MutableLiveData<List<HeroUiModel>> defaultResult = new MutableLiveData<>(new ArrayList<>());
        when(mockHeroRepository.getHeroes(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(defaultResult);

        viewModel = new TestHeroListViewModel(mockHeroRepository);
    }

    @Test
    @DisplayName("初始加载：应调用 Repository 获取第一页数据")
    void initialLoad_callsRepositoryWithPage1() {
        verify(mockHeroRepository).getHeroes(eq(1), eq(20), eq(""), eq(""), eq("winRate"));
    }

    @Test
    @DisplayName("加载英雄列表成功：应返回正确数量的英雄")
    void loadHeroes_success_returnsHeroes() {
        MutableLiveData<List<HeroUiModel>> data = new MutableLiveData<>(createHeroUiModels(20));
        when(mockHeroRepository.getHeroes(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(data);

        viewModel.resetAndLoad();

        List<HeroUiModel> heroes = viewModel.getHeroes().getValue();
        assertNotNull(heroes);
        assertEquals(20, heroes.size());
    }

    @Test
    @DisplayName("加载英雄列表空结果：应返回空列表")
    void loadHeroes_emptyResult_returnsEmptyList() {
        MutableLiveData<List<HeroUiModel>> emptyData = new MutableLiveData<>(new ArrayList<>());
        when(mockHeroRepository.getHeroes(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(emptyData);

        viewModel.resetAndLoad();

        List<HeroUiModel> heroes = viewModel.getHeroes().getValue();
        assertNotNull(heroes);
        assertTrue(heroes.isEmpty());
    }

    @Test
    @DisplayName("搜索英雄：keyword 参数正确传递，currentPage 重置为 1")
    void searchHeroes_passesCorrectKeyword() {
        MutableLiveData<List<HeroUiModel>> data = new MutableLiveData<>(createHeroUiModels(5));
        when(mockHeroRepository.getHeroes(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(data);

        viewModel.searchHeroes("Aatrox");

        verify(mockHeroRepository).getHeroes(eq(1), eq(20), eq("Aatrox"), eq(""), eq("winRate"));
    }

    @Test
    @DisplayName("筛选梯级：tier 参数正确传递")
    void filterByTier_passesCorrectTier() {
        MutableLiveData<List<HeroUiModel>> data = new MutableLiveData<>(createHeroUiModels(10));
        when(mockHeroRepository.getHeroes(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(data);

        viewModel.filterByTier(Tier.S);

        verify(mockHeroRepository).getHeroes(eq(1), eq(20), eq(""), eq("S"), eq("winRate"));
    }

    @Test
    @DisplayName("筛选 S+ 梯级：tier 参数应为 S+")
    void filterByTier_SPlus_passesCorrectTierLabel() {
        MutableLiveData<List<HeroUiModel>> data = new MutableLiveData<>(createHeroUiModels(8));
        when(mockHeroRepository.getHeroes(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(data);

        viewModel.filterByTier(Tier.S_PLUS);

        verify(mockHeroRepository).getHeroes(eq(1), eq(20), eq(""), eq("S+"), eq("winRate"));
    }

    @Test
    @DisplayName("筛选 null 梯级：应清空筛选条件")
    void filterByTier_null_clearsFilter() {
        MutableLiveData<List<HeroUiModel>> data = new MutableLiveData<>(createHeroUiModels(15));
        when(mockHeroRepository.getHeroes(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(data);

        viewModel.filterByTier(null);

        verify(mockHeroRepository).getHeroes(eq(1), eq(20), eq(""), eq(""), eq("winRate"));
    }

    @Test
    @DisplayName("重试加载：应重置离线状态并重新加载")
    void retry_resetsOfflineAndReloads() {
        MutableLiveData<List<HeroUiModel>> data = new MutableLiveData<>(new ArrayList<>());
        when(mockHeroRepository.getHeroes(anyInt(), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(data);

        viewModel.retry();

        verify(mockHeroRepository).setOffline(false);
        verify(mockHeroRepository, atLeast(2)).getHeroes(eq(1), eq(20), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("setOffline：应同步 Repository 离线状态")
    void setOffline_syncsToRepository() {
        viewModel.setOffline(true);
        verify(mockHeroRepository).setOffline(true);

        viewModel.setOffline(false);
        verify(mockHeroRepository).setOffline(false);
    }

    @Test
    @DisplayName("getHeroes 初始值不为 null")
    void getHeroes_initialValueNotNull() {
        assertNotNull(viewModel.getHeroes().getValue());
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
    @DisplayName("getIsLastPage 初始值为 false")
    void getIsLastPage_initialValueIsFalse() {
        assertFalse(viewModel.getIsLastPage().getValue());
    }

    @Test
    @DisplayName("getIsOffline 初始值为 false")
    void getIsOffline_initialValueIsFalse() {
        assertFalse(viewModel.getIsOffline().getValue());
    }

    @Test
    @DisplayName("分页加载更多：返回数据少于 pageSize 时 isLastPage 为 true")
    void loadMore_lastPage_setsIsLastPage() {
        MutableLiveData<List<HeroUiModel>> firstPage = new MutableLiveData<>(createHeroUiModels(20));
        MutableLiveData<List<HeroUiModel>> secondPage = new MutableLiveData<>(createHeroUiModels(5));

        when(mockHeroRepository.getHeroes(eq(1), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(firstPage);
        when(mockHeroRepository.getHeroes(eq(2), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(secondPage);

        viewModel.resetAndLoad();
        viewModel.loadMore();

        assertTrue(viewModel.getIsLastPage().getValue());
    }

    @Test
    @DisplayName("分页加载更多：返回数据等于 pageSize 时 isLastPage 为 false")
    void loadMore_notLastPage_isLastPageFalse() {
        MutableLiveData<List<HeroUiModel>> firstPage = new MutableLiveData<>(createHeroUiModels(20));
        MutableLiveData<List<HeroUiModel>> secondPage = new MutableLiveData<>(createHeroUiModels(20));

        when(mockHeroRepository.getHeroes(eq(1), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(firstPage);
        when(mockHeroRepository.getHeroes(eq(2), anyInt(), anyString(), anyString(), anyString()))
                .thenReturn(secondPage);

        viewModel.resetAndLoad();
        viewModel.loadMore();

        assertFalse(viewModel.getIsLastPage().getValue());
    }

    private List<HeroUiModel> createHeroUiModels(int count) {
        List<HeroUiModel> models = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            models.add(new HeroUiModel(
                    i,
                    "英雄" + i,
                    "Hero" + i,
                    "Title" + i,
                    "Fighter",
                    Tier.S,
                    52.0 + (i % 10),
                    10.0 + (i % 5),
                    "/images/hero" + i + ".png"
            ));
        }
        return models;
    }

    static class TestHeroListViewModel extends HeroListViewModel {

        public TestHeroListViewModel(HeroRepository heroRepository) {
            super(null, heroRepository);
        }

        public int getCurrentPageViaReflection() {
            try {
                Field field = HeroListViewModel.class.getDeclaredField("currentPage");
                field.setAccessible(true);
                return field.getInt(this);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to access currentPage via reflection", e);
            }
        }

        public void resetAndLoad() {
            super.retry();
        }
    }
}
