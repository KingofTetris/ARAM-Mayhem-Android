package com.aram.mayhem.ui.model;

public class AugmentUiModel {

    private final long id;
    private final String name;
    private final String description;
    private final String quality;
    private final String synergySet;
    private final String iconUrl;
    private final boolean isTrap;

    public AugmentUiModel(long id, String name, String description,
                          String quality, String synergySet,
                          String iconUrl, boolean isTrap) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.quality = quality;
        this.synergySet = synergySet;
        this.iconUrl = iconUrl;
        this.isTrap = isTrap;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getQuality() { return quality; }
    public String getSynergySet() { return synergySet; }
    public String getIconUrl() { return iconUrl; }
    public boolean isTrap() { return isTrap; }

    public int getQualityColorRes() {
        if (quality == null) return com.aram.mayhem.ui.R.color.quality_silver;
        switch (quality) {
            case "PRISMATIC":
                return com.aram.mayhem.ui.R.color.quality_prismatic;
            case "GOLD":
                return com.aram.mayhem.ui.R.color.quality_gold;
            default:
                return com.aram.mayhem.ui.R.color.quality_silver;
        }
    }

    public String getSynergyDisplay() {
        return synergySet != null ? "套装：" + synergySet : "";
    }
}
