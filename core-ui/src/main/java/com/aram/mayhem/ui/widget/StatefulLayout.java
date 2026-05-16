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
 * 状态布局组件
 *
 * 功能：管理 Loading / Content / Error / Empty 四种状态的切换
 * 用途：所有列表页面的外层容器，统一状态展示逻辑
 */
public class StatefulLayout extends FrameLayout {

    private View layoutContent;
    private View layoutLoading;
    private View layoutEmpty;
    private View layoutError;
    private TextView textEmptyMessage;
    private TextView textErrorMessage;
    private MaterialButton buttonRetry;

    private OnRetryListener retryListener;

    public interface OnRetryListener {
        void onRetry();
    }

    public StatefulLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public StatefulLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StatefulLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(
                com.aram.mayhem.ui.R.layout.widget_stateful_layout, this, true);

        layoutContent = findViewById(com.aram.mayhem.ui.R.id.layout_content);
        layoutLoading = findViewById(com.aram.mayhem.ui.R.id.layout_loading);
        layoutEmpty = findViewById(com.aram.mayhem.ui.R.id.layout_empty);
        layoutError = findViewById(com.aram.mayhem.ui.R.id.layout_error);
        textEmptyMessage = findViewById(com.aram.mayhem.ui.R.id.text_empty_message);
        textErrorMessage = findViewById(com.aram.mayhem.ui.R.id.text_error_message);
        buttonRetry = findViewById(com.aram.mayhem.ui.R.id.button_retry);

        buttonRetry.setOnClickListener(v -> {
            if (retryListener != null) {
                retryListener.onRetry();
            }
        });

        showLoading();
    }

    public void showContent() {
        layoutContent.setVisibility(VISIBLE);
        layoutLoading.setVisibility(GONE);
        layoutEmpty.setVisibility(GONE);
        layoutError.setVisibility(GONE);
    }

    public void showLoading() {
        layoutContent.setVisibility(GONE);
        layoutLoading.setVisibility(VISIBLE);
        layoutEmpty.setVisibility(GONE);
        layoutError.setVisibility(GONE);
    }

    public void showEmpty() {
        showEmpty(null);
    }

    public void showEmpty(@Nullable String message) {
        layoutContent.setVisibility(GONE);
        layoutLoading.setVisibility(GONE);
        layoutEmpty.setVisibility(VISIBLE);
        layoutError.setVisibility(GONE);
        if (message != null) {
            textEmptyMessage.setText(message);
        }
    }

    public void showError() {
        showError(null);
    }

    public void showError(@Nullable String message) {
        layoutContent.setVisibility(GONE);
        layoutLoading.setVisibility(GONE);
        layoutEmpty.setVisibility(GONE);
        layoutError.setVisibility(VISIBLE);
        if (message != null) {
            textErrorMessage.setText(message);
        }
    }

    public void setOnRetryListener(OnRetryListener listener) {
        this.retryListener = listener;
    }

    public void setContentView(@NonNull View view) {
        FrameLayout contentContainer = findViewById(com.aram.mayhem.ui.R.id.layout_content);
        contentContainer.removeAllViews();
        contentContainer.addView(view);
    }
}
