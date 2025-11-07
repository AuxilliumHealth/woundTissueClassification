package com.auxilliumhealth.woundtissueclassification.ViewModel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.auxilliumhealth.woundtissueclassification.Model.S3UploadResultModel;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;

public class CameraViewModel extends AndroidViewModel {
    private Repository repository;
    private MutableLiveData<ResponseBody> uploadResponse = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private MutableLiveData<List<File>> capturedImages = new MutableLiveData<>();

    public CameraViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
        setupCallbacks();
    }

    private void setupCallbacks() {
        repository.setGetCommonAPIDetails(new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody response) {
                uploadResponse.postValue(response);
                isLoading.postValue(false);
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                errorMessage.postValue(message);
                isLoading.postValue(false);
            }
            
            @Override
            public void onProgressUpdate(int progress) {
                // Handle progress updates here if needed
                // For example, you could update a progress bar
            }
        });
    }

//    public void uploadImage(File imageFile, String userId, String woundId, String sessionId, String token) {
//        isLoading.setValue(true);
//        repository.uploadImage(createImageMultipart(imageFile), userId, woundId, sessionId, token);
//    }

    private MultipartBody.Part createImageMultipart(File file) {
        return MultipartBody.Part.createFormData(
            "file",
            file.getName(),
            okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), file)
        );
    }

    public MutableLiveData<ResponseBody> getUploadResponse() {
        return uploadResponse;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MutableLiveData<List<File>> getCapturedImages() {
        return capturedImages;
    }

    public void addCapturedImage(File imageFile) {
        List<File> currentList = capturedImages.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        currentList.add(imageFile);
        capturedImages.setValue(currentList);
    }

    public void clearCapturedImages() {
        List<File> currentList = capturedImages.getValue();
        if (currentList != null) {
            currentList.clear();
            capturedImages.setValue(currentList);
        }
    }
}
