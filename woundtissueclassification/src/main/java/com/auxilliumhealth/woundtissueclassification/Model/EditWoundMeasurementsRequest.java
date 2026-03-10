package com.auxilliumhealth.woundtissueclassification.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class EditWoundMeasurementsRequest {

    @SerializedName("userId")
    @Expose
    private String userId;

    @SerializedName("sessionId")
    @Expose
    private String sessionId;

    @SerializedName("WoundArea")
    @Expose
    private Double woundArea;

    @SerializedName("WoundLength")
    @Expose
    private Double woundLength;

    @SerializedName("WoundWidth")
    @Expose
    private Double woundWidth;

    @SerializedName("WoundDepth")
    @Expose
    private Double woundDepth;

    public EditWoundMeasurementsRequest(String userId, String sessionId, Double woundArea, Double woundLength, Double woundWidth, Double woundDepth) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.woundArea = woundArea;
        this.woundLength = woundLength;
        this.woundWidth = woundWidth;
        this.woundDepth = woundDepth;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Double getWoundArea() {
        return woundArea;
    }

    public void setWoundArea(Double woundArea) {
        this.woundArea = woundArea;
    }

    public Double getWoundLength() {
        return woundLength;
    }

    public void setWoundLength(Double woundLength) {
        this.woundLength = woundLength;
    }

    public Double getWoundWidth() {
        return woundWidth;
    }

    public void setWoundWidth(Double woundWidth) {
        this.woundWidth = woundWidth;
    }

    public Double getWoundDepth() {
        return woundDepth;
    }

    public void setWoundDepth(Double woundDepth) {
        this.woundDepth = woundDepth;
    }
}
