/**
 * ─────────────────────────────────────────────────────────────────────────────────────
 * Created & Developed by:
 * Aravindhan (Full Stack Engineer)
 * Auxilliumhealth LLC
 * GitHub: https://github.com/AravindhanDeveloper
 * ─────────────────────────────────────────────────────────────────────────────────────
 * Copyright (c) 2024. All rights reserved.
 * ─────────────────────────────────────────────────────────────────────────────────────
 */
package com.auxilliumhealth.woundtissueclassification.LocalDatabase;


import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {

    private static final String USER_PREFERENCES = "userPreferences";

    public static final String PREF_AREA_COEFFS = USER_PREFERENCES + ".areaCoeffsCubic";
    public static final String PREF_PIXEL_PER_UNIT = USER_PREFERENCES + ".pixelPerUnitCoeffsCubic";
    public static final String PREF_MIN_FOCUS_DISTANCE = USER_PREFERENCES + ".minFocusDistance";
    public static final String PREF_MAX_FOCUS_DISTANCE = USER_PREFERENCES + ".maxFocusDistance";
    public static final String PREF_TIPS_SHOWN = USER_PREFERENCES + ".tipsShown";

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(USER_PREFERENCES, Context.MODE_PRIVATE);
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        return getSharedPreferences(context).edit();
    }

    public static void setPreference(Context context, String key, String value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putString(key, value);
        editor.apply();
    }

    public static void setIntPreference(Context context, String key, int value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putInt(key, value);
        editor.apply();
    }

    public static void setBooleanPreference(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = getEditor(context);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static String getPreference(Context context, String key) {
        return getSharedPreferences(context).getString(key, "");
    }

    public static int getIntPreference(Context context, String key) {
        return getSharedPreferences(context).getInt(key, 0);
    }

    public static boolean getBooleanPreference(Context context, String key) {
        return getSharedPreferences(context).getBoolean(key, false);
    }

    public static void signOut(Context context) {
        SharedPreferences.Editor editor = getEditor(context);

        editor.remove(PREF_AREA_COEFFS);
        editor.remove(PREF_PIXEL_PER_UNIT);
        editor.remove(PREF_MIN_FOCUS_DISTANCE);
        editor.remove(PREF_MAX_FOCUS_DISTANCE);
        editor.remove(PREF_TIPS_SHOWN);

        editor.clear();
        editor.apply();
    }
}
