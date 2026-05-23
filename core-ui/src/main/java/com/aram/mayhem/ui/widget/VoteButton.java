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
 * 投票按钮组件 ── 攻略详情页的点赞/踩交互控件
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个组件是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * VoteButton 是社区攻略详情页的投票控件，用户可以对攻略点赞或踩。
 * 它是一个双状态切换按钮，类似 Reddit 的投票机制。
 *
 * 视觉结构：
 * ┌──────────────────────────────────┐
 * │  👍 42        👎 3               │
 * │  (点赞图标)    (踩图标)           │
 * │  (点赞数)      (踩数)            │
 * └──────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、三种投票状态
 * ═══════════════════════════════════════════════════════════════════
 *
 * currentVoteType 有三种可能的值：
 * - null：未投票（默认状态，两个按钮都是灰色）
 * - "UP"：已点赞（点赞按钮变绿色，踩按钮灰色）
 * - "DOWN"：已踩（踩按钮变红色，点赞按钮灰色）
 *
 * 状态切换逻辑：
 * ┌──────────┐  点击👍  ┌──────────┐  点击👍  ┌──────────┐
 * │  未投票   │───────→│  已点赞   │───────→│  未投票   │
 * │ (null)   │←───────│  (UP)    │         │ (null)   │
 * └──────────┘  点击👎  └──────────┘         └──────────┘
 *      │                                       │
 *      │ 点击👎                                │ 点击👎
 *      ↓                                       ↓
 * ┌──────────┐  点击👎  ┌──────────┐
 * │  已踩    │───────→│  未投票   │
 * │ (DOWN)   │         │ (null)   │
 * └──────────┘         └──────────┘
 *
 * 核心规则：再次点击已选中的按钮 = 取消投票
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、颜色变化规则
 * ═══════════════════════════════════════════════════════════════════
 *
 * | 状态       | 点赞图标/文字 | 踩图标/文字 |
 * |-----------|--------------|------------|
 * | 未投票     | 灰色(hint)   | 灰色(hint)  |
 * | 已点赞     | 绿色(vote_up)| 灰色(hint)  |
 * | 已踩       | 灰色(hint)   | 红色(vote_down)|
 *
 * setColorFilter()：给图标着色（类似 CSS filter）
 * setTextColor()：设置文字颜色
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 * 1. Fragment 从 ViewModel 获取攻略详情（包含 upvotes/downvotes/currentVoteType）
 * 2. 调用 setVoteCounts() 和 setCurrentVoteType() 设置初始状态
 * 3. 用户点击按钮 → OnVoteChangeListener 回调
 * 4. Fragment 调用 ViewModel 发起网络请求
 * 5. 网络成功后更新 VoteButton 状态
 */
public class VoteButton extends LinearLayout {

    /** 点赞图标 ── 点击触发点赞/取消点赞 */
    private ImageView imageUp;

    /** 踩图标 ── 点击触发踩/取消踩 */
    private ImageView imageDown;

    /** 点赞数文字 ── 显示当前点赞总数 */
    private TextView textUp;

    /** 踩数文字 ── 显示当前踩总数 */
    private TextView textDown;

    /** 投票变更回调监听器 ── Fragment 实现此接口处理投票逻辑 */
    private OnVoteChangeListener listener;

    /** 当前投票状态 ── 三种值：null(未投票) / "UP"(已点赞) / "DOWN"(已踩) */
    private String currentVoteType;

    /** 点赞数 ── 从服务器获取的当前点赞总数 */
    private int upvotes = 0;

    /** 踩数 ── 从服务器获取的当前踩总数 */
    private int downvotes = 0;

    /**
     * 投票变更监听器接口 ── Fragment 实现此接口处理投票网络请求
     *
     * 三个回调方法分别对应三种操作：
     * - onUpvote()：点赞（未投票/已踩 → 已点赞）
     * - onDownvote()：踩（未投票/已点赞 → 已踩）
     * - onCancelVote()：取消投票（已点赞/已踩 → 未投票）
     *
     * Fragment 中的典型实现：
     * <pre>
     * voteButton.setOnVoteChangeListener(new VoteButton.OnVoteChangeListener() {
     *     public void onUpvote() { viewModel.upvote(strategyId); }
     *     public void onDownvote() { viewModel.downvote(strategyId); }
     *     public void onCancelVote() { viewModel.cancelVote(strategyId); }
     * });
     * </pre>
     */
    public interface OnVoteChangeListener {
        /** 点赞回调 ── 调用 ViewModel 发起点赞网络请求 */
        void onUpvote();
        /** 踩回调 ── 调用 ViewModel 发起踩网络请求 */
        void onDownvote();
        /** 取消投票回调 ── 调用 ViewModel 发起取消投票网络请求 */
        void onCancelVote();
    }

    /**
     * 构造函数（代码调用）── 在 Java 代码中动态创建时使用
     * @param context 上下文
     */
    public VoteButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 布局调用）── 在 XML 中使用 <VoteButton> 时调用
     * @param context 上下文
     * @param attrs   XML 属性集合
     */
    public VoteButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 初始化组件 ── 加载布局、绑定视图、设置点击事件
     *
     * @param context 上下文
     */
    private void init(Context context) {
        // 加载布局文件 widget_vote_button.xml
        // inflate(resource, root, attachToRoot)：
        // - resource：布局资源 ID
        // - root：父容器（this = VoteButton 自身）
        // - attachToRoot：true = 将布局直接添加到 VoteButton 中
        LayoutInflater.from(context).inflate(R.layout.widget_vote_button, this, true);

        // 通过 findViewById 绑定布局中的视图
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
     * @param listener 回调实现（通常是 Fragment）
     */
    public void setOnVoteChangeListener(OnVoteChangeListener listener) {
        this.listener = listener;
    }

    /**
     * 设置投票数量 ── 从服务器获取数据后调用
     *
     * @param upvotes   点赞数
     * @param downvotes 踩数
     */
    public void setVoteCounts(int upvotes, int downvotes) {
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        // 更新 UI 显示
        updateUI();
    }

    /**
     * 设置当前投票类型 ── 从服务器获取用户投票状态后调用
     *
     * @param voteType 投票类型（"UP" / "DOWN" / null）
     */
    public void setCurrentVoteType(String voteType) {
        this.currentVoteType = voteType;
        // 更新 UI 显示（颜色变化）
        updateUI();
    }

    /**
     * 处理点赞按钮点击 ── 切换点赞状态
     *
     * 逻辑：
     * - 当前已点赞 → 取消点赞（回调 onCancelVote）
     * - 当前未点赞 → 点赞（回调 onUpvote）
     *
     * 注意：如果当前已踩，点击点赞不会直接切换到点赞
     * 而是先回调 onUpvote，由 ViewModel 处理切换逻辑
     */
    private void handleUpClick() {
        if (listener == null) return;

        if ("UP".equals(currentVoteType)) {
            // 已点赞 → 取消
            listener.onCancelVote();
        } else {
            // 未点赞/已踩 → 点赞
            listener.onUpvote();
        }
    }

    /**
     * 处理踩按钮点击 ── 切换踩状态
     *
     * 逻辑：
     * - 当前已踩 → 取消踩（回调 onCancelVote）
     * - 当前未踩 → 踩（回调 onDownvote）
     */
    private void handleDownClick() {
        if (listener == null) return;

        if ("DOWN".equals(currentVoteType)) {
            // 已踩 → 取消
            listener.onCancelVote();
        } else {
            // 未踩/已点赞 → 踩
            listener.onDownvote();
        }
    }

    /**
     * 更新 UI 显示 ── 根据当前投票状态刷新图标颜色和文字
     *
     * 执行步骤：
     * 1. 更新投票数文字
     * 2. 根据投票状态计算颜色
     * 3. 应用颜色到图标（setColorFilter）和文字（setTextColor）
     */
    private void updateUI() {
        // 步骤 1：更新投票数文字
        // String.valueOf() 将 int 转为 String（如 42 → "42"）
        textUp.setText(String.valueOf(upvotes));
        textDown.setText(String.valueOf(downvotes));

        // 步骤 2：计算颜色
        int upColor;
        int downColor;

        // 点赞状态：已点赞显示绿色，否则显示灰色
        // ContextCompat.getColor()：兼容低版本的 getColor 方法
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

        // 步骤 3：应用颜色
        // setColorFilter()：给 ImageView 的图标叠加颜色滤镜
        // 效果类似 CSS filter，将图标从默认黑色变为指定颜色
        imageUp.setColorFilter(upColor);
        imageDown.setColorFilter(downColor);
        // setTextColor()：设置 TextView 文字颜色
        textUp.setTextColor(upColor);
        textDown.setTextColor(downColor);
    }
}
