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
 * 发布攻略 ViewModel（社区模块）
 *
 * 功能：管理攻略发布表单数据、表单验证、提交发布请求
 * 数据流：用户输入 → ViewModel → StrategyRepository → 服务器
 *        服务器响应 → StrategyRepository → ViewModel → LiveData → UI
 *
 * @see StrategyRepository
 * @see com.aram.mayhem.feature.community.PublishStrategyFragment
 */
@HiltViewModel
public class PublishStrategyViewModel extends AndroidViewModel {

    /** 攻略数据仓库，提供网络请求能力 */
    private final StrategyRepository strategyRepository;

    /** 选中的英雄ID */
    private final MutableLiveData<Long> selectedHeroId = new MutableLiveData<>();
    /** 攻略标题 */
    private final MutableLiveData<String> title = new MutableLiveData<>();
    /** 攻略描述/内容 */
    private final MutableLiveData<String> description = new MutableLiveData<>();
    /** 选中的强化符文ID列表 */
    private final MutableLiveData<List<Long>> selectedAugmentIds = new MutableLiveData<>(new ArrayList<>());
    /** 选中的装备ID列表 */
    private final MutableLiveData<List<Long>> selectedItemIds = new MutableLiveData<>(new ArrayList<>());

    /** 是否正在发布中 */
    private final MutableLiveData<Boolean> publishing = new MutableLiveData<>(false);
    /** 错误信息 */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    /** 发布成功后的攻略详情数据 */
    private final MutableLiveData<StrategyDetailResponse> publishedStrategy = new MutableLiveData<>();
    /** 表单是否验证通过 */
    private final MutableLiveData<Boolean> isFormValid = new MutableLiveData<>(false);

    /**
     * 构造函数
     *
     * @param application Android 应用上下文
     * @param strategyRepository 攻略数据仓库（通过 Hilt 依赖注入）
     */
    @Inject
    public PublishStrategyViewModel(@NonNull Application application, StrategyRepository strategyRepository) {
        super(application);
        this.strategyRepository = strategyRepository;
    }

    /**
     * 获取选中英雄ID的可观察数据
     *
     * @return LiveData<Long> 选中的英雄ID
     */
    public LiveData<Long> getSelectedHeroId() {
        return selectedHeroId;
    }

    /**
     * 获取标题的可观察数据
     *
     * @return LiveData<String> 攻略标题
     */
    public LiveData<String> getTitle() {
        return title;
    }

    /**
     * 获取描述的可观察数据
     *
     * @return LiveData<String> 攻略描述
     */
    public LiveData<String> getDescription() {
        return description;
    }

    /**
     * 获取选中符文ID列表的可观察数据
     *
     * @return LiveData<List<Long>> 选中的强化符文ID列表
     */
    public LiveData<List<Long>> getSelectedAugmentIds() {
        return selectedAugmentIds;
    }

    /**
     * 获取选中装备ID列表的可观察数据
     *
     * @return LiveData<List<Long>> 选中的装备ID列表
     */
    public LiveData<List<Long>> getSelectedItemIds() {
        return selectedItemIds;
    }

    /**
     * 获取发布状态的可观察数据
     *
     * @return LiveData<Boolean> 是否正在发布中
     */
    public LiveData<Boolean> getPublishing() {
        return publishing;
    }

    /**
     * 获取错误信息的可观察数据
     *
     * @return LiveData<String> 错误信息
     */
    public LiveData<String> getError() {
        return error;
    }

    /**
     * 获取发布成功后攻略详情的可观察数据
     *
     * @return LiveData<StrategyDetailResponse> 攻略详情数据
     */
    public LiveData<StrategyDetailResponse> getPublishedStrategy() {
        return publishedStrategy;
    }

    /**
     * 获取表单验证状态的可观察数据
     *
     * @return LiveData<Boolean> 表单是否验证通过
     */
    public LiveData<Boolean> getIsFormValid() {
        return isFormValid;
    }

    /**
     * 设置选中的英雄ID
     *
     * @param heroId 英雄ID
     */
    public void setSelectedHeroId(Long heroId) {
        selectedHeroId.setValue(heroId);
        validateForm();
    }

    /**
     * 设置攻略标题
     *
     * @param title 攻略标题
     */
    public void setTitle(String title) {
        this.title.setValue(title);
        validateForm();
    }

    /**
     * 设置攻略描述
     *
     * @param description 攻略描述/内容
     */
    public void setDescription(String description) {
        this.description.setValue(description);
        validateForm();
    }

    /**
     * 设置选中的符文ID列表
     *
     * @param augmentIds 符文ID列表
     */
    public void setSelectedAugmentIds(List<Long> augmentIds) {
        selectedAugmentIds.setValue(augmentIds);
    }

    /**
     * 设置选中的装备ID列表
     *
     * @param itemIds 装备ID列表
     */
    public void setSelectedItemIds(List<Long> itemIds) {
        selectedItemIds.setValue(itemIds);
    }

    /**
     * 添加一个符文到选中列表
     *
     * @param augmentId 符文ID
     */
    public void addAugment(Long augmentId) {
        List<Long> current = selectedAugmentIds.getValue();
        if (current == null) {
            current = new ArrayList<>();
        }
        // 避免重复添加
        if (!current.contains(augmentId)) {
            current.add(augmentId);
            selectedAugmentIds.setValue(current);
        }
    }

    /**
     * 从选中列表移除一个符文
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
     * 添加一个装备到选中列表
     *
     * @param itemId 装备ID
     */
    public void addItem(Long itemId) {
        List<Long> current = selectedItemIds.getValue();
        if (current == null) {
            current = new ArrayList<>();
        }
        // 避免重复添加
        if (!current.contains(itemId)) {
            current.add(itemId);
            selectedItemIds.setValue(current);
        }
    }

    /**
     * 从选中列表移除一个装备
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
     * 提交发布攻略请求
     *
     * 流程：表单验证 → 设置发布状态 → 调用 Repository 发布 → 更新结果
     */
    public void publish() {
        Long heroId = selectedHeroId.getValue();
        String titleStr = title.getValue();
        String descStr = description.getValue();

        // 表单验证：英雄、标题、描述必须填写，描述至少10个字符
        if (heroId == null || titleStr == null || titleStr.trim().isEmpty() ||
            descStr == null || descStr.trim().length() < 10) {
            error.setValue("请填写完整信息，描述至少10个字");
            return;
        }

        // 设置发布状态
        publishing.setValue(true);
        error.setValue(null);

        List<Long> augments = selectedAugmentIds.getValue();
        List<Long> items = selectedItemIds.getValue();

        // 调用 Repository 发布攻略
        strategyRepository.publishStrategy(heroId, titleStr.trim(), descStr.trim(),
                augments != null ? augments : new ArrayList<>(),
                items != null ? items : new ArrayList<>())
                .observeForever(result -> {
                    // 发布完成，更新状态
                    publishing.setValue(false);
                    if (result != null) {
                        // 发布成功
                        publishedStrategy.setValue(result);
                    } else {
                        // 发布失败
                        error.setValue("发布失败，请重试");
                    }
                });
    }

    /**
     * 表单验证
     *
     * 验证规则：英雄ID不为空、标题不为空、描述至少10个字符
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
     * 重置表单数据
     *
     * 清空所有表单字段和状态
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