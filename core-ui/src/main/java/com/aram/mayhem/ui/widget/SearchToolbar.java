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
 * 搜索工具栏组件 ── 带防抖的搜索输入框
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * SearchToolbar 是英雄列表页、符文列表页顶部的搜索框。
 * 用户输入关键词后，自动触发搜索回调，不需要手动点击搜索按钮。
 *
 * 视觉结构：
 * ┌─────────────────────────────────────────┐
 * │ 🔍 搜索英雄...                    ✕     │
 * │ (搜索图标) (输入框)           (清除按钮) │
 * └─────────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、防抖机制（核心特性）
 * ═══════════════════════════════════════════════════════════════════
 *
 * 什么是防抖？
 * - 用户快速输入"提莫"时，会触发 2 次 afterTextChanged：
 *   - 第 1 次："提" → 如果立即搜索，会发起"提"的搜索请求
 *   - 第 2 次："提莫" → 发起"提莫"的搜索请求
 * - 两次请求间隔极短，第一次的结果毫无意义，浪费服务器资源
 *
 * 防抖的解决方案：
 * - 每次输入变化时，先取消之前的延迟任务
 * - 然后启动一个新的延迟任务（300ms 后执行搜索）
 * - 如果 300ms 内用户继续输入，之前的任务被取消，只有最后一次输入触发搜索
 *
 * 时序示例：
 * ┌──────────────────────────────────────────────────────┐
 * │ 用户输入 "提"  →  启动 300ms 延迟任务 A              │
 * │ 100ms 后...                                         │
 * │ 用户输入 "莫"  →  取消任务 A，启动 300ms 延迟任务 B   │
 * │ 300ms 后...                                         │
 * │ 任务 B 执行  →  触发搜索回调 "提莫"  ✓               │
 * └──────────────────────────────────────────────────────┘
 *
 * 防抖延迟时间：Constants.SEARCH_DEBOUNCE_MS（300ms）
 * - 太短（如 100ms）：用户输入快时仍会触发多次搜索
 * - 太长（如 1000ms）：用户感觉搜索响应慢
 * - 300ms 是业界常用的平衡点
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、清除按钮
 * ═══════════════════════════════════════════════════════════════════
 *
 * 清除按钮（✕）的行为：
 * - 输入框有内容时显示（VISIBLE）
 * - 输入框为空时隐藏（GONE）
 * - 点击时清空输入框并触发空字符串搜索（恢复完整列表）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、TextWatcher 三个回调说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * TextWatcher 监听输入框文字变化，有三个回调方法：
 * 1. beforeTextChanged(s, start, count, after)：文字变化前
 * 2. onTextChanged(s, start, before, count)：文字变化中
 * 3. afterTextChanged(s)：文字变化后
 *
 * 本组件只使用 onTextChanged（控制清除按钮）和 afterTextChanged（防抖搜索）
 */
public class SearchToolbar extends FrameLayout {

    /** 搜索输入框 ── 用户输入搜索关键词 */
    private EditText editText;

    /** 清除按钮 ── 点击清空输入框内容 */
    private View clearButton;

    /** 防抖 Handler ── 绑定主线程 Looper，用于延迟执行搜索任务 */
    private Handler debounceHandler;

    /** 防抖任务 ── 延迟执行的搜索回调（每次输入变化时重新创建） */
    private Runnable debounceRunnable;

    /** 搜索回调监听器 ── Fragment 实现此接口处理搜索逻辑 */
    private OnSearchListener listener;

    /**
     * 搜索事件监听器接口 ── 防抖延迟后触发
     *
     * Fragment 中的典型实现：
     * <pre>
     * searchToolbar.setOnSearchListener(query -> {
     *     viewModel.searchHeroes(query); // 根据关键词搜索
     * });
     * </pre>
     *
     * @param query 用户输入的搜索关键词（已去除首尾空格）
     */
    public interface OnSearchListener {
        void onSearch(@NonNull String query);
    }

    /**
     * 构造函数（代码调用）
     * @param context 上下文
     */
    public SearchToolbar(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     * @param context 上下文
     * @param attrs   XML 属性集合
     */
    public SearchToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数（带样式）
     * @param context      上下文
     * @param attrs        XML 属性集合
     * @param defStyleAttr 默认样式属性
     */
    public SearchToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化组件 ── 加载布局、绑定视图、设置防抖搜索
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 加载布局文件 widget_search_toolbar.xml
        LayoutInflater.from(context).inflate(R.layout.widget_search_toolbar, this, true);

        // 绑定视图
        editText = findViewById(R.id.edit_search);
        clearButton = findViewById(R.id.button_clear);

        // 创建主线程 Handler（防抖任务必须在主线程执行，因为涉及 UI 更新）
        debounceHandler = new Handler(Looper.getMainLooper());

        // 设置清除按钮点击事件：清空输入并触发空搜索
        // 空搜索 = 恢复完整列表（不带过滤条件）
        clearButton.setOnClickListener(v -> {
            editText.setText("");
            if (listener != null) {
                listener.onSearch("");
            }
        });

        // 设置输入监听（带防抖）
        editText.addTextChangedListener(new TextWatcher() {
            /**
             * 文字变化前回调 ── 本组件不使用
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            /**
             * 文字变化中回调 ── 控制清除按钮的显示/隐藏
             *
             * @param s      变化后的文字
             * @param start  变化起始位置
             * @param before 变化前的字符数
             * @param count  变化后的字符数
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 有内容时显示清除按钮，无内容时隐藏
                clearButton.setVisibility(s.length() > 0 ? VISIBLE : GONE);
            }

            /**
             * 文字变化后回调 ── 防抖搜索的核心逻辑
             *
             * 防抖流程：
             * 1. 取消之前的延迟任务（如果存在）
             * 2. 创建新的延迟任务
             * 3. 延迟 300ms 后执行搜索回调
             *
             * @param s 变化后的可编辑文字
             */
            @Override
            public void afterTextChanged(Editable s) {
                // 步骤 1：取消之前的延迟任务
                // 如果用户在 300ms 内继续输入，之前的任务会被取消
                // 这样只有最后一次输入才会触发搜索
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }

                // 步骤 2：创建新的延迟任务
                debounceRunnable = () -> {
                    if (listener != null) {
                        // trim()：去除首尾空格（避免用户误输入空格触发搜索）
                        listener.onSearch(s != null ? s.toString().trim() : "");
                    }
                };

                // 步骤 3：延迟执行
                // postDelayed(runnable, delayMillis)：延迟 delayMillis 毫秒后执行
                debounceHandler.postDelayed(debounceRunnable, Constants.SEARCH_DEBOUNCE_MS);
            }
        });
    }

    /**
     * 设置搜索回调监听器
     *
     * @param listener 搜索回调实现（通常是 Fragment）
     */
    public void setOnSearchListener(OnSearchListener listener) {
        this.listener = listener;
    }

    /**
     * 设置搜索框提示文字 ── 不同页面使用不同的提示
     *
     * 使用示例：
     * <pre>
     * searchToolbar.setHint("搜索英雄...");   // 英雄列表页
     * searchToolbar.setHint("搜索符文...");   // 符文列表页
     * </pre>
     *
     * @param hint 提示文字
     */
    public void setHint(@NonNull String hint) {
        editText.setHint(hint);
    }

    /**
     * 获取当前搜索关键词 ── 用于 Fragment 保存/恢复搜索状态
     *
     * @return 搜索关键词（已去除首尾空格）
     */
    public String getQuery() {
        return editText.getText().toString().trim();
    }

    /**
     * 清空搜索框内容 ── 用于页面切换时重置搜索状态
     */
    public void clear() {
        editText.setText("");
    }
}
