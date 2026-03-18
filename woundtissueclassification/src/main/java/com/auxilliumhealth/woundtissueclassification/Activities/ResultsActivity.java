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

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.ViewModel.SymptomViewModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ResultsActivity extends AppCompatActivity {

    private SymptomViewModel viewModel;
    private TextView tvResults;
    private String userId, sessionId, woundId, token, primaryColor, imageUrl, whereFrom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        tvResults = findViewById(R.id.tvResults);
        viewModel = new ViewModelProvider(this).get(SymptomViewModel.class);

        // Get data from intent
        getIntentData();
        displayResults();
    }

    private void getIntentData() {
        userId = getIntent().getStringExtra("userId");
        sessionId = getIntent().getStringExtra("sessionId");
        woundId = getIntent().getStringExtra("woundId");
        token = getIntent().getStringExtra("token");
        primaryColor = getIntent().getStringExtra("primaryColor");
        imageUrl = getIntent().getStringExtra("imageUrl");
        whereFrom = getIntent().getStringExtra("whereFrom");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(primaryColor));
        }
    }



    private void displayResults() {
        StringBuilder resultsText = new StringBuilder();
        resultsText.append("Assessment Complete!\n\n");

        // Get data from intent
        int questionsAnswered = getIntent().getIntExtra("questionsAnswered", 0);
        String aiResultsJson = getIntent().getStringExtra("aiResults");

        resultsText.append("=== Symptom Assessment ===\n");
        resultsText.append("Questions Answered: ").append(questionsAnswered).append("\n");
        resultsText.append("Submission: Success\n");

        // Show AI results if available
        resultsText.append("\n=== AI Model Results ===\n");

        JsonObject aiResults = null;
        if (aiResultsJson != null && !aiResultsJson.isEmpty()) {
            try {
                aiResults = JsonParser.parseString(aiResultsJson).getAsJsonObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Also check ViewModel for updated results


        if (aiResults != null) {
            if (aiResults.has("status")) {
                resultsText.append("Status: ").append(aiResults.get("status").getAsString()).append("\n");
            }
            if (aiResults.has("prediction")) {
                resultsText.append("Prediction: ").append(aiResults.get("prediction").getAsString()).append("\n");
            }
            if (aiResults.has("confidence")) {
                resultsText.append("Confidence: ").append(aiResults.get("confidence").getAsString()).append("\n");
            }
            if (aiResults.has("area")) {
                resultsText.append("Area: ").append(aiResults.get("area").getAsString()).append("\n");
            }
            if (aiResults.has("tissue_type")) {
                resultsText.append("Tissue Type: ").append(aiResults.get("tissue_type").getAsString()).append("\n");
            }
        } else {
            resultsText.append("AI results pending or not available\n");
        }

        // Additional session information
        resultsText.append("\n=== Session Information ===\n");
        resultsText.append("User ID: ").append(userId != null ? userId : "N/A").append("\n");
        resultsText.append("Session ID: ").append(sessionId != null ? sessionId : "N/A").append("\n");
        resultsText.append("Wound ID: ").append(woundId != null ? woundId : "N/A").append("\n");

        tvResults.setText(resultsText.toString());
    }
}