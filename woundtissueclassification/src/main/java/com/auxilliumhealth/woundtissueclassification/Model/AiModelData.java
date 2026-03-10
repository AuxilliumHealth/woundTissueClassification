package com.auxilliumhealth.woundtissueclassification.Model;

import java.util.List;

public class AiModelData {
    private float ErythemaPercent;
    private float MacerationPercent;
    private String Post_Processing;
    private Double WoundLength;
    private float NormalPercent;
    private String DisplayImagePath;
    private float CallusPercent;
    private float NormalTissuePercent;
    private Double WoundWidth;
    private double lens_focal_distance;
    private float SloughPercent;
    private Double WoundArea;
    private Double WoundDepth;
    private String WoundTissueOverlayImagePath;
    private String PeriWoundTissueOverlayImagePath;
    private String WoundPeriwoundOverlayImagePath;
    private String WoundMeasurementOverlayImagePath;
    private String ClockwiseMappingVisualizationImagePath;
    private String cropped_Grabcut_mask;

    private List<Double> area_coeffs;
    private float GranulationPercent;
    private String CroppedImagePath;
    private String WoundScore;
    private float EscharPercent;
    private List<Double> pixel_per_unit_coeffs;

    // Getters and setters

    public String getWoundScore() {
        return WoundScore;
    }

    public void setWoundScore(String woundScore) {
        WoundScore = woundScore;
    }

    public float getErythemaPercent() {
        return ErythemaPercent;
    }

    public void setErythemaPercent(float erythemaPercent) {
        ErythemaPercent = erythemaPercent;
    }

    public float getMacerationPercent() {
        return MacerationPercent;
    }

    public void setMacerationPercent(float macerationPercent) {
        MacerationPercent = macerationPercent;
    }

    public String getPost_Processing() {
        return Post_Processing;
    }

    public void setPost_Processing(String post_Processing) {
        Post_Processing = post_Processing;
    }

    public Double getWoundLength() {
        return WoundLength;
    }

    public void setWoundLength(Double woundLength) {
        WoundLength = woundLength;
    }

    public float getNormalPercent() {
        return NormalPercent;
    }

    public void setNormalPercent(float normalPercent) {
        NormalPercent = normalPercent;
    }

    public String getDisplayImagePath() {
        return DisplayImagePath;
    }

    public void setDisplayImagePath(String displayImagePath) {
        DisplayImagePath = displayImagePath;
    }

    public float getCallusPercent() {
        return CallusPercent;
    }

    public void setCallusPercent(float callusPercent) {
        CallusPercent = callusPercent;
    }

    public float getNormalTissuePercent() {
        return NormalTissuePercent;
    }

    public void setNormalTissuePercent(float normalTissuePercent) {
        NormalTissuePercent = normalTissuePercent;
    }

    public Double getWoundWidth() {
        return WoundWidth;
    }

    public void setWoundWidth(Double woundWidth) {
        WoundWidth = woundWidth;
    }

    public double getLens_focal_distance() {
        return lens_focal_distance;
    }

    public void setLens_focal_distance(double lens_focal_distance) {
        this.lens_focal_distance = lens_focal_distance;
    }

    public float getSloughPercent() {
        return SloughPercent;
    }

    public void setSloughPercent(float sloughPercent) {
        SloughPercent = sloughPercent;
    }

    public Double getWoundArea() {
        return WoundArea;
    }

    public void setWoundArea(Double woundArea) {
        WoundArea = woundArea;
    }

    public Double getWoundDepth() {
        return WoundDepth;
    }

    public void setWoundDepth(Double woundDepth) {
        WoundDepth = woundDepth;
    }

    public String getWoundTissueOverlayImagePath() {
        return WoundTissueOverlayImagePath;
    }

    public void setWoundTissueOverlayImagePath(String woundTissueOverlayImagePath) {
        WoundTissueOverlayImagePath = woundTissueOverlayImagePath;
    }

    public String getPeriWoundTissueOverlayImagePath() {
        return PeriWoundTissueOverlayImagePath;
    }

    public void setPeriWoundTissueOverlayImagePath(String periWoundTissueOverlayImagePath) {
        PeriWoundTissueOverlayImagePath = periWoundTissueOverlayImagePath;
    }

    public String getWoundPeriwoundOverlayImagePath() {
        return WoundPeriwoundOverlayImagePath;
    }

    public void setWoundPeriwoundOverlayImagePath(String woundPeriwoundOverlayImagePath) {
        WoundPeriwoundOverlayImagePath = woundPeriwoundOverlayImagePath;
    }

    public List<Double> getArea_coeffs() {
        return area_coeffs;
    }

    public void setArea_coeffs(List<Double> area_coeffs) {
        this.area_coeffs = area_coeffs;
    }

    public float getGranulationPercent() {
        return GranulationPercent;
    }

    public void setGranulationPercent(float granulationPercent) {
        GranulationPercent = granulationPercent;
    }

    public String getCroppedImagePath() {
        return CroppedImagePath;
    }

    public void setCroppedImagePath(String croppedImagePath) {
        CroppedImagePath = croppedImagePath;
    }

    public float getEscharPercent() {
        return EscharPercent;
    }

    public void setEscharPercent(float escharPercent) {
        EscharPercent = escharPercent;
    }

    public List<Double> getPixel_per_unit_coeffs() {
        return pixel_per_unit_coeffs;
    }

    public void setPixel_per_unit_coeffs(List<Double> pixel_per_unit_coeffs) {
        this.pixel_per_unit_coeffs = pixel_per_unit_coeffs;
    }

    public String getWoundMeasurementOverlayImagePath() {
        return WoundMeasurementOverlayImagePath;
    }

    public void setWoundMeasurementOverlayImagePath(String woundMeasurementOverlayImagePath) {
        WoundMeasurementOverlayImagePath = woundMeasurementOverlayImagePath;
    }
    public String getClockwiseMappingVisualizationImagePath() {
        return ClockwiseMappingVisualizationImagePath;
    }

    public void setClockwiseMappingVisualizationImagePath(String clockwiseMappingVisualizationImagePath) {
        ClockwiseMappingVisualizationImagePath = clockwiseMappingVisualizationImagePath;
    }

    public String getCropped_Grabcut_mask() {
        return cropped_Grabcut_mask;
    }

    public void setCropped_Grabcut_mask(String cropped_Grabcut_mask) {
        this.cropped_Grabcut_mask = cropped_Grabcut_mask;
    }
}