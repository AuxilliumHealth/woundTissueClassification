package com.auxilliumhealth.imaging;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

//import com.auxilliumhealth.woundtissueclassification.woundtissueclassification;
import com.auxilliumhealth.woundtissueclassification.woundtissueclassification;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // ✅ Register ActivityResultLauncher for receiving SDK results
    private final ActivityResultLauncher<Intent> woundTissueLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialButton button = findViewById(R.id.wound_layout_button);
        MaterialButton previewButton = findViewById(R.id.wound_list_button);
        button.setOnClickListener(v -> launchWithCallback());
        previewButton.setOnClickListener(v -> launchPreviewWithCallback());

        //reset calibration
        woundtissueclassification.resetCalibration(this);
    }
private void launchPreviewWithCallback() {
    woundtissueclassification.launchPreviewWoundList(
            this,
            "user_id", // userId
            "token", // https://console.woundtele.com
            "#2CA6CC");
}
    private void launchWithCallback() {
            woundtissueclassification.woundtissueclassificationWithLauncher(
                    woundTissueLauncher,
                    this,
                    "user_id", // userId
                    "token", // https://console.woundtele.com
                    "#2CA6CC", // primary color
                    false //riskScore optional
            );

    }


}
