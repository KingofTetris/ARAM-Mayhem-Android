package com.aram.mayhem.feature.bulletin.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aram.mayhem.common.Result;
import com.aram.mayhem.data.local.dao.BulletinDao;
import com.aram.mayhem.data.local.entity.BulletinEntity;
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
 * 公告列表 ViewModel ── 管理公告列表页的数据状态和业务逻辑
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 ViewModel 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * BulletinListViewModel 是公告列表页（BulletinListFragment）的数据管理中心，
 * 负责以下核心功能：
 * 1. 加载公告列表数据（支持分页和类型筛选）
 * 2. 加载轮播公告数据（最新3条置顶公告）
 * 3. 管理离线模式（网络断开时使用缓存数据）
 * 4. 在网络数据与本地缓存之间进行数据转换
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌───────────────────────────────────────────────────────────────┐
 *   │                    BulletinListViewModel                      │
 *   │                                                               │
 *   │  BulletinApi（网络）    BulletinDao（本地缓存）               │
 *   │       │                        │                              │
 *   │       ▼                        ▼                              │
 *   │  BulletinResponse       BulletinEntity                        │
 *   │       │                        │                              │
 *   │       └──── convert ────┬──── convert ────┐                   │
 *   │                         ▼                 ▼                   │
 *   │                   BulletinUiModel    BulletinEntity           │
 *   │                         │           (写入缓存)                │
 *   │                         ▼                                    │
 *   │              LiveData<List<BulletinUiModel>>                  │
 *   │                    (公告列表)                                 │
 *   │              LiveData<List<BulletinUiModel>>                  │
 *   │                   (轮播公告)                                  │
 *   └───────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、离线策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 采用"缓存优先 + 网络刷新"策略（与 StrategyRepository 相同）：
 *
 *   ┌───────────────────────────────────────────────────────────────┐
 *   │  1. 先从本地缓存读取数据，立即返回给 UI（快速响应）           │
 *   │  2. 再发起网络请求获取最新数据                               │
 *   │  3. 网络成功 → 更新 UI + 异步写入缓存                       │
 *   │  4. 网络失败 → 保留缓存数据（用户仍能看到旧数据）            │
 *   │  5. 离线模式 → 跳过网络请求，只返回缓存                     │
 *   └───────────────────────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、分页加载机制
 * ═══════════════════════════════════════════════════════════════════
 *
 *   第1次加载              第2次加载（loadMore）     第3次加载（loadMore）
 *   ┌──────────┐          ┌──────────────────┐     ┌────────────────────┐
 *   │ page=1   │          │ page=1 + page=2  │     │ page=1+2+3        │
 *   │ 10条公告  │    →     │ 20条公告          │  →  │ 30条公告           │
 *   └──────────┘          └──────────────────┘     └────────────────────┘
 *
 *   hasMore 判断逻辑：total > page × size 时还有更多数据
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、公告类型筛选
 * ═══════════════════════════════════════════════════════════════════
 *
 *   Tab 标签        类型参数        说明
 *   ┌──────────┬──────────────┬──────────────────┐
 *   │ 全部     │ null         │ 不筛选，显示所有  │
 *   │ 版本更新 │ "version"    │ 版本更新公告      │
 *   │ 活动     │ "event"      │ 活动公告          │
 *   │ 通知     │ "notice"     │ 通知公告          │
 *   └──────────┴──────────────┴──────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、三种数据模型的转换关系
 * ═══════════════════════════════════════════════════════════════════
 *
 *   BulletinResponse（网络DTO）  ←→  BulletinEntity（本地缓存）
 *              │                           │
 *              └──────→ BulletinUiModel ←──┘
 *                    （UI 展示模型）
 *
 * - BulletinResponse：后端返回的 JSON 对应的 Java 类
 * - BulletinEntity：Room 数据库的实体类，用于本地缓存
 * - BulletinUiModel：UI 层使用的数据模型，包含格式化后的字段
 *
 * @see BulletinApi
 * @see BulletinDao
 * @see BulletinUiModel
 * @see com.aram.mayhem.feature.bulletin.BulletinListFragment
 */
@HiltViewModel
public class BulletinListViewModel extends AndroidViewModel {

    /**
     * 公告 API 接口 ── 用于发起网络请求
     *
     * 由 Hilt 自动注入，提供以下方法：
     * - getLatestBulletins(count)：获取最新N条公告（轮播用）
     * - getBulletins(type, page, size)：分页获取公告列表
     */
    private final BulletinApi bulletinApi;

    /**
     * 公告 DAO ── 用于读写本地缓存数据库
     *
     * 由 Hilt 自动注入，提供以下方法：
     * - getLatestBulletins(count)：从缓存获取最新N条公告
     * - getBulletinsByType(type)：按类型获取缓存公告
     * - getAllBulletins()：获取所有缓存公告
     * - insertAll(entities)：批量插入缓存数据
     */
    private final BulletinDao bulletinDao;

    /**
     * 公告列表数据 ── 展示在 RecyclerView 中的公告列表
     *
     * 数据来源有两种：
     * 1. 网络请求成功 → 替换或追加新数据
     * 2. 本地缓存 → 离线时提供数据
     */
    private final MutableLiveData<List<BulletinUiModel>> bulletins = new MutableLiveData<>();

    /**
     * 加载状态 ── true 表示正在请求网络数据
     *
     * Fragment 观察此 LiveData 来控制加载动画的显示/隐藏
     */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /**
     * 错误信息 ── 网络请求失败或数据异常时的错误提示
     *
     * Fragment 观察此 LiveData 来显示错误页面
     */
    private final MutableLiveData<String> error = new MutableLiveData<>();

    /**
     * 轮播公告数据 ── 顶部轮播图展示的最新3条公告
     *
     * 轮播公告与列表公告是独立加载的：
     * - 轮播公告：loadCarouselBulletins()，获取最新3条
     * - 列表公告：loadBulletins()，分页获取全部
     */
    private final MutableLiveData<List<BulletinUiModel>> carouselBulletins = new MutableLiveData<>();

    /**
     * 当前页码 ── 分页加载的页码计数器
     *
     * 初始值为 1，每次成功加载后递增。
     * 下拉刷新时重置为 1。
     */
    private int currentPage = 1;

    /**
     * 是否还有更多数据 ── 控制是否继续加载下一页
     *
     * 计算公式：hasMore = total > page × size
     * 当 hasMore 为 false 时，loadMore() 不会发起网络请求。
     */
    private boolean hasMore = true;

    /**
     * 当前筛选类型 ── null 表示"全部"，其他值为 "version"/"event"/"notice"
     *
     * 切换 Tab 时更新此值，并重新加载对应类型的公告。
     */
    private String currentType = null;

    /**
     * 是否处于离线模式 ── 由 Fragment 的网络监听器设置
     *
     * 离线模式下：
     * - 不发起网络请求
     * - 只从本地缓存读取数据
     * - 网络恢复后由 Fragment 调用 loadBulletins() 刷新
     */
    private volatile boolean isOffline = false;

    /**
     * 构造函数 ── Hilt 自动注入依赖
     *
     * @param application Application 对象（AndroidViewModel 需要）
     * @param bulletinApi 公告 API 接口（Hilt 注入）
     * @param bulletinDao 公告 DAO（Hilt 注入）
     */
    @Inject
    public BulletinListViewModel(@NonNull Application application, BulletinApi bulletinApi, BulletinDao bulletinDao) {
        super(application);
        this.bulletinApi = bulletinApi;
        this.bulletinDao = bulletinDao;
    }

    /**
     * 设置离线模式 ── 由 Fragment 的网络监听器调用
     *
     * @param offline true 表示网络不可用，false 表示网络已恢复
     */
    public void setOffline(boolean offline) {
        this.isOffline = offline;
    }

    /**
     * 查询当前是否处于离线模式
     *
     * @return true 表示离线，false 表示在线
     */
    public boolean isOffline() {
        return isOffline;
    }

    /**
     * 获取公告列表的可观察数据
     *
     * Fragment 在 observeViewModel() 中观察此 LiveData，
     * 数据变化时调用 adapter.submitList() 更新列表。
     *
     * @return LiveData 包含公告列表数据
     */
    public LiveData<List<BulletinUiModel>> getBulletins() { return bulletins; }

    /**
     * 获取加载状态的可观察数据
     *
     * @return LiveData<Boolean> true 表示正在加载
     */
    public LiveData<Boolean> getLoading() { return loading; }

    /**
     * 获取错误信息的可观察数据
     *
     * @return LiveData<String> 错误提示文字
     */
    public LiveData<String> getError() { return error; }

    /**
     * 获取轮播公告的可观察数据
     *
     * @return LiveData 包含轮播公告列表（最多3条）
     */
    public LiveData<List<BulletinUiModel>> getCarouselBulletins() { return carouselBulletins; }

    /**
     * 加载轮播公告 ── 获取最新3条公告用于顶部轮播图展示
     *
     * ═══════════════════════════════════════════════════════════
     * 加载流程：
     * ═══════════════════════════════════════════════════════════
     *
     *   1. 从本地缓存读取最新3条公告
     *      → 如果缓存有数据，立即通过 postValue() 更新轮播
     *   2. 如果离线模式，跳过网络请求
     *   3. 发起网络请求 getLatestBulletins(3)
     *      → 成功：更新轮播数据 + 异步写入缓存
     *      → 失败：保留缓存数据（不报错，轮播是辅助功能）
     *
     * 为什么轮播加载失败不报错？
     * 因为轮播是锦上添花的功能，即使加载失败，
     * 用户仍然可以正常浏览公告列表，不需要中断体验。
     */
    public void loadCarouselBulletins() {
        new Thread(() -> {
            List<BulletinEntity> cached = bulletinDao.getLatestBulletins(3).getValue();
            if (cached != null && !cached.isEmpty()) {
                List<BulletinUiModel> models = new ArrayList<>();
                for (BulletinEntity be : cached) {
                    models.add(convertEntityToUiModel(be));
                }
                carouselBulletins.postValue(models);
            }
        }).start();

        if (isOffline) return;

        bulletinApi.getLatestBulletins(3).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<Result<List<BulletinResponse>>> call,
                                   retrofit2.Response<Result<List<BulletinResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<BulletinUiModel> models = new ArrayList<>();
                    List<BulletinEntity> entities = new ArrayList<>();
                    for (BulletinResponse br : response.body().getData()) {
                        models.add(convertResponseToUiModel(br));
                        entities.add(convertResponseToEntity(br));
                    }
                    carouselBulletins.setValue(models);
                    new Thread(() -> bulletinDao.insertAll(entities)).start();
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
     * 加载公告列表 ── 核心数据加载方法，支持分页和类型筛选
     *
     * ═══════════════════════════════════════════════════════════
     * 参数说明：
     * ═══════════════════════════════════════════════════════════
     *
     * @param type    公告类型筛选：
     *                - null：加载全部类型
     *                - "version"：版本更新
     *                - "event"：活动
     *                - "notice"：通知
     *
     * @param refresh 是否刷新：
     *                - true：重置页码，替换整个列表（下拉刷新/切换Tab）
     *                - false：追加到现有列表末尾（上拉加载更多）
     *
     * ═══════════════════════════════════════════════════════════
     * 防重复加载机制：
     * ═══════════════════════════════════════════════════════════
     *
     * 1. 如果 loading 为 true（正在加载中），直接返回，避免重复请求
     * 2. 如果 hasMore 为 false 且不是刷新操作，直接返回，避免无效请求
     *
     * ═══════════════════════════════════════════════════════════
     * 数据合并逻辑：
     * ═══════════════════════════════════════════════════════════
     *
     * - refresh=true：新数据替换旧数据（下拉刷新/切换Tab）
     * - refresh=false：新数据追加到旧数据末尾（加载更多）
     */
    public void loadBulletins(String type, boolean refresh) {
        if (Boolean.TRUE.equals(loading.getValue())) return;

        if (refresh) {
            currentPage = 1;
            hasMore = true;
            currentType = type;
        }

        if (!hasMore && !refresh) return;

        new Thread(() -> {
            LiveData<List<BulletinEntity>> cached;
            if (type != null) {
                cached = bulletinDao.getBulletinsByType(type);
            } else {
                cached = bulletinDao.getAllBulletins();
            }
            List<BulletinEntity> cachedList = cached.getValue();
            if (cachedList != null && !cachedList.isEmpty()) {
                List<BulletinUiModel> models = new ArrayList<>();
                for (BulletinEntity be : cachedList) {
                    models.add(convertEntityToUiModel(be));
                }
                bulletins.postValue(models);
            }
        }).start();

        if (isOffline) return;

        loading.setValue(true);
        bulletinApi.getBulletins(currentType, currentPage, 10).enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(retrofit2.Call<Result<PageResponse<BulletinResponse>>> call,
                                   retrofit2.Response<Result<PageResponse<BulletinResponse>>> response) {
                loading.setValue(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    PageResponse<BulletinResponse> data = response.body().getData();
                    List<BulletinUiModel> newItems = new ArrayList<>();
                    List<BulletinEntity> entities = new ArrayList<>();
                    for (BulletinResponse br : data.getRecords()) {
                        newItems.add(convertResponseToUiModel(br));
                        entities.add(convertResponseToEntity(br));
                    }

                    if (refresh || bulletins.getValue() == null) {
                        bulletins.setValue(newItems);
                    } else {
                        List<BulletinUiModel> existing = new ArrayList<>(bulletins.getValue());
                        existing.addAll(newItems);
                        bulletins.setValue(existing);
                    }

                    new Thread(() -> bulletinDao.insertAll(entities)).start();

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
                if (bulletins.getValue() == null || bulletins.getValue().isEmpty()) {
                    error.setValue("网络错误：" + t.getMessage());
                }
                Timber.e(t, "Failed to load bulletins");
            }
        });
    }

    /**
     * 加载更多公告 ── 上拉到底部时调用，加载下一页数据
     *
     * 内部调用 loadBulletins(currentType, false)：
     * - currentType 保持当前筛选类型不变
     * - refresh=false 表示追加而非替换
     */
    public void loadMore() {
        loadBulletins(currentType, false);
    }

    /**
     * 查询是否还有更多数据可加载
     *
     * @return true 表示还有下一页，false 表示已到末尾
     */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * 将网络响应 DTO 转换为 UI 模型
     *
     * 转换逻辑：
     * - isPinned：后端返回 Integer（0或1），转换为 boolean
     *   - isPinned != null && isPinned == 1 → true（置顶）
     *   - 其他情况 → false（非置顶）
     *
     * @param response 后端返回的公告数据
     * @return UI 层使用的公告模型
     */
    private BulletinUiModel convertResponseToUiModel(BulletinResponse response) {
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

    /**
     * 将本地缓存实体转换为 UI 模型
     *
     * 与 convertResponseToUiModel() 类似，但数据来源不同：
     * - BulletinEntity 的字段是直接访问的（public 字段）
     * - BulletinResponse 的字段是通过 getter 访问的
     * - isPinned 在 Entity 中是 int 类型，直接与 1 比较
     *
     * @param entity 本地缓存的公告实体
     * @return UI 层使用的公告模型
     */
    private BulletinUiModel convertEntityToUiModel(BulletinEntity entity) {
        return new BulletinUiModel(
                entity.id,
                entity.type,
                entity.title,
                entity.content,
                entity.imageUrl,
                entity.isPinned == 1,
                entity.publishedAt,
                entity.createdAt
        );
    }

    /**
     * 将网络响应 DTO 转换为本地缓存实体
     *
     * 转换后异步写入 Room 数据库，供离线时使用。
     *
     * 注意：updatedAt 字段使用当前时间戳，而非后端返回的时间，
     * 因为这个字段记录的是"缓存更新时间"，不是"数据修改时间"。
     *
     * @param response 后端返回的公告数据
     * @return Room 数据库实体
     */
    private BulletinEntity convertResponseToEntity(BulletinResponse response) {
        BulletinEntity entity = new BulletinEntity();
        entity.id = response.getId();
        entity.type = response.getType();
        entity.title = response.getTitle();
        entity.content = response.getContent();
        entity.imageUrl = response.getImageUrl();
        entity.isPinned = response.getIsPinned() != null ? response.getIsPinned() : 0;
        entity.publishedAt = response.getPublishedAt();
        entity.createdAt = response.getCreatedAt();
        entity.updatedAt = System.currentTimeMillis();
        return entity;
    }
}
