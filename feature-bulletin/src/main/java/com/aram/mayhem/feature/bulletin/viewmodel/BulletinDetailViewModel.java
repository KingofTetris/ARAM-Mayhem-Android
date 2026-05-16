package com.aram.mayhem.feature.bulletin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aram.mayhem.network.api.BulletinApi;
import com.aram.mayhem.network.dto.BulletinResponse;
import com.aram.mayhem.ui.model.BulletinUiModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * 公告详情 ViewModel（公告模块）
 *
 * 功能：加载公告详情数据，管理详情页状态
 * 数据流：BulletinApi → LiveData<BulletinUiModel> → BulletinDetailFragment
 *
 * @see BulletinApi
 * @see com.aram.mayhem.feature.bulletin.BulletinDetailFragment
 */
@HiltViewModel
public class BulletinDetailViewModel extends ViewModel {

    /** 公告 API 接口 */
    private final BulletinApi bulletinApi;

    /** 公告详情数据 */
    private final MutableLiveData<BulletinUiModel> bulletinDetail = new MutableLiveData<>();
    /** 是否正在加载 */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 错误信息 */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 构造函数
     *
     * @param bulletinApi 公告 API 接口（通过 Hilt 依赖注入）
     */
    @Inject
    public BulletinDetailViewModel(BulletinApi bulletinApi) {
        this.bulletinApi = bulletinApi;
    }

    /**
     * 获取公告详情的可观察数据
     *
     * @return LiveData<BulletinUiModel> 公告详情
     */
    public LiveData<BulletinUiModel> getBulletinDetail() {
        return bulletinDetail;
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
     * 加载公告详情
     *
     * 作用：根据公告ID从服务器获取公告详情数据
     * 实现：调用 BulletinApi.getBulletinDetail()，成功后转换为 UI 模型
     *
     * @param id 公告ID
     */
    public void loadBulletinDetail(long id) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        loading.setValue(true);
        bulletinApi.getBulletinDetail(id).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<com.aram.mayhem.common.Result<BulletinResponse>> call,
                                   Response<com.aram.mayhem.common.Result<BulletinResponse>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    BulletinResponse br = response.body().getData();
                    BulletinUiModel model = new BulletinUiModel(
                            br.getId(), br.getType(), br.getTitle(), br.getContent(),
                            br.getImageUrl(), br.getIsPinned() != null && br.getIsPinned() == 1,
                            br.getPublishedAt(), br.getCreatedAt()
                    );
                    bulletinDetail.setValue(model);
                    Timber.d("Loaded bulletin detail: id=%d", id);
                } else {
                    error.setValue("公告不存在");
                    Timber.w("Bulletin detail not found: id=%d", id);
                }
            }

            @Override
            public void onFailure(Call<com.aram.mayhem.common.Result<BulletinResponse>> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t.getMessage());
                Timber.e(t, "Failed to load bulletin detail: id=%d", id);
            }
        });
    }
}
