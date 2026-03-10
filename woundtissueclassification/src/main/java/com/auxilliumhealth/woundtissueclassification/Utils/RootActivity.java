package com.auxilliumhealth.woundtissueclassification.Utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.auxilliumhealth.woundtissueclassification.R;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;


public class RootActivity extends AppCompatActivity {
    public String TAG;
    public Dialog dialog;
    private String primaryColor = "#007AFF"; // default blue fallback
    static AlertDialog builder;
    public  ActivityResultLauncher<Intent> result = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {


    });
    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        TAG = this.getClass().getSimpleName();
        Log.e("onCreate", TAG);
        dialog = new Dialog(this);

        // Try to get primary color from intent (if activity extends RootActivity)
        if (getIntent() != null && getIntent().hasExtra("primaryColor")) {
            primaryColor = getIntent().getStringExtra("primaryColor");
        }

        // ✅ Check network at start
        checkNetworkConnection();
    }

    @Override
    protected void onResume() {
        Log.e("onResume", TAG);
        super.onResume();
        // ✅ Recheck every time user comes back
        checkNetworkConnection();
    }

    /**
     * Checks if network is available. If not, shows a dialog with option to open settings.
     */
    public void checkNetworkConnection() {
        if (!isNetworkAvailable()) {
            showNoInternetDialog();
        }
    }
    public void showLoader() {
        if (dialog == null) {
            dialog = new Dialog(this);
        }
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.custom_progress_view);
        dialog.setCancelable(false);
        ProgressBar progressBar=dialog.findViewById(R.id.progressBar);
        try {
            // ✅ Apply primaryColor to progress bar
            int color = Color.parseColor(primaryColor);
            progressBar.setIndeterminateTintList(ColorStateList.valueOf(color));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid primary color format: " + primaryColor);
        }        dialog.show();
    }

    public void hideLoader() {
        if (dialog == null) {
            dialog = new Dialog(this);
        }
        dialog.dismiss();
    }
    /**
     * Returns true if device has an active internet connection.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    /**
     * Show a dialog prompting user to enable network.
     */
    private void showNoInternetDialog() {
        if (builder != null && builder.isShowing()) return; // avoid duplicate dialogs

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_no_internet, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);
        builder = dialogBuilder.create();
        builder.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        MaterialButton settingsBtn = dialogView.findViewById(R.id.btn_settings);
        MaterialButton retryBtn = dialogView.findViewById(R.id.btn_retry);

        // ✅ Apply dynamic primary color to both buttons
        try {
            int color = Color.parseColor(primaryColor);
            settingsBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            retryBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid primaryColor format: " + primaryColor);
        }

        settingsBtn.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            builder.dismiss();
        });

        retryBtn.setOnClickListener(v -> {
            builder.dismiss();
            checkNetworkConnection(); // retry check
        });

        builder.show();
    }

    public void showFullScreenImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        Dialog fullImageDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        fullImageDialog.setContentView(R.layout.dialog_full_image_zoom);
        fullImageDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        
        ImageView fullImageView = fullImageDialog.findViewById(R.id.fullImageView);
        ImageButton btnClose = fullImageDialog.findViewById(R.id.btnClose);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_error)
                .into(fullImageView);

        btnClose.setOnClickListener(v -> fullImageDialog.dismiss());
        fullImageDialog.show();
    }

    public static void cancelDialog(Context context) {

        if (builder != null) {
            builder.dismiss();
            builder.cancel();
        }
    }
}
