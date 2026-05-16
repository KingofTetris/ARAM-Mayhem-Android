package com.aram.mayhem.feature.bulletin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.network.api.BulletinApi;
import com.aram.mayhem.network.dto.BulletinResponse;
import com.aram.mayhem.network.dto.PageResponse;
import com.aram.mayhem.ui.model.BulletinUiModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

/**
 * 公告列表 ViewModel（公告模块）
 *
 * 功能：管理公告列表数据（分页加载、类型筛选、轮播数据）
 * 数据流：BulletinApi → LiveData<BulletinUiModel> → BulletinListFragment
 *
 * @see BulletinApi
 * @see com.aram.mayhem.feature.bulletin.BulletinListFragment
 */
@HiltViewModel
public class BulletinListViewModel extends ViewModel {

    /** 公告 API 接口 */
    private final BulletinApi bulletinApi;

    /** 公告列表数据 */
    private final MutableLiveData<List<BulletinUiModel>> bulletins = new MutableLiveData<>();
    /** 是否正在加载 */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** 错误信息 */
    private final MutableLiveData<String> error = new MutableLiveData<>();
    /** 轮播公告数据（首页顶部展示） */
    private final MutableLiveData<List<BulletinUiModel>> carouselBulletins = new MutableLiveData<>();

    /** 当前页码（从1开始） */
    private int currentPage = 1;
    /** 是否还有更多数据 */
    private boolean hasMore = true;
    /** 当前类型筛选条件 */
    private String currentType = null;

    /**
     * 构造函数
     *
     * @param bulletinApi 公告 API 接口（通过 Hilt 依赖注入）
     */
    @Inject
    public BulletinListViewModel(BulletinApi bulletinApi) {
        this.bulletinApi = bulletinApi;
    }

    /**
     * 获取公告列表的可观察数据
     *
     * @return LiveData<List<BulletinUiModel>> 公告列表
     */
    public LiveData<List<BulletinUiModel>> getBulletins() { return bulletins; }

    /**
     * 获取加载状态的可观察数据
     *
     * @return LiveData<Boolean> 是否正在加载
     */
    public LiveData<Boolean> getLoading() { return loading; }

    /**
     * 获取错误信息的可观察数据
     *
     * @return LiveData<String> 错误信息
     */
    public LiveData<String> getError() { return error; }

    /**
     * 获取轮播公告的可观察数据
     *
     * @return LiveData<List<BulletinUiModel>> 轮播公告列表
     */
    public LiveData<List<BulletinUiModel>> getCarouselBulletins() { return carouselBulletins; }

    /**
     * 加载轮播公告数据
     *
     * 作用：获取最新的3条公告作为轮播图展示数据
     * 实现：调用 BulletinApi.getLatestBulletins(3)，成功后转换为 UI 模型
     */
    public void loadCarouselBulletins() {
        bulletinApi.getLatestBulletins(3).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<Result<List<BulletinResponse>>> call,
                                   retrofit2.Response<Result<List<BulletinResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<BulletinUiModel> models = new ArrayList<>();
                    for (BulletinResponse br : response.body().getData()) {
                        models.add(convertToUiModel(br));
                    }
                    carouselBulletins.setValue(models);
                    Timber.d("Loaded %d carousel bulletins", models.size());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Result<List<BulletinResponse>>> call, Throwable t) {
                Timber.e(t, "Failed to load carousel bulletins");
            }
        });
    }

    /**
     * 加载公告列表
     *
     * 作用：根据类型筛选和分页参数加载公告列表
     * 实现：调用 BulletinApi.getBulletins()，支持刷新和分页加载
     *
     * @param type 公告类型（version/event/notice），null 表示全部
     * @param refresh 是否刷新（重新从第一页加载）
     */
    public void loadBulletins(String type, boolean refresh) {
        // 防止重复加载
        if (Boolean.TRUE.equals(loading.getValue())) return;

        // 刷新时重置分页状态
        if (refresh) {
            currentPage = 1;
            hasMore = true;
            currentType = type;
        }

        // 没有更多数据且不是刷新，则不加载
        if (!hasMore && !refresh) return;

        loading.setValue(true);
        bulletinApi.getBulletins(currentType, currentPage, 10).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<Result<PageResponse<BulletinResponse>>> call,
                                   retrofit2.Response<Result<PageResponse<BulletinResponse>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    PageResponse<BulletinResponse> data = response.body().getData();
                    List<BulletinUiModel> newItems = new ArrayList<>();
                    for (BulletinResponse br : data.getRecords()) {
                        newItems.add(convertToUiModel(br));
                    }

                    // 刷新或首次加载直接替换列表，否则追加数据
                    if (refresh || bulletins.getValue() == null) {
                        bulletins.setValue(newItems);
                    } else {
                        List<BulletinUiModel> existing = new ArrayList<>(bulletins.getValue());
                        existing.addAll(newItems);
                        bulletins.setValue(existing);
                    }

                    // 判断是否还有更多数据
                    hasMore = data.getTotal() > (long) data.getPage() * data.getSize();
                    currentPage++;
                    Timber.d("Loaded %d bulletins, hasMore=%s", newItems.size(), hasMore);
                } else {
                    error.setValue("加载公告失败");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Result<PageResponse<BulletinResponse>>> call, Throwable t) {
                loading.setValue(false);
                error.setValue("网络错误：" + t.getMessage());
                Timber.e(t, "Failed to load bulletins");
            }
        });
    }

    /**
     * 加载更多公告
     *
     * 作用：分页加载下一页公告数据
     */
    public void loadMore() {
        loadBulletins(currentType, false);
    }

    /**
     * 判断是否还有更多数据
     *
     * @return 是否还有更多数据
     */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * 将网络响应转换为 UI 模型
     *
     * @param response 网络响应数据
     * @return UI 模型
     */
    private BulletinUiModel convertToUiModel(BulletinResponse response) {
        return new BulletinUiModel(
                response.getId(),
                response.getType(),
                response.getTitle(),
                response.getContent(),
                response.getImageUrl(),
                response.getIsPinned() != null && response.getIsPinned() == 1,
                response.getPublishedAt(),
                response.getCreatedAt()
        );
    }
}
