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

@HiltViewModel
public class StrategyDetailViewModel extends AndroidViewModel {

    private final StrategyRepository strategyRepository;

    private final MutableLiveData<StrategyDetailResponse> strategy = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> voteSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> currentVoteType = new MutableLiveData<>();

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