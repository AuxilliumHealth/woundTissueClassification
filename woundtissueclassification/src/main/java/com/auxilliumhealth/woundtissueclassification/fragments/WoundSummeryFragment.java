package com.auxilliumhealth.woundtissueclassification.fragments;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.auxilliumhealth.woundtissueclassification.Activities.CameraActivity;
import com.auxilliumhealth.woundtissueclassification.Activities.WoundLocationActivity;
import com.auxilliumhealth.woundtissueclassification.LocalDatabase.PreferencesHelper;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.auxilliumhealth.woundtissueclassification.Utils.CustomTextInputEditText;
import com.auxilliumhealth.woundtissueclassification.ViewModel.WoundLocationViewModel;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivityWoundSummeryBinding;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class WoundSummeryFragment extends Fragment implements View.OnClickListener {
    public static final int REQUEST_IMAGE = 100;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;
    public static String fileName;
    public static String sessionId;
    String upperLowerbody, frontBackBody, woundPartBody, partSideBody, userId, token;
    byte[] imageBitmap;
    String filePath;
    Repository repository;
    boolean woundScoreRequired;
    String woundId;
    String TAG = "WoundSummeryFragment";
    String primaryColor = "#000000";
    private WoundLocationViewModel viewModel;
    private String woundLocation;
    private ActivityWoundSummeryBinding binding;
    private final ActivityResultLauncher<Intent> symptomLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String returnedSessionId = data.getStringExtra("sessionId");
                    String returnedUserId = data.getStringExtra("userId");
                    String returnedWoundId = data.getStringExtra("woundId");
                    boolean returnedWoundScoreRequired = data.getBooleanExtra("woundScoreRequired", true);
                    String returnedToken = data.getStringExtra("token");

                    Log.d(TAG, "✅ Received data from SymptomQuestionActivity:");
                    Log.d(TAG, "sessionId: " + returnedSessionId);
                    Log.d(TAG, "userId: " + returnedUserId);
                    Log.d(TAG, "woundId: " + returnedWoundId);
                    Log.d(TAG, "token: " + returnedToken);

                    // TODO: Do something with the returned data (e.g. navigate or update UI)
                } else {
                    Log.w(TAG, "❌ No result returned or operation cancelled.");
                }
            });
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityWoundSummeryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionId = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(new Date());
        repository = new Repository(requireContext());

        viewModel = new ViewModelProvider(this).get(WoundLocationViewModel.class);
        observeViewModel();

        initView();
        setupUI();
        setupClickListeners();
    }

    private void initView() {
        try {

        } catch (Exception e) {
            Log.e(TAG, "Error initializing woundId", e);
        }

        // Get arguments from bundle
        Bundle args = getArguments();
        if (args != null) {
            upperLowerbody = args.getString("upperLowerbody");
            frontBackBody = args.getString("frontBackBody");
            woundPartBody = args.getString("woundPartBody");
            partSideBody = args.getString("partSideBody");
            woundScoreRequired = args.getBoolean("woundScoreRequired");
            woundId = args.getString("woundId");

            if (woundId == null || woundId.isEmpty()) {
                woundId = "1";
            }
            primaryColor = args.getString("primaryColor", "#000000");
            userId = args.getString("userId");
            token = args.getString("token");

            // Get image byte array
            imageBitmap = args.getByteArray("woundPartImg");
        }

        Log.d(TAG, " sessionId: " + sessionId + " userId: " + userId + " woundId: " + woundId + " token: " + token);

        binding.woundPartTxt.setText(woundPartBody + " (" + partSideBody + ")");

        // Set image if available
        if (imageBitmap != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBitmap, 0, imageBitmap.length);
                binding.woundPartImg.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error setting wound part image", e);
            }
        }
    }

    private void setupUI() {
        // Set colors
        binding.summeryTxt.setTextColor(Color.parseColor(primaryColor));
        binding.saveBtn.setBackgroundColor(Color.parseColor(primaryColor));

        // Set body position image and text
        if (frontBackBody != null) {
            switch (frontBackBody) {
                case "Back":
                    binding.bodyPositionImg.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.back));
                    binding.bodyPositionTxt.setText("Back");
                    break;
                case "Front":
                    binding.bodyPositionImg.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.front));
                    binding.bodyPositionTxt.setText("Front");
                    break;
                default:
                    break;
            }
        }

        setEditTextColor(binding.describtionEditText, primaryColor);
    }

    private void setupClickListeners() {
        binding.backImg.setOnClickListener(this);
        binding.saveBtn.setOnClickListener(this);

        binding.describtionEditText.setOnDrawableClickListener(new CustomTextInputEditText.OnDrawableClickListener() {
            @Override
            public void onClick() {
                startSpeechToText();
            }
        });
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech to text", e);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back_img) {
            // Navigate back
            requireActivity().onBackPressed();
        } else if (view.getId() == R.id.save_btn) {
            if (validateInputs()) {
                woundLocation = frontBackBody + "_" + upperLowerbody + "_" + woundPartBody + "_" + partSideBody;
                String description = binding.describtionEditText.getText().toString().trim();

                showLoader();
                updateWoundLocation(userId, woundId, description, woundLocation);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == getActivity().RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String recognizedText = result.get(0);
                    String currentText = binding.describtionEditText.getText().toString();
                    binding.describtionEditText.setText(currentText + " " + recognizedText);
                }
            }
        } else if (requestCode == REQUEST_IMAGE) {
            Log.d(TAG, "onActivityResult: Received result from CameraActivity, resultCode: " + resultCode);
            if (resultCode == getActivity().RESULT_OK) {
                if (data != null) {
                    Log.d(TAG, "onActivityResult: Data received from CameraActivity");
                    // Create a new bundle to pass the result back
                    Bundle result = new Bundle();
                    result.putBoolean("success", true);

                    // Copy all extras from the intent to the result bundle
                    if (data.getExtras() != null) {
                        result.putAll(data.getExtras());
                    }

                    // Set the fragment result
                    getParentFragmentManager().setFragmentResult("wound_summary_result", result);
                    Log.d(TAG, "onActivityResult: Fragment result set with data");
                } else {
                    Log.d(TAG, "onActivityResult: No data received from CameraActivity");
                    Bundle result = new Bundle();
                    result.putBoolean("success", false);
                    getParentFragmentManager().setFragmentResult("wound_summary_result", result);
                }
            } else {
                Log.d(TAG, "onActivityResult: CameraActivity result not OK");
                Bundle result = new Bundle();
                result.putBoolean("success", false);
                getParentFragmentManager().setFragmentResult("wound_summary_result", result);
            }
            // Finish the current activity to go back to MainActivity
            requireActivity().finish();
        }
    }

    private void setEditTextColor(CustomTextInputEditText editText, String colorString) {
        int color = Color.parseColor(colorString);

        // Change cursor color
        try {
            Field fEditor = TextView.class.getDeclaredField("mEditor");
            fEditor.setAccessible(true);
            Object editor = fEditor.get(editText);

            Field fCursorDrawable = editor.getClass().getDeclaredField("mCursorDrawable");
            fCursorDrawable.setAccessible(true);

            ShapeDrawable cursor = new ShapeDrawable();
            cursor.setIntrinsicWidth(4);
            cursor.setIntrinsicHeight(editText.getHeight());
            cursor.getPaint().setColor(color);

            Drawable[] drawables = {cursor, cursor};
            fCursorDrawable.set(editor, drawables);

        } catch (Exception e) {
            e.printStackTrace();
        }

        editText.setHighlightColor(color);

        // Change selection handle color for API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Field fEditor = TextView.class.getDeclaredField("mEditor");
                fEditor.setAccessible(true);
                Object editor = fEditor.get(editText);

                Field fSelectHandle = editor.getClass().getDeclaredField("mSelectHandleLeft");
                fSelectHandle.setAccessible(true);
                Drawable leftHandle = (Drawable) fSelectHandle.get(editor);
                if (leftHandle != null) {
                    leftHandle.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }

                Field fSelectHandleRight = editor.getClass().getDeclaredField("mSelectHandleRight");
                fSelectHandleRight.setAccessible(true);
                Drawable rightHandle = (Drawable) fSelectHandleRight.get(editor);
                if (rightHandle != null) {
                    rightHandle.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }

                Field fSelectHandleCenter = editor.getClass().getDeclaredField("mSelectHandleCenter");
                fSelectHandleCenter.setAccessible(true);
                Drawable centerHandle = (Drawable) fSelectHandleCenter.get(editor);
                if (centerHandle != null) {
                    centerHandle.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean validateInputs() {
        if (binding.describtionEditText.getText().toString().trim().isEmpty()) {
            Log.e(TAG, "Please enter description");
            binding.describtionEditText.setError("Please enter description");


            return false;
        }
        return true;
    }

    private void observeViewModel() {
        viewModel.getUpdateWoundLocationResponse().observe(getViewLifecycleOwner(), response -> {
            hideLoader();
            if (response != null) {
                try {
                    String responseString = response.string();
                    Log.d("WoundLocationUpdate", "Success: " + responseString);

                    // Navigate to CameraActivity
                    Intent i = new Intent(requireContext(), CameraActivity.class);
                    i.putExtra("whereFrom", "woundImage");
                    i.putExtra("woundId", woundId);
                    i.putExtra("woundLocation", woundLocation);
                    i.putExtra("woundScoreRequired", woundScoreRequired);
                    i.putExtra("sessionId", sessionId);
                    i.putExtra("userId", userId);
                    i.putExtra("woundId", woundId);
                    i.putExtra("token", token);
                    i.putExtra("primaryColor", primaryColor);

                    // Start CameraActivity with startActivityForResult
                    startActivityForResult(i, REQUEST_IMAGE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Error processing response", e);
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            hideLoader();
            if (error != null && !error.isEmpty()) {
                Log.e("WoundLocationUpdate", "Error: " + error);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoader();
            } else {
                hideLoader();
            }
        });
    }

    private void updateWoundLocation(String userId, String woundId, String description, String woundLocation) {
        viewModel.updateWoundLocation(userId, description, woundId, woundLocation, token);
    }

    /**
     * Show loading indicator
     */
    private void showLoader() {
        if (binding != null) {
            if (getActivity() instanceof WoundLocationActivity) {
                ((WoundLocationActivity) getActivity()).showLoader();
            }
        }
    }

    /**
     * Hide loading indicator
     */
    private void hideLoader() {
        if (binding != null) {
            if (getActivity() instanceof WoundLocationActivity) {
                ((WoundLocationActivity) getActivity()).hideLoader();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}