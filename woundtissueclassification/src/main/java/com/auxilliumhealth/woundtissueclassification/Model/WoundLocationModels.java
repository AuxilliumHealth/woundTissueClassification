package com.auxilliumhealth.woundtissueclassification.Model;

public class WoundLocationModels {
    private String woundLocation;
    private String woundId;
    private String SK;
    private String description;
    private String PK;
    private String imageKey;
    private String sessionId;
    private String DateTime;
    private String signedImageUrl;
    AiResultModel aiModelData;

    public String getWoundLocation() {
        return woundLocation;
    }

    public void setWoundLocation(String woundLocation) {
        this.woundLocation = woundLocation;
    }

    public String getWoundId() {
        return woundId;
    }

    public void setWoundId(String woundId) {
        this.woundId = woundId;
    }

    public String getSK() {
        return SK;
    }

    public void setSK(String SK) {
        this.SK = SK;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPK() {
        return PK;
    }

    public void setPK(String PK) {
        this.PK = PK;
    }

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
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
        DateTime = dateTime;
    }

    public String getSignedImageUrl() {
        return signedImageUrl;
    }

    public void setSignedImageUrl(String signedImageUrl) {
        this.signedImageUrl = signedImageUrl;
    }

    public AiResultModel getAiModelData() {
        return aiModelData;
    }

    public void setAiModelData(AiResultModel aiModelData) {
        this.aiModelData = aiModelData;
    }
}
