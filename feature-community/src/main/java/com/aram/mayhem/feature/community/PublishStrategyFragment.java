package com.aram.mayhem.feature.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.aram.mayhem.data.local.TokenStore;
import com.aram.mayhem.feature.community.databinding.FragmentPublishStrategyBinding;
import com.aram.mayhem.feature.community.viewmodel.PublishStrategyViewModel;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 发布攻略页 ── 攻略发布表单，支持英雄/符文/装备选择
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个 Fragment 是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * PublishStrategyFragment 是攻略发布页面，用户可以在这里：
 * 1. 选择一个英雄（必选，通过搜索下拉框选择）
 * 2. 填写攻略标题（必填）
 * 3. 填写攻略描述（必填，至少10个字符）
 * 4. 选择关联的强化符文（可选，多选对话框）
 * 5. 选择推荐的出装物品（可选，多选对话框）
 * 6. 点击"发布"按钮提交攻略
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、页面布局结构（fragment_publish_strategy.xml）
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ┌──────────────────────────────────────┐
 *   │  Toolbar（返回按钮）                  │  ← toolbar
 *   ├──────────────────────────────────────┤
 *   │  英雄搜索框（AutoCompleteTextView）   │  ← autoHero
 *   ├──────────────────────────────────────┤
 *   │  标题输入框                          │  ← editTitle
 *   ├──────────────────────────────────────┤
 *   │  描述输入框（多行）                   │  ← editDescription
 *   ├──────────────────────────────────────┤
 *   │  选择符文按钮                        │  ← btnSelectAugments
 *   │  已选符文 ChipGroup                  │  ← chipGroupAugments
 *   ├──────────────────────────────────────┤
 *   │  选择装备按钮                        │  ← btnSelectItems
 *   │  已选装备 ChipGroup                  │  ← chipGroupItems
 *   ├──────────────────────────────────────┤
 *   │  ProgressBar（发布中）               │  ← progressBar
 *   │  发布按钮                            │  ← btnPublish
 *   └──────────────────────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、数据流向
 * ═══════════════════════════════════════════════════════════════════
 *
 *   用户操作           Fragment              ViewModel           Repository
 *   ┌────────┐       ┌──────────────┐     ┌──────────────┐    ┌──────────┐
 *   │ 选择英雄│──────→│ setSelectedHeroId│  │ selectedHeroId│   │          │
 *   │ 输入标题│──────→│ setTitle()   │────→│ title        │    │          │
 *   │ 输入描述│──────→│ setDescription│────→│ description  │    │          │
 *   │ 选择符文│──────→│ addAugment() │────→│ selectedAugmentIds│ │      │
 *   │ 选择装备│──────→│ addItem()    │────→│ selectedItemIds│   │      │
 *   │ 点击发布│──────→│ publish()    │────→│ publish()    │───→│ POST     │
 *   └────────┘       └──────────────┘     └──────────────┘    └──────────┘
 *                          ↓ 观察 LiveData
 *                    publishing  → btnPublish 禁用/启用 + progressBar
 *                    error      → Toast 错误提示
 *                    publishedStrategy → 发布成功，返回上一页
 *                    isFormValid → btnPublish 启用/禁用
 *                    selectedHeroId → autoHero 显示英雄名
 *                    selectedAugmentIds → chipGroupAugments 更新
 *                    selectedItemIds → chipGroupItems 更新
 *
 * ═══════════════════════════════════════════════════════════════════
 * 四、登录状态检查
 * ═══════════════════════════════════════════════════════════════════
 *
 * 发布攻略需要登录，页面打开时会检查 TokenStore 中的登录状态：
 * - 已登录（有 Token 且未过期）→ 正常显示表单
 * - 未登录 → Toast 提示"请先登录"→ 自动返回上一页
 *
 * ═══════════════════════════════════════════════════════════════════
 * 五、Chip 组件说明
 * ═══════════════════════════════════════════════════════════════════
 *
 * Chip 是 Material Design 的"标签芯片"组件，类似这样：
 *   ┌──────────────┐
 *   │ 符文名称  ✕  │  ← 点击 ✕ 可以移除
 *   └──────────────┘
 *
 * 每选择一个符文/装备，就会在 ChipGroup 中添加一个 Chip，
 * 点击 Chip 的关闭图标可以移除对应的选择。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 六、关键依赖
 * ═══════════════════════════════════════════════════════════════════
 *
 * - PublishStrategyViewModel：管理表单数据和发布逻辑
 * - TokenStore：检查登录状态（Hilt 注入）
 * - HeroOption/AugmentOption/ItemOption：内部数据类，存储选项列表
 */
@AndroidEntryPoint
public class PublishStrategyFragment extends Fragment {

    /**
     * ViewBinding 实例 ── 自动生成的布局绑定类
     *
     * 通过 binding 可以直接访问 XML 中的 View。
     * 在 onDestroyView 中置 null 防止内存泄漏。
     */
    private FragmentPublishStrategyBinding binding;

    /**
     * 发布攻略 ViewModel ── 管理表单数据和发布逻辑
     *
     * 提供的 LiveData：
     * - selectedHeroId：选中的英雄 ID
     * - title：攻略标题
     * - description：攻略描述
     * - selectedAugmentIds：选中的符文 ID 列表
     * - selectedItemIds：选中的装备 ID 列表
     * - publishing：是否正在发布
     * - error：错误信息
     * - publishedStrategy：发布成功的攻略数据
     * - isFormValid：表单是否有效（控制发布按钮启用状态）
     */
    private PublishStrategyViewModel viewModel;

    /**
     * 英雄选项列表 ── 供搜索下拉框使用
     *
     * 数据来源：由宿主通过 setHeroOptions() 方法设置
     * 每个元素包含英雄 ID 和名称
     *
     * 使用场景：
     * 用户在 AutoCompleteTextView 中输入时，
     * 系统根据 heroOptions 中的名称进行模糊匹配并显示下拉建议
     */
    private List<HeroOption> heroOptions = new ArrayList<>();

    /**
     * 符文选项列表 ── 供多选对话框使用
     *
     * 数据来源：由宿主通过 setAugmentOptions() 方法设置
     * 每个元素包含符文 ID 和名称
     *
     * 使用场景：
     * 用户点击"选择符文"按钮时，弹出多选对话框，
     * 对话框中的选项来自 augmentOptions
     */
    private List<AugmentOption> augmentOptions = new ArrayList<>();

    /**
     * 装备选项列表 ── 供多选对话框使用
     *
     * 数据来源：由宿主通过 setItemOptions() 方法设置
     * 每个元素包含装备 ID 和名称
     *
     * 使用场景：
     * 用户点击"选择装备"按钮时，弹出多选对话框，
     * 对话框中的选项来自 itemOptions
     */
    private List<ItemOption> itemOptions = new ArrayList<>();

    /**
     * Token 存储 ── 用于检查用户登录状态
     *
     * 通过 Hilt 的 @Inject 注入，不需要手动创建实例。
     * TokenStore 封装了 SharedPreferences，提供：
     * - hasToken()：是否有保存的 Token
     * - isTokenExpired()：Token 是否已过期
     *
     * 为什么需要检查登录状态？
     * 发布攻略需要认证（服务器需要 Bearer Token），
     * 未登录用户无法发布，需要先跳转到登录页。
     */
    @Inject
    TokenStore tokenStore;

    /**
     * 创建 Fragment 实例的工厂方法
     *
     * @return 新的 PublishStrategyFragment 实例
     */
    public static PublishStrategyFragment newInstance() {
        return new PublishStrategyFragment();
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
        binding = FragmentPublishStrategyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后的初始化 ── 登录检查和组件设置
     *
     * ═══════════════════════════════════════════════════════════════════
     * 初始化流程
     * ═══════════════════════════════════════════════════════════════════
     *
     * 1. 检查登录状态
     *    ├─ 未登录 → Toast 提示 → 返回上一页
     *    └─ 已登录 → 继续初始化
     * 2. 获取 ViewModel
     * 3. 设置所有 UI 组件（Toolbar、英雄搜索、符文/装备选择、发布按钮）
     * 4. 观察 ViewModel 的 LiveData
     *
     * @param view Fragment 的根视图
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!isLoggedIn()) {
            Toast.makeText(requireContext(), R.string.please_login_first, Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        viewModel = new ViewModelProvider(this).get(PublishStrategyViewModel.class);

        setupViews();
        observeViewModel();
    }

    /**
     * 检查用户是否已登录 ── 通过 TokenStore 判断
     *
     * 登录条件：有 Token 且 Token 未过期
     *
     * @return true=已登录，false=未登录或 Token 已过期
     */
    private boolean isLoggedIn() {
        return tokenStore.hasToken() && !tokenStore.isTokenExpired();
    }

    /**
     * 导航到登录页 ── 未登录时自动返回上一页
     *
     * 这里使用 Navigation Controller 的 navigateUp() 返回上一页，
     * 宿主 Activity 应该在上一页提供登录入口。
     */
    private void navigateToLogin() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigateUp();
    }

    /**
     * 设置所有 UI 组件 ── 初始化表单的各个输入控件
     *
     * 初始化内容：
     * 1. Toolbar 返回按钮
     * 2. 英雄搜索框（AutoCompleteTextView）
     * 3. 符文选择按钮
     * 4. 装备选择按钮
     * 5. 发布按钮（读取标题和描述，调用 ViewModel 发布）
     */
    private void setupViews() {
        /**
         * Toolbar 返回按钮 ── 点击返回上一页
         *
         * popBackStack() 会将当前 Fragment 从回退栈中弹出，
         * 回到上一个 Fragment（通常是社区列表页）
         */
        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        setupHeroSearch();
        setupAugmentSelection();
        setupItemSelection();

        /**
         * 发布按钮 ── 点击后将表单数据提交到 ViewModel
         *
         * 执行步骤：
         * 1. 从输入框读取标题和描述文本
         * 2. 将文本设置到 ViewModel（触发表单验证）
         * 3. 调用 viewModel.publish() 提交发布请求
         *
         * ViewModel 的 publish() 方法会：
         * - 验证表单（英雄、标题、描述是否填写）
         * - 发送网络请求
         * - 成功后更新 publishedStrategy LiveData
         */
        binding.btnPublish.setOnClickListener(v -> {
            String title = binding.editTitle.getText().toString();
            String description = binding.editDescription.getText().toString();

            viewModel.setTitle(title);
            viewModel.setDescription(description);
            viewModel.publish();
        });
    }

    /**
     * 设置英雄搜索框 ── AutoCompleteTextView 实现模糊搜索
     *
     * ═══════════════════════════════════════════════════════════════════
     * AutoCompleteTextView 的工作原理
     * ═══════════════════════════════════════════════════════════════════
     *
     * AutoCompleteTextView 是一个带自动补全功能的输入框：
     * 1. 用户输入文字 → 系统在 ArrayAdapter 中搜索匹配项
     * 2. 匹配项以下拉列表形式显示
     * 3. 用户点击某个建议项 → 触发 onItemClick 回调
     *
     *   用户输入 "盖" → 下拉显示：
     *   ┌──────────────┐
     *   │ 盖伦          │
     *   └──────────────┘
     *
     * ArrayAdapter 负责管理下拉列表的数据源，
     * 我们在 setHeroOptions() 方法中会更新 ArrayAdapter 的数据。
     */
    private void setupHeroSearch() {
        ArrayAdapter<String> heroAdapter = new ArrayAdapter<>(requireContext(),
                com.google.android.material.R.layout.support_simple_spinner_dropdown_item);
        binding.autoHero.setAdapter(heroAdapter);

        /**
         * 英雄选择回调 ── 用户从下拉列表中选择一个英雄
         *
         * @param parent   AutoCompleteTextView
         * @param view     被点击的列表项 View
         * @param position 被点击项在列表中的位置
         * @param id       被点击项的行 ID
         *
         * 通过 position 从 heroOptions 中获取对应的 HeroOption，
         * 将其 id 设置到 ViewModel。
         */
        binding.autoHero.setOnItemClickListener((parent, view, position, id) -> {
            if (position < heroOptions.size()) {
                HeroOption selected = heroOptions.get(position);
                viewModel.setSelectedHeroId(selected.id);
            }
        });
    }

    /**
     * 设置符文选择按钮 ── 点击弹出多选对话框
     *
     * 用户点击 btnSelectAugments 后会调用 showAugmentPicker()，
     * 弹出一个 AlertDialog 多选对话框。
     */
    private void setupAugmentSelection() {
        binding.btnSelectAugments.setOnClickListener(v -> showAugmentPicker());
    }

    /**
     * 设置装备选择按钮 ── 点击弹出多选对话框
     *
     * 用户点击 btnSelectItems 后会调用 showItemPicker()，
     * 弹出一个 AlertDialog 多选对话框。
     */
    private void setupItemSelection() {
        binding.btnSelectItems.setOnClickListener(v -> showItemPicker());
    }

    /**
     * 显示符文多选对话框 ── 让用户选择关联的强化符文
     *
     * ═══════════════════════════════════════════════════════════════════
     * AlertDialog 多选对话框的工作原理
     * ═══════════════════════════════════════════════════════════════════
     *
     *   ┌──────────────────────────────────────┐
     *   │  选择强化符文                         │  ← 标题
     *   ├──────────────────────────────────────┤
     *   │  ☑ 电刑                              │  ← 已选中
     *   │  ☐ 黑暗收割                          │  ← 未选中
     *   │  ☑ 掠食者                            │  ← 已选中
     *   │  ☐ 召唤：艾黎                         │
     *   ├──────────────────────────────────────┤
     *   │              [ 确定 ]                │  ← 确认按钮
     *   └──────────────────────────────────────┘
     *
     * 执行步骤：
     * 1. 从 augmentOptions 提取所有符文名称作为对话框选项
     * 2. 从 ViewModel 获取当前已选中的符文 ID，标记为选中状态
     * 3. 用户勾选/取消某个选项时，立即通知 ViewModel 更新
     * 4. 点击"确定"关闭对话框
     *
     * 注意：setMultiChoiceItems 的回调是即时触发的，
     * 用户每次勾选/取消都会立即调用 viewModel.addAugment/removeAugment，
     * 而不是等点击"确定"后才批量更新。
     */
    private void showAugmentPicker() {
        String[] names = new String[augmentOptions.size()];
        boolean[] checked = new boolean[augmentOptions.size()];
        List<Long> selectedIds = viewModel.getSelectedAugmentIds().getValue();

        for (int i = 0; i < augmentOptions.size(); i++) {
            names[i] = augmentOptions.get(i).name;
            if (selectedIds != null) {
                checked[i] = selectedIds.contains(augmentOptions.get(i).id);
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_augments)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        viewModel.addAugment(augmentOptions.get(which).id);
                    } else {
                        viewModel.removeAugment(augmentOptions.get(which).id);
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * 显示装备多选对话框 ── 让用户选择推荐的出装物品
     *
     * 逻辑与 showAugmentPicker() 完全相同，只是数据源从 augmentOptions 换成 itemOptions，
     * ViewModel 方法从 addAugment/removeAugment 换成 addItem/removeItem。
     *
     * 参见 showAugmentPicker() 的详细说明。
     */
    private void showItemPicker() {
        String[] names = new String[itemOptions.size()];
        boolean[] checked = new boolean[itemOptions.size()];
        List<Long> selectedIds = viewModel.getSelectedItemIds().getValue();

        for (int i = 0; i < itemOptions.size(); i++) {
            names[i] = itemOptions.get(i).name;
            if (selectedIds != null) {
                checked[i] = selectedIds.contains(itemOptions.get(i).id);
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_items)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        viewModel.addItem(itemOptions.get(which).id);
                    } else {
                        viewModel.removeItem(itemOptions.get(which).id);
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * 观察 ViewModel 的 LiveData ── 建立数据绑定关系
     *
     * ═══════════════════════════════════════════════════════════════════
     * 观察的 LiveData 列表
     * ═══════════════════════════════════════════════════════════════════
     *
     * | LiveData            | 触发时机            | UI 更新                   |
     * |--------------------|--------------------|---------------------------|
     * | publishing         | 发布请求开始/结束    | btnPublish 禁用 + progressBar |
     * | error              | 发布失败/验证失败    | Toast 错误提示             |
     * | publishedStrategy  | 发布成功            | Toast 成功 + 返回上一页     |
     * | isFormValid        | 表单验证结果变化     | btnPublish 启用/禁用       |
     * | selectedHeroId     | 英雄选择变化        | autoHero 显示英雄名        |
     * | selectedAugmentIds | 符文选择变化        | chipGroupAugments 更新     |
     * | selectedItemIds    | 装备选择变化        | chipGroupItems 更新        |
     */
    private void observeViewModel() {
        /**
         * 观察发布状态 ── 控制发布按钮和进度条
         *
         * publishing=true  → 禁用发布按钮（防止重复提交）+ 显示进度条
         * publishing=false → 启用发布按钮 + 隐藏进度条
         */
        viewModel.getPublishing().observe(getViewLifecycleOwner(), publishing -> {
            binding.btnPublish.setEnabled(!publishing);
            binding.progressBar.setVisibility(publishing ? View.VISIBLE : View.GONE);
        });

        /**
         * 观察错误信息 ── 显示错误提示
         *
         * 常见错误场景：
         * - 表单验证失败（标题为空、描述不足10字等）
         * - 网络请求失败
         * - 服务器返回错误
         */
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * 观察发布成功 ── 攻略发布成功后返回上一页
         *
         * publishedStrategy 不为 null 时表示发布成功，
         * 显示成功提示并返回社区列表页。
         *
         * 为什么用 popBackStack() 而不是跳转到 ReleaseSuccessFragment？
         * 当前实现是直接返回上一页，未来可以改为跳转到成功页面。
         */
        viewModel.getPublishedStrategy().observe(getViewLifecycleOwner(), strategy -> {
            if (strategy != null) {
                Toast.makeText(requireContext(), R.string.publish_success, Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });

        /**
         * 观察表单验证状态 ── 控制发布按钮的启用/禁用
         *
         * isFormValid=true  → 启用发布按钮（表单填写完整）
         * isFormValid=false → 禁用发布按钮（必填项未填写）
         *
         * 这样可以防止用户在表单不完整时点击发布。
         */
        viewModel.getIsFormValid().observe(getViewLifecycleOwner(), valid -> {
            binding.btnPublish.setEnabled(valid != null && valid);
        });

        /**
         * 观察选中的英雄 ID ── 在搜索框中显示英雄名称
         *
         * 当 ViewModel 中的 selectedHeroId 变化时（比如从外部设置），
         * 在 AutoCompleteTextView 中显示对应的英雄名称。
         *
         * setText(name, false) 的第二个参数 false 表示不触发过滤，
         * 避免设置文本时弹出下拉列表。
         */
        viewModel.getSelectedHeroId().observe(getViewLifecycleOwner(), heroId -> {
            if (heroId != null) {
                binding.autoHero.setText(getHeroNameById(heroId), false);
            }
        });

        /**
         * 观察选中的符文 ID 列表 ── 更新 ChipGroup 中的标签
         *
         * 每次列表变化时，清空 ChipGroup 并重新创建所有 Chip。
         * 每个 Chip 显示符文名称，点击关闭图标可以移除。
         */
        viewModel.getSelectedAugmentIds().observe(getViewLifecycleOwner(), augmentIds -> {
            updateAugmentChips(augmentIds);
        });

        /**
         * 观察选中的装备 ID 列表 ── 更新 ChipGroup 中的标签
         *
         * 逻辑与符文 Chip 更新相同。
         */
        viewModel.getSelectedItemIds().observe(getViewLifecycleOwner(), itemIds -> {
            updateItemChips(itemIds);
        });
    }

    /**
     * 更新符文 Chip 列表 ── 根据选中的符文 ID 重新生成所有 Chip
     *
     * ═══════════════════════════════════════════════════════════════════
     * Chip 的创建过程
     * ═══════════════════════════════════════════════════════════════════
     *
     * 对每个选中的符文 ID：
     * 1. 创建一个新的 Chip 对象
     * 2. 设置文本为符文名称
     * 3. 启用关闭图标（setCloseIconVisible(true)）
     * 4. 设置关闭图标点击事件 → 调用 viewModel.removeAugment(id)
     * 5. 将 Chip 添加到 chipGroupAugments
     *
     *   chipGroupAugments 显示效果：
     *   ┌────────┐ ┌────────┐ ┌────────┐
     *   │ 电刑  ✕│ │掠食者 ✕│ │艾黎   ✕│
     *   └────────┘ └────────┘ └────────┘
     *
     * @param augmentIds 当前选中的符文 ID 列表
     */
    private void updateAugmentChips(List<Long> augmentIds) {
        binding.chipGroupAugments.removeAllViews();
        if (augmentIds == null || augmentIds.isEmpty()) {
            binding.chipGroupAugments.setVisibility(View.GONE);
            return;
        }
        binding.chipGroupAugments.setVisibility(View.VISIBLE);
        for (Long id : augmentIds) {
            Chip chip = new Chip(requireContext());
            chip.setText(getAugmentNameById(id));
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> viewModel.removeAugment(id));
            binding.chipGroupAugments.addView(chip);
        }
    }

    /**
     * 更新装备 Chip 列表 ── 根据选中的装备 ID 重新生成所有 Chip
     *
     * 逻辑与 updateAugmentChips() 完全相同，只是数据源从符文换成装备。
     * 参见 updateAugmentChips() 的详细说明。
     *
     * @param itemIds 当前选中的装备 ID 列表
     */
    private void updateItemChips(List<Long> itemIds) {
        binding.chipGroupItems.removeAllViews();
        if (itemIds == null || itemIds.isEmpty()) {
            binding.chipGroupItems.setVisibility(View.GONE);
            return;
        }
        binding.chipGroupItems.setVisibility(View.VISIBLE);
        for (Long id : itemIds) {
            Chip chip = new Chip(requireContext());
            chip.setText(getItemNameById(id));
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> viewModel.removeItem(id));
            binding.chipGroupItems.addView(chip);
        }
    }

    /**
     * 根据 ID 查找英雄名称 ── 在 heroOptions 列表中线性搜索
     *
     * @param id 英雄 ID
     * @return 英雄名称，找不到时返回"英雄 #ID"作为占位符
     */
    private String getHeroNameById(Long id) {
        for (HeroOption h : heroOptions) {
            if (h.id.equals(id)) return h.name;
        }
        return "英雄 #" + id;
    }

    /**
     * 根据 ID 查找符文名称 ── 在 augmentOptions 列表中线性搜索
     *
     * @param id 符文 ID
     * @return 符文名称，找不到时返回"符文 #ID"作为占位符
     */
    private String getAugmentNameById(Long id) {
        for (AugmentOption a : augmentOptions) {
            if (a.id.equals(id)) return a.name;
        }
        return "符文 #" + id;
    }

    /**
     * 根据 ID 查找装备名称 ── 在 itemOptions 列表中线性搜索
     *
     * @param id 装备 ID
     * @return 装备名称，找不到时返回"装备 #ID"作为占位符
     */
    private String getItemNameById(Long id) {
        for (ItemOption i : itemOptions) {
            if (i.id.equals(id)) return i.name;
        }
        return "装备 #" + id;
    }

    /**
     * 设置英雄选项列表 ── 由宿主调用，提供可选的英雄数据
     *
     * 设置后同时更新 AutoCompleteTextView 的 ArrayAdapter，
     * 使搜索下拉框能显示新的英雄列表。
     *
     * @param options 英雄选项列表，可为 null（会转为空列表）
     */
    public void setHeroOptions(List<HeroOption> options) {
        this.heroOptions = options != null ? options : new ArrayList<>();
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.autoHero.getAdapter();
        adapter.clear();
        for (HeroOption h : heroOptions) {
            adapter.add(h.name);
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * 设置符文选项列表 ── 由宿主调用，提供可选的符文数据
     *
     * @param options 符文选项列表，可为 null（会转为空列表）
     */
    public void setAugmentOptions(List<AugmentOption> options) {
        this.augmentOptions = options != null ? options : new ArrayList<>();
    }

    /**
     * 设置装备选项列表 ── 由宿主调用，提供可选的装备数据
     *
     * @param options 装备选项列表，可为 null（会转为空列表）
     */
    public void setItemOptions(List<ItemOption> options) {
        this.itemOptions = options != null ? options : new ArrayList<>();
    }

    /**
     * 设置选中的英雄 ID ── 由宿主调用，预选某个英雄
     *
     * 使用场景：从英雄详情页点击"写攻略"时，自动选中该英雄。
     *
     * @param heroId 要预选的英雄 ID
     */
    public void setSelectedHeroId(Long heroId) {
        viewModel.setSelectedHeroId(heroId);
    }

    /**
     * Fragment 视图销毁 ── 清理 binding 引用，防止内存泄漏
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 英雄选项数据类 ── 存储英雄的 ID 和名称
     *
     * 为什么用内部类而不是直接用 HeroUiModel？
     * 因为发布页只需要 ID 和名称两个字段，
     * 使用轻量级数据类避免依赖完整的 HeroUiModel。
     *
     * 字段说明：
     * - id：英雄的唯一标识，用于提交到服务器
     * - name：英雄的显示名称，用于搜索框和 Chip 展示
     */
    public static class HeroOption {
        public final Long id;
        public final String name;

        public HeroOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * 符文选项数据类 ── 存储符文的 ID 和名称
     *
     * 字段说明：
     * - id：符文的唯一标识，用于提交到服务器
     * - name：符文的显示名称，用于多选对话框和 Chip 展示
     */
    public static class AugmentOption {
        public final Long id;
        public final String name;

        public AugmentOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * 装备选项数据类 ── 存储装备的 ID 和名称
     *
     * 字段说明：
     * - id：装备的唯一标识，用于提交到服务器
     * - name：装备的显示名称，用于多选对话框和 Chip 展示
     */
    public static class ItemOption {
        public final Long id;
        public final String name;

        public ItemOption(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
