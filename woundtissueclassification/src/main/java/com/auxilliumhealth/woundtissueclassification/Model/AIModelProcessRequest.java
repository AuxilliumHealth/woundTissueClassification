package com.auxilliumhealth.woundtissueclassification.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AIModelProcessRequest {
    @SerializedName("userId")
    private String userId;

    @SerializedName("img_Path")
    private String imgPath;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("woundId")
    private String woundId;

    @SerializedName("lens_focal_distance")
    private double lensFocalDistance;

    @SerializedName("pixel_per_unit_coeffs")
    private List<Double> pixelPerUnitCoeffs;

    @SerializedName("area_coeffs")
    private List<Double> areaCoeffs;

    public AIModelProcessRequest(String userId, String imgPath, String sessionId, String woundId,
                               double lensFocalDistance, List<Double> pixelPerUnitCoeffs,
                               List<Double> areaCoeffs) {
        this.userId = userId;
        this.imgPath = imgPath;
        this.sessionId = sessionId;
        this.woundId = woundId;
        this.lensFocalDistance = lensFocalDistance;
        this.pixelPerUnitCoeffs = pixelPerUnitCoeffs;
        this.areaCoeffs = areaCoeffs;
    }
}
