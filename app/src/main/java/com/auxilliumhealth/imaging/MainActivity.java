package com.auxilliumhealth.imaging;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.auxilliumhealth.woundtissueclassification.woundtissueclassification;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // ✅ Register ActivityResultLauncher for receiving SDK results
    private final ActivityResultLauncher<Intent> woundTissueLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        Log.d(TAG, "onActivityResult: ResultCode = " + result.getResultCode());


    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialButton button = findViewById(R.id.wound_layout_button);
        MaterialButton previewButton = findViewById(R.id.wound_list_button);
        
        button.setOnClickListener(v -> launchWithCallback());
        previewButton.setOnClickListener(v -> launchPreviewWithCallback());
    }


    private void launchPreviewWithCallback() {
        woundtissueclassification.launchPreviewWoundList(this, "user_id", // userId (Mandatory)
                "wound_id",//woundId  (Mandatory)
                BuildConfig.SDK_TOKEN, // Protected token from local.properties
                "#2CA6CC"); //primaryColor (Mandatory)
    }

    private void launchWithCallback() {
        SwitchMaterial switchRiskScore = findViewById(R.id.switch_risk_score);
        SwitchMaterial switchBodySelection = findViewById(R.id.switch_body_selection);
        SwitchMaterial switchCalibration = findViewById(R.id.switch_calibration);

        boolean riskScoreRequired = switchRiskScore.isChecked();
        boolean bodySelectionRequired = switchBodySelection.isChecked();
        boolean calibrationRequired = switchCalibration.isChecked();
        woundtissueclassification.woundtissueclassificationWithLauncher(woundTissueLauncher, this,
                "user_id", // userId
                "wound_id", // woundId (optional)
                BuildConfig.SDK_TOKEN, // Protected token from local.properties
                "#2CA6CC", // primary color
                riskScoreRequired, //riskScore isRequired
                bodySelectionRequired, //body selection isRequired
                calibrationRequired //calibration isRequired
        );
    }




}
