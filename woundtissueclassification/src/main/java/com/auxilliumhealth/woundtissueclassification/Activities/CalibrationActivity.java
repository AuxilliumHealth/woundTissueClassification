package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.auxilliumhealth.woundtissueclassification.Adapters.CalibrationAdapter;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivityCalibrationBinding;

public class CalibrationActivity extends AppCompatActivity {

    String sessionId, userId, woundId, token, primaryColor;
    private ActivityCalibrationBinding binding;
    String TAG = "CalibrationActivity";
    ActivityResultLauncher<Intent> calibrationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("woundId", woundId);
                            resultIntent.putExtra("sessionId", sessionId);
                            resultIntent.putExtra("userId", userId);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                    }
            );
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalibrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sessionId = getIntent().getStringExtra("sessionId");
        userId = getIntent().getStringExtra("userId");
        woundId = getIntent().getStringExtra("woundId");
        token = getIntent().getStringExtra("token");
        primaryColor = getIntent().getStringExtra("primaryColor");
        boolean woundScoreRequired = getIntent().getBooleanExtra("woundScoreRequired", true);
        boolean woundLocationRequired = getIntent().getBooleanExtra("woundLocationRequired", true);

        binding.appBarLayout.setBackgroundColor(Color.parseColor(primaryColor));

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        try {
            window.setStatusBarColor(Color.parseColor(primaryColor));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid color format: " + primaryColor);
        }

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        binding.toolbar.setBackgroundColor(Color.parseColor(primaryColor));

        Log.d(TAG, " sessionId: " + sessionId + " userId: " + userId + " woundId: " + woundId + " token: " + token + " primaryColor: " + primaryColor);

        CalibrationAdapter adapter = new CalibrationAdapter(this, binding.viewPager, sessionId, userId, woundId, token, primaryColor, woundScoreRequired);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setCurrentItem(0);
        binding.viewPager.setUserInputEnabled(false);

        binding.backImg.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem > 0) { // Check if not on the first fragment
                binding.viewPager.setCurrentItem(currentItem - 1);
            } else {
                finish();
            }
        });
        binding.skipTxt.setOnClickListener(v -> {
            Intent i = new Intent(CalibrationActivity.this, WoundLocationActivity.class);
            i.putExtra("whereFrom", "imaging");
            i.putExtra("userId", userId);
            i.putExtra("token", token);
            i.putExtra("woundId", woundId);
            i.putExtra("primaryColor", primaryColor);
            i.putExtra("woundScoreRequired", woundScoreRequired);
            i.putExtra("woundLocationRequired", woundLocationRequired);
            calibrationLauncher.launch(i);
            finish();



        });

    }
}
