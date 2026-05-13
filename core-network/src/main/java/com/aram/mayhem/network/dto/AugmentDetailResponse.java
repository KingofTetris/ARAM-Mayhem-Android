package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;

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