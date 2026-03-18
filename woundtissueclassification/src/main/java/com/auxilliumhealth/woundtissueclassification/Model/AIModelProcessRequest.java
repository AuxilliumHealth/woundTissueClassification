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

import java.util.List;

public class AIModelProcessRequest {
    @SerializedName(value = "userId", alternate = {"user_id", "patientId"})
    private String userId;

    @SerializedName("img_Path")
    private String imgPath;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName(value = "woundId", alternate = {"wound_id", "woundID"})
    private String woundId;

    @SerializedName("lens_focal_distance")
    private double lensFocalDistance;

    @SerializedName("pixel_per_unit_coeffs")
    private List<Double> pixelPerUnitCoeffs;

    @SerializedName("area_coeffs")
    private List<Double> areaCoeffs;

    @SerializedName("lasso_coordinates")
    private List<Double> lassoCoordinates;


    @SerializedName("left_image")
    private String leftImage;

    @SerializedName("right_image")
    private String rightImage;

    @SerializedName("baseline_cm")
    private double baselineCm;
    @SerializedName("body_part")
    private String bodyPart;
    @SerializedName("image_rotation_deg")
    private String imageRotationDeg;
    @SerializedName("headDirection")
    private String headDirection;

    public AIModelProcessRequest(String userId, String imgPath, String sessionId, String woundId,
                               double lensFocalDistance, List<Double> pixelPerUnitCoeffs,
                               List<Double> areaCoeffs, List<Double> lassoCoordinates, String bodyPart, 
                               String imageRotationDeg, String headDirection, String leftImage, 
                               String rightImage, double baselineCm) {
        this.userId = userId;
        this.imgPath = imgPath;
        this.sessionId = sessionId;
        this.woundId = woundId;
        this.lensFocalDistance = lensFocalDistance;
        this.pixelPerUnitCoeffs = pixelPerUnitCoeffs;
        this.areaCoeffs = areaCoeffs;
        this.lassoCoordinates = lassoCoordinates;
        this.bodyPart = bodyPart;
        this.imageRotationDeg = imageRotationDeg;
        this.headDirection = headDirection;
        this.leftImage = leftImage;
        this.rightImage = rightImage;
        this.baselineCm = baselineCm;
    }
}
