package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.network.dto.AugmentRecommendResponse;
import com.aram.mayhem.network.dto.SynergyProgressResponse;
import com.aram.mayhem.ui.model.HeroUiModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 符文推荐 ViewModel ── 管理智能推荐和套装进度的联动数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentRecommendViewModel 是符文推荐页的"数据管家"，负责：
 * 1. 管理用户选择的英雄
 * 2. 管理用户已选的符文 ID 列表
 * 3. 根据已选符文查询套装进度
 * 4. 根据英雄+已选符文获取智能推荐
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、推荐逻辑
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户选择英雄 + 已选符文列表
 *     ↓
 *   同时发起两个请求：
 *   ├─ 套装进度查询：已选符文触发了哪些套装，进度如何
 *   └─ 智能推荐查询：基于英雄+已选符文，推荐下一个最优符文
 *
 *   推荐结果包含：
 *   - 推荐符文的名称和图标
 *   - 推荐评分（0-100）
 *   - 推荐理由（如"与已选符文形成刺客套装"）
 *   - 推荐符文的胜率和选取率
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户操作                    ViewModel                    Repository
 *   ┌──────────┐              ┌──────────────┐             ┌──────────┐
 *   │ 选择英雄  │ ──────────→ │ setSelectedHero│             │          │
 *   │ 添加符文  │ ──────────→ │ addSelectedAug │ ──refresh─→ │ getSynergy│
 *   │ 移除符文  │ ──────────→ │ removeSelected│ ──refresh─→ │ getRecomm │
 *   └──────────┘              └──────────────┘             └──────────┘
 *                                │
 *                    ┌───────────┴───────────┐
 *                    ↓                       ↓
 *             synergyProgress         recommendations
 *             (套装进度)              (推荐结果)
 *
 * @see com.aram.mayhem.feature.augment.AugmentRecommendFragment
 * @see AugmentRepository
 */
@HiltViewModel
public class AugmentRecommendViewModel extends AndroidViewModel {

    /**
     * 符文数据仓库 ── 提供套装进度和推荐查询接口
     */
    private final AugmentRepository augmentRepository;

    /**
     * 已选英雄 ── 推荐算法的输入之一
     *
     * 不同英雄的推荐结果不同（法师推荐法术符文，战士推荐战斗符文）
     */
    private final MutableLiveData<HeroUiModel> selectedHero = new MutableLiveData<>();

    /**
     * 已选符文 ID 列表 ── 推荐算法的输入之二
     *
     * 已选符文会影响套装进度和推荐结果
     */
    private final MutableLiveData<List<Long>> selectedAugmentIds = new MutableLiveData<>(new ArrayList<>());

    /**
     * 套装进度数据 ── 已选符文触发的套装收集进度
     */
    private final MutableLiveData<List<SynergyProgressResponse>> synergyProgress = new MutableLiveData<>();

    /**
     * 推荐结果数据 ── 智能推荐的符文列表
     */
    private final MutableLiveData<List<AugmentRecommendResponse>> recommendations = new MutableLiveData<>();

    /**
     * 加载状态 ── true=正在查询推荐结果
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 构造函数 ── Hilt 自动注入依赖
     *
     * @param application       Android 应用上下文
     * @param augmentRepository 符文数据仓库（Hilt 自动注入单例）
     */
    @Inject
    public AugmentRecommendViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
    }

    public LiveData<HeroUiModel> getSelectedHero() {
        return selectedHero;
    }

    public LiveData<List<Long>> getSelectedAugmentIds() {
        return selectedAugmentIds;
    }

    public LiveData<List<SynergyProgressResponse>> getSynergyProgress() {
        return synergyProgress;
    }

    public LiveData<List<AugmentRecommendResponse>> getRecommendations() {
        return recommendations;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /**
     * 设置已选英雄 ── 触发推荐数据刷新
     *
     * @param hero 用户选择的英雄
     */
    public void setSelectedHero(HeroUiModel hero) {
        selectedHero.setValue(hero);
        refreshData();
    }

    /**
     * 添加已选符文 ── 增量添加，触发推荐数据刷新
     *
     * 如果该符文 ID 已在列表中则不重复添加。
     *
     * @param augmentId 要添加的符文 ID
     */
    public void addSelectedAugment(long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current == null) {
            current = new ArrayList<>();
        }
        if (!current.contains(augmentId)) {
            current.add(augmentId);
            selectedAugmentIds.setValue(current);
            refreshData();
        }
    }

    /**
     * 移除已选符文 ── 增量移除，触发推荐数据刷新
     *
     * 创建新列表而不是直接修改，确保 LiveData 检测到变化。
     *
     * @param augmentId 要移除的符文 ID
     */
    public void removeSelectedAugment(long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current != null) {
            current.remove(augmentId);
            selectedAugmentIds.setValue(new ArrayList<>(current));
            refreshData();
        }
    }

    /**
     * 刷新数据 ── 同时查询套装进度和推荐结果
     *
     * 前置条件检查：
     * - 如果没选英雄或没选符文 → 清空进度和推荐
     * - 两个条件都满足 → 并行发起两个网络请求
     *
     * 并行请求：
     * 1. getSynergyProgress(ids) → 更新套装进度
     * 2. getRecommendations(heroId, ids) → 更新推荐结果
     *
     * loading 状态由推荐请求控制（推荐请求通常更慢）
     */
    private void refreshData() {
        HeroUiModel hero = selectedHero.getValue();
        List<Long> ids = selectedAugmentIds.getValue();

        if (hero == null || ids == null || ids.isEmpty()) {
            synergyProgress.setValue(new ArrayList<>());
            recommendations.setValue(new ArrayList<>());
            return;
        }

        loading.setValue(true);

        augmentRepository.getSynergyProgress(ids).observeForever(progress -> {
            synergyProgress.setValue(progress);
        });

        augmentRepository.getRecommendations(hero.getId(), ids).observeForever(recs -> {
            loading.setValue(false);
            recommendations.setValue(recs);
        });
    }
}
