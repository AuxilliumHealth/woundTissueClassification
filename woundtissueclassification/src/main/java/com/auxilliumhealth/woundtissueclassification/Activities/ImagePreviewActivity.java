package com.auxilliumhealth.woundtissueclassification.Activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.auxilliumhealth.woundtissueclassification.Adapters.ImageGroupAdapter;
import com.auxilliumhealth.woundtissueclassification.LocalDatabase.PreferencesHelper;
import com.auxilliumhealth.woundtissueclassification.Manager.WoundBoundingBoxManager;
import com.auxilliumhealth.woundtissueclassification.Model.CalibrationModel;
import com.auxilliumhealth.woundtissueclassification.Model.ResultDataModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.auxilliumhealth.woundtissueclassification.Utils.CoinDetector;
import com.auxilliumhealth.woundtissueclassification.Utils.RootActivity;
import com.auxilliumhealth.woundtissueclassification.ViewModel.CustomViewPager;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;

public class ImagePreviewActivity extends RootActivity {

    ArrayList<Float> lensFocusDistances = new ArrayList<>();
    String userId, coinType, pixelCounts, token;
    ArrayList<float[]> woundCoordinates = new ArrayList<float[]>();
    List<ResultDataModel> resultDataList = new ArrayList<>();
    String primaryColor;
    MaterialToolbar materialToolbar;
    AppBarLayout appBarLayout;
    ImageButton btnPrev, btnNextArrow;
    private ImageGroupAdapter adapter;
    private ActivityResultLauncher<Intent> lassoLauncher;
    private Repository repository;
    // UI elements replacing binding
    private CustomViewPager viewPager;
    private MaterialButton btnNext, btnLasso;
    private boolean woundScoreRequired, woundLocationRequired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        repository = new Repository(this);
        userId = getIntent().getStringExtra("userId");
        primaryColor = getIntent().getStringExtra("primaryColor");
        token = getIntent().getStringExtra("token");
        woundScoreRequired = getIntent().getBooleanExtra("woundScoreRequired", false);
        woundLocationRequired = getIntent().getBooleanExtra("woundLocationRequired", false);

        // Java
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(primaryColor));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // dark icons
        }

        // Find views
        viewPager = findViewById(R.id.view_pager);
        btnPrev = findViewById(R.id.btnPrev);
        btnNextArrow = findViewById(R.id.btnNextArrow);
        btnNext = findViewById(R.id.btnNext);
        btnLasso = findViewById(R.id.btnLasso);
        materialToolbar = findViewById(R.id.material_toolbar);
        appBarLayout = findViewById(R.id.app_bar_layout);

        materialToolbar.setBackgroundColor(Color.parseColor(primaryColor));
        appBarLayout.setBackgroundColor(Color.parseColor(primaryColor));
        viewPager.setSwipeEnabled(true);
        coinType = getIntent().getStringExtra("coinType");
        btnNext.setBackgroundColor(Color.parseColor(primaryColor));
        btnLasso.setStrokeColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
        btnLasso.setTextColor(Color.parseColor(primaryColor));
        btnLasso.setIconTint(ColorStateList.valueOf(Color.parseColor(primaryColor)));
        ArrayList<String> filePaths = getIntent().getStringArrayListExtra("filePaths");

        if (filePaths == null || filePaths.isEmpty()) {
            Log.e("ImagePreview", "No file paths received.");
            finish();
            return;
        }

        try {
            List<Float> distances = (List<Float>) getIntent().getSerializableExtra("lensFocusDistances");
            if (distances != null) {
                lensFocusDistances = new ArrayList<>(distances);
            }
        } catch (ClassCastException e) {
            Log.e("ImagePreview", "Error casting lensFocusDistances", e);
        }

        try {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Processing images...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(() -> {
                try {
                    Log.d("TAG", "onCreate: " + filePaths);
                    for (String file : filePaths) {
                        WoundBoundingBoxManager woundBoundingBoxManager = new WoundBoundingBoxManager(getApplicationContext());
                        woundBoundingBoxManager.init(file, "sessionId");
                        woundBoundingBoxManager.process();
                        float[] coords = woundBoundingBoxManager.getWoundCoordinates();
                        woundCoordinates.add(coords);
                    }

                    CoinDetector coinDetector = new CoinDetector();
                    final List<ResultDataModel> results = coinDetector.detectCoinsExtractPixelCounts(ImagePreviewActivity.this, woundCoordinates, filePaths);

                    runOnUiThread(() -> {
                        resultDataList = results;
                        pixelCounts = getCommaSeparatedPixelCounts(resultDataList);

                        adapter = new ImageGroupAdapter(ImagePreviewActivity.this, resultDataList, filePaths);
                        viewPager.setAdapter(adapter);
                    });
                } catch (Exception e) {
                    Log.e("ImageProcessing", "Error processing images", e);
                    runOnUiThread(() -> Log.e("ImageProcessing", "Error processing images: " + e.getMessage()));
                } finally {
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e("ImageProcessing", "Error starting processing thread", e);
        }

        // Register result listener for LassoActivity
        lassoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data == null) return;

                int position = data.getIntExtra("position", -1);
                String overlayPath = data.getStringExtra("overlayPath");
                int pixelCount = data.getIntExtra("pixelCount", 0);

                if (overlayPath != null && !overlayPath.isEmpty() && position >= 0) {
                    try {
                        List<ResultDataModel> currentList = new ArrayList<>(adapter.getCurrentList());
                        if (position <= currentList.size()) {
                            ResultDataModel updatedModel = currentList.get(position);
                            updatedModel.setcontourImage(new File(overlayPath));
                            updatedModel.setPixelCount(pixelCount);
                            currentList.set(position, updatedModel);
                            pixelCounts = getCommaSeparatedPixelCounts(currentList);
                            adapter.updateData(currentList);
                        }
                    } catch (Exception e) {
                        Log.e("LassoActivity", "Error updating list: " + e.getMessage());
                    }
                }
            }
        });

        adapter = new ImageGroupAdapter(this, resultDataList, filePaths);
        viewPager.setAdapter(adapter);

        btnPrev.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1, true);
            }
        });

        btnNextArrow.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < adapter.getCount() - 1) {
                viewPager.setCurrentItem(currentItem + 1, true);
            } else {
                uploadCalibrateData(userId, coinType, lensFocusDistances, pixelCounts);
            }
        });

        btnNext.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < adapter.getCount() - 1) {
                viewPager.setCurrentItem(currentItem + 1, true);
            } else {
                uploadCalibrateData(userId, coinType, lensFocusDistances, pixelCounts);
            }
        });

        btnLasso.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            Intent intent = new Intent(ImagePreviewActivity.this, LassoActivity.class);
            intent.putExtra("imagePath", filePaths.get(currentItem));
            intent.putExtra("primaryColor", primaryColor);
            intent.putExtra("position", currentItem);
            lassoLauncher.launch(intent);
        });
    }


    private void uploadCalibrateData(String userId, String coinType, ArrayList<Float> lenseFocusDistances, String pixelCounts) {
        showLoader();
        new Thread(() -> {
            // delete calibration folder
            File calibration = new File(getCacheDir(), "calibration");
            deleteRecursive(calibration);
            //-----------
            Map<Double, Integer> pixelCountsMap = new HashMap<>();
            for (int i = 0; i < lensFocusDistances.size(); i++) {
                float distance = lensFocusDistances.get(i);
                int pixelCount = resultDataList.get(i).getPixelCount();
                pixelCountsMap.put((double) distance, pixelCount);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lenseFocusDistances.size(); i++) {
                sb.append(lenseFocusDistances.get(i));
                if (i < lenseFocusDistances.size() - 1) {
                    sb.append(", ");
                }
            }

            // Store result as a String
            String lenceFocusDistancesString = sb.toString();


            repository.setGetCommonAPIDetails(new Repository.GetCommonAPIDataSuccessCallBack() {
                @Override
                public void getCommonAPIDataSuccess(ResponseBody apiArrayResponse) {
                    runOnUiThread(() -> {

                        try {
                            hideLoader();
                            if (apiArrayResponse != null) {
                                String responseString = apiArrayResponse.string();
                                Gson gson = new Gson();
                                CalibrationModel result = gson.fromJson(responseString, CalibrationModel.class);

                                if (result != null) {
                                    Double areaError = result.getAreaError();
                                    Double pixelError = result.getPixelError();
                                    Log.d("TAG", "areaError: " + areaError + " pixelError: " + pixelError);
                                    if (areaError <= 11) {
                                        if (!lensFocusDistances.isEmpty()) {


                                            float minFocusDistance = Collections.min(lensFocusDistances);
                                            float maxFocusDistance = Collections.max(lensFocusDistances);
                                            PreferencesHelper.setPreference(ImagePreviewActivity.this, PreferencesHelper.PREF_MIN_FOCUS_DISTANCE, String.valueOf(minFocusDistance));
                                            PreferencesHelper.setPreference(ImagePreviewActivity.this, PreferencesHelper.PREF_MAX_FOCUS_DISTANCE, String.valueOf(maxFocusDistance));
                                        }


                                        PreferencesHelper.setPreference(ImagePreviewActivity.this, PreferencesHelper.PREF_PIXEL_PER_UNIT, result.getPixelPerUnitCoeffsCubic().toString());
                                        PreferencesHelper.setPreference(ImagePreviewActivity.this, PreferencesHelper.PREF_AREA_COEFFS, result.getAreaCoeffsCubic().toString());
                                        Log.d("errorRate", "errorRate: area" + result.getAreaError() + "-----pixcel" + result.getPixelError());
                                        dialogbox();
                                    } else {
                                        Log.d("errorRate", "errorRate: area" + result.getAreaError() + "-----pixcel" + result.getPixelError());
                                        retryDialogbox();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            hideLoader();
                            Log.d("TAG", "getCommonAPIDataSuccess: " + e);
                        }
                    });
                }

                @Override
                public void getCommonAPIDataFailure(String message) {
                    runOnUiThread(() -> {
                        Log.d("TAG", "message: " + message);
                        hideLoader();

                        retryDialogbox();

                    });

                }

                @Override
                public void onProgressUpdate(int progress) {

                }
            });

            repository.getCalibrate(userId, coinType, lenceFocusDistancesString, pixelCounts);
        }).start();
    }
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                File[] children = fileOrDirectory.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursive(child);
                    }
                }
            }
            fileOrDirectory.delete();
        }
    }
    private void dialogbox() {
        final Dialog dialog = new Dialog(ImagePreviewActivity.this);
        dialog.setContentView(R.layout.wound_alert_dialogbox);
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        MaterialButton oldBtn = dialog.findViewById(R.id.old_btn);
        MaterialButton newBtn = dialog.findViewById(R.id.new_btn);
        newBtn.setBackgroundColor(Color.parseColor(primaryColor));
        newBtn.setTextColor(Color.WHITE);
        oldBtn.setTextColor(Color.parseColor(primaryColor));
        oldBtn.setStrokeColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
        oldBtn.setOnClickListener(v -> {

            finish();
            dialog.dismiss();
        });

        newBtn.setOnClickListener(v -> {
            Intent mIntent = new Intent(ImagePreviewActivity.this, WoundLocationActivity.class);
            mIntent.putExtra("whereFrom", "imaging");
            mIntent.putExtra("userId", userId);
            mIntent.putExtra("token", token);
            mIntent.putExtra("primaryColor", primaryColor);
            mIntent.putExtra("woundLocationRequired", woundLocationRequired);
            startActivity(mIntent);
            finish();
            dialog.dismiss();
        });
    }

    private void retryDialogbox() {
        final Dialog dialog = new Dialog(ImagePreviewActivity.this);
        dialog.setContentView(R.layout.wound_alert_dialogbox);
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        MaterialButton oldBtn = dialog.findViewById(R.id.old_btn);
        MaterialButton newBtn = dialog.findViewById(R.id.new_btn);
        newBtn.setBackgroundColor(Color.parseColor(primaryColor));
        newBtn.setTextColor(Color.WHITE);
        oldBtn.setTextColor(Color.parseColor(primaryColor));
        oldBtn.setStrokeColor(ColorStateList.valueOf(Color.parseColor(primaryColor)));
        MaterialTextView areaErrorText = dialog.findViewById(R.id.textinput_error);
        areaErrorText.setText("Calibration failed due to high error rate. Ensure images have a white background and are in clear focus.");
        areaErrorText.setTextColor(Color.RED);
        newBtn.setText("Retry");

        oldBtn.setOnClickListener(v -> {
            Intent i = new Intent(ImagePreviewActivity.this, WoundLocationActivity.class);
            i.putExtra("whereFrom", "imaging");
            i.putExtra("userId", userId);
            i.putExtra("token", token);
            i.putExtra("primaryColor", primaryColor);
            i.putExtra("woundLocationRequired", woundLocationRequired);
            startActivity(i);
            finish();
            dialog.dismiss();
        });

        newBtn.setOnClickListener(v -> {
            Intent i = new Intent(ImagePreviewActivity.this, CameraActivity.class);
            i.putExtra("whereFrom", "calibrate");
            i.putExtra("coinType", coinType);
            i.putExtra("userId", userId);
            i.putExtra("token", token);
            i.putExtra("primaryColor", primaryColor);
            startActivity(i);
            finish();
            dialog.dismiss();
        });
    }

    public String convertToCommaSeparated(ArrayList<Float> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public String getCommaSeparatedPixelCounts(List<ResultDataModel> resultDataList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resultDataList.size(); i++) {
            sb.append(resultDataList.get(i).getPixelCount());
            if (i < resultDataList.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }


}
