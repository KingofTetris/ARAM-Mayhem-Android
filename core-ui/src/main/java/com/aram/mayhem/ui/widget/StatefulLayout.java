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
 * 状态布局组件（核心UI模块）
 *
 * 功能：统一管理 Loading / Content / Error / Empty 四种状态的切换
 * 用途：所有列表页面和数据展示页面的外层容器，统一状态展示逻辑
 * 状态流转：默认显示 Loading → 数据加载成功显示 Content → 数据为空显示 Empty → 加载失败显示 Error
 * 特性：支持自定义空状态和错误状态提示文字，支持重试按钮回调
 */
public class StatefulLayout extends FrameLayout {

    /** 内容布局 */
    private View layoutContent;
    /** 加载中布局 */
    private View layoutLoading;
    /** 空状态布局 */
    private View layoutEmpty;
    /** 错误状态布局 */
    private View layoutError;
    /** 空状态提示文字 */
    private TextView textEmptyMessage;
    /** 错误状态提示文字 */
    private TextView textErrorMessage;
    /** 重试按钮 */
    private MaterialButton buttonRetry;

    /** 重试回调监听器 */
    private OnRetryListener retryListener;

    /**
     * 重试事件监听器接口
     */
    public interface OnRetryListener {
        void onRetry();
    }

    /**
     * 构造函数（代码调用）
     *
     * @param context 上下文
     */
    public StatefulLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     *
     * @param context 上下文
     * @param attrs   属性集合
     */
    public StatefulLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数（带样式）
     *
     * @param context      上下文
     * @param attrs        属性集合
     * @param defStyleAttr 默认样式属性
     */
    public StatefulLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 加载布局文件
        LayoutInflater.from(context).inflate(
                com.aram.mayhem.ui.R.layout.widget_stateful_layout, this, true);

        // 绑定视图
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

        // 默认显示加载状态
        showLoading();
    }

    /**
     * 显示内容状态
     */
    public void showContent() {
        layoutContent.setVisibility(VISIBLE);
        layoutLoading.setVisibility(GONE);
        layoutEmpty.setVisibility(GONE);
        layoutError.setVisibility(GONE);
    }

    /**
     * 显示加载中状态
     */
    public void showLoading() {
        layoutContent.setVisibility(GONE);
        layoutLoading.setVisibility(VISIBLE);
        layoutEmpty.setVisibility(GONE);
        layoutError.setVisibility(GONE);
    }

    /**
     * 显示空状态（使用默认提示文字）
     */
    public void showEmpty() {
        showEmpty(null);
    }

    /**
     * 显示空状态（自定义提示文字）
     *
     * @param message 自定义空状态提示文字
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
     * 显示错误状态（使用默认提示文字）
     */
    public void showError() {
        showError(null);
    }

    /**
     * 显示错误状态（自定义提示文字）
     *
     * @param message 自定义错误提示文字
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
     * @param listener 重试回调实现
     */
    public void setOnRetryListener(OnRetryListener listener) {
        this.retryListener = listener;
    }

    /**
     * 设置内容视图
     *
     * @param view 要显示的内容视图（通常是 RecyclerView 或其他容器）
     */
    public void setContentView(@NonNull View view) {
        FrameLayout contentContainer = findViewById(com.aram.mayhem.ui.R.id.layout_content);
        // 先清空容器（避免重复添加）
        contentContainer.removeAllViews();
        // 添加内容视图
        contentContainer.addView(view);
    }
}
