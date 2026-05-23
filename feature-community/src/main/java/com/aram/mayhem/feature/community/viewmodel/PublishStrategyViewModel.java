package com.aram.mayhem.feature.community.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.community.repository.StrategyRepository;
import com.aram.mayhem.network.dto.StrategyDetailResponse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 发布攻略 ViewModel ── 管理攻略发布表单的数据状态和提交逻辑
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * PublishStrategyViewModel 是攻略发布页的"数据管家"，负责：
 * 1. 管理表单数据（英雄、标题、描述、符文、装备）
 * 2. 实时表单验证（标题不为空、描述至少10字）
 * 3. 提交发布请求到服务器
 * 4. 管理发布状态（发布中/发布成功/发布失败）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、表单验证规则
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────┬──────────────────────┬──────────────────────┐
 *   │ 字段         │ 验证规则             │ 错误提示             │
 *   ├──────────────┼──────────────────────┼──────────────────────┤
 *   │ 英雄ID       │ 不为 null            │ 请选择英雄           │
 *   │ 标题         │ 不为空（trim后）     │ 请填写标题           │
 *   │ 描述         │ 至少10个字符（trim后）│ 描述至少10个字       │
 *   │ 符文列表     │ 可选（无强制要求）   │ -                    │
 *   │ 装备列表     │ 可选（无强制要求）   │ -                    │
 *   └──────────────┴──────────────────────┴──────────────────────┘
 *
 *   isFormValid = (heroId != null) && (title 不为空) && (description >= 10字)
 *   发布按钮的 enabled 状态绑定到 isFormValid
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户输入                    ViewModel                    Repository
 *   ┌──────────┐              ┌──────────────┐             ┌──────────┐
 *   │ 选择英雄  │ ──────────→ │ setSelected  │             │          │
 *   │ 输入标题  │ ──────────→ │ setTitle     │             │          │
 *   │ 输入描述  │ ──────────→ │ setDescription│             │          │
 *   │ 选择符文  │ ──────────→ │ addAugment   │             │          │
 *   │ 选择装备  │ ──────────→ │ addItem      │             │          │
 *   │ 点击发布  │ ──────────→ │ publish()    │ ─────────→ │ publish  │
 *   └──────────┘              └──────────────┘             └──────────┘
 *                                │
 *                    ┌───────────┴───────────┐
 *                    ↓                       ↓
 *             isFormValid              publishedStrategy
 *             (按钮可用性)            (发布结果)
 *
 * @see StrategyRepository
 * @see com.aram.mayhem.feature.community.PublishStrategyFragment
 */
@HiltViewModel
public class PublishStrategyViewModel extends AndroidViewModel {

    private final StrategyRepository strategyRepository;

    private final MutableLiveData<Long> selectedHeroId = new MutableLiveData<>();
    private final MutableLiveData<String> title = new MutableLiveData<>();
    private final MutableLiveData<String> description = new MutableLiveData<>();
    private final MutableLiveData<List<Long>> selectedAugmentIds = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> selectedItemIds = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> publishing = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<StrategyDetailResponse> publishedStrategy = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFormValid = new MutableLiveData<>(false);

    @Inject
    public PublishStrategyViewModel(@NonNull Application application, StrategyRepository strategyRepository) {
        super(application);
        this.strategyRepository = strategyRepository;
    }

    public LiveData<Long> getSelectedHeroId() {
        return selectedHeroId;
    }

    public LiveData<String> getTitle() {
        return title;
    }

    public LiveData<String> getDescription() {
        return description;
    }

    public LiveData<List<Long>> getSelectedAugmentIds() {
        return selectedAugmentIds;
    }

    public LiveData<List<Long>> getSelectedItemIds() {
        return selectedItemIds;
    }

    public LiveData<Boolean> getPublishing() {
        return publishing;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<StrategyDetailResponse> getPublishedStrategy() {
        return publishedStrategy;
    }

    public LiveData<Boolean> getIsFormValid() {
        return isFormValid;
    }

    /**
     * 设置选中的英雄ID ── 用户从 AutoCompleteTextView 选择英雄时调用
     *
     * 设置后自动触发表单验证。
     *
     * @param heroId 英雄ID
     */
    public void setSelectedHeroId(Long heroId) {
        selectedHeroId.setValue(heroId);
        validateForm();
    }

    /**
     * 设置攻略标题 ── 用户在 EditText 中输入时调用
     *
     * 设置后自动触发表单验证。
     *
     * @param title 攻略标题
     */
    public void setTitle(String title) {
        this.title.setValue(title);
        validateForm();
    }

    /**
     * 设置攻略描述 ── 用户在 EditText 中输入时调用
     *
     * 设置后自动触发表单验证。
     *
     * @param description 攻略描述/内容
     */
    public void setDescription(String description) {
        this.description.setValue(description);
        validateForm();
    }

    public void setSelectedAugmentIds(List<Long> augmentIds) {
        selectedAugmentIds.setValue(augmentIds);
    }

    public void setSelectedItemIds(List<Long> itemIds) {
        selectedItemIds.setValue(itemIds);
    }

    /**
     * 添加一个符文到选中列表 ── 用户在多选对话框中勾选时调用
     *
     * 避免重复添加：如果列表中已包含该 ID，则跳过。
     *
     * @param augmentId 符文ID
     */
    public void addAugment(Long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current == null) {
            current = new ArrayList<>();
        }
        if (!current.contains(augmentId)) {
            current.add(augmentId);
            selectedAugmentIds.setValue(current);
        }
    }

    /**
     * 从选中列表移除一个符文 ── 用户点击 Chip 的关闭图标时调用
     *
     * @param augmentId 符文ID
     */
    public void removeAugment(Long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current != null) {
            current.remove(augmentId);
            selectedAugmentIds.setValue(current);
        }
    }

    /**
     * 添加一个装备到选中列表 ── 用户在多选对话框中勾选时调用
     *
     * @param itemId 装备ID
     */
    public void addItem(Long itemId) {
        List<Long> current = selectedItemIds.getValue();
        if (current == null) {
            current = new ArrayList<>();
        }
        if (!current.contains(itemId)) {
            current.add(itemId);
            selectedItemIds.setValue(current);
        }
    }

    /**
     * 从选中列表移除一个装备 ── 用户点击 Chip 的关闭图标时调用
     *
     * @param itemId 装备ID
     */
    public void removeItem(Long itemId) {
        List<Long> current = selectedItemIds.getValue();
        if (current != null) {
            current.remove(itemId);
            selectedItemIds.setValue(current);
        }
    }

    /**
     * 提交发布攻略 ── 核心方法，将表单数据提交到服务器
     *
     * 执行流程：
     * 1. 表单验证：英雄、标题、描述必须填写，描述至少10个字符
     * 2. 设置发布状态 publishing=true
     * 3. 调用 Repository 发布攻略
     * 4. 成功 → 设置 publishedStrategy（Fragment 观察到后跳转）
     * 5. 失败 → 设置 error 提示用户重试
     *
     * null 安全处理：
     * - augmentIds 和 itemIds 可能为 null，默认传空列表
     * - 标题和描述 trim() 后再提交，去除首尾空格
     */
    public void publish() {
        Long heroId = selectedHeroId.getValue();
        String titleStr = title.getValue();
        String descStr = description.getValue();

        if (heroId == null || titleStr == null || titleStr.trim().isEmpty() ||
            descStr == null || descStr.trim().length() < 10) {
            error.setValue("请填写完整信息，描述至少10个字");
            return;
        }

        publishing.setValue(true);
        error.setValue(null);

        List<Long> augments = selectedAugmentIds.getValue();
        List<Long> items = selectedItemIds.getValue();

        strategyRepository.publishStrategy(heroId, titleStr.trim(), descStr.trim(),
                augments != null ? augments : new ArrayList<>(),
                items != null ? items : new ArrayList<>())
                .observeForever(result -> {
                    publishing.setValue(false);
                    if (result != null) {
                        publishedStrategy.setValue(result);
                    } else {
                        error.setValue("发布失败，请重试");
                    }
                });
    }

    /**
     * 表单验证 ── 检查必填字段是否满足要求
     *
     * 验证规则：
     * - heroId 不为 null
     * - title 不为 null 且 trim 后不为空
     * - description 不为 null 且 trim 后长度 >= 10
     *
     * 每次设置 heroId/title/description 时自动调用，
     * 实时更新 isFormValid，控制发布按钮的 enabled 状态。
     */
    private void validateForm() {
        Long heroId = selectedHeroId.getValue();
        String titleStr = title.getValue();
        String descStr = description.getValue();

        boolean valid = heroId != null &&
                titleStr != null && !titleStr.trim().isEmpty() &&
                descStr != null && descStr.trim().length() >= 10;

        isFormValid.setValue(valid);
    }

    /**
     * 重置表单 ── 清空所有字段和状态
     *
     * 用于发布成功后清空表单，或用户主动放弃编辑时重置。
     */
    public void reset() {
        selectedHeroId.setValue(null);
        title.setValue(null);
        description.setValue(null);
        selectedAugmentIds.setValue(new ArrayList<>());
        selectedItemIds.setValue(new ArrayList<>());
        error.setValue(null);
        publishedStrategy.setValue(null);
        isFormValid.setValue(false);
    }
}
