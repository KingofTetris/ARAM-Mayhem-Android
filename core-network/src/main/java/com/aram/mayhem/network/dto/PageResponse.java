package com.aram.mayhem.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PageResponse<T> {

    @SerializedName("total")
    private long total;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    @SerializedName("records")
    private List<T> records;

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public List<T> getRecords() {
        return records;
    }
}
