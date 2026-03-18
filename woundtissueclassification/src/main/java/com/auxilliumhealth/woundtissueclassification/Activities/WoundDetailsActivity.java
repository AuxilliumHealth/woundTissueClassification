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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.auxilliumhealth.woundtissueclassification.Adapters.ImageAdapter;
import com.auxilliumhealth.woundtissueclassification.Model.AiModelData;
import com.auxilliumhealth.woundtissueclassification.Model.AnalysisImage;
import com.auxilliumhealth.woundtissueclassification.Model.WoundAnalysis;
import com.auxilliumhealth.woundtissueclassification.Model.WoundDetailsModel;
import com.auxilliumhealth.woundtissueclassification.Model.WoundListModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.auxilliumhealth.woundtissueclassification.UiComponent.PieChartView;
import com.auxilliumhealth.woundtissueclassification.UiComponent.PieHelper;
import com.auxilliumhealth.woundtissueclassification.Utils.RootActivity;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivityWoundDetailsBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.ResponseBody;

public class WoundDetailsActivity extends RootActivity {

    private ActivityWoundDetailsBinding binding;
    private Repository repository;
    private static final String TAG = "WoundDetailsActivity";
    String sessionId;
    String userId;
    String woundId;
    String primaryColor;
    String token;

    float sloughPercent, escharPercent, granulationPercent, woundTissueNormalPercent, callusPercent, erythemaPercent, macerationPercent, periWoundNormalPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new Repository(WoundDetailsActivity.this);

        // Inflate layout using ViewBinding
        binding = ActivityWoundDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
         sessionId = getIntent().getStringExtra("sessionId");
         userId = getIntent().getStringExtra("userId");
         woundId = getIntent().getStringExtra("woundId");
         primaryColor = getIntent().getStringExtra("primaryColor");
         token = getIntent().getStringExtra("token");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.view.Window window = getWindow();
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.parseColor(primaryColor));
        }
        binding.resultMaterialToolbar.setBackgroundColor(Color.parseColor(primaryColor));
        binding.resultAppBarLayout.setBackgroundColor(Color.parseColor(primaryColor));
        binding.resultMaterialToolbar.setNavigationOnClickListener(v -> finish());
        getWoundDetails(userId,sessionId,woundId,token);


    }
    private void getWoundDetails(String userId,String sessionId, String woundId, String token) {
showLoader();
        WoundDetailsModel request = new WoundDetailsModel();
        request.setUserId(userId);
        request.setWoundId(woundId);
        request.setSessionId(sessionId);

        repository.getWoundDetails(request, token, new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody responseBody) {
                try {
                    if (responseBody != null) {
                        hideLoader();
                        Gson gson = new Gson();
                        String responseString = responseBody.string();
                        WoundDetailsModel woundListModel = gson.fromJson(responseString, WoundDetailsModel.class);
                        runOnUiThread(() -> {
                            if (woundListModel != null && woundListModel.getAiModelData() != null ) {
                                showFinalResults(woundListModel);
                            } else {
                                showFinalResults(woundListModel);

                            }
                        });

                    } else {
                        hideLoader();
                        showFinalResults(null);
                    }
                } catch (IOException | JsonSyntaxException e) {
                    hideLoader();
                    Log.e(TAG, "Error parsing wound list response", e);
                } catch (Exception e) {
                    hideLoader();
                    Log.e(TAG, "Unexpected error in wound list processing", e);
                }
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                hideLoader();
                Log.e(TAG, "API call failed: " + message);
            }

            @Override
            public void onProgressUpdate(int progress) {
                Log.d(TAG, "Wound list loading progress: " + progress + "%");
            }
        });
    }

    private void showFinalResults(WoundDetailsModel result) {
        try {
            binding.modelResultLayout.setVisibility(View.VISIBLE);


            if (result != null && result.getAiModelData() != null && result.getAiModelData().getDisplayImagePath() != null) {
                binding.noWoundDetectedLayout.setVisibility(View.GONE);
                binding.woundDetectedLayoutLayout.setVisibility(View.VISIBLE);
                displayAIResults(result);
                // Create a data class first
                ArrayList<AnalysisImage> imageList = new ArrayList<>();
                imageList.add(new AnalysisImage("Cropped Image", result.getAiModelData().getCroppedImagePath()));
                imageList.add(new AnalysisImage("Wound & Peri-wound Tissue", result.getAiModelData().getWoundPeriwoundOverlayImagePath()));
                imageList.add(new AnalysisImage("Wound Tissue ", result.getAiModelData().getWoundTissueOverlayImagePath()));
                imageList.add(new AnalysisImage("Peri-wound Tissue", result.getAiModelData().getPeriWoundTissueOverlayImagePath()));


                LinearLayoutManager layoutManager = new LinearLayoutManager(WoundDetailsActivity.this, LinearLayoutManager.HORIZONTAL, false);
                binding.processedImageRecyclerview.setLayoutManager(layoutManager);
                binding.processedImageRecyclerview.scrollToPosition(0);
                ImageAdapter imageAdapter = new ImageAdapter(WoundDetailsActivity.this, imageList);
                binding.processedImageRecyclerview.setAdapter(imageAdapter);
                imageAdapter.notifyDataSetChanged();

                // FIXED: Properly set the click listener for finish button

            } else {
                showErrorState(result != null ? result.getImageUrl() : null);
            }



        } catch (Exception e) {
            Log.e(TAG, "Error showing final results", e);
            Toast.makeText(this, "Error displaying results", Toast.LENGTH_SHORT).show();
        }
    }
    private void displayAIResults(WoundDetailsModel woundAnalysis) {
        try {

            // Load images with Glide
            loadResultImages(woundAnalysis);

            // Display wound tissue analysis
            sloughPercent = truncateToOneDecimal(woundAnalysis.getAiModelData().getSloughPercent());
            escharPercent = truncateToOneDecimal(woundAnalysis.getAiModelData().getEscharPercent());
            granulationPercent = truncateToOneDecimal(woundAnalysis.getAiModelData().getGranulationPercent());
            woundTissueNormalPercent = truncateToOneDecimal(woundAnalysis.getAiModelData().getNormalTissuePercent());
            callusPercent = truncateToOneDecimal(woundAnalysis.getAiModelData().getCallusPercent());
            erythemaPercent = truncateToOneDecimal(woundAnalysis.getAiModelData().getErythemaPercent());
            macerationPercent = truncateToOneDecimal(woundAnalysis.getAiModelData().getMacerationPercent());
            periWoundNormalPercent = truncateToOneDecimal(woundAnalysis.getAiModelData().getNormalPercent());
            String woundScore = woundAnalysis.getWoundScore();
            if (woundScore != null) {

                binding.woundScoreLayout.setVisibility(View.VISIBLE);
                binding.riskLevelValueTextview.setText(woundScore);

                int textColor;
                int backgroundColor;

                switch (woundScore) {
                    case "RED":
                        textColor = Color.parseColor("#B71C1C");        // Dark red text
                        backgroundColor = Color.parseColor("#FFCDD2");  // Light red background
                        break;
                    case "YELLOW":
                        textColor = Color.parseColor("#F57F17");        // Dark yellow text
                        backgroundColor = Color.parseColor("#FFF9C4");  // Light yellow background
                        break;
                    case "GREEN":
                    default:
                        textColor = Color.parseColor("#2E7D32");        // Dark green text
                        backgroundColor = Color.parseColor("#C8E6C9");  // Light green background
                        break;
                }

                binding.riskLevelValueTextview.setTextColor(textColor);
                binding.riskStatusIndicator.setBackgroundColor(textColor);
                binding.riskLevelValueTextview.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
                binding.woundScoreCard.setCardBackgroundColor(Color.WHITE);
            } else {
                binding.woundScoreLayout.setVisibility(View.GONE);
            }



            displayWoundTissueAnalysis(binding.woundTissuePieChart, sloughPercent, escharPercent, granulationPercent, woundTissueNormalPercent);
            displayPeriWoundAnalysis(binding.periWoundTissuePieChart, callusPercent, erythemaPercent, macerationPercent, periWoundNormalPercent);

            // Display wound measurements
            displayWoundMeasurements(woundAnalysis.getAiModelData());

            // Make the wound analytics card visible
            binding.woundanalyticCard.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Log.e(TAG, "Error displaying AI results", e);
        }
    }
    float truncateToOneDecimal(float value) {
        return (int) (value * 10) / 10f;
    }
    private void displayWoundTissueAnalysis(PieChartView pieChartView, float sloughPercent, float escharPercent, float granulationPercent, float woundTissueNormalPercent) {
        ArrayList<PieHelper> pieHelperArrayList = new ArrayList<>();

        if (woundTissueNormalPercent > 0) {
            binding.woundtissueNormalRelativeLayout.setVisibility(View.VISIBLE);
            binding.woundTissueNormalTxt.setText(woundTissueNormalPercent + "%");
            pieHelperArrayList.add(new PieHelper(woundTissueNormalPercent, WoundDetailsActivity.this.getColor(R.color.other_color)));
        }

        if (granulationPercent > 0) {
            binding.granulationTxt.setText(granulationPercent + "%");
            binding.granulationRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(granulationPercent, WoundDetailsActivity.this.getColor(R.color.granulation_color)));
        }

        if (sloughPercent > 0) {
            binding.sloughTxt.setText(sloughPercent + "%");
            binding.sloudhRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(sloughPercent, WoundDetailsActivity.this.getColor(R.color.slough_color)));
        }

        if (escharPercent > 0) {
            binding.escharTxt.setText(escharPercent + "%");
            binding.escharRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(escharPercent, WoundDetailsActivity.this.getColor(R.color.eschar_color)));
        }

        pieChartView.setDate(pieHelperArrayList);
    }

    private void displayPeriWoundAnalysis(PieChartView pieChartView, float callusPercent, float erythemaPercent, float macerationPercent, float periWoundNormalPercent) {
        ArrayList<PieHelper> pieHelperArrayList = new ArrayList<PieHelper>();

        if (periWoundNormalPercent > 0) {
            binding.normalperiWoundTxt.setText(periWoundNormalPercent + "%");
            binding.periWoundRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(periWoundNormalPercent, WoundDetailsActivity.this.getColor(R.color.other_color)));
        }

        if (callusPercent > 0) {
            binding.callusTxt.setText(callusPercent + "%");
            binding.callusRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(callusPercent, WoundDetailsActivity.this.getColor(R.color.callus_color)));
        }

        if (erythemaPercent > 0) {
            binding.erythemaTxt.setText(erythemaPercent + "%");
            binding.erythemaRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(erythemaPercent, WoundDetailsActivity.this.getColor(R.color.erythema_color)));
        }

        if (macerationPercent > 0) {
            binding.macerationTxt.setText(macerationPercent + "%");
            binding.macerationRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(macerationPercent, WoundDetailsActivity.this.getColor(R.color.maceration_color)));
        }

        pieChartView.setDate(pieHelperArrayList);
    }

    private void displayWoundMeasurements(WoundDetailsModel.AiModelDataModel aiData) {
        try {
            // Check if wound area is detected
            if (aiData.getWoundArea() > 0) {
                binding.woundAreaLinearLayout.setVisibility(View.VISIBLE);

                String tempImagePath = aiData.getClockwiseMappingVisualizationImagePath();
                if (tempImagePath == null || tempImagePath.isEmpty()) {
                    tempImagePath = aiData.getWoundMeasurementOverlayImagePath();
                }
                final String imagePath = tempImagePath;

                if (imagePath != null && !imagePath.isEmpty()) {
                    binding.woundMeasurementAxisCard.setVisibility(View.VISIBLE);
                    binding.measurementDisclaimerTxt.setVisibility(View.VISIBLE);
                    Glide.with(this).load(imagePath).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.woundMeasurementAxisImg);
                } else {
                    binding.woundMeasurementAxisCard.setVisibility(View.GONE);
                    binding.measurementDisclaimerTxt.setVisibility(View.GONE);
                }


                // Display measurements
                binding.woundAreaTxt.setText(String.format("%.2f cm²", aiData.getWoundArea()));
                binding.woundWidthTxt.setText(String.format("%.2f cm", aiData.getWoundWidth()));
                binding.woundLengthTxt.setText(String.format("%.2f cm", aiData.getWoundLength()));
                // Use the getter from WoundDetailsModel.AiModelDataModel if it's available. Assuming getWoundDepth is added as for SymptomQuestionActivity logic.
                Float depth = aiData.getWoundDepth();
                if (depth != null) {
                    binding.woundDepthTxt.setText(String.format("%.2f mm", depth));
                } else {
                    binding.woundDepthTxt.setText("- mm");
                }

                binding.woundMeasurementAxisImg.setOnClickListener(v -> showFullScreenImage(imagePath));

            } else {
                // No wound detected
                binding.woundAreaLinearLayout.setVisibility(View.GONE);
                binding.measurementDisclaimerTxt.setVisibility(View.GONE);
            }





        } catch (Exception e) {
            Log.e(TAG, "Error displaying wound measurements", e);
        }
    }

    private void showErrorState(String imageUrl) {
        try {
            binding.modelResultLayout.setVisibility(View.VISIBLE);
            binding.noWoundDetectedLayout.setVisibility(View.VISIBLE);
            binding.woundDetectedLayoutLayout.setVisibility(View.GONE);

            Glide.with(this).load(imageUrl).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.originalImage);
            binding.originalImage.setOnClickListener(v -> showFullScreenImage(imageUrl));


            // Show error message
            Toast.makeText(this, "Unable to load AI analysis results", Toast.LENGTH_LONG).show();

            // You can also show a specific error state in the UI
            binding.woundanalyticCard.setVisibility(View.GONE);

        } catch (Exception e) {
            Log.e(TAG, "Error showing error state", e);
        }
    }
    private void loadResultImages(WoundDetailsModel aiData) {
        try {
            // Load original image
            if (aiData.getImageUrl() != null && !aiData.getImageUrl().isEmpty()) {
                Glide.with(this).load(aiData.getImageUrl()).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.capturedImage);
            }

            // Load bounding box/image with wound localization
            if (aiData.getAiModelData().getDisplayImagePath() != null && !aiData.getAiModelData().getDisplayImagePath().isEmpty()) {
                Glide.with(this).load(aiData.getAiModelData().getDisplayImagePath()).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.boundingImage);
            }

            binding.capturedImage.setOnClickListener(v -> showFullScreenImage(aiData.getImageUrl()));
            binding.boundingImage.setOnClickListener(v -> showFullScreenImage(aiData.getAiModelData().getDisplayImagePath()));


        } catch (Exception e) {
            Log.e(TAG, "Error loading result images", e);
        }
    }

}
