package com.aram.mayhem.feature.hero;

/** 英雄选中回调接口，用于 HeroListFragment → MainActivity → HeroDetailFragment 的导航通信 */
public interface OnHeroSelectedListener {
    void onHeroSelected(long heroId);
}
