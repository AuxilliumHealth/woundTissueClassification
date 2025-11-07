package com.auxilliumhealth.woundtissueclassification.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LatestSessionModel {
    @SerializedName("data")
    @Expose
    private List<Datum> data;

    public List<Datum> getData() {
        return data;
    }

    public void setData(List<Datum> data) {
        this.data = data;
    }


public class Datum {

    @SerializedName("woundId")
    @Expose
    private String woundId;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("woundLocation")
    @Expose
    private String woundLocation;
    @SerializedName("sessionId")
    @Expose
    private String sessionId;
    @SerializedName("dateTime")
    @Expose
    private String dateTime;
    @SerializedName("imageUrl")
    @Expose
    private String imageUrl;

    public String getWoundId() {
        return woundId;
    }

    public void setWoundId(String woundId) {
        this.woundId = woundId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWoundLocation() {
        return woundLocation;
    }

    public void setWoundLocation(String woundLocation) {
        this.woundLocation = woundLocation;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
}