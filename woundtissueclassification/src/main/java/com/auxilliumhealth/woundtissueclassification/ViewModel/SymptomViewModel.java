package com.auxilliumhealth.woundtissueclassification.ViewModel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.auxilliumhealth.woundtissueclassification.Model.AIModelProcessRequest;
import com.auxilliumhealth.woundtissueclassification.Model.Question;
import com.auxilliumhealth.woundtissueclassification.Model.SubmitAnswersRequest;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;

public class SymptomViewModel extends AndroidViewModel {
    private Repository repository;
    private MutableLiveData<List<Question>> questionsLiveData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Boolean> submissionSuccess = new MutableLiveData<>();

    public SymptomViewModel(@NonNull Application application) {
        super(application);
        repository = new Repository(application);
        setupCallbacks();
    }






    public void loadQuestions(String token) {
        isLoading.setValue(true);
        repository.getSymptomQuestions(token);
    }

    public MutableLiveData<List<Question>> getQuestionsLiveData() {
        return questionsLiveData;
    }

    public MutableLiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }











    // In the setupCallbacks method, fix the response parsing:
    private void setupCallbacks() {
        repository.setGetCommonAPIDetails(new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody responseBody) {
                isLoading.postValue(false);
                try {
                    String jsonResponse = responseBody.string();
                    Gson gson = new Gson();

                    // Better way to detect response type
                    if (jsonResponse.contains("\"questionId\"") || jsonResponse.contains("\"options\"")) {
                        // This is the questions response
                        Type listType = new TypeToken<List<Question>>(){}.getType();
                        List<Question> questions = gson.fromJson(jsonResponse, listType);
                        questionsLiveData.postValue(questions);
                    } else {
                        // This is the submission response
                        submissionSuccess.postValue(true);
                        Log.d("SymptomViewModel", "Answers submitted successfully");
                    }
                } catch (IOException e) {
                    errorMessage.postValue("Error parsing response: " + e.getMessage());
                    submissionSuccess.postValue(false);
                }
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
                submissionSuccess.postValue(false);
            }

            @Override
            public void onProgressUpdate(int progress) {
                // Handle progress updates if needed
                Log.d("SymptomViewModel", "Progress update: " + progress + "%");
            }
        });
    }
}