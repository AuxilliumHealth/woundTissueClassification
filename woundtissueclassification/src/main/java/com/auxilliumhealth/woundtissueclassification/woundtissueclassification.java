package com.auxilliumhealth.woundtissueclassification;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import com.auxilliumhealth.woundtissueclassification.Activities.CalibrationActivity;
import com.auxilliumhealth.woundtissueclassification.Activities.CameraActivity;
import com.auxilliumhealth.woundtissueclassification.Activities.WoundListActivity;
import com.auxilliumhealth.woundtissueclassification.Activities.WoundLocationActivity;
import com.auxilliumhealth.woundtissueclassification.LocalDatabase.PreferencesHelper;
import com.auxilliumhealth.woundtissueclassification.Utils.StereoCameraDetector;

public class woundtissueclassification {
    private static final String TAG = "woundtissueclassification";

    public static void woundtissueclassificationWithLauncher(
            ActivityResultLauncher<Intent> launcher,
            Context context,
            String userId,
            String woundId,
            String token,
            String colorHex,
            boolean woundScoreRequired,
            boolean woundLocationRequired,
            boolean calibrationRequired
    ) {
        if (context == null) {
            Log.e(TAG, "Context cannot be null");
            return;
        }

        try {
            // Check if calibration is needed
            String minFocusDistance = PreferencesHelper.getPreference(context, PreferencesHelper.PREF_MIN_FOCUS_DISTANCE);
            String maxFocusDistance = PreferencesHelper.getPreference(context, PreferencesHelper.PREF_MAX_FOCUS_DISTANCE);

            boolean hasFocusData = (isNotEmpty(minFocusDistance) && isNotEmpty(maxFocusDistance));

            // Stereo devices don't need manual calibration
            StereoCameraDetector detector = new StereoCameraDetector(context);
            boolean isStereoSupported = detector.isStereoSupported();

            Intent intent;

            // Only go to calibration if requested AND not already calibrated AND not a stereo device
            if (calibrationRequired && !hasFocusData && !isStereoSupported) {
                intent = new Intent(context, CalibrationActivity.class);
            } else {
                intent = new Intent(context, WoundLocationActivity.class);
            }

            // Pass extras
            intent.putExtra("userId", userId != null ? userId : "");
            intent.putExtra("token", token != null ? token : "");
            intent.putExtra("woundId", woundId != null ? woundId : "");
            intent.putExtra("primaryColor", (colorHex != null && colorHex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) ? colorHex : "#1A1A2E");
            intent.putExtra("woundScoreRequired", woundScoreRequired);
            intent.putExtra("woundLocationRequired", woundLocationRequired);
            launcher.launch(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error launching with ActivityResultLauncher: " + e.getMessage(), e);
        }
    }

    private static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    public static void resetCalibration(Context context) {
        PreferencesHelper.signOut(context);
    }
    public static void launchWoundLocation(Context context, String userId,String woundId, String token, String colorHex, boolean woundScoreRequired) {
        try {
            Intent intent = new Intent(context, WoundLocationActivity.class);
            intent.putExtra("userId", validateString(userId));
            intent.putExtra("token", validateString(token));
            intent.putExtra("woundId", validateString(woundId));
            intent.putExtra("primaryColor", validateColorHex(colorHex));
            intent.putExtra("woundScoreRequired", woundScoreRequired);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Verify the activity exists
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                ContextCompat.startActivity(context, intent, null);
            } else {
                Log.e(TAG, "WoundLocationActivity not found");
                // Fallback to calibration
                launchCalibration(context, userId,woundId, token, colorHex,woundScoreRequired);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching WoundLocation: " + e.getMessage(), e);
            launchCalibration(context, userId,woundId, token, colorHex,woundScoreRequired);
        }
    }

    public static void launchCalibration(Context context, String userId, String woundId, String token, String colorHex, boolean woundScoreRequired) {
        try {
            Intent intent = new Intent(context, CalibrationActivity.class);
            intent.putExtra("userId", validateString(userId));
            intent.putExtra("woundId", validateString(woundId));
            intent.putExtra("token", validateString(token));
            intent.putExtra("primaryColor", validateColorHex(colorHex));
            intent.putExtra("woundScoreRequired", woundScoreRequired);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                ContextCompat.startActivity(context, intent, null);
            } else {
                Log.e(TAG, "CalibrationActivity not found");
                // Last resort - show error or launch default activity
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching Calibration: " + e.getMessage(), e);
        }
    }

    private static String validateString(String value) {
        return value != null ? value : "";
    }

    private static String validateColorHex(String colorHex) {
        if (colorHex == null || !colorHex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
            return "#1A1A2E"; // Default color
        }
        return colorHex;
    }
public static void launchPreviewWoundList(Context context, String userId,String woundId, String token, String colorHex) {
    try {
        Intent intent = new Intent(context, WoundListActivity.class);
        intent.putExtra("userId", validateString(userId));
        intent.putExtra("token", validateString(token));
        intent.putExtra("woundId", validateString(woundId));
        intent.putExtra("primaryColor", validateColorHex(colorHex));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            ContextCompat.startActivity(context, intent, null);
        } else {
            Log.e(TAG, "WoundListActivity not found");
            // Fallback to calibration
        }
    } catch (Exception e) {
        Log.e(TAG, "Error launching PreviewWoundList: " + e.getMessage(), e);
    }
}

}