package com.aram.mayhem.feature.augment;

/**
 * 符文选中回调接口 ── Fragment 之间的导航通信桥梁
 *
 * ═══════════════════════════════════════════════════════════════════
 * 一、这个接口是干什么的？
 * ═══════════════════════════════════════════════════════════════════
 *
 * 当用户在符文列表页点击某个符文卡片时，需要弹出符文详情底部弹窗。
 * 但 Fragment 不能直接操作其他 Fragment/BottomSheet，必须通过宿主 Activity 来中转。
 * OnAugmentSelectedListener 就是这个"中转协议"。
 *
 * ═══════════════════════════════════════════════════════════════════
 * 二、通信流程
 * ═══════════════════════════════════════════════════════════════════
 *
 *   AugmentListFragment          MainActivity              AugmentDetailBottomSheet
 *   ┌──────────────┐            ┌──────────────┐          ┌──────────────────────┐
 *   │ 用户点击卡片  │            │  实现         │          │                      │
 *   │ augmentId=5  │ ─────────→ │  接口         │ ───────→ │ 接收 augmentId=5     │
 *   │              │  调用       │  创建         │  参数     │ 加载符文详情         │
 *   │ onAugmentSelected(5)      │  BottomSheet  │          │                      │
 *   └──────────────┘            └──────────────┘          └──────────────────────┘
 *
 * ═══════════════════════════════════════════════════════════════════
 * 三、与 OnHeroSelectedListener 的区别
 * ═══════════════════════════════════════════════════════════════════
 *
 * - OnHeroSelectedListener：列表 → 详情页（Fragment 导航）
 * - OnAugmentSelectedListener：列表 → 底部弹窗（BottomSheet 弹出）
 * - 英雄详情是全屏 Fragment，符文详情是底部弹窗，交互方式不同
 *
 * @see com.aram.mayhem.feature.augment.AugmentListFragment
 * @see com.aram.mayhem.feature.augment.AugmentDetailBottomSheet
 */
public interface OnAugmentSelectedListener {
    /**
     * 符文被选中时的回调方法
     *
     * 当用户在符文列表中点击某个符文卡片时触发。
     * 实现者（通常是 MainActivity）负责创建并显示 AugmentDetailBottomSheet。
     *
     * @param augmentId 被选中符文的唯一标识符（数据库主键）
     *                  例如：5 表示 ID 为 5 的符文
     */
    void onAugmentSelected(long augmentId);
}
