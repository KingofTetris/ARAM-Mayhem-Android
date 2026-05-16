package com.aram.mayhem.ui.model;

import com.aram.mayhem.common.Tier;

/**
 * 英雄 UI 模型
 *
 * 数据流向：HeroEntity/Response → HeroUiModel → HeroCardAdapter → 列表项视图
 * 用途：英雄列表卡片展示
 */
public class HeroUiModel {

    private final long id;
    private final String nameZh;
    private final String nameEn;
    private final String title;
    private final String role;
    private final Tier tier;
    private final double winRate;
    private final double pickRate;
    private final String avatarUrl;
    private final boolean isTrap;

    public HeroUiModel(long id, String nameZh, String nameEn, String title,
                       String role, Tier tier, double winRate, double pickRate,
                       String avatarUrl) {
        this.id = id;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.title = title;
        this.role = role;
        this.tier = tier;
        this.winRate = winRate;
        this.pickRate = pickRate;
        this.avatarUrl = avatarUrl;
        this.isTrap = false;
    }

    public long getId() { return id; }
    public String getNameZh() { return nameZh; }
    public String getNameEn() { return nameEn; }
    public String getTitle() { return title; }
    public String getRole() { return role; }
    public Tier getTier() { return tier; }
    public double getWinRate() { return winRate; }
    public double getPickRate() { return pickRate; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isTrap() { return isTrap; }

    public String getWinRateDisplay() {
        return String.format("胜率 %.1f%%", winRate);
    }
}
