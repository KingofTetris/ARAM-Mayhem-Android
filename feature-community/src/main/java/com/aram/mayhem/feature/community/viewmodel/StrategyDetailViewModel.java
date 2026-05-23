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
 * 攻略详情 ViewModel ── 管理攻略详情页的数据状态和投票逻辑
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * StrategyDetailViewModel 是攻略详情页的"数据管家"，负责：
 * 1. 根据 strategyId 从 Repository 加载攻略详情
 * 2. 管理加载状态（加载中/加载完成/加载失败）
 * 3. 处理投票操作（点赞/点踩/取消投票）
 * 4. 本地乐观更新投票计数（不等待服务器返回最新数据）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、投票机制详解
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户操作              ViewModel               Repository
 *   ┌──────────┐         ┌──────────────┐        ┌──────────┐
 *   │ 点赞     │ ──────→ │ vote("UP")   │ ─────→ │ POST /vote│
 *   │          │         │ 本地+1       │        │          │
 *   │ 点踩     │ ──────→ │ vote("DOWN") │ ─────→ │ POST /vote│
 *   │          │         │ 本地+1       │        │          │
 *   │ 取消投票 │ ──────→ │ cancelVote() │ ─────→ │ DELETE   │
 *   │          │         │ 本地-1       │        │          │
 *   └──────────┘         └──────────────┘        └──────────┘
 *
 * "乐观更新"策略：
 * - 投票成功后，本地立即修改 upvotes/downvotes 计数
 * - 不需要再发一次请求获取最新数据
 * - 如果投票失败，本地计数不会变化（因为只在 success 时更新）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   StrategyDetailFragment
 *     → viewModel.loadStrategy(42)
 *     → strategyRepository.getStrategyDetail(42)
 *     → 网络请求
 *     → strategy LiveData 更新
 *     → Fragment 观察到数据变化，调用 displayStrategy() 更新 UI
 *
 * @see StrategyRepository
 * @see com.aram.mayhem.feature.community.StrategyDetailFragment
 */
@HiltViewModel
public class StrategyDetailViewModel extends AndroidViewModel {

    private final StrategyRepository strategyRepository;

    private final MutableLiveData<StrategyDetailResponse> strategy = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> voteSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> currentVoteType = new MutableLiveData<>();

    /**
     * 当前攻略ID ── 用于投票操作和避免重复加载
     *
     * 初始值为 -1 表示未加载任何攻略。
     * loadStrategy() 时更新为实际 ID。
     */
    private long currentStrategyId = -1;

    @Inject
    public StrategyDetailViewModel(@NonNull Application application, StrategyRepository strategyRepository) {
        super(application);
        this.strategyRepository = strategyRepository;
    }

    public LiveData<StrategyDetailResponse> getStrategy() {
        return strategy;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getVoteSuccess() {
        return voteSuccess;
    }

    public LiveData<String> getCurrentVoteType() {
        return currentVoteType;
    }

    /**
     * 加载攻略详情 ── 核心方法，根据 strategyId 获取完整攻略信息
     *
     * 执行流程：
     * 1. 避免重复加载：相同 ID 且已有数据则直接返回
     * 2. 更新当前攻略 ID
     * 3. 设置 loading=true
     * 4. 调用 Repository 获取详情数据
     * 5. 成功 → 设置 strategy + 记录用户投票状态
     * 6. 失败 → 设置 error
     *
     * 为什么需要避免重复加载？
     * - 屏幕旋转时 Fragment 会重建，但 ViewModel 保留
     * - 如果数据已经加载过，不需要再发网络请求
     *
     * @param strategyId 攻略唯一标识符，从 Fragment 的 Arguments 中获取
     */
    public void loadStrategy(long strategyId) {
        if (strategyId == currentStrategyId && strategy.getValue() != null) {
            return;
        }

        currentStrategyId = strategyId;
        loading.setValue(true);
        error.setValue(null);

        strategyRepository.getStrategyDetail(strategyId).observeForever(detail -> {
            loading.setValue(false);
            if (detail != null) {
                strategy.setValue(detail);
                currentVoteType.setValue(detail.getUserVoteType());
            } else {
                error.setValue("加载失败，请重试");
            }
        });
    }

    /**
     * 发起投票 ── 对当前攻略进行点赞或点踩
     *
     * ═══════════════════════════════════════════════════════════════════
     * 投票状态机
     * ═══════════════════════════════════════════════════════════════════
     *
     *   未投票 → vote("UP") → 已点赞
     *   未投票 → vote("DOWN") → 已点踩
     *   已点赞 → cancelVote() → 未投票
     *   已点踩 → cancelVote() → 未投票
     *   已点赞 → cancelVote() + vote("DOWN") → 已点踩（改投）
     *
     * 投票成功后：
     * 1. 更新 currentVoteType（记录当前投票状态）
     * 2. 调用 updateVoteCount() 本地更新投票计数
     *
     * @param voteType 投票类型："UP"=点赞，"DOWN"=点踩
     */
    public void vote(String voteType) {
        long strategyId = currentStrategyId;
        if (strategyId == -1) return;

        strategyRepository.vote(strategyId, voteType).observeForever(success -> {
            voteSuccess.setValue(success);
            if (success) {
                currentVoteType.setValue(voteType);
                updateVoteCount(voteType, 1);
            }
        });
    }

    /**
     * 取消投票 ── 撤销对当前攻略的投票
     *
     * 取消成功后：
     * 1. 清空 currentVoteType（回到未投票状态）
     * 2. 根据之前的投票类型，本地减1
     *
     * 注意：必须先记录 previousVoteType，因为取消成功后
     * currentVoteType 会被清空，就不知道之前投的是什么了。
     */
    public void cancelVote() {
        long strategyId = currentStrategyId;
        if (strategyId == -1) return;

        String previousVoteType = currentVoteType.getValue();

        strategyRepository.cancelVote(strategyId).observeForever(success -> {
            voteSuccess.setValue(success);
            if (success) {
                if (previousVoteType != null) {
                    updateVoteCount(previousVoteType, -1);
                }
                currentVoteType.setValue(null);
            }
        });
    }

    /**
     * 更新投票计数（本地乐观更新）── 不等待服务器返回最新数据
     *
     * ═══════════════════════════════════════════════════════════════════
     * 为什么用乐观更新？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 传统方式：投票 → 等待服务器成功 → 重新请求攻略详情 → 更新 UI
     * 乐观更新：投票 → 等待服务器成功 → 本地直接修改计数 → 更新 UI
     *
     * 乐观更新更快，用户体验更好（投票后立即看到数字变化）。
     * 风险：如果本地计数和服务器不同步，可能显示错误数字。
     * 但投票数不需要精确，差1不影响体验。
     *
     * @param voteType 投票类型："UP" 或 "DOWN"
     * @param delta    变化量：+1（投票）或 -1（取消投票）
     */
    private void updateVoteCount(String voteType, int delta) {
        StrategyDetailResponse current = strategy.getValue();
        if (current != null) {
            if ("UP".equals(voteType)) {
                int newUpvotes = (current.getUpvotes() != null ? current.getUpvotes() : 0) + delta;
                current.setUpvotes(Math.max(0, newUpvotes));
            } else if ("DOWN".equals(voteType)) {
                int newDownvotes = (current.getDownvotes() != null ? current.getDownvotes() : 0) + delta;
                current.setDownvotes(Math.max(0, newDownvotes));
            }
            strategy.setValue(current);
        }
    }
}
