package com.auxilliumhealth.woundtissueclassification;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

import com.auxilliumhealth.woundtissueclassification.Activities.CalibrationActivity;
import com.auxilliumhealth.woundtissueclassification.Activities.LatestWoundListActivity;
import com.auxilliumhealth.woundtissueclassification.Activities.WoundListActivity;
import com.auxilliumhealth.woundtissueclassification.Activities.WoundLocationActivity;
import com.auxilliumhealth.woundtissueclassification.LocalDatabase.PreferencesHelper;

public class woundtissueclassification {
    private static final String TAG = "woundtissueclassification";

    public static void woundtissueclassification(Context context, String userId, String token, String colorHex,  boolean woundScoreRequired
    ) {
        if (context == null) {
            Log.e(TAG, "Context cannot be null");
            return;
        }

        try {
            String minFocusDistance = PreferencesHelper.getPreference(context, PreferencesHelper.PREF_MIN_FOCUS_DISTANCE);
            String maxFocusDistance = PreferencesHelper.getPreference(context, PreferencesHelper.PREF_MAX_FOCUS_DISTANCE);

            boolean hasFocusData = (minFocusDistance != null && !minFocusDistance.isEmpty() &&
                    maxFocusDistance != null && !maxFocusDistance.isEmpty());

            if (hasFocusData) {
                launchWoundLocation(context, userId, token, colorHex,woundScoreRequired);
            } else {
                launchCalibration(context, userId, token, colorHex,woundScoreRequired);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in woundLocation: " + e.getMessage(), e);
            // Fallback to calibration on error
            launchCalibration(context, userId, token, colorHex,woundScoreRequired);
        }
    }
    public static void woundtissueclassificationWithLauncher(
            ActivityResultLauncher<Intent> launcher,
            Context context,
            String userId,
            String token,
            String colorHex,
            boolean woundScoreRequired
    ) {
        if (context == null) {
            Log.e(TAG, "Context cannot be null");
            return;
        }

        try {
            // Check if calibration is needed
            String minFocusDistance = PreferencesHelper.getPreference(context, PreferencesHelper.PREF_MIN_FOCUS_DISTANCE);
            String maxFocusDistance = PreferencesHelper.getPreference(context, PreferencesHelper.PREF_MAX_FOCUS_DISTANCE);

            boolean hasFocusData = (minFocusDistance != null && !minFocusDistance.isEmpty() &&
                    maxFocusDistance != null && !maxFocusDistance.isEmpty());

            Intent intent;

            if (hasFocusData) {
                intent = new Intent(context, LatestWoundListActivity.class);
            } else {
                intent = new Intent(context, CalibrationActivity.class);
            }

            // Pass extras
            intent.putExtra("userId", userId != null ? userId : "");
            intent.putExtra("token", token != null ? token : "");

            intent.putExtra("primaryColor", (colorHex != null && colorHex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) ? colorHex : "#1A1A2E");
            intent.putExtra("woundScoreRequired", woundScoreRequired);
            launcher.launch(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error launching with ActivityResultLauncher: " + e.getMessage(), e);
        }
    }

    public static void resetCalibration(Context context) {
        PreferencesHelper.signOut(context);
    }
    public static void launchWoundLocation(Context context, String userId, String token, String colorHex, boolean woundScoreRequired) {
        try {
            Intent intent = new Intent(context, LatestWoundListActivity.class);
            intent.putExtra("userId", validateString(userId));
            intent.putExtra("token", validateString(token));
            intent.putExtra("primaryColor", validateColorHex(colorHex));
            intent.putExtra("woundScoreRequired", woundScoreRequired);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Verify the activity exists
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                ContextCompat.startActivity(context, intent, null);
            } else {
                Log.e(TAG, "WoundLocationActivity not found");
                // Fallback to calibration
                launchCalibration(context, userId, token, colorHex,woundScoreRequired);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching WoundLocation: " + e.getMessage(), e);
            launchCalibration(context, userId, token, colorHex,woundScoreRequired);
        }
    }

    public static void launchCalibration(Context context, String userId, String token, String colorHex, boolean woundScoreRequired) {
        try {
            Intent intent = new Intent(context, CalibrationActivity.class);
            intent.putExtra("userId", validateString(userId));
            intent.putExtra("token", validateString(token));
            intent.putExtra("primaryColor", validateColorHex(colorHex));
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
public static void launchPreviewWoundList(Context context, String userId, String token, String colorHex) {
    try {
        Intent intent = new Intent(context, WoundListActivity.class);
        intent.putExtra("userId", validateString(userId));
        intent.putExtra("token", validateString(token));
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