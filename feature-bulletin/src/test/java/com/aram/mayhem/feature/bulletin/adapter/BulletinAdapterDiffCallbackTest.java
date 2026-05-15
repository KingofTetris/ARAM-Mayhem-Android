package com.aram.mayhem.feature.bulletin.adapter;

import com.aram.mayhem.ui.model.BulletinUiModel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BulletinAdapterDiffCallbackTest {

    @Test
    @DisplayName("相同 ID 的 BulletinUiModel areItemsTheSame 返回 true")
    void areItemsTheSame_sameId_returnsTrue() {
        BulletinAdapter.BulletinDiffCallback callback = new BulletinAdapter.BulletinDiffCallback();
        BulletinUiModel item1 = new BulletinUiModel(1L, "version", "标题1", "内容", null, false, null, null);
        BulletinUiModel item2 = new BulletinUiModel(1L, "event", "标题2", "内容", null, true, null, null);

        assertTrue(callback.areItemsTheSame(item1, item2));
    }

    @Test
    @DisplayName("不同 ID 的 BulletinUiModel areItemsTheSame 返回 false")
    void areItemsTheSame_differentId_returnsFalse() {
        BulletinAdapter.BulletinDiffCallback callback = new BulletinAdapter.BulletinDiffCallback();
        BulletinUiModel item1 = new BulletinUiModel(1L, "version", "标题", "内容", null, false, null, null);
        BulletinUiModel item2 = new BulletinUiModel(2L, "version", "标题", "内容", null, false, null, null);

        assertFalse(callback.areItemsTheSame(item1, item2));
    }

    @Test
    @DisplayName("相同 ID 和标题且置顶状态相同 areContentsTheSame 返回 true")
    void areContentsTheSame_sameContent_returnsTrue() {
        BulletinAdapter.BulletinDiffCallback callback = new BulletinAdapter.BulletinDiffCallback();
        BulletinUiModel item1 = new BulletinUiModel(1L, "version", "标题", "内容1", null, false, null, null);
        BulletinUiModel item2 = new BulletinUiModel(1L, "event", "标题", "内容2", null, false, null, null);

        assertTrue(callback.areContentsTheSame(item1, item2));
    }

    @Test
    @DisplayName("置顶状态不同 areContentsTheSame 返回 false")
    void areContentsTheSame_differentPinned_returnsFalse() {
        BulletinAdapter.BulletinDiffCallback callback = new BulletinAdapter.BulletinDiffCallback();
        BulletinUiModel item1 = new BulletinUiModel(1L, "version", "标题", "内容", null, false, null, null);
        BulletinUiModel item2 = new BulletinUiModel(1L, "version", "标题", "内容", null, true, null, null);

        assertFalse(callback.areContentsTheSame(item1, item2));
    }

    @Test
    @DisplayName("标题不同 areContentsTheSame 返回 false")
    void areContentsTheSame_differentTitle_returnsFalse() {
        BulletinAdapter.BulletinDiffCallback callback = new BulletinAdapter.BulletinDiffCallback();
        BulletinUiModel item1 = new BulletinUiModel(1L, "version", "标题1", "内容", null, false, null, null);
        BulletinUiModel item2 = new BulletinUiModel(1L, "version", "标题2", "内容", null, false, null, null);

        assertFalse(callback.areContentsTheSame(item1, item2));
    }
}
