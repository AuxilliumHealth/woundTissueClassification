package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.auxilliumhealth.woundtissueclassification.Adapters.WoundListAdapter;
import com.auxilliumhealth.woundtissueclassification.Model.WoundListModel;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.auxilliumhealth.woundtissueclassification.Utils.RootActivity;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivityWoundListBinding;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;

public class WoundListActivity extends RootActivity {
    private final List<WoundListModel.Datum> woundList = new ArrayList<>();
    ActivityWoundListBinding binding;
    String TAG = "WoundListActivity";
    Repository repository;
    String primaryColor, userId, token,woundId;
    private WoundListAdapter woundImageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWoundListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new Repository(WoundListActivity.this);

        userId = getIntent().getStringExtra("userId");
        token = getIntent().getStringExtra("token");
        woundId = getIntent().getStringExtra("woundId");
        primaryColor = getIntent().getStringExtra("primaryColor");
        binding.appBarLayout.setBackgroundColor(Color.parseColor(primaryColor));
        binding.materialToolbar.setBackgroundColor(Color.parseColor(primaryColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(primaryColor));
        }

        setupRecyclerView();
        getWoundList(userId, token,woundId);
    }

    private void setupRecyclerView() {
        woundImageAdapter = new WoundListAdapter(woundList, new WoundListAdapter.OnWoundItemClickListener() {
            @Override
            public void onWoundItemClick(WoundListModel.Datum wound) {


            }

            @Override
            public void onSessionItemClick(WoundListModel.ImagingSession session) {
                // Handle session item click
                Log.d(TAG, "Session clicked: " + session.getSessionId());
                Intent intent = new Intent(WoundListActivity.this, WoundDetailsActivity.class);
                intent.putExtra("woundId", session.getWoundId());
                intent.putExtra("sessionId", session.getSessionId());
                intent.putExtra("userId", userId);
                intent.putExtra("token", token);
                intent.putExtra("primaryColor", primaryColor);
                startActivity(intent);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(WoundListActivity.this, LinearLayoutManager.VERTICAL, false);

        binding.woundRecyclerView.setLayoutManager(layoutManager);
        binding.woundRecyclerView.setAdapter(woundImageAdapter);
    }


    private void getWoundList(String userId, String token,String woundId) {
        showLoadingState();

        WoundListModel request = new WoundListModel();
        request.setUserId(userId);
        request.setWoundId(woundId);

        repository.getWoundList(request, token, new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody responseBody) {
                try {
                    if (responseBody != null) {
                        Gson gson = new Gson();
                        String responseString = responseBody.string();
                        WoundListModel woundListModel = gson.fromJson(responseString, WoundListModel.class);

                        runOnUiThread(() -> {
                            if (woundListModel != null && woundListModel.getData() != null && !woundListModel.getData().isEmpty()) {
                                woundList.clear();
                                woundList.addAll(woundListModel.getData());
                                woundImageAdapter.notifyDataSetChanged();
                                showWoundListState();
                            } else {
                                showEmptyState();
                            }
                        });
                    } else {
                        runOnUiThread(() -> showErrorState("No data received from server"));
                    }
                } catch (IOException | JsonSyntaxException e) {
                    Log.e(TAG, "Error parsing wound list response", e);
                    runOnUiThread(() -> showErrorState("Failed to parse wound data"));
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error in wound list processing", e);
                    runOnUiThread(() -> showErrorState("Unexpected error occurred"));
                }
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                Log.e(TAG, "API call failed: " + message);
                runOnUiThread(() -> showErrorState(message));
            }

            @Override
            public void onProgressUpdate(int progress) {
                Log.d(TAG, "Wound list loading progress: " + progress + "%");
            }
        });
    }

    private void showLoadingState() {
        binding.woundRecyclerView.setVisibility(View.GONE);
        showLoader();

    }

    private void showWoundListState() {
        binding.woundRecyclerView.setVisibility(View.VISIBLE);
        hideLoader();

    }

    private void showEmptyState() {
        binding.woundRecyclerView.setVisibility(View.GONE);
        hideLoader();
    }

    private void showErrorState(String errorMessage) {
        binding.woundRecyclerView.setVisibility(View.GONE);
        hideLoader();
        if(errorMessage != null){
            if(errorMessage.equals("Invalid or non-existent token")) {
                Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();

            }
        }
        finish();

    }

}