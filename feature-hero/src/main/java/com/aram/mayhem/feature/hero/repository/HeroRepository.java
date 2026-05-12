package com.aram.mayhem.feature.hero.repository;

import com.aram.mayhem.data.local.dao.HeroDao;
import com.aram.mayhem.network.api.HeroApi;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HeroRepository {

    private final HeroApi heroApi;
    private final HeroDao heroDao;

    @Inject
    public HeroRepository(HeroApi heroApi, HeroDao heroDao) {
        this.heroApi = heroApi;
        this.heroDao = heroDao;
    }

    public HeroApi getHeroApi() {
        return heroApi;
    }

    public HeroDao getHeroDao() {
        return heroDao;
    }
}
