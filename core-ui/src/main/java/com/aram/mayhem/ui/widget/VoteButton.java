package com.aram.mayhem.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.aram.mayhem.ui.R;

/**
 * 投票按钮组件（核心UI模块）
 *
 * 功能：攻略详情页的点赞/踩按钮，支持状态切换
 * 状态：未投票(null) / UP(点赞) / DOWN(踩)
 * 交互逻辑：已点赞时再次点击取消点赞，未点赞时点击点赞；踩按钮同理
 * 关联：StrategyDetailFragment，用于攻略投票功能
 */
public class VoteButton extends LinearLayout {

    /** 点赞图标 */
    private ImageView imageUp;
    /** 踩图标 */
    private ImageView imageDown;
    /** 点赞数文字 */
    private TextView textUp;
    /** 踩数文字 */
    private TextView textDown;
    /** 投票变更回调监听器 */
    private OnVoteChangeListener listener;

    /** 当前投票状态（UP/DOWN/null） */
    private String currentVoteType;
    /** 点赞数 */
    private int upvotes = 0;
    /** 踩数 */
    private int downvotes = 0;

    /**
     * 投票变更监听器接口
     */
    public interface OnVoteChangeListener {
        /** 点赞回调 */
        void onUpvote();
        /** 踩回调 */
        void onDownvote();
        /** 取消投票回调 */
        void onCancelVote();
    }

    /**
     * 构造函数（代码调用）
     *
     * @param context 上下文
     */
    public VoteButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）
     *
     * @param context 上下文
     * @param attrs   属性集合
     */
    public VoteButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 初始化组件
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 加载布局文件
        LayoutInflater.from(context).inflate(R.layout.widget_vote_button, this, true);
        
        // 绑定视图
        imageUp = findViewById(R.id.image_up);
        imageDown = findViewById(R.id.image_down);
        textUp = findViewById(R.id.text_up);
        textDown = findViewById(R.id.text_down);

        // 设置点击事件
        imageUp.setOnClickListener(v -> handleUpClick());
        imageDown.setOnClickListener(v -> handleDownClick());
    }

    /**
     * 设置投票变更回调监听器
     *
     * @param listener 回调实现
     */
    public void setOnVoteChangeListener(OnVoteChangeListener listener) {
        this.listener = listener;
    }

    /**
     * 设置投票数量
     *
     * @param upvotes   点赞数
     * @param downvotes 踩数
     */
    public void setVoteCounts(int upvotes, int downvotes) {
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        updateUI();
    }

    /**
     * 设置当前投票类型
     *
     * @param voteType 投票类型（UP/DOWN/null）
     */
    public void setCurrentVoteType(String voteType) {
        this.currentVoteType = voteType;
        updateUI();
    }

    /**
     * 处理点赞按钮点击
     */
    private void handleUpClick() {
        if (listener == null) return;

        // 如果当前已点赞，点击取消；否则点赞
        if ("UP".equals(currentVoteType)) {
            listener.onCancelVote();
        } else {
            listener.onUpvote();
        }
    }

    /**
     * 处理踩按钮点击
     */
    private void handleDownClick() {
        if (listener == null) return;

        // 如果当前已踩，点击取消；否则踩
        if ("DOWN".equals(currentVoteType)) {
            listener.onCancelVote();
        } else {
            listener.onDownvote();
        }
    }

    /**
     * 更新 UI 显示
     */
    private void updateUI() {
        // 更新投票数显示
        textUp.setText(String.valueOf(upvotes));
        textDown.setText(String.valueOf(downvotes));

        // 根据投票状态设置颜色
        int upColor;
        int downColor;

        // 点赞状态：已点赞显示绿色，否则显示灰色
        if ("UP".equals(currentVoteType)) {
            upColor = ContextCompat.getColor(getContext(), R.color.vote_up);
        } else {
            upColor = ContextCompat.getColor(getContext(), R.color.text_hint);
        }

        // 踩状态：已踩显示红色，否则显示灰色
        if ("DOWN".equals(currentVoteType)) {
            downColor = ContextCompat.getColor(getContext(), R.color.vote_down);
        } else {
            downColor = ContextCompat.getColor(getContext(), R.color.text_hint);
        }

        // 应用颜色到图标和文字
        imageUp.setColorFilter(upColor);
        imageDown.setColorFilter(downColor);
        textUp.setTextColor(upColor);
        textDown.setTextColor(downColor);
    }
}