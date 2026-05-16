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
 * 搜索工具栏组件
 *
 * 功能：带搜索输入框的工具栏，支持防抖搜索回调
 * 防抖延迟：Constants.SEARCH_DEBOUNCE_MS (300ms)
 * 用途：英雄列表、符文列表页顶部搜索
 */
public class SearchToolbar extends FrameLayout {

    private EditText editText;
    private View clearButton;
    private Handler debounceHandler;
    private Runnable debounceRunnable;
    private OnSearchListener listener;

    public interface OnSearchListener {
        void onSearch(@NonNull String query);
    }

    public SearchToolbar(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SearchToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_search_toolbar, this, true);
        editText = findViewById(R.id.edit_search);
        clearButton = findViewById(R.id.button_clear);

        debounceHandler = new Handler(Looper.getMainLooper());

        clearButton.setOnClickListener(v -> {
            editText.setText("");
            if (listener != null) {
                listener.onSearch("");
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearButton.setVisibility(s.length() > 0 ? VISIBLE : GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }
                debounceRunnable = () -> {
                    if (listener != null) {
                        listener.onSearch(s != null ? s.toString().trim() : "");
                    }
                };
                debounceHandler.postDelayed(debounceRunnable, Constants.SEARCH_DEBOUNCE_MS);
            }
        });
    }

    public void setOnSearchListener(OnSearchListener listener) {
        this.listener = listener;
    }

    public void setHint(@NonNull String hint) {
        editText.setHint(hint);
    }

    public String getQuery() {
        return editText.getText().toString().trim();
    }

    public void clear() {
        editText.setText("");
    }
}
