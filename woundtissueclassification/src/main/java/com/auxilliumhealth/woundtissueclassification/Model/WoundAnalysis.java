package com.auxilliumhealth.woundtissueclassification.Model;

public class WoundAnalysis {
    private AiModelData aiModelData;
    private String imageUrl;
    private String sessionId;
    private String DateTime;
    private String userId;
    private String SK;
    private String woundId;

    // Getters and setters
    public AiModelData getAiModelData() {
        return aiModelData;
    }

    public void setAiModelData(AiModelData aiModelData) {
        this.aiModelData = aiModelData;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDateTime() {
        return DateTime;
    }

    public void setDateTime(String dateTime) {
        this.DateTime = dateTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSK() {
        return SK;
    }

    public void setSK(String SK) {
        this.SK = SK;
    }

    public String getWoundId() {
        return woundId;
    }

    public void setWoundId(String woundId) {
        this.woundId = woundId;
    }
}

