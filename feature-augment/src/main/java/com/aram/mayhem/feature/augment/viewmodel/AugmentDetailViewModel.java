package com.aram.mayhem.feature.augment.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.feature.augment.repository.AugmentRepository;
import com.aram.mayhem.ui.model.AugmentUiModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 符文详情 ViewModel ── 管理符文详情页的数据状态
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * AugmentDetailViewModel 是符文详情底部弹窗的"数据管家"，负责：
 * 1. 根据 augmentId 从 Repository 加载符文详情数据
 * 2. 管理加载状态（加载中/加载完成/加载失败）
 * 3. 支持离线模式（网络失败时回退到本地缓存）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、与其他 ViewModel 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * - AugmentViewModel：管理列表数据（分页、筛选）
 * - AugmentDetailViewModel：管理单个符文的完整数据
 * - SynergyProgressViewModel：管理套装进度数据
 * - AugmentRecommendViewModel：管理推荐数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   AugmentDetailBottomSheet
 *     → viewModel.loadAugmentDetail(5)
 *     → augmentRepository.getAugmentDetail(5)
 *     → 网络请求 / 本地缓存
 *     → augmentDetail LiveData 更新
 *     → BottomSheet 观察到数据变化，更新 UI
 *
 * @see com.aram.mayhem.feature.augment.AugmentDetailBottomSheet
 * @see AugmentRepository
 */
@HiltViewModel
public class AugmentDetailViewModel extends AndroidViewModel {

    /**
     * 符文数据仓库 ── 提供网络请求和本地缓存的数据访问
     */
    private final AugmentRepository augmentRepository;

    /**
     * 符文详情数据 ── 包含符文的完整信息
     *
     * 包含字段：名称、描述、品质、套装、胜率、选取率、平均排名、是否陷阱等
     */
    private final MutableLiveData<AugmentUiModel> augmentDetail = new MutableLiveData<>();

    /**
     * 加载状态 ── true=正在加载，false=加载完成
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息 ── 加载失败时的错误描述
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 构造函数 ── Hilt 自动注入依赖
     *
     * @param application       Android 应用上下文
     * @param augmentRepository 符文数据仓库（Hilt 自动注入单例）
     */
    @Inject
    public AugmentDetailViewModel(@NonNull Application application, AugmentRepository augmentRepository) {
        super(application);
        this.augmentRepository = augmentRepository;
    }

    public LiveData<AugmentUiModel> getAugmentDetail() {
        return augmentDetail;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    /**
     * 加载符文详情 ── 核心方法，根据 augmentId 获取完整符文信息
     *
     * 由 AugmentDetailBottomSheet.onViewCreated() 调用。
     *
     * 执行流程：
     * 1. 设置 loading=true
     * 2. 调用 Repository 获取详情数据
     * 3. 成功 → 设置 augmentDetail + loading=false
     * 4. 失败 → 设置 error + loading=false
     *
     * 注意：使用 observeForever 而不是 observe(LifecycleOwner)，
     * 因为 ViewModel 没有 LifecycleOwner。observeForever 需要
     * 手动管理生命周期，但这里只在数据返回时设置一次，风险可控。
     *
     * @param augmentId 符文唯一标识符，从 BottomSheet 的 Arguments 中获取
     */
    public void loadAugmentDetail(long augmentId) {
        loading.setValue(true);
        augmentRepository.getAugmentDetail(augmentId).observeForever(uiModel -> {
            loading.setValue(false);
            if (uiModel != null) {
                augmentDetail.setValue(uiModel);
            } else {
                error.setValue("无法加载符文详情");
            }
        });
    }
}
