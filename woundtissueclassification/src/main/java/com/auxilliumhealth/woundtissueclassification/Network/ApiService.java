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
package com.auxilliumhealth.woundtissueclassification.Network;

import com.auxilliumhealth.woundtissueclassification.Model.AIModelProcessRequest;
import com.auxilliumhealth.woundtissueclassification.Model.EditWoundMeasurementsRequest;
import com.auxilliumhealth.woundtissueclassification.Model.Question;
import com.auxilliumhealth.woundtissueclassification.Model.SubmitAnswersRequest;
import com.auxilliumhealth.woundtissueclassification.Model.WoundDetailsModel;
import com.auxilliumhealth.woundtissueclassification.Model.WoundListModel;
import com.auxilliumhealth.woundtissueclassification.Model.WoundLocationRequest;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;

public interface ApiService {
    @Multipart
    @POST("calibrate")
    Call<ResponseBody> getCalibrate(@Part("user_id") RequestBody userId, @Part("coin_type") RequestBody coinType, @Part("lense_focus_distances") RequestBody lenseFocusDistances, @Part("pixel_counts") RequestBody pixelCounts);

    @Multipart
    @POST("v1/data/uploadCaptureImage")
    Call<ResponseBody> uploadFile(@Part("userId") RequestBody userId, @Part("woundId") RequestBody woundId, @Part("sessionId") RequestBody sessionId, @Part MultipartBody.Part file);

    @POST("v1/data/updateWoundLocation")
    Call<ResponseBody> updateWoundLocation(@Body WoundLocationRequest request);
    
    @GET("v1/data/getSymptomsQuestions")
    Call<ResponseBody> getSymptomQuestions();
    
    @POST("v1/data/calculateWoundScore")
    Call<ResponseBody> submitAnswers(@Body SubmitAnswersRequest request);
    @POST("v1/data/aiModelprocessImage")
    Call<ResponseBody> processAIModelImage(@Body AIModelProcessRequest request);
    @POST("/v1/data/getWoundList")
    Call<ResponseBody> getWoundList(@Body WoundListModel request);
    @POST("/v1/data/getWoundDetails")
    Call<ResponseBody> getWoundDetails(@Body WoundDetailsModel request);
    @POST("/v1/data/getLatestSession")
    Call<ResponseBody> getLatestSession(@Body WoundListModel request);

    @Multipart
    @PUT("v1/data/editWoundMeasurements")
    Call<ResponseBody> editWoundMeasurements(
            @PartMap Map<String, RequestBody> parts,
            @Part MultipartBody.Part file   // nullable — omit by passing null
    );
}
