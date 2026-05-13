package com.aram.mayhem.feature.hero.viewmodel;

import com.aram.mayhem.common.Tier;
import com.aram.mayhem.feature.hero.repository.HeroRepository;
import com.aram.mayhem.ui.model.HeroDetailUiModel;
import com.aram.mayhem.ui.model.HeroDetailUiModel.SkillUiModel;

import androidx.lifecycle.MutableLiveData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeroDetailViewModelTest {

    @Mock
    private HeroRepository mockRepository;

    private HeroDetailViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new HeroDetailViewModel(null, mockRepository);
    }

    @Test
    @DisplayName("loadHeroDetail 成功：应设置 heroDetail LiveData")
    void loadHeroDetail_success_setsHeroDetail() {
        MutableLiveData<HeroDetailUiModel> liveData = new MutableLiveData<>();
        HeroDetailUiModel detail = createTestDetail();
        liveData.setValue(detail);

        when(mockRepository.getHeroDetail(anyLong())).thenReturn(liveData);

        viewModel.loadHeroDetail(1L);

        assertEquals(detail, viewModel.getHeroDetail().getValue());
        assertFalse(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("loadHeroDetail 开始时 loading 为 true")
    void loadHeroDetail_initialState_loadingIsTrue() {
        MutableLiveData<HeroDetailUiModel> liveData = new MutableLiveData<>();
        when(mockRepository.getHeroDetail(anyLong())).thenReturn(liveData);

        viewModel.loadHeroDetail(1L);

        assertTrue(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("loadHeroDetail 返回 null：应设置 error")
    void loadHeroDetail_nullResult_setsError() {
        MutableLiveData<HeroDetailUiModel> liveData = new MutableLiveData<>();
        liveData.setValue(null);

        when(mockRepository.getHeroDetail(anyLong())).thenReturn(liveData);

        viewModel.loadHeroDetail(999L);

        assertEquals("获取英雄详情失败", viewModel.getError().getValue());
        assertFalse(viewModel.getLoading().getValue());
    }

    @Test
    @DisplayName("getHeroDetail 初始值为 null")
    void getHeroDetail_initialValueIsNull() {
        assertNull(viewModel.getHeroDetail().getValue());
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
    @DisplayName("isOffline 初始值为 false")
    void isOffline_initialValueIsFalse() {
        assertFalse(viewModel.getIsOffline().getValue());
    }

    @Test
    @DisplayName("loadHeroDetail 调用 repository.getHeroDetail 传入正确 heroId")
    void loadHeroDetail_callsRepositoryWithCorrectId() {
        MutableLiveData<HeroDetailUiModel> liveData = new MutableLiveData<>();
        when(mockRepository.getHeroDetail(anyLong())).thenReturn(liveData);

        viewModel.loadHeroDetail(42L);

        verify(mockRepository).getHeroDetail(42L);
    }

    @Test
    @DisplayName("HeroDetailUiModel 字段完整性验证")
    void heroDetailUiModel_allFieldsCorrect() {
        MutableLiveData<HeroDetailUiModel> liveData = new MutableLiveData<>();
        HeroDetailUiModel detail = createTestDetail();
        liveData.setValue(detail);

        when(mockRepository.getHeroDetail(anyLong())).thenReturn(liveData);

        viewModel.loadHeroDetail(1L);

        HeroDetailUiModel result = viewModel.getHeroDetail().getValue();
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("艾希", result.getNameZh());
        assertEquals("Ashe", result.getNameEn());
        assertEquals("寒冰射手", result.getTitle());
        assertEquals("Marksman", result.getRole());
        assertEquals(Tier.S_PLUS, result.getTier());
        assertEquals(52.5, result.getWinRate());
        assertEquals(15.3, result.getPickRate());
        assertNotNull(result.getDescription());
        assertNotNull(result.getSkills());
        assertEquals(5, result.getSkills().size());
        assertNotNull(result.getCounterTips());
        assertEquals(2, result.getCounterTips().size());
        assertNotNull(result.getSynergies());
        assertEquals(2, result.getSynergies().size());
        assertEquals(5.5, result.getAvgKills());
        assertEquals(4.2, result.getAvgDeaths());
        assertEquals(7.1, result.getAvgAssists());
        assertNotNull(result.getRecommendedBuild());
    }

    @Test
    @DisplayName("HeroDetailUiModel 辅助方法验证")
    void heroDetailUiModel_helperMethods() {
        HeroDetailUiModel detail = createTestDetail();

        assertEquals("52.5%", detail.getWinRateDisplay());
        assertEquals("15.3%", detail.getPickRateDisplay());
        assertEquals("5.5 / 4.2 / 7.1", detail.getKdaDisplay());
    }

    private HeroDetailUiModel createTestDetail() {
        List<SkillUiModel> skills = List.of(
                new SkillUiModel("P", "被动技能", "被动效果描述"),
                new SkillUiModel("Q", "技能Q", "Q技能描述"),
                new SkillUiModel("W", "技能W", "W技能描述"),
                new SkillUiModel("E", "技能E", "E技能描述"),
                new SkillUiModel("R", "技能R", "R技能描述")
        );

        return new HeroDetailUiModel(
                1L,
                "艾希",
                "Ashe",
                "寒冰射手",
                "Marksman",
                Tier.S_PLUS,
                52.5,
                15.3,
                "艾希，寒冰射手。在ARAM模式中定位为射手。",
                skills,
                List.of("保持距离风筝", "利用突进贴身"),
                List.of("保护型辅助", "前排坦克"),
                5.5,
                4.2,
                7.1,
                "海妖杀手 → 无尽之刃 → 幻影之舞",
                "/images/heroes/Ashe.png"
        );
    }
}
