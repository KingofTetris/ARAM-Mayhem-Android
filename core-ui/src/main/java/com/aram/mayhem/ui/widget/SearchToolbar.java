package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aram.mayhem.common.Constants;
import com.aram.mayhem.ui.R;

/**
 * 搜索工具栏组件（核心UI模块）
 *
 * 功能：带搜索输入框的工具栏，支持防抖搜索回调
 * 防抖机制：使用 Handler.postDelayed 实现，延迟时间 Constants.SEARCH_DEBOUNCE_MS (300ms)
 * 用途：英雄列表页、符文列表页顶部搜索框
 * 特性：自动隐藏/显示清除按钮、输入防抖、搜索回调
 */
public class SearchToolbar extends FrameLayout {

    /** 搜索输入框 */
    private EditText editText;
    /** 清除按钮 */
    private View clearButton;
    /** 防抖 Handler（主线程） */
    private Handler debounceHandler;
    /** 防抖任务（用于取消重复任务） */
    private Runnable debounceRunnable;
    /** 搜索回调监听器 */
    private OnSearchListener listener;

    /**
     * 搜索事件监听器接口
     *
     * @param query 用户输入的搜索关键词
     */
    public interface OnSearchListener {
        void onSearch(@NonNull String query);
    }

    /**
     * 构造函数（代码调用）
     *
     * @param context 上下文
     */
    public SearchToolbar(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     *
     * @param context 上下文
     * @param attrs   属性集合
     */
    public SearchToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
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
    public SearchToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
        LayoutInflater.from(context).inflate(R.layout.widget_search_toolbar, this, true);
        
        // 绑定视图
        editText = findViewById(R.id.edit_search);
        clearButton = findViewById(R.id.button_clear);

        // 创建主线程 Handler（用于防抖）
        debounceHandler = new Handler(Looper.getMainLooper());

        // 设置清除按钮点击事件：清空输入并触发空搜索
        clearButton.setOnClickListener(v -> {
            editText.setText("");
            if (listener != null) {
                listener.onSearch("");
            }
        });

        // 设置输入监听（带防抖）
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 输入时有内容显示清除按钮，无内容隐藏
                clearButton.setVisibility(s.length() > 0 ? VISIBLE : GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 防抖处理：先取消之前的延迟任务
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }
                
                // 创建新的延迟任务
                debounceRunnable = () -> {
                    if (listener != null) {
                        // 触发搜索回调（去除首尾空格）
                        listener.onSearch(s != null ? s.toString().trim() : "");
                    }
                };
                
                // 延迟执行（默认300ms后触发搜索）
                debounceHandler.postDelayed(debounceRunnable, Constants.SEARCH_DEBOUNCE_MS);
            }
        });
    }

    /**
     * 设置搜索回调监听器
     *
     * @param listener 搜索回调实现
     */
    public void setOnSearchListener(OnSearchListener listener) {
        this.listener = listener;
    }

    /**
     * 设置搜索框提示文字
     *
     * @param hint 提示文字
     */
    public void setHint(@NonNull String hint) {
        editText.setHint(hint);
    }

    /**
     * 获取当前搜索关键词
     *
     * @return 搜索关键词（已去除首尾空格）
     */
    public String getQuery() {
        return editText.getText().toString().trim();
    }

    /**
     * 清空搜索框内容
     */
    public void clear() {
        editText.setText("");
    }
}
