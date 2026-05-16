package com.aram.mayhem.feature.community.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.community.repository.StrategyRepository;
import com.aram.mayhem.network.dto.StrategyDetailResponse;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 攻略详情 ViewModel（社区模块）
 *
 * 功能：加载攻略详情、管理投票状态、更新投票计数
 * 数据流：StrategyRepository → LiveData<StrategyDetailResponse> → StrategyDetailFragment
 *
 * @see StrategyRepository
 * @see com.aram.mayhem.feature.community.StrategyDetailFragment
 */
@HiltViewModel
public class StrategyDetailViewModel extends AndroidViewModel {

    /** 攻略数据仓库 */
    private final StrategyRepository strategyRepository;

    /** 攻略详情数据 */
    private final MutableLiveData<StrategyDetailResponse> strategy = new MutableLiveData<>();
    /** 是否正在加载 */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 错误信息 */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    /** 投票是否成功 */
    private final MutableLiveData<Boolean> voteSuccess = new MutableLiveData<>();
    /** 当前用户投票类型（UP/DOWN/null） */
    private final MutableLiveData<String> currentVoteType = new MutableLiveData<>();

    /** 当前攻略ID（用于投票操作） */
    private long currentStrategyId = -1;

    /**
     * 构造函数
     *
     * @param application Android 应用上下文
     * @param strategyRepository 攻略数据仓库（通过 Hilt 依赖注入）
     */
    @Inject
    public StrategyDetailViewModel(@NonNull Application application, StrategyRepository strategyRepository) {
        super(application);
        this.strategyRepository = strategyRepository;
    }

    /**
     * 获取攻略详情的可观察数据
     *
     * @return LiveData<StrategyDetailResponse> 攻略详情
     */
    public LiveData<StrategyDetailResponse> getStrategy() {
        return strategy;
    }

    /**
     * 获取加载状态的可观察数据
     *
     * @return LiveData<Boolean> 是否正在加载
     */
    public LiveData<Boolean> getLoading() {
        return loading;
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
     * 获取投票成功状态的可观察数据
     *
     * @return LiveData<Boolean> 投票是否成功
     */
    public LiveData<Boolean> getVoteSuccess() {
        return voteSuccess;
    }

    /**
     * 获取当前用户投票类型的可观察数据
     *
     * @return LiveData<String> 投票类型（UP/DOWN/null）
     */
    public LiveData<String> getCurrentVoteType() {
        return currentVoteType;
    }

    /**
     * 加载攻略详情
     *
     * @param strategyId 攻略ID
     */
    public void loadStrategy(long strategyId) {
        // 避免重复加载：相同ID且已有数据则直接返回
        if (strategyId == currentStrategyId && strategy.getValue() != null) {
            return;
        }

        // 更新当前攻略ID
        currentStrategyId = strategyId;
        loading.setValue(true);
        error.setValue(null);

        // 调用 Repository 获取攻略详情
        strategyRepository.getStrategyDetail(strategyId).observeForever(detail -> {
            loading.setValue(false);
            if (detail != null) {
                // 加载成功
                strategy.setValue(detail);
                // 记录用户当前投票状态
                currentVoteType.setValue(detail.getUserVoteType());
            } else {
                // 加载失败
                error.setValue("加载失败，请重试");
            }
        });
    }

    /**
     * 发起投票
     *
     * @param voteType 投票类型（UP-点赞 / DOWN-点踩）
     */
    public void vote(String voteType) {
        long strategyId = currentStrategyId;
        if (strategyId == -1) return;

        // 调用 Repository 投票
        strategyRepository.vote(strategyId, voteType).observeForever(success -> {
            voteSuccess.setValue(success);
            if (success) {
                // 投票成功，更新本地状态
                currentVoteType.setValue(voteType);
                // 更新投票计数（本地缓存）
                updateVoteCount(voteType, 1);
            }
        });
    }

    /**
     * 取消投票
     */
    public void cancelVote() {
        long strategyId = currentStrategyId;
        if (strategyId == -1) return;

        // 记录当前投票类型（用于后续更新计数）
        String previousVoteType = currentVoteType.getValue();

        // 调用 Repository 取消投票
        strategyRepository.cancelVote(strategyId).observeForever(success -> {
            voteSuccess.setValue(success);
            if (success) {
                // 取消成功，更新投票计数（减1）
                if (previousVoteType != null) {
                    updateVoteCount(previousVoteType, -1);
                }
                // 清空投票类型
                currentVoteType.setValue(null);
            }
        });
    }

    /**
     * 更新投票计数（本地缓存）
     *
     * @param voteType 投票类型（UP/DOWN）
     * @param delta 变化量（+1或-1）
     */
    private void updateVoteCount(String voteType, int delta) {
        StrategyDetailResponse current = strategy.getValue();
        if (current != null) {
            if ("UP".equals(voteType)) {
                // 更新点赞数
                int newUpvotes = (current.getUpvotes() != null ? current.getUpvotes() : 0) + delta;
                current.setUpvotes(Math.max(0, newUpvotes));
            } else if ("DOWN".equals(voteType)) {
                // 更新点踩数
                int newDownvotes = (current.getDownvotes() != null ? current.getDownvotes() : 0) + delta;
                current.setDownvotes(Math.max(0, newDownvotes));
            }
            // 通知UI更新
            strategy.setValue(current);
        }
    }
}