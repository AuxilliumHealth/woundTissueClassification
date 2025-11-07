package com.auxilliumhealth.woundtissueclassification.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.auxilliumhealth.woundtissueclassification.Model.WoundLocationRequest;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;

import okhttp3.ResponseBody;

public class WoundLocationViewModel extends AndroidViewModel {
    private Repository repository;
    private MutableLiveData<ResponseBody> updateWoundLocationResponse = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public WoundLocationViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
        setupCallbacks();
    }

    private void setupCallbacks() {
        repository.setGetCommonAPIDetails(new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody models) {
                isLoading.postValue(false);
                updateWoundLocationResponse.postValue(models);
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }

            @Override
            public void onProgressUpdate(int progress) {
                // Handle progress updates if needed
            }
        });
    }

    public void updateWoundLocation(String userId, String description, String woundId, String woundLocation, String token) {
        isLoading.setValue(true);
        repository.updateWoundLocation(userId, description, woundId, woundLocation, token);
    }

    public MutableLiveData<ResponseBody> getUpdateWoundLocationResponse() {
        return updateWoundLocationResponse;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}