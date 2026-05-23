package com.aram.mayhem.feature.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aram.mayhem.feature.community.databinding.FragmentReleaseSuccessBinding;
import com.aram.mayhem.network.dto.StrategyDetailResponse;

/**
 * 发布成功页 ── 攻略发布成功后的确认页面
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * ReleaseSuccessFragment 是攻略发布成功后显示的确认页面，用户可以：
 * 1. 查看刚发布的攻略预览（标题、英雄、描述）
 * 2. 分享攻略到其他应用（微信、QQ 等）
 * 3. 点击"返回社区"按钮回到攻略列表
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_release_success.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────┐
 *   │         ✅ 发布成功！                 │  ← 成功图标/文字
 *   ├──────────────────────────────────────┤
 *   │  攻略标题预览                        │  ← textPreviewTitle
 *   │  英雄名称预览                        │  ← textPreviewHero
 *   │  攻略描述预览                        │  ← textPreviewDescription
 *   ├──────────────────────────────────────┤
 *   │  [ 分享攻略 ]                        │  ← btnShare
 *   │  [ 返回社区 ]                        │  ← btnBackCommunity
 *   └──────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据传递方式
 * ═══════════════════════════════════════════════════════════════════
 *
 * 攻略数据通过 Bundle（Fragment 参数）传递：
 *
 *   PublishStrategyFragment              ReleaseSuccessFragment
 *   ┌──────────────────┐               ┌──────────────────┐
 *   │ 发布成功          │               │                  │
 *   │ StrategyDetailResponse            │                  │
 *   │   .id            │─── Bundle ───→ │ getArguments()   │
 *   │   .title         │               │   .getString     │
 *   │   .heroName      │               │     ("title")    │
 *   │   .description   │               │     ("heroName") │
 *   └──────────────────┘               │     ("description")│
 *                                       └──────────────────┘
 *
 * 为什么用 Bundle 而不是直接传对象？
 * - Bundle 是 Android 推荐的 Fragment 参数传递方式
 * - 支持配置变更后自动恢复（屏幕旋转等）
 * - StrategyDetailResponse 可能包含复杂嵌套对象，不适合全部放入 Bundle
 * - 这里只需要标题、英雄名、描述三个字符串，足够展示预览
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、分享功能
 * ═══════════════════════════════════════════════════════════════════
 *
 * 使用 Android 系统的 Intent.ACTION_SEND 分享机制：
 * 1. 构造分享文本（包含英雄名、标题、描述）
 * 2. 创建 ACTION_SEND Intent
 * 3. 通过 Intent.createChooser 弹出分享选择器
 * 4. 用户选择目标应用（微信、QQ、短信等）完成分享
 *
 *   分享文本格式：
 *   🎮 ARAM Mayhem - 玩法分享
 *   英雄: 盖伦
 *   标题: 大乱斗盖伦出装攻略
 *   描述: 这是一篇关于盖伦的...
 */
public class ReleaseSuccessFragment extends Fragment {

    /**
     * ViewBinding 实例 ── 自动生成的布局绑定类
     *
     * 在 onDestroyView 中置 null 防止内存泄漏。
     */
    private FragmentReleaseSuccessBinding binding;

    /**
     * 攻略详情数据 ── 当前未完整使用
     *
     * 在 onCreate 中创建了空对象，但实际数据通过 Bundle 读取。
     * 这个字段保留用于未来扩展（如加载完整攻略详情）。
     */
    private StrategyDetailResponse strategy;

    /**
     * 创建 Fragment 实例的工厂方法 ── 传递攻略数据
     *
     * 将 StrategyDetailResponse 中的关键字段提取到 Bundle 中：
     * - strategyId：攻略 ID
     * - title：攻略标题
     * - heroName：英雄名称
     * - description：攻略描述
     *
     * 为什么不直接将 StrategyDetailResponse 序列化到 Bundle？
     * - StrategyDetailResponse 可能没有实现 Serializable/Parcelable
     * - 只需要几个字段用于预览，没必要传递整个对象
     * - 简单类型（Long、String）在 Bundle 中更可靠
     *
     * @param strategy 发布成功的攻略详情对象
     * @return 配置好参数的 Fragment 实例
     */
    public static ReleaseSuccessFragment newInstance(StrategyDetailResponse strategy) {
        ReleaseSuccessFragment fragment = new ReleaseSuccessFragment();
        Bundle args = new Bundle();
        args.putLong("strategyId", strategy.getId() != null ? strategy.getId() : -1);
        args.putString("title", strategy.getTitle());
        args.putString("heroName", strategy.getHeroName());
        args.putString("description", strategy.getDescription());
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Fragment 创建时的回调 ── 初始化攻略数据对象
     *
     * 如果有传入参数，创建一个空的 StrategyDetailResponse 对象。
     * 实际数据通过 getArguments() 在 onViewCreated 中读取。
     *
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            strategy = new StrategyDetailResponse();
        }
    }

    /**
     * 创建 Fragment 的视图 ── 加载布局文件
     *
     * @param inflater  布局加载器
     * @param container 父容器
     * @param savedInstanceState 保存的实例状态
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReleaseSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化 ── 显示攻略预览和设置按钮
     *
     * 初始化步骤：
     * 1. 从 Bundle 读取攻略数据（标题、英雄名、描述）
     * 2. 将数据显示到预览文本框
     * 3. 设置分享按钮点击事件
     * 4. 设置返回社区按钮点击事件
     *
     * @param view Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /**
         * 从 Bundle 读取攻略数据并显示预览
         *
         * getArguments() 返回 newInstance() 中设置的 Bundle，
         * getString() 的第二个参数是默认值（key 不存在时返回空字符串）
         */
        if (getArguments() != null) {
            String title = getArguments().getString("title", "");
            String heroName = getArguments().getString("heroName", "");
            String description = getArguments().getString("description", "");

            binding.textPreviewTitle.setText(title);
            binding.textPreviewHero.setText(heroName);
            binding.textPreviewDescription.setText(description);
        }

        /**
         * 分享按钮 ── 点击后弹出系统分享选择器
         *
         * 调用 shareStrategy() 方法，构造分享文本并通过 Intent 发送。
         */
        binding.btnShare.setOnClickListener(v -> shareStrategy());

        /**
         * 返回社区按钮 ── 点击后返回社区攻略列表
         *
         * popBackStack() 将当前 Fragment 从回退栈中弹出，
         * 回到社区列表页。同时 PublishStrategyFragment 也会被弹出，
         * 用户直接回到列表，不会看到发布表单。
         */
        binding.btnBackCommunity.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    /**
     * 分享攻略 ── 通过 Android 系统分享机制将攻略内容发送到其他应用
     *
     * ═══════════════════════════════════════════════════════════════════
     * 分享流程
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. 从 Bundle 读取攻略数据
     * 2. 构造分享文本（格式化的字符串）
     * 3. 创建 ACTION_SEND Intent
     *    - setType("text/plain")：指定分享内容类型为纯文本
     *    - putExtra(EXTRA_TEXT, shareText)：设置分享内容
     * 4. 通过 Intent.createChooser 创建选择器
     *    - 弹出系统分享面板，列出所有支持文本分享的应用
     *    - 第二个参数是选择器的标题
     * 5. startActivity 启动用户选择的目标应用
     *
     * ═══════════════════════════════════════════════════════════════════
     * 分享文本示例
     * ═══════════════════════════════════════════════════════════════════
     *
     * 🎮 ARAM Mayhem - 玩法分享
     * 英雄: 盖伦
     * 标题: 大乱斗盖伦出装攻略
     * 描述: 这是一篇关于盖伦的ARAM攻略...
     */
    private void shareStrategy() {
        String title = getArguments() != null ? getArguments().getString("title", "") : "";
        String heroName = getArguments() != null ? getArguments().getString("heroName", "") : "";
        String description = getArguments() != null ? getArguments().getString("description", "") : "";

        String shareText = "🎮 ARAM Mayhem - 玩法分享\n" +
                "英雄: " + heroName + "\n" +
                "标题: " + title + "\n" +
                "描述: " + description;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_strategy)));
    }

    /**
     * Fragment 视图销毁 ── 清理 binding 引用，防止内存泄漏
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
