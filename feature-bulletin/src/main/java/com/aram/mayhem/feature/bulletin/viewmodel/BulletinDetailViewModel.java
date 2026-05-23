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
 * 公告详情 ViewModel ── 管理公告详情页的数据状态
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * BulletinDetailViewModel 负责加载单条公告的详情数据，供
 * BulletinDetailFragment 展示。与 BulletinListViewModel 不同，
 * 本 ViewModel 只处理一条公告的数据，不涉及分页和缓存。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   Fragment                     ViewModel                    网络
 *   ┌──────────┐                ┌──────────────┐            ┌──────────┐
 *   │ 传入ID   │ ──loadDetail()─→│ 发起请求     │ ──enqueue()─→│ GET /{id}│
 *   │          │                │              │            │          │
 *   │ 观察数据 │ ←──setValue()──│ 转换为UiModel│ ←──onResponse─│ 返回JSON │
 *   └──────────┘                └──────────────┘            └──────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与 BulletinListViewModel 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * ┌──────────────────┬──────────────────────┬──────────────────────┐
 * │ 对比项           │ BulletinListViewModel│ BulletinDetailVM     │
 * ├──────────────────┼──────────────────────┼──────────────────────┤
 * │ 父类             │ AndroidViewModel     │ ViewModel            │
 * │ 数据量           │ 公告列表（多条）     │ 单条公告详情         │
 * │ 分页             │ 有                   │ 无                   │
 * │ 本地缓存         │ 有（BulletinDao）    │ 无                   │
 * │ 离线支持         │ 有                   │ 无                   │
 * │ 轮播             │ 有                   │ 无                   │
 * └──────────────────┴──────────────────────┴──────────────────────┘
 *
 * 为什么详情页不使用本地缓存？
 * 1. 详情页只在用户主动点击时才加载，频率远低于列表页
 * 2. 详情内容可能包含图片等大文本，缓存策略更复杂
 * 3. 用户查看详情时期望看到最新数据，缓存可能导致信息过时
 *
 * @see BulletinApi
 * @see com.aram.mayhem.feature.bulletin.BulletinDetailFragment
 */
@HiltViewModel
public class BulletinDetailViewModel extends ViewModel {

    /**
     * 公告 API 接口 ── 用于发起网络请求获取公告详情
     *
     * 由 Hilt 自动注入，提供 getBulletinDetail(id) 方法。
     */
    private final BulletinApi bulletinApi;

    /**
     * 公告详情数据 ── 当前展示的公告完整内容
     *
     * Fragment 观察此 LiveData，数据变化时更新页面显示：
     * - 标题、类型、发布时间、正文内容、封面图片
     */
    private final MutableLiveData<BulletinUiModel> bulletinDetail = new MutableLiveData<>();

    /**
     * 加载状态 ── true 表示正在请求网络数据
     *
     * Fragment 观察此 LiveData 来控制加载进度条的显示/隐藏
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息 ── 网络请求失败或公告不存在时的错误提示
     *
     * Fragment 观察此 LiveData 来显示错误提示
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 构造函数 ── Hilt 自动注入 BulletinApi
     *
     * @param bulletinApi 公告 API 接口（Hilt 注入）
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
     * 加载公告详情 ── 根据公告ID从服务器获取完整内容
     *
     * ═══════════════════════════════════════════════════════════
     * 加载流程：
     * ═══════════════════════════════════════════════════════════
     *
     *   1. 防重复加载：如果 loading 为 true，直接返回
     *   2. 设置 loading = true，显示加载动画
     *   3. 调用 bulletinApi.getBulletinDetail(id) 发起网络请求
     *   4. 成功响应：
     *      a. 将 BulletinResponse 转换为 BulletinUiModel
     *      b. isPinned 转换：Integer(0/1) → boolean
     *      c. 设置 bulletinDetail，触发 Fragment 更新 UI
     *   5. 失败响应：
     *      a. 设置 error = "公告不存在"
     *   6. 网络异常：
     *      a. 设置 error = 异常信息
     *
     * @param id 公告ID，由 Fragment 从导航参数中获取
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
