package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.network.dto.SynergyProgressResponse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 套装进度 ViewModel ── 管理符文套装的收集进度数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * SynergyProgressViewModel 负责追踪用户已选择的符文，并查询这些符文
 * 触发的套装进度。当用户添加或移除符文时，自动刷新套装进度数据。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、套装进度是什么？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 在 ARAM 混乱模式中，符文可以组成套装（Synergy Set），例如：
 * - "刺客套装"：需要 3 个刺客类符文才能激活
 * - "法师套装"：需要 2 个法师类符文才能激活
 *
 * 套装进度表示：已选符文中有多少个属于某个套装，距离激活还差多少。
 *
 *   例如：已选 [刺客符文A, 刺客符文B, 法师符文C]
 *   → 刺客套装进度：2/3（还差1个）
 *   → 法师套装进度：1/2（还差1个）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户添加/移除符文
 *     → addAugment(5) / removeAugment(5)
 *     → 更新 selectedAugmentIds 列表
 *     → loadSynergyProgress()
 *     → augmentRepository.getSynergyProgress(ids)
 *     → synergyProgress LiveData 更新
 *     → UI 观察到变化，更新进度条
 *
 * @see com.aram.mayhem.feature.augment.SynergyProgressAdapter
 * @see AugmentRepository
 */
@HiltViewModel
public class SynergyProgressViewModel extends AndroidViewModel {

    /**
     * 符文数据仓库 ── 提供套装进度查询接口
     */
    private final AugmentRepository augmentRepository;

    /**
     * 套装进度数据 ── 每个套装的收集进度信息
     */
    private final MutableLiveData<List<SynergyProgressResponse>> synergyProgress = new MutableLiveData<>();

    /**
     * 加载状态 ── true=正在查询套装进度
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息 ── 查询失败时的错误描述
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 已选符文 ID 列表 ── 用户当前选择的符文集合
     *
     * 当列表变化时，会自动触发 loadSynergyProgress() 刷新进度。
     */
    private List<Long> selectedAugmentIds = new ArrayList<>();

    /**
     * 构造函数 ── Hilt 自动注入依赖
     *
     * @param application       Android 应用上下文
     * @param augmentRepository 符文数据仓库（Hilt 自动注入单例）
     */
    @Inject
    public SynergyProgressViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
    }

    public LiveData<List<SynergyProgressResponse>> getSynergyProgress() {
        return synergyProgress;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    /**
     * 设置已选符文 ID 列表 ── 批量替换
     *
     * 替换整个已选列表，并立即触发套装进度查询。
     * 用于初始化或从外部恢复选择状态。
     *
     * @param ids 新的已选符文 ID 列表
     */
    public void setSelectedAugmentIds(List<Long> ids) {
        this.selectedAugmentIds = ids != null ? ids : new ArrayList<>();
        loadSynergyProgress();
    }

    /**
     * 添加已选符文 ── 增量添加
     *
     * 如果该符文 ID 已在列表中则不重复添加。
     * 添加后自动触发套装进度查询。
     *
     * @param augmentId 要添加的符文 ID
     */
    public void addAugment(long augmentId) {
        if (!selectedAugmentIds.contains(augmentId)) {
            selectedAugmentIds.add(augmentId);
            loadSynergyProgress();
        }
    }

    /**
     * 移除已选符文 ── 增量移除
     *
     * 移除后自动触发套装进度查询。
     *
     * @param augmentId 要移除的符文 ID
     */
    public void removeAugment(long augmentId) {
        selectedAugmentIds.remove(augmentId);
        loadSynergyProgress();
    }

    /**
     * 加载套装进度 ── 核心方法，查询已选符文的套装进度
     *
     * 执行流程：
     * 1. 检查已选列表是否为空 → 空则直接返回空进度
     * 2. 设置 loading=true
     * 3. 调用 Repository 查询套装进度
     * 4. 成功 → 设置 synergyProgress
     * 5. 失败 → 设置空列表
     *
     * 注意：套装进度没有本地缓存，纯网络请求。
     * 如果网络失败，返回空列表而不是报错（因为进度是辅助信息）。
     */
    public void loadSynergyProgress() {
        if (selectedAugmentIds.isEmpty()) {
            synergyProgress.setValue(new ArrayList<>());
            return;
        }

        loading.setValue(true);
        augmentRepository.getSynergyProgress(selectedAugmentIds).observeForever(progress -> {
            loading.setValue(false);
            if (progress != null) {
                synergyProgress.setValue(progress);
            } else {
                synergyProgress.setValue(new ArrayList<>());
            }
        });
    }

    /**
     * 获取已选符文 ID 列表的副本 ── 防止外部修改内部状态
     *
     * @return 新的 ArrayList，包含当前所有已选符文 ID
     */
    public List<Long> getSelectedAugmentIds() {
        return new ArrayList<>(selectedAugmentIds);
    }
}
