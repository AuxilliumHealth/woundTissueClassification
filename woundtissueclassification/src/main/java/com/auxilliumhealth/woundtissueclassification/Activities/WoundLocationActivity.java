package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.auxilliumhealth.woundtissueclassification.Utils.RootActivity;
import com.auxilliumhealth.woundtissueclassification.databinding.ActivityWoundLocationBinding;
import com.auxilliumhealth.woundtissueclassification.fragments.WoundLocationFragment;

public class WoundLocationActivity extends RootActivity {

    private static final int REQUEST_CODE_WOUND_SUMMARY = 1001;
    String userId, token, primaryColor, woundLocation, woundId;
    String TAG = "WoundLocationActivity";
    Repository repository;
    private ActivityWoundLocationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWoundLocationBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        repository = new Repository(WoundLocationActivity.this);

        userId = getIntent().getStringExtra("userId");
        token = getIntent().getStringExtra("token");
        boolean woundScoreRequired = getIntent().getBooleanExtra("woundScoreRequired", true);

        woundLocation = getIntent().getStringExtra("woundLocation");
        primaryColor = getIntent().getStringExtra("primaryColor");
        woundId = getIntent().getStringExtra("woundId");
        if (woundLocation == null) woundLocation = "";

        Log.d(TAG, " userId: " + userId + " token: " + token);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(primaryColor));
        }
        // Set status bar color


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // dark icons
        }

        // Pass data to fragment using newInstance
        WoundLocationFragment fragment = new WoundLocationFragment();
        Bundle args = new Bundle();
        args.putString("frontBackBody", woundLocation); // default or dynamic value
        args.putString("userId", userId);
        args.putString("token", token);
        args.putString("woundId", woundId);
        args.putString("primaryColor", primaryColor);
        args.putBoolean("woundScoreRequired", woundScoreRequired);
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
    }


}
