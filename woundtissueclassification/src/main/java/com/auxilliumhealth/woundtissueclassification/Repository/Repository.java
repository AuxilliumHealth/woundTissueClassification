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
package com.auxilliumhealth.woundtissueclassification.Repository;

import android.content.Context;

import com.auxilliumhealth.woundtissueclassification.Model.AIModelProcessRequest;
import com.auxilliumhealth.woundtissueclassification.Model.ErrorResponseModel;
import com.auxilliumhealth.woundtissueclassification.Model.SubmitAnswersRequest;
import com.auxilliumhealth.woundtissueclassification.Model.WoundDetailsModel;
import com.auxilliumhealth.woundtissueclassification.Model.WoundListModel;
import com.auxilliumhealth.woundtissueclassification.Model.WoundLocationRequest;
import com.auxilliumhealth.woundtissueclassification.Model.EditWoundMeasurementsRequest;
import com.auxilliumhealth.woundtissueclassification.Network.ApiClient;
import com.auxilliumhealth.woundtissueclassification.Network.ApiService;
import com.auxilliumhealth.woundtissueclassification.Utils.Constants;
import com.auxilliumhealth.woundtissueclassification.Utils.ProgressRequestBody;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Repository {
    private Repository.GetCommonAPIDataSuccessCallBack getCommonAPIDataSuccessCallBack;
    Context context;

    public Repository(Context context) {
        this.context = context;
    }

    public void getCalibrate(String userId, String coinType, String lenseFocusDistances, String pixelCounts) {

        RequestBody userIdRequestBody = RequestBody.create(MediaType.parse("text/plain"), userId);
        RequestBody coinTypeRequestBody = RequestBody.create(MediaType.parse("text/plain"), coinType);
        RequestBody lenseFocusDistancesRequestBody = RequestBody.create(MediaType.parse("text/plain"), lenseFocusDistances);
        RequestBody pixelCountsRequestBody = RequestBody.create(MediaType.parse("text/plain"), pixelCounts);
        Retrofit retrofit = ApiClient.getInstance(true, "");
        ApiService authAPIServices = retrofit.create(ApiService.class);

        Call<ResponseBody> call = authAPIServices.getCalibrate(userIdRequestBody, coinTypeRequestBody, lenseFocusDistancesRequestBody, pixelCountsRequestBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                ResponseBody apiArrayResponse = response.body();
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(apiArrayResponse);
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";

                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(errorBody.string(), ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }

    public void uploadImage(File file, String userId, String woundId, String sessionId, String token, ProgressRequestBody.UploadCallbacks progressListener) {
        RequestBody sessionIdRequestBody = RequestBody.create(MediaType.parse("text/plain"), sessionId);
        RequestBody woundIdRequestBody = RequestBody.create(MediaType.parse("text/plain"), woundId);
        RequestBody userIdRequestBody = RequestBody.create(MediaType.parse("text/plain"), userId);

        // Create ProgressRequestBody with the listener
        ProgressRequestBody fileBody = new ProgressRequestBody(file, "*/*", new ProgressRequestBody.UploadCallbacks() {
            @Override
            public void onProgressUpdate(int percentage) {
                // Forward progress to the main callback
                if (getCommonAPIDataSuccessCallBack != null) {
                    getCommonAPIDataSuccessCallBack.onProgressUpdate(percentage);
                }
                // Also call the original progress listener if provided
                if (progressListener != null) {
                    progressListener.onProgressUpdate(percentage);
                }
            }

            @Override
            public void onError() {
                if (progressListener != null) {
                    progressListener.onError();
                }
            }

            @Override
            public void onFinish() {
                if (progressListener != null) {
                    progressListener.onFinish();
                }
            }
        });

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService authAPIServices = retrofit.create(ApiService.class);

        Call<ResponseBody> call = authAPIServices.uploadFile(userIdRequestBody, woundIdRequestBody, sessionIdRequestBody, filePart);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    String message = "";
                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(response.errorBody().string(), ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }

    public void updateWoundLocation(String userId, String description, String woundId, String woundLocation, String token) {
        WoundLocationRequest request = new WoundLocationRequest(userId, description, woundId, woundLocation);

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.updateWoundLocation(request);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";
                    try {
                        if (errorBody != null) {
                            message = errorBody.string();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(t.getMessage());
            }
        });
    }

    public void submitAnswers(SubmitAnswersRequest request, String token) {

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.submitAnswers(request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";

                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(errorBody.string(), ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }

    public void getSymptomQuestions(String token) {

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.getSymptomQuestions();

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";
                    try {
                        if (errorBody != null) {
                            message = errorBody.string();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(t.getMessage());
            }
        });
    }
    public void getWoundList(WoundListModel request, String token, GetCommonAPIDataSuccessCallBack callback) {
        this.getCommonAPIDataSuccessCallBack = callback;

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.getWoundList(request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";

                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(errorBody.string(), ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }

    public void getLatestSession(WoundListModel request, String token, GetCommonAPIDataSuccessCallBack callback) {
        this.getCommonAPIDataSuccessCallBack = callback;

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.getLatestSession(request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";

                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(errorBody.string(), ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }

    public void getWoundDetails(WoundDetailsModel request, String token, GetCommonAPIDataSuccessCallBack callback) {
        this.getCommonAPIDataSuccessCallBack = callback;

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.getWoundDetails(request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";

                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(errorBody.string(), ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }

    public void deleteSession(WoundDetailsModel request, String token, GetCommonAPIDataSuccessCallBack callback) {
        this.getCommonAPIDataSuccessCallBack = callback;

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.deleteSession(request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";

                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(errorBody.string(), ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }

    public void processAIModelImage(AIModelProcessRequest request, String token, GetCommonAPIDataSuccessCallBack callback) {
        this.getCommonAPIDataSuccessCallBack = callback;

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.processAIModelImage(request);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";

                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(errorBody.string(), ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }

    /**
     * PUT /v1/data/editWoundMeasurements   (multipart/form-data)
     *
     * @param request     Request object containing userId, sessionId and measurements
     * @param imagePath   absolute path to the clockwise-axis image, or null if none
     * @param token       Bearer token
     * @param callback    result callback
     */
    public void editWoundMeasurements(
            EditWoundMeasurementsRequest request,
            String imagePath,
            String token,
            GetCommonAPIDataSuccessCallBack callback) {

        this.getCommonAPIDataSuccessCallBack = callback;

        MediaType text = MediaType.parse("text/plain");

        String userId = request.getUserId();
        String sessionId = request.getSessionId();
        Double woundArea = request.getWoundArea();
        Double woundLength = request.getWoundLength();
        Double woundWidth = request.getWoundWidth();
        Double woundDepth = request.getWoundDepth();

        Map<String, RequestBody> parts = new HashMap<>();
        parts.put("userId",    RequestBody.create(text, userId    != null ? userId    : ""));
        parts.put("sessionId", RequestBody.create(text, sessionId != null ? sessionId : ""));
        
        // For optional numeric fields: send "null" string so the server knows to skip them
        parts.put("WoundArea",   RequestBody.create(text, woundArea   != null ? String.format("%.6f", woundArea)   : "null"));
        parts.put("WoundLength", RequestBody.create(text, woundLength != null ? String.format("%.6f", woundLength) : "null"));
        parts.put("WoundWidth",  RequestBody.create(text, woundWidth  != null ? String.format("%.6f", woundWidth)  : "null"));
        parts.put("WoundDepth",  RequestBody.create(text, woundDepth  != null ? String.format("%.6f", woundDepth)  : "null"));
        parts.put("manualVerification", RequestBody.create(text, request.getManualVerification() != null ? String.valueOf(request.getManualVerification()) : "false"));

        // Optional file part — null is fine; Retrofit omits null @Part values in multipart
        MultipartBody.Part filePart = null;
        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                RequestBody fileBody = RequestBody.create(
                        MediaType.parse("image/jpeg"), imgFile);
                filePart = MultipartBody.Part.createFormData("file", imgFile.getName(), fileBody);
            }
        }

        Retrofit retrofit = ApiClient.getInstance(false, token);
        ApiService apiService = retrofit.create(ApiService.class);

        Call<ResponseBody> call = apiService.editWoundMeasurements(parts, filePart);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataSuccess(response.body());
                } else {
                    ResponseBody errorBody = response.errorBody();
                    String message = "";
                    try {
                        Gson gson = new Gson();
                        ErrorResponseModel errorResponse = gson.fromJson(
                                errorBody != null ? errorBody.string() : "", ErrorResponseModel.class);
                        if (errorResponse != null && errorResponse.getError() != null) {
                            message = errorResponse.getError();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(message);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                getCommonAPIDataSuccessCallBack.getCommonAPIDataFailure(
                        String.format("%s %s", Constants.API_FAILURE, t.toString()));
            }
        });
    }


    public interface GetCommonAPIDataSuccessCallBack {
        void getCommonAPIDataSuccess(ResponseBody models);

        void getCommonAPIDataFailure(String message);

        void onProgressUpdate(int progress); // Added progress update method
    }

    public void setGetCommonAPIDetails(Repository.GetCommonAPIDataSuccessCallBack getCommonAPIDetails) {
        getCommonAPIDataSuccessCallBack = getCommonAPIDetails;
    }
}