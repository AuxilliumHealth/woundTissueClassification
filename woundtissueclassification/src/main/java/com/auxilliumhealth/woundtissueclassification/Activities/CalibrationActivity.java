package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.auxilliumhealth.woundtissueclassification.Adapters.CalibrationAdapter;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivityCalibrationBinding;

public class CalibrationActivity extends AppCompatActivity {

    String sessionId, userId, woundId, token, primaryColor;
    private ActivityCalibrationBinding binding;
    String TAG = "CalibrationActivity";

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
        binding.appBarLayout.setBackgroundColor(Color.parseColor(primaryColor));
        // Java

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            try {
                window.setStatusBarColor(Color.parseColor(primaryColor));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid color format: " + primaryColor);
            }
        }

        // Optional: Set dark icons for light status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        binding.toolbar.setBackgroundColor(Color.parseColor(primaryColor));

        Log.d(TAG, " sessionId: " + sessionId + " userId: " + userId + " woundId: " + woundId + " token: " + token+ " primaryColor: " + primaryColor);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // dark icons
        }
        CalibrationAdapter adapter = new CalibrationAdapter(this, binding.viewPager, sessionId, userId, woundId, token,primaryColor);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setCurrentItem(0);
        binding.viewPager.setUserInputEnabled(false);

        binding.backImg.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem > 0) { // Check if not on the first fragment
                binding.viewPager.setCurrentItem(currentItem - 1);
            }else{
                finish();
            }
        });
        binding.skipTxt.setOnClickListener(v -> {
            Intent i = new Intent(CalibrationActivity.this, LatestWoundListActivity.class);
            i.putExtra("whereFrom", "imaging");
            i.putExtra("userId", userId);
            i.putExtra("token", token);
            i.putExtra("primaryColor", primaryColor);
            startActivity(i);
            finish();
        });

    }
}
