package com.aram.mayhem.feature.augment;

/** 符文选中回调接口，用于 AugmentListFragment → AugmentDetailBottomSheet 的导航通信 */
public interface OnAugmentSelectedListener {
    void onAugmentSelected(long augmentId);
}