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
package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.auxilliumhealth.woundtissueclassification.Adapters.OldWoundLoctionAdapter;
import com.auxilliumhealth.woundtissueclassification.Utils.RootActivity;
import com.auxilliumhealth.woundtissueclassification.ViewModel.LatestWoundListViewModel;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivityLatestWoundListBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LatestWoundListActivity extends RootActivity {

    public static String sessionId;
    private ActivityLatestWoundListBinding binding;
    private LatestWoundListViewModel viewModel;
    private String userId, token, primaryColor, woundId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLatestWoundListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        userId = getIntent().getStringExtra("userId");
        token = getIntent().getStringExtra("token");
        primaryColor = getIntent().getStringExtra("primaryColor");
        woundId = getIntent().getStringExtra("woundId");
        boolean woundScoreRequired = getIntent().getBooleanExtra("woundScoreRequired", true);
        boolean woundLocationRequired = getIntent().getBooleanExtra("woundLocationRequired", true);
        sessionId = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(new Date());
        // InitializeViewModel
        viewModel = new ViewModelProvider(this).get(LatestWoundListViewModel.class);
        binding.newWoundBtn.setStrokeColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
        binding.newWoundBtn.setTextColor(Color.parseColor(primaryColor));
        // Observe LiveData
        observeViewModel(woundScoreRequired);

        // Fetch data
        showLoader();
        binding.coordLayout.setVisibility(View.GONE);
        viewModel.fetchLatestSession(userId, token);

        // Button listeners
        binding.newWoundBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, WoundLocationActivity.class);
            i.putExtra("whereFrom", "imaging");
            i.putExtra("userId", userId);
            i.putExtra("token", token);
            i.putExtra("woundId", woundId);
            i.putExtra("woundScoreRequired", woundScoreRequired);
            i.putExtra("woundLocationRequired", woundLocationRequired);
            i.putExtra("primaryColor", primaryColor);
            startActivity(i);
            finish();
        });

        binding.backImg.setOnClickListener(v -> finish());
    }

    private void observeViewModel(boolean woundScoreRequired) {
        viewModel.getLatestSessionData().observe(this, latestSessionModel -> {
            hideLoader();

            binding.shimmerContainer.setVisibility(View.GONE);
            binding.newWoundBtn.setVisibility(View.VISIBLE);

            if (latestSessionModel != null && latestSessionModel.getData() != null && !latestSessionModel.getData().isEmpty()) {
                // Data exists - show the list
                binding.coordLayout.setVisibility(View.VISIBLE);

                binding.oldwoundLayout.setVisibility(View.VISIBLE);
                woundId = String.valueOf(latestSessionModel.getData().size()+1);
                Log.d("LatestWoundListActivity", "woundId: " + woundId);
                LinearLayoutManager layoutManager = new LinearLayoutManager(this);
                binding.oldRecyclerview.setLayoutManager(layoutManager);
                OldWoundLoctionAdapter adapter = new OldWoundLoctionAdapter(this, latestSessionModel.getData(), userId, token, primaryColor,woundScoreRequired);
                binding.oldRecyclerview.setAdapter(adapter);
            } else {
                binding.coordLayout.setVisibility(View.GONE);

                Intent i = new Intent(this, WoundLocationActivity.class);
                i.putExtra("whereFrom", "imaging");
                i.putExtra("userId", userId);
                i.putExtra("token", token);
                i.putExtra("primaryColor", primaryColor);
                i.putExtra("woundScoreRequired", woundScoreRequired);
                i.putExtra("woundId", woundId);
                startActivity(i);
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, message -> {
            hideLoader();
            binding.coordLayout.setVisibility(View.GONE);

            binding.oldwoundLayout.setVisibility(View.GONE);
            binding.newWoundBtn.setVisibility(View.VISIBLE);
            binding.shimmerContainer.setVisibility(View.GONE);
            if(message != null){
                    if(message.equals("Invalid or non-existent token")) {
                        Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();

                    }
            }
            finish();
        });
    }
}
