/**
 * ─────────────────────────────────────────────────────────────────────────────────────
 * Created & Developed by:
 * Aravindhan (Full Stack Engineer)
 * Auxilliumhealth LLC
 * GitHub: https://github.com/AravindhanDeveloper
 * ─────────────────────────────────────────────────────────────────────────────────────
 * Copyright (c) 2024. All rights reserved.
 * ─────────────────────────────────────────────────────────────────────────────────────
 */
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

    @SerializedName("manualVerification")
    @Expose
    private Boolean manualVerification;

    public EditWoundMeasurementsRequest(String userId, String sessionId, Double woundArea, Double woundLength, Double woundWidth, Double woundDepth, Boolean manualVerification) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.woundArea = woundArea;
        this.woundLength = woundLength;
        this.woundWidth = woundWidth;
        this.woundDepth = woundDepth;
        this.manualVerification = manualVerification;
    }

    public Boolean getManualVerification() {
        return manualVerification;
    }

    public void setManualVerification(Boolean manualVerification) {
        this.manualVerification = manualVerification;
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
