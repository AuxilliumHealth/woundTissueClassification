package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.auxilliumhealth.woundtissueclassification.Utils.RootActivity;
import com.auxilliumhealth.woundtissueclassification.ViewModel.WoundLocationViewModel;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivityWoundLocationBinding;
import com.auxilliumhealth.woundtissueclassification.fragments.WoundLocationFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WoundLocationActivity extends RootActivity {

    String userId, token, primaryColor, woundLocation, woundId;
    String TAG = "WoundLocationActivity";
    Repository repository;
    private ActivityWoundLocationBinding binding;
    private WoundLocationViewModel viewModel;
    String sessionId;
    boolean woundScoreRequired;
    boolean woundLocationRequired;
    private final ActivityResultLauncher<Intent> woundSummeryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK ) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("woundId", woundId);
                    resultIntent.putExtra("sessionId", sessionId);
                    resultIntent.putExtra("userId", userId);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Log.w(TAG, "❌ No result returned or operation cancelled.");
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWoundLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new Repository(WoundLocationActivity.this);

        userId = getIntent().getStringExtra("userId");
        token = getIntent().getStringExtra("token");
         woundScoreRequired = getIntent().getBooleanExtra("woundScoreRequired", true);
         woundLocationRequired = getIntent().getBooleanExtra("woundLocationRequired", true);

        woundLocation = getIntent().getStringExtra("woundLocation");
        primaryColor = getIntent().getStringExtra("primaryColor");
        if (primaryColor == null || primaryColor.isEmpty()) {
            primaryColor = "#007AFF"; // Keep as string for parsing later
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.view.Window window = getWindow();
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(Color.parseColor(primaryColor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting status bar color", e);
            // Fallback to a safe color if primaryColor is invalid
            primaryColor = "#007AFF";
            getWindow().setStatusBarColor(Color.parseColor(primaryColor));
        }

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        woundId = getIntent().getStringExtra("woundId");
        viewModel = new ViewModelProvider(this).get(WoundLocationViewModel.class);
        observeViewModel();
        if (woundLocation == null) woundLocation = "";
        if (woundLocationRequired) {
            // Pass data to fragment using newInstance
            WoundLocationFragment fragment = new WoundLocationFragment();
            Bundle args = new Bundle();
            args.putString("frontBackBody", woundLocation); // default or dynamic value
            args.putString("userId", userId);
            args.putString("token", token);
            args.putString("woundId", woundId);
            args.putString("primaryColor", primaryColor);
            args.putBoolean("woundScoreRequired", woundScoreRequired);
            args.putBoolean("woundLocationRequired", woundLocationRequired);
            fragment.setArguments(args);

            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fm.beginTransaction();
            fragmentTransaction.replace(R.id.container, fragment);
            fragmentTransaction.commit();

            // Set up result handling for the fragment
            getSupportFragmentManager().setFragmentResultListener("wound_summary_result", this, (requestKey, result) -> {
                if (result.getBoolean("success", false)) {
                    // If we have a result from the fragment, pass it back to the parent activity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtras(result);
                    setResult(RESULT_OK, resultIntent);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
            });
        } else {
            sessionId = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(new Date());
            repository = new Repository(WoundLocationActivity.this);

            viewModel = new ViewModelProvider(this).get(WoundLocationViewModel.class);

            observeViewModel();
            updateWoundLocation(userId, woundId, "", "");
        }
    }

    private void updateWoundLocation(String userId, String woundId, String description, String woundLocation) {
        viewModel.updateWoundLocation(userId, description, woundId, woundLocation, token);
    }
    private void observeViewModel() {
        viewModel.getUpdateWoundLocationResponse().observe(WoundLocationActivity.this, response -> {
            hideLoader();
            if (response != null) {
                try {
                    String responseString = response.string();
                    Log.d("WoundLocationUpdate", "Success: " + responseString);

                    // Navigate to CameraActivity
                    Intent i = new Intent(WoundLocationActivity.this, CameraActivity.class);
                    i.putExtra("whereFrom", "woundImage");
                    i.putExtra("woundId", woundId);
                    i.putExtra("woundLocation", woundLocation);
                    i.putExtra("woundScoreRequired", woundScoreRequired);
                    i.putExtra("sessionId", sessionId);
                    i.putExtra("userId", userId);
                    i.putExtra("woundId", woundId);
                    i.putExtra("token", token);
                    i.putExtra("primaryColor", primaryColor);
                    i.putExtra("woundLocationRequired", woundLocationRequired);
                    // Start CameraActivity with startActivityForResult
                    woundSummeryLauncher.launch(i);
                    // Do not finish() here, wait for result in woundSummeryLauncher callback
                } catch (Exception e) {
                    e.printStackTrace();
                    finish();
                    Log.e(TAG, "Error processing response", e);
                }
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            hideLoader();
            if (error != null && !error.isEmpty()) {
                Log.e("WoundLocationUpdate", "Error: " + error);
                Toast.makeText(this, "Failed to save wound information. Please try again.", Toast.LENGTH_SHORT).show();
                // Optionally finish if the error is terminal
                // finish(); 
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                showLoader();
            } else {
                hideLoader();
            }
        });
    }

}