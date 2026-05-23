package com.aram.mayhem.feature.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.aram.mayhem.feature.community.databinding.FragmentStrategyDetailBinding;
import com.aram.mayhem.feature.community.ui.AugmentListAdapter;
import com.aram.mayhem.feature.community.ui.ItemListAdapter;
import com.aram.mayhem.ui.widget.VoteButton;
import com.aram.mayhem.feature.community.viewmodel.StrategyDetailViewModel;
import com.aram.mayhem.network.dto.StrategyDetailResponse;
import com.bumptech.glide.Glide;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 攻略详情页 ── 展示攻略的完整内容和投票功能
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * StrategyDetailFragment 是攻略详情页，用户在社区列表中点击某条攻略后
 * 进入此页面，可以查看攻略的完整内容，包括：
 * 1. 攻略标题、描述、作者信息
 * 2. 关联的英雄名称和头像
 * 3. 推荐的强化符文列表（水平滚动）
 * 4. 推荐的出装物品列表（水平滚动）
 * 5. 投票功能（点赞/点踩/取消投票）
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_strategy_detail.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────┐
 *   │  ProgressBar（加载中）               │  ← progressBar
 *   ├──────────────────────────────────────┤
 *   │  英雄头像 + 英雄名称                 │  ← imageHero + textHero
 *   ├──────────────────────────────────────┤
 *   │  攻略标题                            │  ← textTitle
 *   │  攻略描述                            │  ← textDescription
 *   │  作者昵称                            │  ← textAuthor
 *   ├──────────────────────────────────────┤
 *   │  推荐符文区域                        │  ← layoutAugments
 *   │  ┌────┐ ┌────┐ ┌────┐              │
 *   │  │符文│ │符文│ │符文│  → 水平滚动   │  ← recyclerAugments
 *   │  └────┘ └────┘ └────┘              │
 *   ├──────────────────────────────────────┤
 *   │  推荐出装区域                        │  ← layoutItems
 *   │  ┌────┐ ┌────┐ ┌────┐              │
 *   │  │装备│ │装备│ │装备│  → 水平滚动   │  ← recyclerItems
 *   │  └────┘ └────┘ └────┘              │
 *   ├──────────────────────────────────────┤
 *   │  投票按钮（赞 ↑  ↓ 踩）             │  ← voteButton
 *   └──────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户操作           Fragment              ViewModel           Repository
 *   ┌────────┐       ┌──────────────┐     ┌──────────────┐    ┌──────────┐
 *   │ 进入页面│──────→│ loadStrategy │────→│ loadStrategy │───→│ 网络请求  │
 *   │ 点赞   │──────→│ vote("UP")   │────→│ vote("UP")   │───→│ POST /vote│
 *   │ 点踩   │──────→│ vote("DOWN") │────→│ vote("DOWN") │───→│ POST /vote│
 *   │ 取消投票│──────→│ cancelVote() │────→│ cancelVote() │───→│ DELETE   │
 *   └────────┘       └──────────────┘     └──────────────┘    └──────────┘
 *                          ↓ 观察 LiveData
 *                    strategy   → displayStrategy() 更新所有 UI
 *                    loading    → progressBar 可见性
 *                    error      → Toast 提示
 *                    voteSuccess → Toast 投票结果
 *                    currentVoteType → voteButton 状态同步
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、投票机制详解
 * ═══════════════════════════════════════════════════════════════════
 *
 * VoteButton 是自定义投票组件，支持三种状态：
 * - 未投票：两个按钮都未选中
 * - 已点赞：赞按钮高亮，计数+1
 * - 已点踩：踩按钮高亮，计数+1
 *
 * 投票后 ViewModel 使用"乐观更新"策略：
 * 本地先修改计数（用户立即看到变化），再发送网络请求。
 * 如果网络失败，ViewModel 会回滚本地计数。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、关键依赖
 * ═══════════════════════════════════════════════════════════════════
 *
 * - StrategyDetailViewModel：提供攻略详情数据、投票操作
 * - AugmentListAdapter：符文列表的水平滚动适配器
 * - ItemListAdapter：出装列表的水平滚动适配器
 * - VoteButton：自定义投票组件（赞/踩/取消）
 * - Glide：图片加载库（英雄头像、符文/装备图标）
 */
@AndroidEntryPoint
public class StrategyDetailFragment extends Fragment {

    /**
     * ViewBinding 实例 ── 自动生成的布局绑定类
     *
     * 通过 binding 可以直接访问 XML 中带 id 的 View，
     * 如 binding.textTitle、binding.voteButton 等。
     * 在 onDestroyView 中置 null 防止内存泄漏。
     */
    private FragmentStrategyDetailBinding binding;

    /**
     * 攻略详情 ViewModel ── 管理攻略详情数据和投票逻辑
     *
     * 提供的 LiveData：
     * - strategy：攻略详情数据
     * - loading：加载状态
     * - error：错误信息
     * - voteSuccess：投票是否成功
     * - currentVoteType：当前用户的投票类型（"UP"/"DOWN"/null）
     */
    private StrategyDetailViewModel viewModel;

    /**
     * 符文列表适配器 ── 在详情页中水平展示关联的强化符文
     *
     * 数据源：List<StrategyDetailResponse.AugmentResponse>
     * 每个元素包含符文名称（nameZh）和图标 URL（icon）
     */
    private AugmentListAdapter augmentAdapter;

    /**
     * 出装列表适配器 ── 在详情页中水平展示推荐的装备物品
     *
     * 数据源：List<StrategyDetailResponse.ItemResponse>
     * 每个元素包含物品名称（nameZh）和图标 URL（icon）
     */
    private ItemListAdapter itemAdapter;

    /**
     * 当前攻略的 ID ── 用于从服务器加载攻略详情
     *
     * 获取方式：
     * - 通过 newInstance(strategyId) 工厂方法传入
     * - 在 onCreate 中从 getArguments() 读取
     *
     * 默认值 -1 表示无效 ID（理论上不应出现）
     */
    private long strategyId = -1;

    /**
     * 创建 Fragment 实例的工厂方法 ── 传递攻略 ID
     *
     * ═══════════════════════════════════════════════════════════════════
     * 为什么用工厂方法而不是构造函数？
     * ═══════════════════════════════════════════════════════════════════
     *
     * Android 系统在配置变更（如屏幕旋转）时会通过无参构造函数重新创建 Fragment，
     * 然后通过 setArguments() 恢复之前保存的参数。
     * 如果用带参构造函数，系统重建时参数会丢失。
     *
     * 使用方式：
     * StrategyDetailFragment fragment = StrategyDetailFragment.newInstance(123L);
     * // 内部将 strategyId=123 存入 Bundle
     *
     * @param strategyId 攻略 ID，用于从服务器加载对应攻略的详情数据
     * @return 配置好参数的 Fragment 实例
     */
    public static StrategyDetailFragment newInstance(long strategyId) {
        StrategyDetailFragment fragment = new StrategyDetailFragment();
        Bundle args = new Bundle();
        args.putLong("strategyId", strategyId);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Fragment 创建时的回调 ── 读取传入的攻略 ID
     *
     * 这个方法在 onCreateView 之前调用，适合读取参数。
     * 我们从 Bundle 中取出 strategyId，后续加载详情时使用。
     *
     * @param savedInstanceState 保存的实例状态（屏幕旋转恢复用）
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            strategyId = getArguments().getLong("strategyId", -1);
        }
    }

    /**
     * 创建 Fragment 的视图 ── 加载布局文件
     *
     * 使用 ViewBinding 加载 fragment_strategy_detail.xml 布局，
     * 返回根视图给系统显示。
     *
     * @param inflater  布局加载器
     * @param container 父容器
     * @param savedInstanceState 保存的实例状态
     * @return Fragment 的根视图
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStrategyDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化 ── 设置所有组件和数据观察
     *
     * 初始化步骤：
     * 1. 获取 ViewModel 实例
     * 2. 设置符文和出装的 RecyclerView
     * 3. 设置投票按钮的监听器
     * 4. 观察 ViewModel 的 LiveData
     * 5. 如果 strategyId 有效，加载攻略详情
     *
     * @param view Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StrategyDetailViewModel.class);

        setupRecyclerViews();
        setupVoteButton();
        observeViewModel();

        if (strategyId != -1) {
            viewModel.loadStrategy(strategyId);
        }
    }

    /**
     * 设置 RecyclerView ── 初始化符文和出装的水平滚动列表
     *
     * ═══════════════════════════════════════════════════════════════════
     * 为什么用水平滚动？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 符文和出装通常有 3~6 个，不适合垂直列表（太占空间），
     * 水平滚动更紧凑，用户可以左右滑动查看更多。
     *
     *   ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐
     *   │符文1│ │符文2│ │符文3│ │符文4│ │符文5│ →
     *   └────┘ └────┘ └────┘ └────┘ └────┘
     *
     * LinearLayoutManager 的第二个参数 HORIZONTAL 表示水平方向，
     * 第三个参数 false 表示不反转布局（从左到右排列）。
     */
    private void setupRecyclerViews() {
        augmentAdapter = new AugmentListAdapter();
        binding.recyclerAugments.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerAugments.setAdapter(augmentAdapter);

        itemAdapter = new ItemListAdapter();
        binding.recyclerItems.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerItems.setAdapter(itemAdapter);
    }

    /**
     * 设置投票按钮 ── 监听用户的投票操作
     *
     * ═══════════════════════════════════════════════════════════════════
     * VoteButton 的三种回调
     * ═══════════════════════════════════════════════════════════════════
     *
     * VoteButton.OnVoteChangeListener 接口定义了三个方法：
     * - onUpvote()：用户点击了"赞"按钮 → 调用 viewModel.vote("UP")
     * - onDownvote()：用户点击了"踩"按钮 → 调用 viewModel.vote("DOWN")
     * - onCancelVote()：用户取消投票 → 调用 viewModel.cancelVote()
     *
     * ViewModel 的 vote() 方法会：
     * 1. 发送网络请求到服务器
     * 2. 成功后本地乐观更新投票计数
     * 3. 更新 currentVoteType LiveData
     *
     * ═══════════════════════════════════════════════════════════════════
     * 投票状态流转
     * ═══════════════════════════════════════════════════════════════════
     *
     *   未投票 ──点赞──→ 已赞（UP）
     *   未投票 ──点踩──→ 已踩（DOWN）
     *   已赞   ──取消──→ 未投票
     *   已踩   ──取消──→ 未投票
     *   已赞   ──点踩──→ 已踩（DOWN）  ← 先取消再点踩
     *   已踩   ──点赞──→ 已赞（UP）    ← 先取消再点赞
     */
    private void setupVoteButton() {
        binding.voteButton.setOnVoteChangeListener(new VoteButton.OnVoteChangeListener() {
            @Override
            public void onUpvote() {
                viewModel.vote("UP");
            }

            @Override
            public void onDownvote() {
                viewModel.vote("DOWN");
            }

            @Override
            public void onCancelVote() {
                viewModel.cancelVote();
            }
        });
    }

    /**
     * 观察 ViewModel 的 LiveData ── 建立数据绑定关系
     *
     * ═══════════════════════════════════════════════════════════════════
     * 观察的 LiveData 列表
     * ═══════════════════════════════════════════════════════════════════
     *
     * | LiveData          | 触发时机               | UI 更新               |
     * |-------------------|----------------------|----------------------|
     * | strategy          | 攻略详情加载完成       | displayStrategy()    |
     * | loading           | 加载状态变化           | progressBar 可见性    |
     * | error             | 发生错误              | Toast 提示           |
     * | voteSuccess       | 投票操作完成           | Toast 投票结果        |
     * | currentVoteType   | 投票类型变化           | voteButton 状态同步   |
     */
    private void observeViewModel() {
        /**
         * 观察攻略详情数据 ── 数据变化时更新整个页面
         *
         * 触发时机：
         * - 首次加载完成（loadStrategy 成功）
         * - 投票后本地乐观更新（upvotes/downvotes 计数变化）
         *
         * displayStrategy() 方法会将数据绑定到所有 UI 组件
         */
        viewModel.getStrategy().observe(getViewLifecycleOwner(), this::displayStrategy);

        /**
         * 观察加载状态 ── 控制进度条的显示/隐藏
         *
         * loading=true  → 显示 progressBar（正在加载攻略详情）
         * loading=false → 隐藏 progressBar（加载完成或失败）
         */
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        /**
         * 观察错误信息 ── 显示错误提示
         *
         * 常见错误场景：
         * - 网络请求失败
         * - 攻略不存在（strategyId 无效）
         * - 服务器内部错误
         */
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * 观察投票结果 ── 显示投票成功/失败提示
         *
         * success=true  → "投票成功"
         * success=false → "投票失败"
         */
        viewModel.getVoteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                Toast.makeText(requireContext(),
                        success ? R.string.vote_success : R.string.vote_failed,
                        Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * 观察当前投票类型 ── 同步 VoteButton 的选中状态
         *
         * voteType 的可能值：
         * - "UP"：用户已点赞 → VoteButton 高亮赞按钮
         * - "DOWN"：用户已点踩 → VoteButton 高亮踩按钮
         * - null：用户未投票 → VoteButton 两个按钮都不高亮
         *
         * 为什么还要检查 strategy 不为 null？
         * 因为 currentVoteType 可能在攻略数据加载之前就触发（初始值），
         * 此时还没有攻略数据，不需要更新 VoteButton。
         */
        viewModel.getCurrentVoteType().observe(getViewLifecycleOwner(), voteType -> {
            StrategyDetailResponse current = viewModel.getStrategy().getValue();
            if (current != null) {
                binding.voteButton.setCurrentVoteType(voteType);
            }
        });
    }

    /**
     * 显示攻略详情 ── 将数据绑定到所有 UI 组件
     *
     * ═══════════════════════════════════════════════════════════════════
     * 这个方法做了什么？
     * ═══════════════════════════════════════════════════════════════════
     *
     * 将 StrategyDetailResponse 中的每个字段绑定到对应的 UI 组件：
     *
     * | 数据字段              | UI 组件           | 说明                     |
     * |---------------------|-------------------|-------------------------|
     * | title               | textTitle         | 攻略标题                  |
     * | description         | textDescription   | 攻略描述                  |
     * | authorNickname      | textAuthor        | 作者昵称                  |
     * | heroName            | textHero          | 英雄名称                  |
     * | heroIcon            | imageHero         | 英雄头像（Glide 加载）     |
     * | upvotes/downvotes   | voteButton        | 投票计数                  |
     * | userVoteType        | voteButton        | 当前用户投票状态           |
     * | augments            | recyclerAugments  | 符文列表（水平滚动）       |
     * | items               | recyclerItems     | 出装列表（水平滚动）       |
     *
     * @param strategy 攻略详情数据对象，null 时直接返回不做任何操作
     */
    private void displayStrategy(StrategyDetailResponse strategy) {
        if (strategy == null) return;

        binding.textTitle.setText(strategy.getTitle());
        binding.textDescription.setText(strategy.getDescription());
        binding.textAuthor.setText(strategy.getAuthorNickname());
        binding.textHero.setText(strategy.getHeroName());

        /**
         * 设置投票计数 ── 传入赞/踩数量给 VoteButton
         *
         * upvotes/downvotes 可能为 null（新发布的攻略还没有投票），
         * 使用三元运算符提供默认值 0。
         */
        Integer upvotes = strategy.getUpvotes();
        Integer downvotes = strategy.getDownvotes();
        binding.voteButton.setVoteCounts(
                upvotes != null ? upvotes : 0,
                downvotes != null ? downvotes : 0
        );

        /**
         * 设置当前用户的投票状态 ── 同步 VoteButton 的选中状态
         *
         * userVoteType 的值：
         * - "UP"：当前用户已点赞 → VoteButton 的赞按钮高亮
         * - "DOWN"：当前用户已点踩 → VoteButton 的踩按钮高亮
         * - null：当前用户未投票 → 两个按钮都不高亮
         */
        String voteType = strategy.getUserVoteType();
        binding.voteButton.setCurrentVoteType(voteType);

        /**
         * 加载英雄头像 ── 使用 Glide 图片加载库
         *
         * Glide.with(this)        → 绑定 Fragment 生命周期，Fragment 销毁时自动取消加载
         * .load(strategy.getHeroIcon()) → 加载图片 URL
         * .circleCrop()           → 圆形裁剪（英雄头像通常是圆形）
         * .into(binding.imageHero) → 加载到 ImageView
         *
         * 只在 heroIcon 不为空时加载，避免 Glide 加载空 URL 报错
         */
        if (strategy.getHeroIcon() != null && !strategy.getHeroIcon().isEmpty()) {
            Glide.with(this)
                    .load(strategy.getHeroIcon())
                    .circleCrop()
                    .into(binding.imageHero);
        }

        /**
         * 更新符文列表 ── 显示或隐藏符文区域
         *
         * - augments 不为 null 且不为空 → 显示符文区域，更新适配器数据
         * - augments 为 null 或为空 → 隐藏整个符文区域（包括标题）
         *
         * 为什么要隐藏整个区域而不是显示"暂无符文"？
         * 因为符文是可选的（发布攻略时不强制选择符文），
         * 如果攻略没有关联符文，显示"暂无符文"反而让页面显得空旷。
         */
        if (strategy.getAugments() != null) {
            binding.layoutAugments.setVisibility(strategy.getAugments().isEmpty() ? View.GONE : View.VISIBLE);
            augmentAdapter.setAugments(strategy.getAugments());
        } else {
            binding.layoutAugments.setVisibility(View.GONE);
        }

        /**
         * 更新出装列表 ── 显示或隐藏出装区域
         *
         * 逻辑与符文列表相同：
         * - 有数据 → 显示，更新适配器
         * - 无数据 → 隐藏整个区域
         */
        if (strategy.getItems() != null) {
            binding.layoutItems.setVisibility(strategy.getItems().isEmpty() ? View.GONE : View.VISIBLE);
            itemAdapter.setItems(strategy.getItems());
        } else {
            binding.layoutItems.setVisibility(View.GONE);
        }
    }

    /**
     * Fragment 视图销毁 ── 清理 binding 引用，防止内存泄漏
     *
     * Fragment 的 View 可能在 Fragment 还存活时就被销毁（比如返回上一页），
     * 如果不将 binding 置为 null，它会持有所有 View 的引用，导致内存泄漏。
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
