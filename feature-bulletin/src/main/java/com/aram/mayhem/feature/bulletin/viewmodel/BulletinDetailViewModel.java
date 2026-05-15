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

@HiltViewModel
public class BulletinDetailViewModel extends ViewModel {

    private final BulletinApi bulletinApi;

    private final MutableLiveData<BulletinUiModel> bulletinDetail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    @Inject
    public BulletinDetailViewModel(BulletinApi bulletinApi) {
        this.bulletinApi = bulletinApi;
    }

    public LiveData<BulletinUiModel> getBulletinDetail() { return bulletinDetail; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void loadBulletinDetail(long id) {
        if (Boolean.TRUE.equals(loading.getValue())) return;

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
