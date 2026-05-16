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
 * 投票按钮组件
 *
 * 功能：攻略详情页的点赞/踩按钮，支持状态切换和动画
 * 状态：未投票 / UP / DOWN
 * 关联：StrategyDetailFragment
 */
public class VoteButton extends LinearLayout {

    private ImageView imageUp;
    private ImageView imageDown;
    private TextView textUp;
    private TextView textDown;
    private OnVoteChangeListener listener;

    private String currentVoteType;
    private int upvotes = 0;
    private int downvotes = 0;

    public interface OnVoteChangeListener {
        void onUpvote();
        void onDownvote();
        void onCancelVote();
    }

    public VoteButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VoteButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_vote_button, this, true);
        imageUp = findViewById(R.id.image_up);
        imageDown = findViewById(R.id.image_down);
        textUp = findViewById(R.id.text_up);
        textDown = findViewById(R.id.text_down);

        imageUp.setOnClickListener(v -> handleUpClick());
        imageDown.setOnClickListener(v -> handleDownClick());
    }

    public void setOnVoteChangeListener(OnVoteChangeListener listener) {
        this.listener = listener;
    }

    public void setVoteCounts(int upvotes, int downvotes) {
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        updateUI();
    }

    public void setCurrentVoteType(String voteType) {
        this.currentVoteType = voteType;
        updateUI();
    }

    private void handleUpClick() {
        if (listener == null) return;

        if ("UP".equals(currentVoteType)) {
            listener.onCancelVote();
        } else {
            listener.onUpvote();
        }
    }

    private void handleDownClick() {
        if (listener == null) return;

        if ("DOWN".equals(currentVoteType)) {
            listener.onCancelVote();
        } else {
            listener.onDownvote();
        }
    }

    private void updateUI() {
        textUp.setText(String.valueOf(upvotes));
        textDown.setText(String.valueOf(downvotes));

        int upColor;
        int downColor;

        if ("UP".equals(currentVoteType)) {
            upColor = ContextCompat.getColor(getContext(), R.color.vote_up);
        } else {
            upColor = ContextCompat.getColor(getContext(), R.color.text_hint);
        }

        if ("DOWN".equals(currentVoteType)) {
            downColor = ContextCompat.getColor(getContext(), R.color.vote_down);
        } else {
            downColor = ContextCompat.getColor(getContext(), R.color.text_hint);
        }

        imageUp.setColorFilter(upColor);
        imageDown.setColorFilter(downColor);
        textUp.setTextColor(upColor);
        textDown.setTextColor(downColor);
    }
}