package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

/** 符文详情响应模型（继承 AugmentResponse，与后端 AugmentVO 对应） */
public class AugmentDetailResponse extends AugmentResponse {

    @SerializedName("description")
    private String description;

    @SerializedName("synergySet2")
    private String synergySet2;

    @SerializedName("synergySet3")
    private String synergySet3;

    @SerializedName("isTrap")
    private Boolean isTrap;

    public String getDescription() {
        return description;
    }

    public String getSynergySet2() {
        return synergySet2;
    }

    public String getSynergySet3() {
        return synergySet3;
    }

    public Boolean getIsTrap() {
        return isTrap;
    }
}