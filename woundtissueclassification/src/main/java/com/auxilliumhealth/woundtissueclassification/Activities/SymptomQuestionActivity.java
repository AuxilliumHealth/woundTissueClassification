package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
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
import com.auxilliumhealth.woundtissueclassification.Utils.RootActivity;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.auxilliumhealth.woundtissueclassification.Network.ApiClient;
import com.auxilliumhealth.woundtissueclassification.Network.ApiService;
import com.auxilliumhealth.woundtissueclassification.Model.EditWoundMeasurementsRequest;
import com.auxilliumhealth.woundtissueclassification.Utils.StereoCameraDetector;
import com.auxilliumhealth.woundtissueclassification.Utils.WoundDepthProcessor;
import android.graphics.BitmapFactory;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class SymptomQuestionActivity extends RootActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<Integer, Integer> selectedAnswers = new HashMap<>();
    private final Map<Integer, Integer> selectedSubAnswers = new HashMap<>();
    private final AtomicBoolean isProcessingAI = new AtomicBoolean(false);
    private final AtomicBoolean isSubmittingAnswers = new AtomicBoolean(false);

    String imageUrl, lensFocusDistance, woundId, sessionId, coinType, whereFrom, userId, token, woundLocation, imageRotationDeg, headDirection;
    String TAG = "SymptomQuestionActivity";
    String primaryColor;
    SubmitAnswersRequest submitAnswersRequest;
    WoundAnalysis woundAnalysis;
    List<Double> currentLassoCoords = null; // Stores lasso coords for head-direction reprocessing
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
    private boolean woundScoreRequired = true;
    // Add this flag to track if result was set
    private boolean isResultSet = false;
    private static final int LASSO_MARK_REQUEST_CODE = 1002;

    // Edited measurement values from WoundImageEditActivity — survive showFinalResults override
    private double editedLengthCm  = 0;
    private double editedWidthCm   = 0;
    private double editedAreaVal   = 0;
    private double editedDepthVal  = 0;
    private double localDepthMm    = 0;
    private String leftFilePath    = null;
    private String rightFilePath   = null;
    private String editedImgPath   = null;
    private double baselineCm      = 0;

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
            setupButtonListeners();

            // Start AI processing in background immediately
            startBackgroundAIProcessing();

            // Choose flow based on woundScoreRequired
            if (woundScoreRequired) {
                // Show symptom questions flow
                Log.d(TAG, "Starting symptom questions flow (woundScoreRequired = true)");
                loadQuestions();
            } else {
                // Show direct AI results flow
                Log.d(TAG, "Starting direct AI results flow (woundScoreRequired = false)");
                showDirectAIResults();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
//            Toast.makeText(this, "App initialization failed", Toast.LENGTH_SHORT).show();
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
            woundLocation = getIntent().getStringExtra("woundLocation");
            userId = getIntent().getStringExtra("userId");
            token = getIntent().getStringExtra("token");
            coinType = getIntent().getStringExtra("coinType");
            whereFrom = getIntent().getStringExtra("whereFrom");
            woundScoreRequired = getIntent().getBooleanExtra("woundScoreRequired", true);
            imageRotationDeg = getIntent().getStringExtra("imageRotationDeg");
            if (imageRotationDeg == null) imageRotationDeg = "0.0";

            headDirection = getIntent().getStringExtra("headDirection");
            if (headDirection == null) headDirection = "TOP"; // Default fallback

            localDepthMm = getIntent().getDoubleExtra("localDepth", 0.0);
            baselineCm = getIntent().getDoubleExtra("baselineCm", 0.0);
            leftFilePath = getIntent().getStringExtra("leftFilePath");
            rightFilePath = getIntent().getStringExtra("rightFilePath");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(Color.parseColor(primaryColor));
            }

            binding.materialToolbar.setNavigationOnClickListener(v -> onBackPressed());
            binding.resultMaterialToolbar.setNavigationOnClickListener(v -> onBackPressed());

            Log.d(TAG, "sessionId: " + sessionId + " userId: " + userId + " woundId: " + woundId);

            Log.d(TAG, "woundScoreRequired: " + woundScoreRequired);

            setupUIWithPrimaryColor();

        } catch (Exception e) {
            Log.e(TAG, "Error in initViews", e);
            throw new RuntimeException("View initialization failed", e);
        }
    }

    private void showDirectAIResults() {
        Log.d(TAG, "Showing direct AI results (woundScoreRequired = false)");

        // Hide symptom question layout
        binding.symptomQuestionLayout.setVisibility(View.GONE);

        // Show loading immediately
        showLoadingDialog("Analyzing Image", "Processing wound image...");

        // Set up a timeout for AI processing
        handler.postDelayed(() -> {
            if (!isAiProcessingCompleted) {
                Log.w(TAG, "AI processing timeout - proceeding with available data");
                hideLoadingDialog();
                showFinalResults(woundAnalysis, null);
            }
            // If AI completes, it will automatically show results via the callback
        }, 60000); // 60 second timeout
    }

    private void setupUIWithPrimaryColor() {
        try {
            binding.btnPrev.setStrokeColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnPrev.setTextColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnNext.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnNext.setTextColor(ColorStateList.valueOf(Color.WHITE));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.view.Window window = getWindow();
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(Color.parseColor(primaryColor));
            }
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
//                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadQuestions() {
        if (token != null) {
            viewModel.loadQuestions(token);
        } else {
//            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startBackgroundAIProcessing() {
        if (imageUrl != null && woundId != null && token != null) {
            if (!isProcessingAI.get()) {
                Log.d(TAG, "Starting background AI processing...");
                String areaCoff = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_AREA_COEFFS);
                String pixelPerUnit = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_PIXEL_PER_UNIT);

                // Safely parse lensFocusDistance with fallback value
                double focusDistance = 0.0;
                if (lensFocusDistance != null && !lensFocusDistance.isEmpty()) {
                    try {
                        focusDistance = Double.parseDouble(lensFocusDistance);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid lensFocusDistance: " + lensFocusDistance + ", using default: 0.0", e);
                        focusDistance = 0.0;
                    }
                }

                processAIModelImageInBackground(userId, sessionId, imageUrl, woundId, focusDistance, token, areaCoff, pixelPerUnit, currentLassoCoords, imageRotationDeg, leftFilePath, rightFilePath, baselineCm);
            }
        } else {
            Log.w(TAG, "Missing required data for AI processing - imageUrl: " + (imageUrl != null) + ", woundId: " + (woundId != null) + ", token: " + (token != null));
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
//                Toast.makeText(this, "Question data not available", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!selectedAnswers.containsKey(questionList.get(currentIndex).questionId)) {
//                Toast.makeText(this, "Please select an option to continue", Toast.LENGTH_SHORT).show();
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
//                Toast.makeText(this, "Please select a sub-option to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            navigateToNextQuestion();
        });

        binding.btnLasso.setOnClickListener(v -> {
            Log.d(TAG, "Manual/Lasso button clicked - launching LassoActivity");
            if (imageUrl != null) {
                Intent intent = new Intent(this, LassoActivity.class);
                intent.putExtra("imagePath", imageUrl);
                intent.putExtra("whereFrom", "Wound");
                intent.putExtra("primaryColor", primaryColor);
                intent.putExtra("token", token);
                intent.putExtra("userId", userId);
                intent.putExtra("sessionId", sessionId);
                intent.putExtra("woundId", woundId);
                intent.putExtra("lensFocusDistance", lensFocusDistance);
                intent.putExtra("coinType", coinType);
                intent.putExtra("woundScoreRequired", woundScoreRequired);

                startActivityForResult(intent, LASSO_MARK_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Original image not available", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnMarkReprocess.setOnClickListener(v -> {
            Log.d(TAG, "Mark Reprocess button clicked - launching LassoActivity");
            if (imageUrl != null) {
                Intent intent = new Intent(this, LassoActivity.class);
                intent.putExtra("imagePath", imageUrl);
                intent.putExtra("whereFrom", "Wound");
                intent.putExtra("primaryColor", primaryColor);
                intent.putExtra("token", token);
                intent.putExtra("userId", userId);
                intent.putExtra("sessionId", sessionId);
                intent.putExtra("woundId", woundId);
                intent.putExtra("lensFocusDistance", lensFocusDistance);
                intent.putExtra("coinType", coinType);
                intent.putExtra("woundScoreRequired", woundScoreRequired);

                startActivityForResult(intent, LASSO_MARK_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Original image not available", Toast.LENGTH_SHORT).show();
            }
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
//            Toast.makeText(this, "Error loading question", Toast.LENGTH_SHORT).show();
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
//                        Toast.makeText(SymptomQuestionActivity.this, "Image failed to load", Toast.LENGTH_SHORT).show();
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
//            Toast.makeText(this, "Questions not loaded", Toast.LENGTH_SHORT).show();
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

    private void processAIModelImageInBackground(String userId, String sessionId, String imagePath, String woundId, double lensFocalDistance, String token, String areaCoff, String pixelPerUnit, List<Double> lassoCoordinates, String imageRotationDeg, String leftImagePath, String rightImagePath, double baselineCmValue) {
        if (isProcessingAI.get()) {
            Log.d(TAG, "AI Model processing already in progress");
            return;
        }
        woundAnalysis=null;
        isProcessingAI.set(true);
        Log.d(TAG, "Starting background AI image processing...");

        List<Double> emptyCoeffs = new ArrayList<>();
        AIModelProcessRequest request = new AIModelProcessRequest(userId, imagePath, sessionId, woundId, lensFocalDistance, 
                convertToDoubleList(pixelPerUnit), convertToDoubleList(areaCoff), lassoCoordinates, woundLocation, 
                imageRotationDeg, headDirection, leftImagePath, rightImagePath, baselineCmValue);
        Log.d(TAG, "headDirection: "+headDirection);
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

                        // Handle different flows based on woundScoreRequired
                        if (!woundScoreRequired) {
                            // Direct AI results flow — open edit screen first
                            runOnUiThread(() -> {
                                hideLoadingDialog();
//                                if (woundAnalysis.getAiModelData() != null) {
//                                    showWoundImageEditScreen(woundAnalysis.getAiModelData());
//                                } else {
                                    showFinalResults(woundAnalysis, null);
//                                }
                            });
                        } else {
                            // Symptom questions flow - if answers are already submitted, proceed with results
                            if (isAnswersSubmitted && loadingDialog != null && loadingDialog.isShowing()) {
                                Log.d(TAG, "AI completed after answers, submitting now...");
                                submitSymptomsAnswers(woundAnalysis, submitAnswersRequest, token);
                            }
                        }
                    }
                } catch (IOException | JsonSyntaxException e) {
                    Log.e(TAG, "Error parsing AI response in background", e);
                    handleBackgroundAIError("AI analysis completed with issues");
                    if (!woundScoreRequired) {
                        runOnUiThread(() -> {
                            hideLoadingDialog();
                            showFinalResults(null, null);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Unexpected error in background AI processing", e);
                    handleBackgroundAIError("AI processing encountered an error");
                    if (!woundScoreRequired) {
                        runOnUiThread(() -> {
                            hideLoadingDialog();
                            showFinalResults(null, null);
                        });
                    }
                }
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                isProcessingAI.set(false);
                isAiProcessingCompleted = true;
                Log.e(TAG, "Background AI processing failed: " + message);

                if (!woundScoreRequired) {
                    // Direct AI results flow - show results even if AI failed
                    runOnUiThread(() -> {
                        hideLoadingDialog();
                        showFinalResults(null, null);
                    });
                } else {
                    // Symptom questions flow - if answers are submitted and we're waiting for AI, proceed anyway
                    if (isAnswersSubmitted && loadingDialog != null && loadingDialog.isShowing()) {
                        Log.w(TAG, "AI failed but answers submitted, proceeding...");
                        submitSymptomsAnswers(null, submitAnswersRequest, token);
                    }
                }
            }

            @Override
            public void onProgressUpdate(int progress) {
                Log.d(TAG, "Background AI processing progress: " + progress + "%");
                if (!woundScoreRequired) {
                    updateLoadingMessage("Analyzing image... " + progress + "%");
                }
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

    /**
     * Called immediately after showFinalResults() to restore user-edited measurements
     * displayed in WoundImageEditActivity, which would otherwise be overwritten by displayWoundMeasurements().
     */
    private void applyEditedMeasurementsOverride() {
        try {
            if (editedLengthCm > 0)
                binding.woundLengthTxt.setText(String.format("%.2f cm", editedLengthCm));
            if (editedWidthCm > 0)
                binding.woundWidthTxt.setText(String.format("%.2f cm", editedWidthCm));
            if (editedAreaVal > 0)
                binding.woundAreaTxt.setText(String.format("%.2f cm²", editedAreaVal));
            if (editedDepthVal > 0)
                binding.woundDepthTxt.setText(String.format("%.2f mm", editedDepthVal));

            if (editedImgPath != null) {
                binding.woundMeasurementAxisCard.setVisibility(View.VISIBLE);
                binding.measurementDisclaimerTxt.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(new java.io.File(editedImgPath))
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.image_placeholder)
                        .into(binding.woundMeasurementAxisImg);
                final String fp = editedImgPath;
                binding.woundMeasurementAxisImg.setOnClickListener(v -> showFullScreenImage(fp));
            }
        } catch (Exception e) {
            Log.e(TAG, "applyEditedMeasurementsOverride error", e);
        }
    }

    private void showFinalResults(WoundAnalysis result, String woundScore) {
        try {
            // Hide appropriate layouts based on flow
            if (woundScoreRequired) {
                binding.symptomQuestionLayout.setVisibility(View.GONE);
            } else {
                // For direct AI flow, we might not have the symptom layout visible at all
                binding.symptomQuestionLayout.setVisibility(View.GONE);
            }

            binding.modelResultLayout.setVisibility(View.VISIBLE);
            binding.resultMaterialToolbar.setBackgroundColor(Color.parseColor(primaryColor));
            binding.resultAppBarLayout.setBackgroundColor(Color.parseColor(primaryColor));
            binding.btnFinish.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnEditMarkings.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.measurementsEditBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnMarkReprocess.setStrokeColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnMarkReprocess.setTextColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
            binding.btnMarkReprocess.setIconTint(ColorStateList.valueOf(Color.parseColor(primaryColor)));

            if (result != null && result.getAiModelData() != null) {
                binding.noWoundDetectedLayout.setVisibility(View.GONE);
                binding.woundDetectedLayoutLayout.setVisibility(View.VISIBLE);
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

                if (woundScore != null && woundScoreRequired) {
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
                    // Hide wound score card for direct AI flow
                    binding.woundScoreCard.setVisibility(View.GONE);
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
                Log.d(TAG, "Wound Score: " + woundScore);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing final results", e);
//            Toast.makeText(this, "Error displaying results", Toast.LENGTH_SHORT).show();
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
            resultIntent.putExtra("status", "success");
            resultIntent.putExtra("imageUrl", imageUrl);
            resultIntent.putExtra("coinType", coinType);
            resultIntent.putExtra("whereFrom", whereFrom);
            
            try {
                resultIntent.putExtra("woundArea", binding.woundAreaTxt.getText().toString().replace(" cm²", "").replace(" cm", "").trim());
                resultIntent.putExtra("woundWidth", binding.woundWidthTxt.getText().toString().replace(" cm²", "").replace(" cm", "").trim());
                resultIntent.putExtra("woundLength", binding.woundLengthTxt.getText().toString().replace(" cm²", "").replace(" cm", "").trim());
                resultIntent.putExtra("woundDepth", binding.woundDepthTxt.getText().toString().replace(" cm²", "").replace(" cm", "").trim());
            } catch (Exception e) {
                Log.e(TAG, "Error passing edited measurements", e);
            }

            // Note: WoundAnalysis object is not serializable, so we pass only the UI data
            // If you need the full analysis, retrieve it from server using sessionId

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
            Intent errorIntent = new Intent();
            errorIntent.putExtra("status", "error");
            errorIntent.putExtra("sessionId", sessionId);
            errorIntent.putExtra("userId", userId);
            errorIntent.putExtra("woundId", woundId);
            setResult(RESULT_CANCELED, errorIntent);
            finish();
        }
    }

    private void displayAIResults(WoundAnalysis woundAnalysis) {
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

            // If we have local stereo images, we can attempt a refined depth calculation
            if (leftFilePath != null && rightFilePath != null && (woundAnalysis.getAiModelData().getWoundDepth() == null || woundAnalysis.getAiModelData().getWoundDepth() <= 0)) {
                refineStereoDepthLocally();
            }

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
            if (aiData.getWoundArea() != null && aiData.getWoundArea() > 0) {
                binding.woundAreaLinearLayout.setVisibility(View.VISIBLE);

                String clockwisePath = aiData.getClockwiseMappingVisualizationImagePath();
                String overlayPath = aiData.getWoundMeasurementOverlayImagePath();
                String displayPath = null;

                if (clockwisePath != null && !clockwisePath.isEmpty()) {
                    displayPath = clockwisePath;
                } else if (overlayPath != null && !overlayPath.isEmpty()) {
                    displayPath = overlayPath;
                }

                if (displayPath != null) {
                    binding.woundMeasurementAxisCard.setVisibility(View.VISIBLE);
                    binding.measurementDisclaimerTxt.setVisibility(View.VISIBLE);
                    Glide.with(this).load(displayPath).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.woundMeasurementAxisImg);
                    
                    final String finalPath = displayPath;
                    binding.woundMeasurementAxisImg.setOnClickListener(v -> showFullScreenImage(finalPath));
                } else {
                    binding.woundMeasurementAxisCard.setVisibility(View.GONE);
                    binding.measurementDisclaimerTxt.setVisibility(View.GONE);
                }

                // Display measurements
                binding.woundAreaTxt.setText(String.format("%.2f cm²", aiData.getWoundArea()));
                
                if (aiData.getWoundWidth() == null || aiData.getWoundWidth() <= 0) {
                    binding.woundWidthTxt.setText("-");
                } else {
                    binding.woundWidthTxt.setText(String.format("%.2f cm", aiData.getWoundWidth()));
                }
                
                if (aiData.getWoundLength() == null || aiData.getWoundLength() <= 0) {
                    binding.woundLengthTxt.setText("-");
                } else {
                    binding.woundLengthTxt.setText(String.format("%.2f cm", aiData.getWoundLength()));
                }
                
                if (aiData.getWoundDepth() == null || aiData.getWoundDepth() <= 0) {
                    if (localDepthMm > 0) {
                        binding.woundDepthTxt.setText(String.format("%.2f mm", localDepthMm));
                    } else {
                        binding.woundDepthTxt.setText("-");
                    }
                } else {
                    binding.woundDepthTxt.setText(String.format("%.2f mm", aiData.getWoundDepth()));
                }

                binding.measurementsEditBtn.setOnClickListener(v -> showEditDimensionsBottomSheet());
                binding.btnEditMarkings.setOnClickListener(v -> showWoundImageEditScreen(aiData));

            } else {
                // No wound detected
                binding.woundAreaLinearLayout.setVisibility(View.GONE);
                binding.woundMeasurementAxisCard.setVisibility(View.GONE); 
                binding.measurementDisclaimerTxt.setVisibility(View.GONE);
            }


        } catch (Exception e) {
            Log.e(TAG, "Error displaying wound measurements", e);
        }
    }

    private static final int REQ_WOUND_EDIT = WoundImageEditActivity.REQUEST_CODE;

    private void showWoundImageEditScreen(AiModelData aiData) {
        if (aiData.getCroppedImagePath() != null && aiData.getCropped_Grabcut_mask() != null&& aiData.getWoundArea()!=0 && aiData.getWoundLength()!=0) {
            String areaCoeffs   = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_AREA_COEFFS);
            String pixelPerUnit = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_PIXEL_PER_UNIT);

            Intent intent = new Intent(this, WoundImageEditActivity.class);
            intent.putExtra("croppedImagePath",   aiData.getCroppedImagePath());
            intent.putExtra("croppedGrabcutMask", aiData.getCropped_Grabcut_mask());
            intent.putExtra("primaryColor",       primaryColor);
            intent.putExtra("areaCoeffs",         areaCoeffs);
            intent.putExtra("pixelPerUnit",       pixelPerUnit);
            intent.putExtra("area",            aiData.getWoundArea());
            intent.putExtra("depth",           aiData.getWoundDepth());
            intent.putExtra("lensFocusDistance",  lensFocusDistance != null ? lensFocusDistance : "0");
            startActivityForResult(intent, REQ_WOUND_EDIT);
        } else {
            showFinalResults(woundAnalysis, null);
        }
    }


    private void showEditDimensionsBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_dimensions, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        com.google.android.material.textfield.TextInputEditText editArea = bottomSheetView.findViewById(R.id.edit_area_input);
        com.google.android.material.textfield.TextInputEditText editWidth = bottomSheetView.findViewById(R.id.edit_width_input);
        com.google.android.material.textfield.TextInputEditText editLength = bottomSheetView.findViewById(R.id.edit_length_input);
        com.google.android.material.textfield.TextInputEditText editDepth = bottomSheetView.findViewById(R.id.edit_depth_input);
        
        com.google.android.material.button.MaterialButton btnCancel = bottomSheetView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton btnSave = bottomSheetView.findViewById(R.id.btn_save);

        if (primaryColor != null) {
            try {
                int c = Color.parseColor(primaryColor);
                btnSave.setBackgroundTintList(ColorStateList.valueOf(c));
                btnCancel.setStrokeColor(ColorStateList.valueOf(c));
                btnCancel.setTextColor(ColorStateList.valueOf(c));
            } catch (Exception ignored) {}
        }

        // Pre-fill
        editArea.setText(binding.woundAreaTxt.getText().toString().replace(" cm²", "").replace("-", "").trim());
        editWidth.setText(binding.woundWidthTxt.getText().toString().replace(" cm", "").replace("-", "").trim());
        editLength.setText(binding.woundLengthTxt.getText().toString().replace(" cm", "").replace("-", "").trim());
        editDepth.setText(binding.woundDepthTxt.getText().toString().replace(" mm", "").replace("-", "").trim());

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String areaStr = editArea.getText().toString().trim();
            String widthStr = editWidth.getText().toString().trim();
            String lengthStr = editLength.getText().toString().trim();
            String depthStr = editDepth.getText().toString().trim();

            Double area = areaStr.isEmpty() ? 0.0 : Double.parseDouble(areaStr);
            Double width = widthStr.isEmpty() ? 0.0 : Double.parseDouble(widthStr);
            Double length = lengthStr.isEmpty() ? 0.0 : Double.parseDouble(lengthStr);
            Double depth = depthStr.isEmpty() ? 0.0 : Double.parseDouble(depthStr);

            if (loadingDialog == null) {
                loadingDialog = new LoadingDialog(SymptomQuestionActivity.this);
            }
            loadingDialog.show();
            loadingDialog.updateTitle("Please Wait");
            loadingDialog.updateMessage("Updating measurements...");

            EditWoundMeasurementsRequest request = new EditWoundMeasurementsRequest(userId, sessionId, area, length, width, depth);
            repository.editWoundMeasurements(
                    request,
                    null,   // no axis image from the type-values bottom sheet
                    token,
                    new Repository.GetCommonAPIDataSuccessCallBack() {
                @Override
                public void getCommonAPIDataSuccess(ResponseBody models) {
                    loadingDialog.dismiss();
                    if (!areaStr.isEmpty()) binding.woundAreaTxt.setText(areaStr + " cm²");
                    if (!widthStr.isEmpty()) binding.woundWidthTxt.setText(widthStr + " cm");
                    if (!lengthStr.isEmpty()) binding.woundLengthTxt.setText(lengthStr + " cm");
                    if (!depthStr.isEmpty()) binding.woundDepthTxt.setText(depthStr + " mm");

                    bottomSheetDialog.dismiss();
                }

                @Override
                public void getCommonAPIDataFailure(String message) {
                    loadingDialog.dismiss();
                    String errorMsg = message != null && !message.isEmpty() ? message : "Error processing updating measurements";
                    Toast.makeText(SymptomQuestionActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProgressUpdate(int progress) {
                    // No implementation needed for progress here
                }
            });
        });

        bottomSheetDialog.show();
    }

    private void showErrorState() {
        try {
            binding.btnFinish.setVisibility(View.GONE);
            binding.symptomQuestionLayout.setVisibility(View.GONE);
            binding.modelResultLayout.setVisibility(View.VISIBLE);
            binding.noWoundDetectedLayout.setVisibility(View.VISIBLE);
            binding.woundDetectedLayoutLayout.setVisibility(View.GONE);
            binding.btnRecapture.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(primaryColor)));
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
                i.putExtra("woundScoreRequired", woundScoreRequired);
                startActivity(i);
                finish();
            });
            binding.btnSkip.setOnClickListener(v -> {
                Log.d(TAG, "Skip button clicked - returning success result");
                returnResultSuccess();
            });
            Glide.with(this).load(imageUrl).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.image_placeholder).error(R.drawable.image_error).into(binding.originalImage);
            binding.originalImage.setOnClickListener(v -> showFullScreenImage(imageUrl));


            // Show error message
//            Toast.makeText(this, "Unable to load AI analysis results", Toast.LENGTH_LONG).show();

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // ── Wound Image Edit result ───────────────────────────────────────
        if (requestCode == REQ_WOUND_EDIT) {
            if (resultCode == RESULT_OK && data != null) {
                double lengthCm = data.getDoubleExtra(WoundImageEditActivity.EXTRA_LENGTH_CM, 0);
                double widthCm  = data.getDoubleExtra(WoundImageEditActivity.EXTRA_WIDTH_CM,  0);
                String imgPath  = data.getStringExtra(WoundImageEditActivity.EXTRA_IMAGE_PATH);
                String area     = data.getStringExtra("area");
                String depth    = data.getStringExtra("depth");

                // Parse area/depth strings safely to Double
                double areaVal  = 0.0, depthVal = 0.0;
                try { if (area  != null && !area.isEmpty())  areaVal  = Double.parseDouble(area);  } catch (Exception ignored) {}
                try { if (depth != null && !depth.isEmpty()) depthVal = Double.parseDouble(depth); } catch (Exception ignored) {}

                // Save edited values as instance fields so they survive showFinalResults() rewrite
                editedLengthCm = lengthCm;
                editedWidthCm  = widthCm;
                editedAreaVal  = areaVal;
                editedDepthVal = depthVal;
                editedImgPath  = imgPath;

                // Call editWoundMeasurements API to persist new dimensions
                if (lengthCm > 0 || widthCm > 0) {
                    showLoadingDialog("Saving Measurements", "Updating wound dimensions...");

                    EditWoundMeasurementsRequest request = new EditWoundMeasurementsRequest(
                            userId, sessionId, areaVal, lengthCm, widthCm, depthVal);

                    repository.editWoundMeasurements(
                            request,
                            imgPath,   // axis image captured by WoundImageEditActivity
                            token,
                            new Repository.GetCommonAPIDataSuccessCallBack() {
                        @Override
                        public void getCommonAPIDataSuccess(ResponseBody models) {
                            Log.d(TAG, "Wound measurements updated: length=" + lengthCm + " width=" + widthCm);
                            runOnUiThread(() -> {
                                hideLoadingDialog();
                                showFinalResults(woundAnalysis, null);
                                applyEditedMeasurementsOverride();
                            });
                        }

                        @Override
                        public void getCommonAPIDataFailure(String message) {
                            Log.w(TAG, "editWoundMeasurements failed: " + message);
                            runOnUiThread(() -> {
                                hideLoadingDialog();
                                showFinalResults(woundAnalysis, null);
                                applyEditedMeasurementsOverride();
                            });
                        }

                        @Override
                        public void onProgressUpdate(int progress) {}
                    });
                } else {
                    showFinalResults(woundAnalysis, null);
                    applyEditedMeasurementsOverride();
                }
            } else {
                // User pressed back/cancelled - still show the results screen with original AI defaults
                showFinalResults(woundAnalysis, null);
            }
            return;
        }

        // ── Lasso result ──────────────────────────────────────────────────
        if (requestCode == LASSO_MARK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {

            String overlayPath = data.getStringExtra("overlayPath");
            String imagePath   = data.getStringExtra("imagePath");
            float[] coords     = data.getFloatArrayExtra("coordinates");

            Log.d(TAG, "LassoActivity returned. overlayPath=" + overlayPath + " imagePath=" + imagePath + " coords length=" + (coords != null ? coords.length : 0));

            lensFocusDistance = data.getStringExtra("lensFocusDistance");
            woundId           = data.getStringExtra("woundId");
            sessionId         = data.getStringExtra("sessionId");
            userId            = data.getStringExtra("userId");
            token             = data.getStringExtra("token");
            coinType          = data.getStringExtra("coinType");
            woundScoreRequired = data.getBooleanExtra("woundScoreRequired", true);

            // Convert float[] to List<Double>
            List<Double> lassoCoords = new ArrayList<>();
            if (coords != null) {
                for (float f : coords) {
                    lassoCoords.add((double) f);
                }
            }

            // Save lasso coords for future reprocessing
            currentLassoCoords = lassoCoords;
            
            // Clear previous manual measurement overrides before reprocessing
            editedLengthCm = 0;
            editedWidthCm = 0;
            editedAreaVal = 0;
            editedDepthVal = 0;
            editedImgPath = null;

            // Use imagePath as imageUrl if returned (S3 URL from lasso), else keep existing imageUrl
            if (imagePath != null && !imagePath.isEmpty()) {
                imageUrl = imagePath;
            }

            // Remove old results from view while reprocessing
            woundAnalysis = null;
            binding.modelResultLayout.setVisibility(View.GONE);
            binding.noWoundDetectedLayout.setVisibility(View.GONE);

            // Always reprocess with the coordinates — use imageUrl (S3 URL)
            String reprocessImageUrl = (imageUrl != null) ? imageUrl : imagePath;

            if (reprocessImageUrl == null) {
                Log.w(TAG, "LassoActivity: no image URL available, cannot reprocess");
                Toast.makeText(this, "No image available to reprocess", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading dialog
            showLoadingDialog("Analyzing Manual Mark", "Processing wound analysis for selected area...");

            // Trigger re-analysis with manual lasso coordinates
            String areaCoff      = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_AREA_COEFFS);
            String pixelPerUnit  = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_PIXEL_PER_UNIT);
            double focusDistance = 0.0;
            try {
                if (lensFocusDistance != null && !lensFocusDistance.isEmpty()) {
                    focusDistance = Double.parseDouble(lensFocusDistance);
                }
            } catch (Exception e) {
                Log.w(TAG, "Focus distance parse failed", e);
            }

            // Reset processing guard — lasso always triggers a fresh API call
            isProcessingAI.set(false);
            isAiProcessingCompleted = false;

            processAIModelImageInBackground(userId, sessionId, reprocessImageUrl, woundId, focusDistance, token, areaCoff, pixelPerUnit, currentLassoCoords, imageRotationDeg, leftFilePath, rightFilePath, baselineCm);
        }
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
        Intent resultIntent = new Intent();
        resultIntent.putExtra("sessionId", sessionId);
        resultIntent.putExtra("userId", userId);
        resultIntent.putExtra("woundId", woundId);
        resultIntent.putExtra("status", "cancelled");
        setResult(RESULT_CANCELED, resultIntent);
        super.onBackPressed();
    }

    private void refineStereoDepthLocally() {
        if (leftFilePath == null || rightFilePath == null) return;

        new Thread(() -> {
            try {
                Bitmap leftBmp = BitmapFactory.decodeFile(leftFilePath);
                Bitmap rightBmp = BitmapFactory.decodeFile(rightFilePath);

                if (leftBmp != null && rightBmp != null) {
                    StereoCameraDetector detector = new StereoCameraDetector(this);
                    double baseline = detector.getBaselineMm();
                    double focalLen = detector.getFocalLengthPx(leftBmp.getWidth());

                    WoundDepthProcessor depthProcessor = new WoundDepthProcessor(baseline, focalLen);
                    
                    // Re-calculate with local images (can add mask refinement later if we download mask)
                    WoundDepthProcessor.DepthResult result = depthProcessor.process(leftBmp, rightBmp, null);

                    Log.d(TAG, "Refined Stereo Depth: " + result.maxDepthMm + " mm");

                    runOnUiThread(() -> {
                        if (result.maxDepthMm > 0) {
                            localDepthMm = result.maxDepthMm;
                            binding.woundDepthTxt.setText(String.format("%.2f mm", localDepthMm));
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Refined depth calculation failed", e);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        // If the activity is being destroyed without setting a result, set canceled
        if (isFinishing() && !isResultSet) {
            Log.d(TAG, "Activity finishing without result set - defaulting to CANCELED with data");
            Intent resultIntent = new Intent();
            resultIntent.putExtra("sessionId", sessionId);
            resultIntent.putExtra("userId", userId);
            resultIntent.putExtra("woundId", woundId);
            resultIntent.putExtra("status", "destroyed");
            setResult(RESULT_CANCELED, resultIntent);
        }

        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        hideLoadingDialog();
        isProcessingAI.set(false);
        isSubmittingAnswers.set(false);
        binding = null; // Clean up binding reference
    }
}