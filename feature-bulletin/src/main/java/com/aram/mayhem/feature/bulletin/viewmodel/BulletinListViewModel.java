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

@HiltViewModel
/**
 * 公告列表 ViewModel
 *
 * 功能：管理公告列表数据（分页加载、类型筛选）
 * 数据流：BulletinApi → LiveData<BulletinUiModel> → BulletinListFragment
 */
public class BulletinListViewModel extends ViewModel {

    private final BulletinApi bulletinApi;

    private final MutableLiveData<List<BulletinUiModel>> bulletins = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<BulletinUiModel>> carouselBulletins = new MutableLiveData<>();

    private int currentPage = 1;
    private boolean hasMore = true;
    private String currentType = null;

    @Inject
    public BulletinListViewModel(BulletinApi bulletinApi) {
        this.bulletinApi = bulletinApi;
    }

    public LiveData<List<BulletinUiModel>> getBulletins() { return bulletins; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<BulletinUiModel>> getCarouselBulletins() { return carouselBulletins; }

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

    public void loadBulletins(String type, boolean refresh) {
        if (Boolean.TRUE.equals(loading.getValue())) return;

        if (refresh) {
            currentPage = 1;
            hasMore = true;
            currentType = type;
        }

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

                    if (refresh || bulletins.getValue() == null) {
                        bulletins.setValue(newItems);
                    } else {
                        List<BulletinUiModel> existing = new ArrayList<>(bulletins.getValue());
                        existing.addAll(newItems);
                        bulletins.setValue(existing);
                    }

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

    public void loadMore() {
        loadBulletins(currentType, false);
    }

    public boolean hasMore() {
        return hasMore;
    }

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
