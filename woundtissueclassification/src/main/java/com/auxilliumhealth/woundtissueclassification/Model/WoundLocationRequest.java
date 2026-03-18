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

import com.google.gson.annotations.SerializedName;

public class WoundLocationRequest {
    @SerializedName(value = "userId", alternate = {"user_id", "patientId"})
    private String userId;

    @SerializedName("description")
    private String description;

    @SerializedName(value = "woundId", alternate = {"wound_id"})
    private String woundId;

    @SerializedName("woundLocation")
    private String woundLocation;

    public WoundLocationRequest(String userId, String description, String woundId, String woundLocation) {
        this.userId = userId;
        this.description = description;
        this.woundId = woundId;
        this.woundLocation = woundLocation;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWoundId() {
        return woundId;
    }

    public void setWoundId(String woundId) {
        this.woundId = woundId;
    }

    public String getWoundLocation() {
        return woundLocation;
    }

    public void setWoundLocation(String woundLocation) {
        this.woundLocation = woundLocation;
    }
}
