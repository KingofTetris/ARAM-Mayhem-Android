package com.aram.mayhem.feature.bulletin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.aram.mayhem.feature.bulletin.databinding.FragmentBulletinDetailBinding;
import com.aram.mayhem.feature.bulletin.viewmodel.BulletinDetailViewModel;
import com.aram.mayhem.ui.model.BulletinUiModel;
import com.bumptech.glide.Glide;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 公告详情页 ── 展示单条公告的完整内容
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * BulletinDetailFragment 是公告详情页，用户在公告列表页
 * （BulletinListFragment）点击某条公告后跳转到此页面。
 * 它展示公告的完整信息，包括：
 * 1. 公告标题 ── 顶部 Toolbar 显示公告类型，内容区显示标题
 * 2. 公告类型标签 ── 如"版本更新"、"活动"、"通知"
 * 3. 发布日期 ── 公告的发布时间
 * 4. 公告正文 ── 公告的完整文字内容
 * 5. 公告图片 ── 可选的封面/配图（部分公告没有图片）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_bulletin_detail.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────────────┐
 *   │  ← 返回    公告类型标签              │  ← toolbar
 *   ├──────────────────────────────────────────────┤
 *   │  加载中...                                   │  ← progressContainer
 *   │  （加载完成后隐藏）                           │
 *   ├──────────────────────────────────────────────┤
 *   │  公告标题                                    │  ← textTitle
 *   │  类型标签 · 发布日期                         │  ← textType + textDate
 *   ├──────────────────────────────────────────────┤
 *   │  ┌──────────────────────────────────────┐   │
 *   │  │                                      │   │
 *   │  │  公告封面图片                         │   │  ← imageBulletin
 *   │  │  （可选，无图片时隐藏）               │   │
 *   │  │                                      │   │
 *   │  └──────────────────────────────────────┘   │
 *   ├──────────────────────────────────────────────┤
 *   │  公告正文内容...                             │  ← textContent
 *   │  这里是公告的完整文字描述，                   │
 *   │  可以很长，支持滚动查看。                     │
 *   │                                              │
 *   └──────────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流
 * ═══════════════════════════════════════════════════════════════════
 *
 *   BulletinListFragment              BulletinDetailFragment
 *   ┌──────────────┐                  ┌──────────────────────┐
 *   │ 用户点击公告  │ ──── id ──────→ │ newInstance(id)      │
 *   │ 卡片         │                  │         │            │
 *   └──────────────┘                  │         ▼            │
 *                                     │ ViewModel.loadDetail │
 *                                     │         │            │
 *                                     │         ▼            │
 *                                     │ API 返回详情数据     │
 *                                     │         │            │
 *                                     │         ▼            │
 *                                     │ displayBulletin()    │
 *                                     │ 渲染到视图           │
 *                                     └──────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、导航方式
 * ═══════════════════════════════════════════════════════════════════
 *
 * 本 Fragment 通过 newInstance() 工厂方法创建，公告 ID 通过
 * Bundle 参数传递。这是 Android Fragment 传参的标准模式：
 *
 *   // 在 BulletinListFragment 中跳转
 *   long bulletinId = bulletin.getId();
 *   BulletinDetailFragment fragment = BulletinDetailFragment.newInstance(bulletinId);
 *   // 然后通过 FragmentManager 进行页面切换
 *
 * 返回按钮有两种处理方式：
 * 1. 如果设置了 OnBackListener（由宿主 Activity/Fragment 提供），调用自定义回调
 * 2. 如果没有设置，调用 Activity 的 onBackPressed() 返回上一页
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、图片加载策略
 * ═══════════════════════════════════════════════════════════════════
 *
 * 公告图片使用 Glide 库加载，处理逻辑如下：
 * - imageUrl 不为空 → 显示 ImageView，用 Glide 加载网络图片
 * - imageUrl 为空   → 隐藏 ImageView（GONE），不占空间
 *
 * Glide 的 centerCrop() 确保图片填满 ImageView 并居中裁剪，
 * 避免图片变形或留白。
 *
 * @see BulletinDetailViewModel  管理公告详情数据的 ViewModel
 * @see BulletinUiModel          公告的 UI 数据模型
 * @see BulletinListFragment     公告列表页（跳转来源）
 */
@AndroidEntryPoint
public class BulletinDetailFragment extends Fragment {

    /**
     * 公告 ID 的 Bundle 参数键名
     *
     * 为什么用 static final String 作为键名？
     * ── Bundle 读取数据时需要用字符串键来查找值，
     *    如果键名拼写错误会导致取不到数据。
     *    用常量可以避免拼写错误，且修改时只需改一处。
     *
     * 使用方式：
     *   // 写入 Bundle
     *   args.putLong(ARG_BULLETIN_ID, bulletinId);
     *   // 从 Bundle 读取
     *   long id = getArguments().getLong(ARG_BULLETIN_ID);
     */
    private static final String ARG_BULLETIN_ID = "bulletin_id";

    /**
     * 视图绑定对象
     *
     * ViewBinding 是 Android 官方推荐的视图访问方式，
     * 它为每个 XML 布局文件自动生成一个 Binding 类，
     * 可以通过 binding.textView 直接访问视图，无需 findViewById()。
     *
     * 为什么设为可空并在 onDestroyView 中置 null？
     * ── Fragment 的视图生命周期与 Fragment 本身不同：
     *    视图在 onDestroyView 时销毁，但 Fragment 对象可能还在。
     *    如果视图已销毁但 binding 还持有引用，会导致内存泄漏
     *    和 NullPointerException。所以在 onDestroyView 中
     *    将 binding 置为 null 是标准做法。
     */
    private FragmentBulletinDetailBinding binding;

    /**
     * 公告详情 ViewModel
     *
     * BulletinDetailViewModel 负责从后端 API 加载公告详情数据，
     * 并通过 LiveData 将数据暴露给本 Fragment 观察。
     *
     * 数据流：
     *   viewModel.loadBulletinDetail(id)
     *       → 调用 bulletinApi.getBulletinDetail(id)
     *       → API 返回 BulletinResponse
     *       → 转换为 BulletinUiModel
     *       → 存入 bulletinDetail LiveData
     *       → Fragment 观察到变化，调用 displayBulletin()
     */
    private BulletinDetailViewModel viewModel;

    /**
     * 返回按钮点击监听器
     *
     * 为什么需要自定义返回监听器？
     * ── 在某些导航结构中，Fragment 的返回行为需要由
     *    宿主 Activity 或父 Fragment 来控制。
     *    例如：使用 Navigation Component 时，可能需要
     *    调用 navController.popBackStack() 而不是
     *    Activity.onBackPressed()。
     *
     * 如果没有设置 OnBackListener，默认调用
     * requireActivity().onBackPressed() 返回上一页。
     */
    private OnBackListener onBackListener;

    /**
     * 返回按钮回调接口
     *
     * 宿主 Activity 或父 Fragment 实现此接口，
     * 可以自定义返回按钮的行为。
     *
     * 使用示例：
     *   bulletinDetailFragment.setOnBackListener(() -> {
     *       navController.popBackStack();
     *   });
     */
    public interface OnBackListener {
        /**
         * 用户点击返回按钮时调用
         */
        void onBack();
    }

    /**
     * 设置返回按钮点击监听器
     *
     * @param listener 返回监听器，由宿主组件提供
     */
    public void setOnBackListener(OnBackListener listener) {
        this.onBackListener = listener;
    }

    /**
     * 创建公告详情页实例（工厂方法）
     *
     * ═══════════════════════════════════════════════════════════
     * 为什么用 newInstance() 而不是直接 new + setArguments？
     * ═══════════════════════════════════════════════════════════
     *
     * 这是 Android Fragment 传参的标准模式（Factory Method 模式）。
     * 原因：Android 系统在配置变更（如屏幕旋转）时会重新创建
     * Fragment，它只通过无参构造函数创建，然后通过 setArguments
     * 恢复之前保存的参数。如果参数通过构造函数传入，配置变更后
     * 参数会丢失。
     *
     * Bundle 的工作原理：
     *   Bundle 本质上是一个键值对容器，类似于 Map，
     *   但它支持 Android 的序列化机制（Parcel），
     *   可以在进程间传递、在配置变更时自动保存恢复。
     *
     * @param bulletinId 公告 ID，从列表页传入
     * @return 配置好参数的 BulletinDetailFragment 实例
     */
    public static BulletinDetailFragment newInstance(long bulletinId) {
        BulletinDetailFragment fragment = new BulletinDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_BULLETIN_ID, bulletinId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 创建 Fragment 的视图
     *
     * ═══════════════════════════════════════════════════════════
     * Fragment 生命周期方法调用顺序：
     * ═══════════════════════════════════════════════════════════
     *
     *   onAttach → onCreate → onCreateView → onViewCreated
     *                                      → onStart → onResume
     *
     * onCreateView 的职责：
     * ── 只负责"创建视图"，即加载 XML 布局并返回根视图。
     *    不要在这里做数据初始化或事件绑定，
     *    那些工作应在 onViewCreated 中完成。
     *
     * ViewBinding 的 inflate 过程：
     *   1. LayoutInflater.from(inflater.getContext())
     *      ── 获取布局填充器
     *   2. inflate(R.layout.fragment_bulletin_detail, parent, false)
     *      ── 将 XML 布局解析为 View 对象树
     *   3. 返回 Binding 对象，其中包含所有视图的引用
     *
     * @param inflater           布局填充器，用于将 XML 转为 View 对象
     * @param container          父容器，Fragment 的视图将插入其中
     * @param savedInstanceState 保存的实例状态（配置变更恢复时非空）
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBulletinDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化操作
     *
     * ═══════════════════════════════════════════════════════════
     * 为什么在 onViewCreated 而不是 onCreateView 中初始化？
     * ═══════════════════════════════════════════════════════════
     *
     * onViewCreated 在 onCreateView 之后调用，此时：
     * 1. 视图已经创建完成，所有 UI 组件都可以安全访问
     * 2. binding 对象已经初始化完成
     * 3. 适合做事件绑定、数据观察、ViewModel 初始化等操作
     *
     * 本方法执行三个初始化步骤：
     * ┌────────────────────────────────────────────────────┐
     * │ 1. 初始化 ViewModel 并设置返回按钮监听             │
     * │ 2. 从 Bundle 获取公告 ID，触发数据加载             │
     * │ 3. 观察 ViewModel 的 LiveData 数据变化             │
     * └────────────────────────────────────────────────────┘
     *
     * @param view               Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BulletinDetailViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> {
            if (onBackListener != null) {
                onBackListener.onBack();
            } else {
                requireActivity().onBackPressed();
            }
        });

        if (getArguments() != null) {
            long bulletinId = getArguments().getLong(ARG_BULLETIN_ID);
            viewModel.loadBulletinDetail(bulletinId);
        }

        observeViewModel();
    }

    /**
     * 观察 ViewModel 的 LiveData 数据变化
     *
     * ═══════════════════════════════════════════════════════════
     * LiveData 观察机制详解
     * ═══════════════════════════════════════════════════════════
     *
     * LiveData 是 Android 的生命周期感知型数据容器：
     * - 只有当 Fragment 处于 STARTED 或 RESUMED 状态时，
     *   才会通知观察者（避免在后台更新 UI 导致崩溃）
     * - 当 Fragment 销毁时自动移除观察者（避免内存泄漏）
     *
     * observe() 的两个参数：
     *   getViewLifecycleOwner() ── 视图的生命周期所有者，
     *      确保观察者在视图销毁时自动移除
     *   this::displayBulletin ── 方法引用，等价于：
     *      bulletin -> displayBulletin(bulletin)
     *
     * 数据流：
     *   ViewModel.bulletinDetail.setValue(data)
     *       → LiveData 检测到数据变化
     *       → 通知所有活跃的观察者
     *       → displayBulletin() 被调用
     *       → UI 更新
     */
    private void observeViewModel() {
        viewModel.getBulletinDetail().observe(getViewLifecycleOwner(), this::displayBulletin);

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                binding.progressContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * 将公告数据渲染到视图上
     *
     * ═══════════════════════════════════════════════════════════
     * 数据绑定过程
     * ═══════════════════════════════════════════════════════════
     *
     *   BulletinUiModel                    视图组件
     *   ┌──────────────────┐              ┌──────────────────┐
     *   │ getTypeDisplay() │ ──────────→  │ toolbar.title    │
     *   │ getTitle()       │ ──────────→  │ textTitle        │
     *   │ getTypeDisplay() │ ──────────→  │ textType         │
     *   │ getPublishedAt() │ ──────────→  │ textDate         │
     *   │ getContent()     │ ──────────→  │ textContent      │
     *   │ getImageUrl()    │ ──────────→  │ imageBulletin    │
     *   └──────────────────┘              └──────────────────┘
     *
     * 图片加载的特殊处理：
     * ── 不是所有公告都有图片。对于没有图片的公告：
     *    1. 将 imageBulletin 设为 GONE（完全隐藏，不占空间）
     *    2. 不调用 Glide 加载（避免无意义的网络请求）
     *
     *    对于有图片的公告：
     *    1. 将 imageBulletin 设为 VISIBLE
     *    2. 使用 Glide 加载图片，centerCrop() 确保填满区域
     *
     * @param bulletin 公告详情数据，可能为 null（首次加载时）
     */
    private void displayBulletin(BulletinUiModel bulletin) {
        if (bulletin == null) return;

        binding.toolbar.setTitle(bulletin.getTypeDisplay());
        binding.textTitle.setText(bulletin.getTitle());
        binding.textType.setText(bulletin.getTypeDisplay());
        binding.textDate.setText(bulletin.getPublishedAt());
        binding.textContent.setText(bulletin.getContent());

        if (bulletin.getImageUrl() != null && !bulletin.getImageUrl().isEmpty()) {
            binding.imageBulletin.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(bulletin.getImageUrl())
                    .centerCrop()
                    .into(binding.imageBulletin);
        } else {
            binding.imageBulletin.setVisibility(View.GONE);
        }
    }

    /**
     * 视图销毁时清理资源
     *
     * ═══════════════════════════════════════════════════════════
     * 为什么必须将 binding 置为 null？
     * ═══════════════════════════════════════════════════════════
     *
     * Fragment 的视图在 onDestroyView 时被销毁，但 Fragment
     * 对象本身可能仍然存活（例如在 ViewPager 中切换页面时，
     * Fragment 的视图被销毁但 Fragment 实例被保留）。
     *
     * 如果 binding 不置 null：
     * 1. 内存泄漏 ── binding 持有已销毁视图的引用，
     *    GC 无法回收这些视图对象
     * 2. NullPointerException ── 如果在视图销毁后
     *    仍然通过 binding 访问视图，会抛出异常
     *
     * 这也是为什么 binding 声明时不加 final 的原因：
     * 需要在 onDestroyView 中重新赋值为 null。
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
