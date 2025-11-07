package com.auxilliumhealth.woundtissueclassification.Model;

public class AnalysisImage {
    private String title;
    private String imageUrl;

    public AnalysisImage(String title, String imageUrl) {
        this.title = title;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
}
