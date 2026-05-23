package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

/**
 * 状态布局组件 ── 统一管理 Loading / Content / Error / Empty 四种状态的容器
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * StatefulLayout 是所有列表页面和数据展示页面的外层容器。
 * 它解决了 Android 开发中一个常见问题：每个页面都需要处理 4 种状态，
 * 如果每个页面都单独写状态切换逻辑，代码会大量重复。
 *
 * 四种状态：
 * ┌──────────────────────────────────────────┐
 * │  Loading（加载中）                        │
 * │  ┌────────────────────────────────────┐  │
 * │  │         🔄 加载中...               │  │
 * │  │         (ProgressBar)              │  │
 * │  └────────────────────────────────────┘  │
 * ├──────────────────────────────────────────┤
 * │  Content（内容）                          │
 * │  ┌────────────────────────────────────┐  │
 * │  │         RecyclerView/列表          │  │
 * │  │         (实际数据内容)              │  │
 * │  └────────────────────────────────────┘  │
 * ├──────────────────────────────────────────┤
 * │  Empty（空数据）                          │
 * │  ┌────────────────────────────────────┐  │
 * │  │         📭 暂无数据               │  │
 * │  │         (空状态提示)               │  │
 * │  └────────────────────────────────────┘  │
 * ├──────────────────────────────────────────┤
 * │  Error（加载失败）                        │
 * │  ┌────────────────────────────────────┐  │
 * │  │         ❌ 加载失败               │  │
 * │  │         [重试]                     │  │
 * │  └────────────────────────────────────┘  │
 * └──────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、状态流转
 * ═══════════════════════════════════════════════════════════════════
 *
 * 典型的状态流转路径：
 *
 *   showLoading()          数据成功          showContent()
 *  ────────────→ ────────────────→ ────────────────→
 *  [Loading]     ViewModel 获取数据   [Content]
 *                     │
 *                     │ 数据为空
 *                     ↓
 *                showEmpty()
 *                [Empty]
 *
 *   showLoading()          网络失败          showError()
 *  ────────────→ ────────────────→ ────────────────→
 *  [Loading]     ViewModel 报错     [Error]
 *                                        │
 *                                        │ 点击重试
 *                                        ↓
 *                                   showLoading()
 *                                   [Loading] → 重新请求
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、为什么用 FrameLayout？
 * ═══════════════════════════════════════════════════════════════════
 *
 * FrameLayout 是最简单的布局，所有子视图堆叠在左上角。
 * 通过 VISIBLE / GONE 控制哪个子视图显示：
 * - VISIBLE：可见且占空间
 * - GONE：不可见且不占空间（完全移除布局）
 *
 * 四种状态布局叠加在一起，同一时间只有一个 VISIBLE，其余都是 GONE
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、使用方式
 * ═══════════════════════════════════════════════════════════════════
 *
 * XML 布局中：
 * <pre>
 * &lt;com.aram.mayhem.ui.widget.StatefulLayout
 *     android:id="@+id/stateful_layout"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" /&gt;
 * </pre>
 *
 * Fragment 中：
 * <pre>
 * // 设置内容视图（RecyclerView）
 * statefulLayout.setContentView(recyclerView);
 *
 * // 设置重试回调
 * statefulLayout.setOnRetryListener(() -> viewModel.refresh());
 *
 * // 观察数据状态
 * viewModel.uiState.observe(getViewLifecycleOwner(), state -> {
 *     if (state.isLoading()) statefulLayout.showLoading();
 *     else if (state.isSuccess()) statefulLayout.showContent();
 *     else if (state.isEmpty()) statefulLayout.showEmpty("暂无英雄数据");
 *     else if (state.isError()) statefulLayout.showError("网络连接失败");
 * });
 * </pre>
 */
public class StatefulLayout extends FrameLayout {

    /** 内容布局 ── 实际的数据展示区域（通常是 RecyclerView） */
    private View layoutContent;

    /** 加载中布局 ── 显示 ProgressBar 和"加载中"提示 */
    private View layoutLoading;

    /** 空状态布局 ── 显示空数据图标和提示文字 */
    private View layoutEmpty;

    /** 错误状态布局 ── 显示错误图标、提示文字和重试按钮 */
    private View layoutError;

    /** 空状态提示文字 ── 可自定义（如"暂无英雄数据"） */
    private TextView textEmptyMessage;

    /** 错误状态提示文字 ── 可自定义（如"网络连接失败"） */
    private TextView textErrorMessage;

    /** 重试按钮 ── 点击后触发 OnRetryListener 回调 */
    private MaterialButton buttonRetry;

    /** 重试回调监听器 ── Fragment 实现此接口处理重新加载逻辑 */
    private OnRetryListener retryListener;

    /**
     * 重试事件监听器接口 ── 用户点击"重试"按钮时回调
     *
     * Fragment 中的典型实现：
     * <pre>
     * statefulLayout.setOnRetryListener(() -> {
     *     viewModel.refresh(); // 重新发起网络请求
     * });
     * </pre>
     */
    public interface OnRetryListener {
        /** 重试回调 ── 在此方法中重新发起数据请求 */
        void onRetry();
    }

    /**
     * 构造函数（代码调用）
     * @param context 上下文
     */
    public StatefulLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     * @param context 上下文
     * @param attrs   XML 属性集合
     */
    public StatefulLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数（带样式）
     * @param context      上下文
     * @param attrs        XML 属性集合
     * @param defStyleAttr 默认样式属性
     */
    public StatefulLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件 ── 加载布局、绑定视图、设置默认状态
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 加载布局文件 widget_stateful_layout.xml
        // 该布局包含 4 个叠加的子布局：Content / Loading / Empty / Error
        LayoutInflater.from(context).inflate(
                com.aram.mayhem.ui.R.layout.widget_stateful_layout, this, true);

        // 绑定各状态布局
        layoutContent = findViewById(com.aram.mayhem.ui.R.id.layout_content);
        layoutLoading = findViewById(com.aram.mayhem.ui.R.id.layout_loading);
        layoutEmpty = findViewById(com.aram.mayhem.ui.R.id.layout_empty);
        layoutError = findViewById(com.aram.mayhem.ui.R.id.layout_error);
        textEmptyMessage = findViewById(com.aram.mayhem.ui.R.id.text_empty_message);
        textErrorMessage = findViewById(com.aram.mayhem.ui.R.id.text_error_message);
        buttonRetry = findViewById(com.aram.mayhem.ui.R.id.button_retry);

        // 设置重试按钮点击事件
        buttonRetry.setOnClickListener(v -> {
            if (retryListener != null) {
                retryListener.onRetry();
            }
        });

        // 默认显示加载状态（页面刚创建时，数据还未加载）
        showLoading();
    }

    /**
     * 显示内容状态 ── 数据加载成功后调用
     *
     * 效果：隐藏 Loading/Empty/Error，显示 Content
     */
    public void showContent() {
        layoutContent.setVisibility(VISIBLE);
        layoutLoading.setVisibility(GONE);
        layoutEmpty.setVisibility(GONE);
        layoutError.setVisibility(GONE);
    }

    /**
     * 显示加载中状态 ── 开始加载数据时调用
     *
     * 效果：隐藏 Content/Empty/Error，显示 Loading
     */
    public void showLoading() {
        layoutContent.setVisibility(GONE);
        layoutLoading.setVisibility(VISIBLE);
        layoutEmpty.setVisibility(GONE);
        layoutError.setVisibility(GONE);
    }

    /**
     * 显示空状态（使用默认提示文字）── 数据为空时调用
     */
    public void showEmpty() {
        showEmpty(null);
    }

    /**
     * 显示空状态（自定义提示文字）── 数据为空时调用
     *
     * @param message 自定义空状态提示文字（如"暂无英雄数据"），null 则使用默认文字
     */
    public void showEmpty(@Nullable String message) {
        layoutContent.setVisibility(GONE);
        layoutLoading.setVisibility(GONE);
        layoutEmpty.setVisibility(VISIBLE);
        layoutError.setVisibility(GONE);
        // 设置自定义提示文字（如果提供）
        if (message != null) {
            textEmptyMessage.setText(message);
        }
    }

    /**
     * 显示错误状态（使用默认提示文字）── 加载失败时调用
     */
    public void showError() {
        showError(null);
    }

    /**
     * 显示错误状态（自定义提示文字）── 加载失败时调用
     *
     * @param message 自定义错误提示文字（如"网络连接失败"），null 则使用默认文字
     */
    public void showError(@Nullable String message) {
        layoutContent.setVisibility(GONE);
        layoutLoading.setVisibility(GONE);
        layoutEmpty.setVisibility(GONE);
        layoutError.setVisibility(VISIBLE);
        // 设置自定义提示文字（如果提供）
        if (message != null) {
            textErrorMessage.setText(message);
        }
    }

    /**
     * 设置重试回调监听器
     *
     * @param listener 重试回调实现（通常是 Fragment）
     */
    public void setOnRetryListener(OnRetryListener listener) {
        this.retryListener = listener;
    }

    /**
     * 设置内容视图 ── 将实际的数据展示视图（如 RecyclerView）添加到 Content 区域
     *
     * 为什么需要手动设置？
     * - StatefulLayout 是通用容器，不知道具体要展示什么内容
     * - Fragment 创建 RecyclerView 后，通过此方法将其放入 Content 区域
     * - 这样 StatefulLayout 就能控制 Content 的显示/隐藏
     *
     * @param view 要显示的内容视图（通常是 RecyclerView 或 ScrollView）
     */
    public void setContentView(@NonNull View view) {
        FrameLayout contentContainer = findViewById(com.aram.mayhem.ui.R.id.layout_content);
        // 先清空容器（避免重复添加导致视图叠加）
        contentContainer.removeAllViews();
        // 添加内容视图
        contentContainer.addView(view);
    }
}
