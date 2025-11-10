package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.auxilliumhealth.woundtissueclassification.Adapters.ImageAdapter;
import com.auxilliumhealth.woundtissueclassification.LocalDatabase.PreferencesHelper;
import com.auxilliumhealth.woundtissueclassification.Model.AIModelProcessRequest;
import com.auxilliumhealth.woundtissueclassification.Model.AiModelData;
import com.auxilliumhealth.woundtissueclassification.Model.AnalysisImage;
import com.auxilliumhealth.woundtissueclassification.Model.Question;
import com.auxilliumhealth.woundtissueclassification.Model.SubmitAnswersRequest;
import com.auxilliumhealth.woundtissueclassification.Model.WoundAnalysis;
import com.auxilliumhealth.woundtissueclassification.Model.WoundScoreModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.auxilliumhealth.woundtissueclassification.UiComponent.PieChartView;
import com.auxilliumhealth.woundtissueclassification.UiComponent.PieHelper;
import com.auxilliumhealth.woundtissueclassification.Utils.LoadingDialog;
import com.auxilliumhealth.woundtissueclassification.ViewModel.SymptomViewModel;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivitySymptomQuestionBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import okhttp3.ResponseBody;

public class SymptomQuestionActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<Integer, Integer> selectedAnswers = new HashMap<>();
    private final Map<Integer, Integer> selectedSubAnswers = new HashMap<>();
    private final AtomicBoolean isProcessingAI = new AtomicBoolean(false);
    private final AtomicBoolean isSubmittingAnswers = new AtomicBoolean(false);

    String imageUrl, lensFocusDistance, woundId, sessionId, coinType, whereFrom, userId, token;
    String TAG = "SymptomQuestionActivity";
    String primaryColor;
    SubmitAnswersRequest submitAnswersRequest;
    WoundAnalysis woundAnalysis;
    // Pie chart data variables
    float sloughPercent, escharPercent, granulationPercent, woundTissueNormalPercent, callusPercent, erythemaPercent, macerationPercent, periWoundNormalPercent;
    private List<Question> questionList;
    private int currentIndex = 0;
    private boolean isAutoNavigating = false;
    private SymptomViewModel viewModel;
    private boolean isAiProcessingCompleted = false;
    private boolean isAnswersSubmitted = false;
    private Repository repository;
    private LoadingDialog loadingDialog;
    // View Binding
    private ActivitySymptomQuestionBinding binding;

    // Add this flag to track if result was set
    private boolean isResultSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize view binding
        binding = ActivitySymptomQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            initViews();
            viewModel = new ViewModelProvider(this).get(SymptomViewModel.class);
            setupObservers();
            loadQuestions();
            setupButtonListeners();

            // Start AI processing in background immediately
            startBackgroundAIProcessing();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "App initialization failed", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        try {
            repository = new Repository(SymptomQuestionActivity.this);
            primaryColor = getIntent().getStringExtra("primaryColor");
            if (primaryColor == null || !primaryColor.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                primaryColor = "#1A1A2E";
            }
binding.llPoweredBy.setOnClickListener(v -> {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.auxilliumhealth.ai/"));
    startActivity(intent);
});
            imageUrl = getIntent().getStringExtra("imageUrl");
            lensFocusDistance = getIntent().getStringExtra("lensFocusDistance");
            woundId = getIntent().getStringExtra("woundId");
            sessionId = getIntent().getStringExtra("sessionId");
            userId = getIntent().getStringExtra("userId");
            token = getIntent().getStringExtra("token");
            coinType = getIntent().getStringExtra("coinType");
            whereFrom = getIntent().getStringExtra("whereFrom");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(Color.parseColor(primaryColor));
            }
            Log.d(TAG, "sessionId: " + sessionId + " userId: " + userId + " woundId: " + woundId);

            setupUIWithPrimaryColor();

        } catch (Exception e) {
            Log.e(TAG, "Error in initViews", e);
            throw new RuntimeException("View initialization failed", e);
        }
    }

    private void setupUIWithPrimaryColor() {
        try {
            binding.btnPrev.setStrokeColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnPrev.setTextColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnNext.setBackgroundColor((Color.parseColor(primaryColor)));
            binding.btnNext.setTextColor(ColorStateList.valueOf(Color.WHITE));

            binding.materialToolbar.setBackgroundColor(Color.parseColor(primaryColor));
            binding.appBarLayout.setBackgroundColor(Color.parseColor(primaryColor));

            binding.progressBar.setIndicatorColor(Color.parseColor(primaryColor));

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(ContextCompat.getColor(this, R.color.gray_light));
            shape.setCornerRadius(getResources().getDimension(R.dimen._8dp));
            shape.setStroke((int) getResources().getDimension(R.dimen._1dp), Color.parseColor(primaryColor));

            binding.layoutSubOptions.setBackground(shape);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI colors", e);
        }
    }

    private void setupObservers() {
        viewModel.getQuestionsLiveData().observe(this, questions -> {
            if (questions != null && !questions.isEmpty()) {
                questionList = questions;
                binding.progressBar.setMax(questionList.size());
                showQuestion(currentIndex);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestions() {
        if (token != null) {
            viewModel.loadQuestions(token);
        } else {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startBackgroundAIProcessing() {
        if (imageUrl != null && woundId != null && lensFocusDistance != null && token != null) {
            if (!isProcessingAI.get()) {
                Log.d(TAG, "Starting background AI processing...");
                String areaCoff = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_AREA_COEFFS);
                String pixelPerUnit = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_PIXEL_PER_UNIT);

                processAIModelImageInBackground(userId, sessionId, imageUrl, woundId, Double.parseDouble(lensFocusDistance), token, areaCoff, pixelPerUnit);
            }
        } else {
            Log.w(TAG, "Missing required data for AI processing");
        }
    }

    private void setupButtonListeners() {
        binding.rgOptions.setOnCheckedChangeListener((group, checkedId) -> onOptionSelected(checkedId));

        binding.btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showQuestion(currentIndex);
                animateSlideOut();
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (questionList == null || currentIndex >= questionList.size()) {
                Toast.makeText(this, "Question data not available", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!selectedAnswers.containsKey(questionList.get(currentIndex).questionId)) {
                Toast.makeText(this, "Please select an option to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            Question currentQuestion = questionList.get(currentIndex);
            int selectedOptionId = selectedAnswers.get(currentQuestion.questionId);
            Question.Option selectedOption = null;
            for (Question.Option option : currentQuestion.options) {
                if (option.optionId == selectedOptionId) {
                    selectedOption = option;
                    break;
                }
            }

            if (selectedOption != null && selectedOption.subOption != null && !selectedSubAnswers.containsKey(currentQuestion.questionId)) {
                Toast.makeText(this, "Please select a sub-option to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            navigateToNextQuestion();
        });
    }

    private void onOptionSelected(int checkedId) {
        if (checkedId != -1 && !isAutoNavigating && questionList != null && currentIndex < questionList.size()) {
            RadioButton selectedRb = binding.getRoot().findViewById(checkedId);
            if (selectedRb == null || selectedRb.getTag() == null) return;

            int optionId = (int) selectedRb.getTag();
            Question currentQuestion = questionList.get(currentIndex);
            selectedAnswers.put(currentQuestion.questionId, optionId);

            Question.Option selectedOption = null;
            for (Question.Option option : currentQuestion.options) {
                if (option.optionId == optionId) {
                    selectedOption = option;
                    break;
                }
            }

            if (selectedOption != null && selectedOption.subOption != null) {
                binding.layoutSubOptions.setVisibility(View.VISIBLE);
                binding.tvSubQuestion.setText(selectedOption.subOption.subQuestion);
                loadSubOptions(selectedOption.subOption.subOptions, currentQuestion.questionId);
                animateCardExpansion();
                binding.btnNext.setEnabled(false);
            } else {
                binding.layoutSubOptions.setVisibility(View.GONE);
                selectedSubAnswers.remove(currentQuestion.questionId);
                binding.btnNext.setEnabled(true);
                autoNavigateToNextQuestion();
            }
        }
    }

    private void showQuestion(int index) {
        if (questionList == null || index < 0 || index >= questionList.size()) {
            Log.e(TAG, "Invalid question index: " + index);
            return;
        }

        isAutoNavigating = false;
        Question q = questionList.get(index);

        try {
            if (q.imageResource != null && !q.imageResource.isEmpty()) {
                loadImageWithGlide(q.imageResource);
            } else {
                binding.ivQuestion.setVisibility(View.GONE);
            }

            binding.tvProgress.setText(String.format("Question %d of %d", index + 1, questionList.size()));
            binding.progressBar.setProgress(index + 1);
            binding.tvQuestion.setText(q.question);

            binding.rgOptions.setOnCheckedChangeListener(null);
            binding.rgOptions.removeAllViews();
            binding.layoutSubOptions.setVisibility(View.GONE);

            for (int i = 0; i < q.options.size(); i++) {
                RadioButton rb = new RadioButton(this);
                rb.setText(q.options.get(i).option);
                rb.setId(View.generateViewId());
                rb.setTag(q.options.get(i).optionId);
                rb.setButtonTintList(ColorStateList.valueOf(Color.parseColor(primaryColor)));
                rb.setTextSize(16);
                rb.setTextColor(Color.BLACK);
                rb.setPadding(32, 32, 32, 32);
                rb.setBackground(createRadioButtonBackground(SymptomQuestionActivity.this, primaryColor));
                binding.rgOptions.addView(rb);
            }

            if (selectedAnswers.containsKey(q.questionId)) {
                int savedOptionId = selectedAnswers.get(q.questionId);
                for (int i = 0; i < binding.rgOptions.getChildCount(); i++) {
                    View child = binding.rgOptions.getChildAt(i);
                    if (child instanceof RadioButton) {
                        RadioButton rb = (RadioButton) child;
                        if (rb.getTag() != null && (int) rb.getTag() == savedOptionId) {
                            binding.rgOptions.check(rb.getId());
                            Question.Option selectedOption = null;
                            for (Question.Option option : q.options) {
                                if (option.optionId == savedOptionId) {
                                    selectedOption = option;
                                    break;
                                }
                            }
                            if (selectedOption != null && selectedOption.subOption != null) {
                                binding.layoutSubOptions.setVisibility(View.VISIBLE);
                                binding.tvSubQuestion.setText(selectedOption.subOption.subQuestion);
                                loadSubOptions(selectedOption.subOption.subOptions, q.questionId);
                            }
                            break;
                        }
                    }
                }
            }

            binding.btnPrev.setEnabled(index > 0);
            binding.btnNext.setText(index == questionList.size() - 1 ? "Finish" : "Next");

            boolean hasAnswer = selectedAnswers.containsKey(q.questionId);
            if (hasAnswer) {
                int selectedOptionId = selectedAnswers.get(q.questionId);
                Question.Option selectedOption = null;
                for (Question.Option option : q.options) {
                    if (option.optionId == selectedOptionId) {
                        selectedOption = option;
                        break;
                    }
                }
                binding.btnNext.setEnabled(selectedOption == null || selectedOption.subOption == null || selectedSubAnswers.containsKey(q.questionId));
            } else {
                binding.btnNext.setEnabled(false);
            }

            binding.rgOptions.setOnCheckedChangeListener((group, checkedId) -> onOptionSelected(checkedId));

        } catch (Exception e) {
            Log.e(TAG, "Error showing question", e);
            Toast.makeText(this, "Error loading question", Toast.LENGTH_SHORT).show();
        }
    }

    private StateListDrawable createRadioButtonBackground(Context context, String primaryColor) {
        try {
            GradientDrawable checkedDrawable = new GradientDrawable();
            checkedDrawable.setShape(GradientDrawable.RECTANGLE);
            checkedDrawable.setColor(Color.parseColor("#E8EAF6"));
            checkedDrawable.setCornerRadius(8f * context.getResources().getDisplayMetrics().density);
            checkedDrawable.setStroke((int) (1 * context.getResources().getDisplayMetrics().density), Color.parseColor(primaryColor));

            GradientDrawable defaultDrawable = new GradientDrawable();
            defaultDrawable.setShape(GradientDrawable.RECTANGLE);
            defaultDrawable.setColor(Color.TRANSPARENT);
            defaultDrawable.setCornerRadius(8f * context.getResources().getDisplayMetrics().density);

            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_checked}, checkedDrawable);
            states.addState(new int[]{}, defaultDrawable);

            return states;
        } catch (Exception e) {
            Log.e(TAG, "Error creating radio button background", e);
            return new StateListDrawable();
        }
    }

    private void loadImageWithGlide(String imageUrl) {
        try {
            binding.ivQuestion.setVisibility(View.VISIBLE);
            Glide.with(this).asGif().load(imageUrl).diskCacheStrategy(DiskCacheStrategy.ALL).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).timeout(15000).listener(new RequestListener<GifDrawable>() {
                @Override
                public boolean onLoadFailed(GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                    runOnUiThread(() -> {
                        binding.ivQuestion.setVisibility(View.GONE);
                        Toast.makeText(SymptomQuestionActivity.this, "Image failed to load", Toast.LENGTH_SHORT).show();
                    });
                    return false;
                }

                @Override
                public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                    return false;
                }
            }).into(binding.ivQuestion);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image with Glide", e);
            binding.ivQuestion.setVisibility(View.GONE);
        }
    }

    private void loadSubOptions(List<Question.Option> subOptions, int questionId) {
        try {
            binding.rgSubOptions.setOnCheckedChangeListener(null);
            binding.rgSubOptions.removeAllViews();

            for (Question.Option subOption : subOptions) {
                RadioButton rb = new RadioButton(this);
                if (subOption.emoji != null && !subOption.emoji.isEmpty()) {
                    rb.setText(subOption.emoji + "  " + subOption.option);
                } else {
                    rb.setText(subOption.option);
                }
                rb.setId(View.generateViewId());
                rb.setTag(subOption.optionId);
                rb.setTextSize(14);
                rb.setButtonTintList(ColorStateList.valueOf(Color.parseColor(primaryColor)));
                rb.setTextColor(Color.BLACK);
                rb.setPadding(24, 24, 24, 24);
                binding.rgSubOptions.addView(rb);
            }

            if (selectedSubAnswers.containsKey(questionId)) {
                int savedSubId = selectedSubAnswers.get(questionId);
                for (int i = 0; i < binding.rgSubOptions.getChildCount(); i++) {
                    View child = binding.rgSubOptions.getChildAt(i);
                    if (child instanceof RadioButton) {
                        RadioButton rb = (RadioButton) child;
                        if (rb.getTag() != null && (int) rb.getTag() == savedSubId) {
                            binding.rgSubOptions.check(rb.getId());
                            break;
                        }
                    }
                }
            }

            binding.rgSubOptions.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId != -1) {
                    RadioButton selectedRb = binding.getRoot().findViewById(checkedId);
                    if (selectedRb != null && selectedRb.getTag() != null) {
                        int optionId = (int) selectedRb.getTag();
                        selectedSubAnswers.put(questionId, optionId);
                        binding.btnNext.setEnabled(true);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading sub-options", e);
        }
    }

    private void autoNavigateToNextQuestion() {
        isAutoNavigating = true;
        navigateToNextQuestion(); // instead of handler.postDelayed(...)
    }

    private void navigateToNextQuestion() {
        if (questionList == null) {
            Toast.makeText(this, "Questions not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentIndex < questionList.size() - 1) {
            currentIndex++;
            showQuestion(currentIndex);
            animateSlideIn();
        } else {
            // All questions answered - show loading dialog and submit
            submitAllAnswers();
        }
    }

    private void submitAllAnswers() {
        Log.d(TAG, "All questions answered, submitting answers...");

        try {
            List<SubmitAnswersRequest.Answer> answers = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : selectedAnswers.entrySet()) {
                answers.add(new SubmitAnswersRequest.Answer(entry.getKey(), entry.getValue()));
            }
            for (Map.Entry<Integer, Integer> entry : selectedSubAnswers.entrySet()) {
                answers.add(new SubmitAnswersRequest.Answer(entry.getKey(), entry.getValue()));
            }

            submitAnswersRequest = new SubmitAnswersRequest(userId, sessionId, answers);
            isAnswersSubmitted = true;

            // Show loading dialog
            showLoadingDialog("Processing Results", "Please wait while we analyze your answers and process the image...");

            // Check if AI processing is already completed
            if (isAiProcessingCompleted) {
                Log.d(TAG, "AI processing already completed, submitting answers...");
                submitSymptomsAnswers(woundAnalysis, submitAnswersRequest, token);
            } else {
                Log.d(TAG, "Waiting for AI processing to complete...");
                // Wait for AI processing to complete or timeout
            }

        } catch (Exception e) {
            Log.e(TAG, "Error submitting answers", e);
            hideLoadingDialog();
        }
    }

    private void submitSymptomsAnswers(WoundAnalysis result, SubmitAnswersRequest request, String token) {
        if (isSubmittingAnswers.get()) {
            Log.d(TAG, "Already submitting answers, skipping duplicate call");
            return;
        }

        isSubmittingAnswers.set(true);
        updateLoadingMessage("Submitting your answers...");

        repository.setGetCommonAPIDetails(new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody apiArrayResponse) {
                isSubmittingAnswers.set(false);
                try {
                    if (apiArrayResponse != null) {
                        Gson gson = new Gson();
                        String responseString = apiArrayResponse.string();
                        WoundScoreModel woundScoreModel = gson.fromJson(responseString, WoundScoreModel.class);
                        Log.d(TAG, "Answers submitted successfully. Wound score: " + woundScoreModel.getWoundScore());

                        updateLoadingMessage("Generating final report...");

                        // Show final results
                        handler.postDelayed(() -> {
                            hideLoadingDialog();
                            showFinalResults(result, woundScoreModel.getWoundScore());
                        }, 1500);

                    } else {
                        hideLoadingDialog();
                        showFinalResults(result, null);
                    }
                } catch (IOException | JsonSyntaxException e) {
                    Log.e(TAG, "Error parsing response", e);
                    hideLoadingDialog();
                    showFinalResults(result, null);
                }
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                isSubmittingAnswers.set(false);
                Log.e(TAG, "Answer submission failed: " + message);

                if ("Ai Model data not found.".equals(message)) {
                    updateLoadingMessage("Finalizing results...");
                    handler.postDelayed(() -> {
                        hideLoadingDialog();
                        showFinalResults(null, null);
                    }, 1500);
                } else {
                    hideLoadingDialog();
                    showFinalResults(result, null);
                }
            }

            @Override
            public void onProgressUpdate(int progress) {
                updateLoadingMessage("Processing... " + progress + "%");
            }
        });

        repository.submitAnswers(request, token);
    }

    private void processAIModelImageInBackground(String userId, String sessionId, String imagePath, String woundId, double lensFocalDistance, String token, String areaCoff, String pixelPerUnit) {
        if (isProcessingAI.get()) {
            Log.d(TAG, "AI Model processing already in progress");
            return;
        }

        isProcessingAI.set(true);
        Log.d(TAG, "Starting background AI image processing...");

        List<Double> emptyCoeffs = new ArrayList<>();
        AIModelProcessRequest request = new AIModelProcessRequest(userId, imagePath, sessionId, woundId, lensFocalDistance, convertToDoubleList(pixelPerUnit), convertToDoubleList(areaCoff));

        repository.processAIModelImage(request, token, new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody responseBody) {
                isProcessingAI.set(false);
                isAiProcessingCompleted = true;

                try {
                    if (responseBody != null) {
                        Gson gson = new Gson();
                        String responseString = responseBody.string();
                        woundAnalysis = gson.fromJson(responseString, WoundAnalysis.class);
                        Log.d(TAG, "Background AI processing completed: " + woundAnalysis.getAiModelData().getDisplayImagePath());

                        // If answers are already submitted, proceed with results
                        if (isAnswersSubmitted && loadingDialog != null && loadingDialog.isShowing()) {
                            Log.d(TAG, "AI completed after answers, submitting now...");
                            submitSymptomsAnswers(woundAnalysis, submitAnswersRequest, token);
                        }
                    }
                } catch (IOException | JsonSyntaxException e) {
                    Log.e(TAG, "Error parsing AI response in background", e);
                    handleBackgroundAIError("AI analysis completed with issues");
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error in background AI processing", e);
                    handleBackgroundAIError("AI processing encountered an error");
                }
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                isProcessingAI.set(false);
                isAiProcessingCompleted = true;
                Log.e(TAG, "Background AI processing failed: " + message);

                // If answers are submitted and we're waiting for AI, proceed anyway
                if (isAnswersSubmitted && loadingDialog != null && loadingDialog.isShowing()) {
                    Log.w(TAG, "AI failed but answers submitted, proceeding...");
                    submitSymptomsAnswers(null, submitAnswersRequest, token);
                }
            }

            @Override
            public void onProgressUpdate(int progress) {
                Log.d(TAG, "Background AI processing progress: " + progress + "%");
                // Don't update UI since this is background processing
            }
        });
    }
    public static List<Double> convertToDoubleList(String input) {
        if (input == null || input.isEmpty()) {
            return List.of(); // Return an empty list if input is null or empty
        }

        // Clean up the string by removing square brackets if they exist
        input = input.replace("[", "").replace("]", "");

        // Now split the string and convert to a list of doubles
        return Arrays.stream(input.split(","))
                .map(String::trim) // Remove any leading/trailing spaces
                .map(Double::parseDouble) // Convert each element to Double
                .collect(Collectors.toList());
    }

    private void handleBackgroundAIError(String errorMessage) {
        Log.w(TAG, errorMessage);
        // Don't show Toast for background errors to avoid interrupting user
    }

    private void showFinalResults(WoundAnalysis result, String woundScore) {
        try {
            binding.symptomQuestionLayout.setVisibility(View.GONE);
            binding.modelResultLayout.setVisibility(View.VISIBLE);
            binding.resultMaterialToolbar.setBackgroundColor(Color.parseColor(primaryColor));
            binding.resultAppBarLayout.setBackgroundColor(Color.parseColor(primaryColor));
            binding.btnFinish.setBackgroundColor(Color.parseColor(primaryColor));

            if (result != null && result.getAiModelData() != null) {
                displayAIResults(result);
                // Create a data class first
                ArrayList<AnalysisImage> imageList = new ArrayList<>();
                imageList.add(new AnalysisImage("Cropped Image", result.getAiModelData().getCroppedImagePath()));
                imageList.add(new AnalysisImage("Wound & Peri-wound Tissue", result.getAiModelData().getWoundPeriwoundOverlayImagePath()));
                imageList.add(new AnalysisImage("Wound Tissue ", result.getAiModelData().getWoundTissueOverlayImagePath()));
                imageList.add(new AnalysisImage("Peri-wound Tissue", result.getAiModelData().getPeriWoundTissueOverlayImagePath()));

                binding.btnFinish.setVisibility(View.VISIBLE);
                LinearLayoutManager layoutManager = new LinearLayoutManager(SymptomQuestionActivity.this, LinearLayoutManager.HORIZONTAL, false);
                binding.processedImageRecyclerview.setLayoutManager(layoutManager);
                binding.processedImageRecyclerview.scrollToPosition(0);
                ImageAdapter imageAdapter = new ImageAdapter(SymptomQuestionActivity.this, imageList);
                binding.processedImageRecyclerview.setAdapter(imageAdapter);
                imageAdapter.notifyDataSetChanged();
                if (woundScore != null) {

                    binding.woundScoreCard.setVisibility(View.VISIBLE);
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
                    binding.riskLevelTextview.setTextColor(textColor);
                    binding.woundScoreCard.setCardBackgroundColor(backgroundColor);
                }



                binding.btnFinish.setOnClickListener(v -> {
                    Log.d(TAG, "Finish button clicked - returning success result");
                    returnResultSuccess();
                });
            } else {
                showErrorState();
            }

            // Display wound score if available
            if (woundScore != null) {
                // You can add wound score display logic here
                Log.d(TAG, "Wound Score: " + woundScore);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing final results", e);
            Toast.makeText(this, "Error displaying results", Toast.LENGTH_SHORT).show();
        }
    }

    // NEW METHOD: Properly return success result
    private void returnResultSuccess() {
        Log.d(TAG, "=== PREPARING TO RETURN SUCCESS RESULT ===");
        isResultSet = true; // Set this first to prevent onDestroy from overriding our result

        try {
            Log.d(TAG, "=== RETURNING SUCCESS RESULT ===");
            Log.d(TAG, "sessionId: " + sessionId);
            Log.d(TAG, "userId: " + userId);
            Log.d(TAG, "woundId: " + woundId);
            Log.d(TAG, "token: " + (token != null ? "exists" : "null"));
            Log.d(TAG, "isFinishing: " + isFinishing() + ", isDestroyed: " + isDestroyed());

            // Create the result intent with all the data
            Intent resultIntent = new Intent();
            resultIntent.putExtra("sessionId", sessionId);
            resultIntent.putExtra("userId", userId);
            resultIntent.putExtra("woundId", woundId);
            resultIntent.putExtra("token", token);

            // Check if we're using direct callback
            boolean useDirectCallback = getIntent().getBooleanExtra("useDirectCallback", false);

            if (useDirectCallback) {
                Log.d(TAG, "Using direct callback - setting result and finishing");
                // Set the result and finish
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                // Traditional approach for backward compatibility
                setResult(RESULT_OK, resultIntent);
                Log.d(TAG, "Result set with RESULT_OK, finishing activity");
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error returning success result", e);
            isResultSet = true; // Mark as result set even on error
            setResult(RESULT_CANCELED);
            finish();
        }
    }    private void displayAIResults(WoundAnalysis woundAnalysis) {
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


    private void loadResultImages(WoundAnalysis aiData) {
        try {
            // Load original image
            if (aiData.getImageUrl() != null && !aiData.getImageUrl().isEmpty()) {
                Glide.with(this).load(aiData.getImageUrl()).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.capturedImage);
            }

            // Load bounding box/image with wound localization
            if (aiData.getAiModelData().getDisplayImagePath() != null && !aiData.getAiModelData().getDisplayImagePath().isEmpty()) {
                Glide.with(this).load(aiData.getAiModelData().getDisplayImagePath()).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.boundingImage);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading result images", e);
        }
    }

    private void displayWoundTissueAnalysis(PieChartView pieChartView, float sloughPercent, float escharPercent, float granulationPercent, float woundTissueNormalPercent) {
        ArrayList<PieHelper> pieHelperArrayList = new ArrayList<>();

        if (woundTissueNormalPercent > 0) {
            binding.woundtissueNormalRelativeLayout.setVisibility(View.VISIBLE);
            binding.woundTissueNormalTxt.setText(woundTissueNormalPercent + "%");
            pieHelperArrayList.add(new PieHelper(woundTissueNormalPercent, SymptomQuestionActivity.this.getColor(R.color.other_color)));
        }

        if (granulationPercent > 0) {
            binding.granulationTxt.setText(granulationPercent + "%");
            binding.granulationRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(granulationPercent, SymptomQuestionActivity.this.getColor(R.color.granulation_color)));
        }

        if (sloughPercent > 0) {
            binding.sloughTxt.setText(sloughPercent + "%");
            binding.sloudhRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(sloughPercent, SymptomQuestionActivity.this.getColor(R.color.slough_color)));
        }

        if (escharPercent > 0) {
            binding.escharTxt.setText(escharPercent + "%");
            binding.escharRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(escharPercent, SymptomQuestionActivity.this.getColor(R.color.eschar_color)));
        }

        pieChartView.setDate(pieHelperArrayList);
    }

    private void displayPeriWoundAnalysis(PieChartView pieChartView, float callusPercent, float erythemaPercent, float macerationPercent, float periWoundNormalPercent) {
        ArrayList<PieHelper> pieHelperArrayList = new ArrayList<PieHelper>();

        if (periWoundNormalPercent > 0) {
            binding.normalperiWoundTxt.setText(periWoundNormalPercent + "%");
            binding.periWoundRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(periWoundNormalPercent, SymptomQuestionActivity.this.getColor(R.color.other_color)));
        }

        if (callusPercent > 0) {
            binding.callusTxt.setText(callusPercent + "%");
            binding.callusRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(callusPercent, SymptomQuestionActivity.this.getColor(R.color.callus_color)));
        }

        if (erythemaPercent > 0) {
            binding.erythemaTxt.setText(erythemaPercent + "%");
            binding.erythemaRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(erythemaPercent, SymptomQuestionActivity.this.getColor(R.color.erythema_color)));
        }

        if (macerationPercent > 0) {
            binding.macerationTxt.setText(macerationPercent + "%");
            binding.macerationRelativeLayout.setVisibility(View.VISIBLE);
            pieHelperArrayList.add(new PieHelper(macerationPercent, SymptomQuestionActivity.this.getColor(R.color.maceration_color)));
        }

        pieChartView.setDate(pieHelperArrayList);
    }

    private void displayWoundMeasurements(AiModelData aiData) {
        try {
            // Check if wound area is detected
            if (aiData.getWoundArea() > 0) {
                binding.woundAreaLinearLayout.setVisibility(View.VISIBLE);
                // Display measurements
                binding.woundAreaTxt.setTextColor(Color.parseColor(primaryColor));
                binding.woundWidthTxt.setTextColor(Color.parseColor(primaryColor));
                binding.woundLengthTxt.setTextColor(Color.parseColor(primaryColor));
                binding.woundAreaTxt.setText(String.format("%.2f cm²", aiData.getWoundArea()));
                binding.woundWidthTxt.setText(String.format("%.2f cm", aiData.getWoundWidth()));
                binding.woundLengthTxt.setText(String.format("%.2f cm", aiData.getWoundLength()));
            } else {
                // No wound detected
                binding.woundAreaLinearLayout.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error displaying wound measurements", e);
        }
    }

    private void showErrorState() {
        try {
            binding.btnFinish.setVisibility(View.GONE);
            binding.symptomQuestionLayout.setVisibility(View.GONE);
            binding.modelResultLayout.setVisibility(View.VISIBLE);
            binding.noWoundDetectedLayout.setVisibility(View.VISIBLE);
            binding.woundDetectedLayoutLayout.setVisibility(View.GONE);
            binding.btnRecapture.setBackgroundColor(Color.parseColor(primaryColor));
            binding.btnSkip.setStrokeColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnSkip.setTextColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnSkip.setStrokeWidth(2);
            binding.btnRecapture.setOnClickListener(v -> {
                Intent i = new Intent(SymptomQuestionActivity.this, CameraActivity.class);
                i.putExtra("whereFrom", "imaging");
                i.putExtra("sessionId", sessionId);
                i.putExtra("userId", userId);
                i.putExtra("woundId", woundId);
                i.putExtra("token", token);
                i.putExtra("primaryColor", primaryColor);
                startActivity(i);
            });
            binding.btnSkip.setOnClickListener(v -> {
                Log.d(TAG, "Skip button clicked - returning success result");
                returnResultSuccess();
            });
            Glide.with(this).load(imageUrl).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.originalImage);

            // Show error message
            Toast.makeText(this, "Unable to load AI analysis results", Toast.LENGTH_LONG).show();

            // You can also show a specific error state in the UI
            binding.woundanalyticCard.setVisibility(View.GONE);

        } catch (Exception e) {
            Log.e(TAG, "Error showing error state", e);
        }
    }

    private void showLoadingDialog(String title, String message) {
        hideLoadingDialog();

        runOnUiThread(() -> {
            try {
                loadingDialog = new LoadingDialog(this, title, message, false);
                loadingDialog.show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing loading dialog", e);
            }
        });
    }

    private void updateLoadingMessage(String message) {
        runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.updateMessage(message);
            }
        });
    }

    private void hideLoadingDialog() {
        runOnUiThread(() -> {
            try {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error hiding loading dialog", e);
            }
        });
    }

    private void animateSlideIn() {
        try {
            binding.questionCard.setAlpha(0f);
            binding.questionCard.setTranslationX(1f);
            binding.questionCard.animate().alpha(1f).translationX(0f).setDuration(1).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in slide in animation", e);
        }
    }

    private void animateSlideOut() {
        try {
            binding.questionCard.setAlpha(1f);
            binding.questionCard.setTranslationX(0f);
            binding.questionCard.animate().alpha(0f).translationX(-1f).setDuration(1).withEndAction(() -> {
                binding.questionCard.setTranslationX(1f);
                animateSlideIn();
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in slide out animation", e);
        }
    }

    private void animateCardExpansion() {
        try {
            binding.layoutSubOptions.setScaleY(0f);
            binding.layoutSubOptions.animate().scaleY(1f).setDuration(1).start();
        } catch (Exception e) {
            Log.e(TAG, "Error in card expansion animation", e);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed - returning canceled result");
        isResultSet = true;
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        // If the activity is being destroyed without setting a result, set canceled
//        if (isFinishing() && !isResultSet) {
//            Log.d(TAG, "Activity finishing without result set - defaulting to CANCELED");
//            setResult(RESULT_CANCELED);
//        }

        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        hideLoadingDialog();
        isProcessingAI.set(false);
        isSubmittingAnswers.set(false);
        binding = null; // Clean up binding reference
    }
}