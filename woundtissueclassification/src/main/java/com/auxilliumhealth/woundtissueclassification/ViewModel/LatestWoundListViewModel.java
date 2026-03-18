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
package com.auxilliumhealth.woundtissueclassification.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.auxilliumhealth.woundtissueclassification.Model.LatestSessionModel;
import com.auxilliumhealth.woundtissueclassification.Model.WoundListModel;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.ResponseBody;

public class LatestWoundListViewModel extends AndroidViewModel {

    private final Repository repository;
    private final MutableLiveData<LatestSessionModel> latestSessionData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LatestWoundListViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
    }

    public LiveData<LatestSessionModel> getLatestSessionData() {
        return latestSessionData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void fetchLatestSession(String userId, String token) {
        WoundListModel request = new WoundListModel();
        request.setUserId(userId);

        repository.getLatestSession(request, token, new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody responseBody) {
                try {
                    if (responseBody != null) {
                        Gson gson = new Gson();
                        LatestSessionModel model = gson.fromJson(responseBody.string(), LatestSessionModel.class);
                        latestSessionData.postValue(model);
                    }
                } catch (IOException e) {
                    errorMessage.postValue("Parsing error: " + e.getMessage());
                }
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                errorMessage.postValue(message);
            }

            @Override
            public void onProgressUpdate(int progress) { }
        });
    }
}
