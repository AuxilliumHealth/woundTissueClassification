package com.auxilliumhealth.woundtissueclassification.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import androidx.exifinterface.media.ExifInterface;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.auxilliumhealth.woundtissueclassification.LocalDatabase.PreferencesHelper;
import com.auxilliumhealth.woundtissueclassification.Model.S3UploadResultModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.auxilliumhealth.woundtissueclassification.Utils.CameraPermissionHelper;
import com.auxilliumhealth.woundtissueclassification.Utils.FocusCircle;
import com.auxilliumhealth.woundtissueclassification.Utils.GyroscopeChecker;
import com.auxilliumhealth.woundtissueclassification.ViewModel.CameraViewModel;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.ResponseBody;

public class CameraActivity extends AppCompatActivity implements GyroscopeChecker.OnFlatStatusChangeListener {

    private static final String TAG = "CameraActivity";
    private static final long CLICK_THROTTLE_MS = 1000;
    private static final int CALIBRATION_IMAGE_COUNT = 7;
    private static final int REQUEST_CODE_SYMPTOM_ACTIVITY = 1001;

    private final List<String> filePaths = new ArrayList<>();
    private final List<Float> lensFocusDistances = new ArrayList<>();
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    private final Executor cameraExecutor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private long lastClickTime = 0;
    private ImageButton captureButton, backButton;
    private MaterialTextView warningText, imageCaptureText;
    private LinearLayout progressContainer;
    private ImageButton helpButton;
    private ImageView flashButton, captureImg;
    private PreviewView previewView;
    private MaterialTextView permissionTextView;
    private MaterialButton permissionButton;
    private FocusCircle focusIndicator;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageCapture imageCapture;
    private int currentFlashMode = ImageCapture.FLASH_MODE_ON;

    private Repository repository;
    private MediaActionSound mediaActionSound;
    private GyroscopeChecker gyroscopeChecker;
    private View[] segments;

    private boolean isImaging = false;
    private boolean isFlat = false;
    private Float focalLength;
    private String whereFrom, woundId, sessionId, userId, coinType, token, primaryColor, woundLocation;
    private boolean woundScoreRequired = true;
    private boolean woundLocationRequired = true;
    private boolean hasPermission = false;
    private boolean hasRequestedPermission = false;
    private CameraViewModel viewModel;
    private boolean isFocusDistanceSupported = true;
    private AlertDialog uploadProgressDialog;
    private ProgressBar uploadProgressBar;
    private MaterialTextView uploadStatusText;

    ActivityResultLauncher<Intent> cameraLauncher =
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
        setContentView(R.layout.activity_camera);

        try {
            initView();
            initProgressSegments();
            initGyroscope();
            checkFocusDistanceSupport();
            initCamera();
            initExposureControl();
            viewModel = new ViewModelProvider(this).get(CameraViewModel.class);
            setupObservers();
        } catch (Exception e) {
            Log.e(TAG, "Initialization failed", e);
            showToast("Initialization error");
            finish();
        }

        if (helpButton != null) {
            helpButton.setOnClickListener(v -> startActivity(new Intent(CameraActivity.this, VideoActivity.class)));
        }
    }

    private void initView() {
        repository = new Repository(CameraActivity.this);
        warningText = findViewById(R.id.warningText);

        whereFrom = getIntent().getStringExtra("whereFrom");
        woundId = getIntent().getStringExtra("woundId");
        coinType = getIntent().getStringExtra("coinType");
        sessionId = getIntent().getStringExtra("sessionId");
        woundScoreRequired = getIntent().getBooleanExtra("woundScoreRequired", true);
        woundLocationRequired = getIntent().getBooleanExtra("woundLocationRequired", true);
        userId = getIntent().getStringExtra("userId");
        woundLocation = getIntent().getStringExtra("woundLocation");
        token = getIntent().getStringExtra("token");
        primaryColor = getIntent().getStringExtra("primaryColor");
//        Toast.makeText(this, ""+woundId, Toast.LENGTH_SHORT).show();

        Log.d(TAG, "sessionId: " + sessionId + " userId: " + userId + " woundId: " + woundId + " token: " + token);

        previewView = findViewById(R.id.previewView);
        permissionTextView = findViewById(R.id.permissionTextView);
        progressContainer = findViewById(R.id.progressContainer);
        permissionButton = findViewById(R.id.permissionButton);
        helpButton = findViewById(R.id.helpButton);
        imageCaptureText = findViewById(R.id.image_capture_text);
        captureButton = findViewById(R.id.captureButton);
        backButton = findViewById(R.id.backButton);
        flashButton = findViewById(R.id.flash_button);
        focusIndicator = findViewById(R.id.focusIndicator);
        captureImg = findViewById(R.id.capture_img);

        if (captureImg != null && primaryColor != null) {
            captureImg.setColorFilter(Color.parseColor(primaryColor));
        }

        // Lock PreviewView scale type to prevent resizing
        if (previewView != null) {
            previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        }
        getWindow().setStatusBarColor(Color.parseColor(primaryColor));
        setupClickListeners();
        configureUIForFlow();
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            // Handle loading state if needed
        });

        viewModel.getUploadResponse().observe(this, response -> {
            try {
                String responseString = response.string();
                Log.d(TAG, "Upload successful: " + responseString);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing response", e);
            }
        });
    }

    private void updateViewVisibility() {
        if (hasPermission) {
            if (permissionTextView != null) permissionTextView.setVisibility(View.INVISIBLE);
            if (permissionButton != null) permissionButton.setVisibility(View.INVISIBLE);
            if (previewView != null) previewView.setVisibility(View.VISIBLE);
            return;
        }

        if (previewView != null) previewView.setVisibility(View.INVISIBLE);

        if (hasRequestedPermission) {
            if (CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                if (permissionButton != null) {
                    permissionButton.setText(R.string.request_permission);
                    permissionButton.setOnClickListener(view -> CameraPermissionHelper.requestCameraPermission(this));
                }
            } else {
                if (permissionButton != null) {
                    permissionButton.setText(R.string.open_app_settings);
                    permissionButton.setOnClickListener(view -> CameraPermissionHelper.launchPermissionSettings(this));
                }
            }
            if (permissionTextView != null) permissionTextView.setVisibility(View.VISIBLE);
            if (permissionButton != null) permissionButton.setVisibility(View.VISIBLE);
        } else {
            if (permissionTextView != null) permissionTextView.setVisibility(View.INVISIBLE);
            if (permissionButton != null) permissionButton.setVisibility(View.INVISIBLE);
        }
    }

    private void initProgressSegments() {
        segments = new View[]{findViewById(R.id.progressSegment1), findViewById(R.id.progressSegment2), findViewById(R.id.progressSegment3), findViewById(R.id.progressSegment4), findViewById(R.id.progressSegment5), findViewById(R.id.progressSegment6), findViewById(R.id.progressSegment7)};
    }

    private void initGyroscope() {
        if (whereFrom == null) {
            isFlat = true;
            return;
        }

        if ("calibrate".equals(whereFrom)) {
            gyroscopeChecker = new GyroscopeChecker(this, this);
            if (gyroscopeChecker.isGyroscopeAvailable()) {
                gyroscopeChecker.startListening();
            } else {
                Log.d(TAG, "Gyroscope not available");
                isFlat = true;
            }
        } else {
            isFlat = true;
        }
    }

    private void setupClickListeners() {
        if (captureButton != null) {
            captureButton.setOnClickListener(view -> handleCaptureClick());
        }

        if (flashButton != null) {
            flashButton.setOnClickListener(view -> {
                switch (currentFlashMode) {
                    case ImageCapture.FLASH_MODE_OFF:
                        setFlashMode(ImageCapture.FLASH_MODE_ON);
                        break;
                    case ImageCapture.FLASH_MODE_ON:
                        setFlashMode(ImageCapture.FLASH_MODE_AUTO);
                        break;
                    case ImageCapture.FLASH_MODE_AUTO:
                    default:
                        setFlashMode(ImageCapture.FLASH_MODE_OFF);
                        break;
                }
            });
        }

        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (whereFrom != null && whereFrom.equals("calibrate")) {
                    Toast.makeText(this, "calibrate", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(CameraActivity.this, CalibrationActivity.class);
                    i.putExtra("whereFrom", "calibrate");
                    i.putExtra("userId", userId);
                    i.putExtra("woundScoreRequired", woundScoreRequired);
                    i.putExtra("woundLocationRequired", woundLocationRequired);
                    i.putExtra("token", token);
                    i.putExtra("primaryColor", primaryColor);
                    startActivity(i);
                    finish();
                } else {
                    Intent i = new Intent(CameraActivity.this, WoundLocationActivity.class);
                    i.putExtra("whereFrom", "calibrate");
                    i.putExtra("userId", userId);
                    i.putExtra("token", token);
                    i.putExtra("woundId", woundId);
                    i.putExtra("woundLocation", woundLocation);
                    i.putExtra("woundScoreRequired", woundScoreRequired);
                    i.putExtra("woundLocationRequired", woundLocationRequired);
                    i.putExtra("primaryColor", primaryColor);
                    startActivity(i);
                    finish();
                }
            });
        }
    }

    private void handleCaptureClick() {
        if (isCapturing.get()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_THROTTLE_MS) {
            return;
        }
        lastClickTime = currentTime;

        if (!isImaging && !isFlat) {
            showToast("Capture from a different distance & hold device parallel");
        } else if (!isImaging) {
            showToast("Capture from a different distance");
        } else if (!isFlat) {
            showToast("Hold device parallel while capturing");
        } else {
            captureImage();
            updateFocusUI(true);
        }
    }

    private void configureUIForFlow() {
        if (whereFrom == null || imageCaptureText == null || captureImg == null || progressContainer == null || helpButton == null) {
            return;
        }

        if ("calibrate".equals(whereFrom)) {
            imageCaptureText.setVisibility(View.VISIBLE);
            captureImg.setVisibility(View.VISIBLE);
            progressContainer.setVisibility(View.VISIBLE);
            helpButton.setVisibility(View.VISIBLE);
            if (!isFocusDistanceSupported && warningText != null) {
                String warningMessage = "Your device does not support focus distance detection. Please capture images at different distances.";
                warningText.setText(warningMessage);
                warningText.setVisibility(View.VISIBLE);
                Log.w(TAG, warningMessage);
            }
        } else {
            progressContainer.setVisibility(View.GONE);
            helpButton.setVisibility(View.GONE);
            String min = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_MIN_FOCUS_DISTANCE);
            String max = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_MAX_FOCUS_DISTANCE);
            boolean hasFocusData = isNotEmpty(min) && isNotEmpty(max);
            imageCaptureText.setVisibility(hasFocusData ? View.VISIBLE : View.GONE);
            captureImg.setVisibility(hasFocusData ? View.VISIBLE : View.GONE);
        }
    }

    private void checkFocusDistanceSupport() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Float minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
            isFocusDistanceSupported = minFocusDistance != null && minFocusDistance > 0.0f;
            if (!isFocusDistanceSupported) {
                Log.w(TAG, "LENS_FOCUS_DISTANCE not supported or fixed-focus camera detected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking focus distance support", e);
            isFocusDistanceSupported = false;
        }
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void initCamera() {
        if (isDestroyed()) return;

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (cameraProvider == null || isDestroyed()) return;

                Preview.Builder previewBuilder = new Preview.Builder();
                // The orientation will be handled by the camera configuration
                new Camera2Interop.Extender<>(previewBuilder).setSessionCaptureCallback(new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        handleCaptureResult(result);
                    }
                });

                Preview preview = previewBuilder.build();
                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setFlashMode(currentFlashMode).build();

                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                if (previewView != null) {
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
                }

                if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                    camera.getCameraControl().enableTorch(currentFlashMode == ImageCapture.FLASH_MODE_ON);
                }

                // Lock zoom to prevent scaling
                if (camera != null) {
                    camera.getCameraControl().setZoomRatio(1.0f);
                }
            } catch (Exception e) {
                Log.e(TAG, "Camera initialization failed", e);
                showToast("Camera start error");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private void initExposureControl() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Range<Integer> exposureRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            if (exposureRange != null) {
                int minExposure = exposureRange.getLower();
                int maxExposure = exposureRange.getUpper();
                Log.d(TAG, "Exposure range: [" + minExposure + ", " + maxExposure + "]");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing exposure control", e);
        }
    }


    private void handleCaptureResult(TotalCaptureResult result) {
        if (result == null) return;

        Float focusDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
//        Log.d(TAG, "Focus distance: " + focusDistance);

        if (focusDistance == null || focusDistance == 0.0f) {
            if (!isFocusDistanceSupported) {
                focusDistance = (float) (lensFocusDistances.size() + 1);
//                Log.d(TAG, "Using placeholder focus distance: " + focusDistance);
            } else {
                return;
            }
        }

        if (!focusDistance.equals(focalLength)) {
            focalLength = focusDistance;
            boolean shouldMoveAway = isFocusDistanceSimilar(focusDistance);
            updateFocusUI(shouldMoveAway);
        }
    }

    private boolean isFocusDistanceSimilar(float lensFocusDistance) {
        if (!isFocusDistanceSupported) {
            return lensFocusDistances.size() >= CALIBRATION_IMAGE_COUNT;
        }

        if (lensFocusDistances.contains(lensFocusDistance) || lensFocusDistance < 1.0f) {
            return true;
        }

        if ("calibrate".equals(whereFrom)) {
            return !isExistingValueInArray(lensFocusDistance);
        }

        String min = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_MIN_FOCUS_DISTANCE);
        String max = PreferencesHelper.getPreference(this, PreferencesHelper.PREF_MAX_FOCUS_DISTANCE);

        if (!isNotEmpty(min) || !isNotEmpty(max)) {
            return false;
        }

        try {
            float minFloat = Float.parseFloat(min);
            float maxFloat = Float.parseFloat(max);
            return !(lensFocusDistance >= minFloat && lensFocusDistance <= maxFloat);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing focus distances", e);
            return false;
        }
    }

    private void showImagePreview(File imageFile) {
        if (isDestroyed() || isFinishing() || imageFile == null) return;

        runOnUiThread(() -> {
            try {
                View previewView = getLayoutInflater().inflate(R.layout.image_preview_dialog, null);
                ImageView previewImage = previewView.findViewById(R.id.previewImage);
                MaterialButton retakeButton = previewView.findViewById(R.id.retakeButton);
                MaterialButton confirmButton = previewView.findViewById(R.id.confirmButton);
                if (primaryColor != null) {
                    retakeButton.setBackgroundColor(Color.parseColor(primaryColor));
                    confirmButton.setBackgroundColor(Color.parseColor(primaryColor));
                }
                Glide.with(this).load(imageFile).into(previewImage);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(previewView);
                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
                }

                dialog.show();

                retakeButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    isCapturing.set(false);
                    if ("calibrate".equals(whereFrom) && !filePaths.isEmpty() && !lensFocusDistances.isEmpty()) {
                        filePaths.remove(filePaths.size() - 1);
                        lensFocusDistances.remove(lensFocusDistances.size() - 1);
                        updateProgressBar(filePaths.size());
                    }
                });

                confirmButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    handleImageConfirmed(imageFile);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error showing image preview", e);
                isCapturing.set(false);
            }
        });
    }

    private void handleImageConfirmed(File photoFile) {
        if (photoFile == null) {
            isCapturing.set(false);
            return;
        }

        if ("calibrate".equals(whereFrom)) {
            if (filePaths.size() >= CALIBRATION_IMAGE_COUNT) {
                navigateToImagePreview();
            }
            isCapturing.set(false);
        } else {
            navigateToSymptoms(photoFile);
        }
    }

    private void handleImageSaved(File photoFile) {
        if (isDestroyed() || isFinishing() || photoFile == null) {
            isCapturing.set(false);
            return;
        }

        runOnUiThread(() -> {
            try {
                File correctedFile = correctImageOrientationAndAspect(photoFile);
                if (correctedFile == null) {
                    isCapturing.set(false);
                    return;
                }

                filePaths.add(correctedFile.getAbsolutePath());
                if (focalLength != null) {
                    lensFocusDistances.add(focalLength);
                }

                if ("calibrate".equals(whereFrom)) {
                    updateProgressBar(filePaths.size());
                    if (filePaths.size() < CALIBRATION_IMAGE_COUNT) {
                        showImagePreview(correctedFile);
                    } else {
                        navigateToImagePreview();
                    }
                } else {
                    showImagePreview(correctedFile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling saved image", e);
                isCapturing.set(false);
            }
        });
    }

    private boolean isExistingValueInArray(float newValue) {
        if (!isFocusDistanceSupported) {
            return true;
        }

        int newGroup = (int) newValue;
        for (float existing : lensFocusDistances) {
            int existingGroup = (int) existing;
            if (existingGroup == newGroup) {
                return false;
            }
        }
        return true;
    }

    private File correctImageOrientationAndAspect(File imageFile) {
        if (imageFile == null) return null;

        try {
            Bitmap originalBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (originalBitmap == null) return imageFile;

            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            Bitmap rotatedBitmap = rotateBitmap(originalBitmap, orientation);
            Bitmap scaledBitmap = scaleBitmapToAspectRatio(rotatedBitmap);

            File correctedFile = createImageFile();
            if (correctedFile != null) {
                try (FileOutputStream out = new FileOutputStream(correctedFile)) {
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    return correctedFile;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error correcting image", e);
        }
        return imageFile;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        if (bitmap == null) return null;

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Bitmap scaleBitmapToAspectRatio(Bitmap bitmap) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float aspectRatio = (float) width / height;

        int targetWidth = 1080;
        int targetHeight = Math.round(targetWidth / aspectRatio);

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    private void updateFocusUI(boolean shouldMoveAway) {
        if (isDestroyed() || imageCaptureText == null || captureImg == null || warningText == null || captureButton == null) {
            return;
        }

        uiHandler.post(() -> {
            if (isDestroyed()) return;

            try {
                if (shouldMoveAway) {
                    imageCaptureText.setVisibility(View.VISIBLE);
                    imageCaptureText.setText(isFocusDistanceSupported ? "Move to different distance" : "Capture image at a different distance manually (e.g., 10 cm, 20 cm)");
                    captureImg.setVisibility(View.VISIBLE);
                    Glide.with(this).load(R.drawable.move_arrow).into(captureImg);

                    if (isImaging) {
                        isImaging = false;
                        captureButton.setImageResource(R.drawable.camera_icon);
                        warningText.setVisibility(View.VISIBLE);
                    }
                } else {
                    imageCaptureText.setVisibility(View.GONE);
                    captureImg.setVisibility(View.GONE);

                    if (!isImaging) {
                        isImaging = true;
                        if (isFlat) {
                            warningText.setVisibility(View.GONE);
                            captureButton.setImageResource(R.drawable.green_camera_icon);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "UI update failed", e);
            }
        });
    }

    private void setFlashMode(int mode) {
        currentFlashMode = mode;

        if (imageCapture == null || flashButton == null) return;

        imageCapture.setFlashMode(mode);

        int newDrawableRes;
        boolean enableTorch = false;

        switch (mode) {
            case ImageCapture.FLASH_MODE_OFF:
                newDrawableRes = R.drawable.flash_off;
                break;
            case ImageCapture.FLASH_MODE_ON:
                newDrawableRes = R.drawable.flash_on;
                enableTorch = true;
                break;
            case ImageCapture.FLASH_MODE_AUTO:
            default:
                newDrawableRes = R.drawable.flash_auto;
                break;
        }

        flashButton.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            flashButton.setImageResource(newDrawableRes);
            flashButton.animate().alpha(1f).setDuration(150).start();
        }).start();

        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(enableTorch);
        }
    }

    private void captureImage() {
        if (isCapturing.get() || isDestroyed() || imageCapture == null) {
            return;
        }
        isCapturing.set(true);

        File photoFile = createImageFile();
        if (photoFile == null) {
            isCapturing.set(false);
            return;
        }

        playShutterSound();

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                handleImageSaved(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                handleCaptureError(exception);
            }
        });
    }

    private File createImageFile() {
        try {
            File cacheDir = new File(getCacheDir(), "calibration");
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdirs()) {
                    Log.w(TAG, "Failed to create cache directory");
                }
            }
            return File.createTempFile("captured_image_" + System.currentTimeMillis(), ".jpg", cacheDir);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create image file", e);
            return null;
        }
    }

    private void navigateToSymptoms(File photoFile) {
        if (isDestroyed() || isFinishing() || photoFile == null) return;

        try {
            uploadImage(photoFile, woundId, sessionId);
        } catch (Exception e) {
            Log.e(TAG, "Navigation failed", e);
            showToast("Error saving image");
        }
    }

    private void navigateToImagePreview() {
        if (isDestroyed() || isFinishing()) return;

        try {
            Intent intent = new Intent(CameraActivity.this, ImagePreviewActivity.class);
            intent.putStringArrayListExtra("filePaths", new ArrayList<>(filePaths));
            intent.putExtra("lensFocusDistances", new ArrayList<>(lensFocusDistances));
            intent.putExtra("coinType", coinType);
            intent.putExtra("userId", userId);
            intent.putExtra("token", token);
            intent.putExtra("woundScoreRequired", woundScoreRequired);
            intent.putExtra("woundLocationRequired", woundLocationRequired);
            intent.putExtra("primaryColor", primaryColor);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to preview", e);
        }
    }

    private void handleCaptureError(ImageCaptureException exception) {
        Log.e(TAG, "Photo capture failed", exception);
        isCapturing.set(false);
        showToast("Capture failed");
    }

    private void playShutterSound() {
        try {
            if (mediaActionSound == null) {
                mediaActionSound = new MediaActionSound();
            }
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK);
        } catch (Exception e) {
            Log.e(TAG, "Shutter sound error", e);
        }
    }

    private void uploadImage(File file, String woundId, String sessionId) {
        if (isDestroyed()) return;

        // Show upload progress dialog
        showUploadProgressDialog();

        // Set up the repository callbacks
        repository.setGetCommonAPIDetails(new Repository.GetCommonAPIDataSuccessCallBack() {
            @Override
            public void getCommonAPIDataSuccess(ResponseBody apiArrayResponse) {
                closeUploadProgressDialog();
                runOnUiThread(() -> handleUploadSuccess(apiArrayResponse));
            }

            @Override
            public void getCommonAPIDataFailure(String message) {
                closeUploadProgressDialog();
                runOnUiThread(() -> handleUploadFailure(message));
            }

            @Override
            public void onProgressUpdate(int progress) {
                // Update progress bar with smooth animation
                if (uploadProgressBar != null && !isDestroyed()) {
                    runOnUiThread(() -> {
                        uploadProgressBar.setProgress(progress, true);
                        if (uploadStatusText != null) {
                            uploadStatusText.setText(String.format(getString(R.string.uploading_progress_format), progress));
                        }
                        if (progress >= 100) {
                            if (uploadStatusText != null) {
                                uploadStatusText.setText(R.string.uploading_finalizing);
                            }
                        }
                    });
                }
                Log.d(TAG, "Upload progress: " + progress + "%");
            }
        });

        // Start the upload - pass null for progressListener since we're handling it through the repository callback
        repository.uploadImage(file, userId, woundId, sessionId, token, null);
    }

    private void showUploadProgressDialog() {
        if (isDestroyed()) return;

        runOnUiThread(() -> {
            try {
                // Create dialog view with progress bar
                View dialogView = getLayoutInflater().inflate(R.layout.upload_progress_dialog, null);
                uploadProgressBar = dialogView.findViewById(R.id.uploadProgressBar);
                uploadStatusText = dialogView.findViewById(R.id.uploadStatusText);

                // Set initial values
                uploadProgressBar.setProgress(0);
                uploadStatusText.setText(R.string.uploading_zero_percent);

                // Create and show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(dialogView);
                builder.setCancelable(false);  // Prevent cancellation during upload
                uploadProgressDialog = builder.create();

                if (uploadProgressDialog.getWindow() != null) {
                    uploadProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                }

                uploadProgressDialog.show();
                Log.d(TAG, "Upload progress dialog shown");
            } catch (Exception e) {
                Log.e(TAG, "Error showing upload dialog", e);
            }
        });
    }

    private void closeUploadProgressDialog() {
        if (isDestroyed()) return;

        runOnUiThread(() -> {
            try {
                if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
                    uploadProgressDialog.dismiss();
                    uploadProgressDialog = null;
                    Log.d(TAG, "Upload progress dialog dismissed");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing upload dialog", e);
            }
        });
    }

    private void handleUploadSuccess(ResponseBody apiArrayResponse) {
        if (isDestroyed() || isFinishing() || apiArrayResponse == null) return;

        runOnUiThread(() -> {
            try {
                Gson gson = new Gson();
                S3UploadResultModel result = gson.fromJson(apiArrayResponse.string(), S3UploadResultModel.class);

                Intent intent = new Intent(CameraActivity.this, SymptomQuestionActivity.class);
                intent.putExtra("imageUrl", result.getImageUrl());
                intent.putExtra("lensFocusDistance", lensFocusDistances.isEmpty() ? "0" : String.valueOf(lensFocusDistances.get(0)) );
                intent.putExtra("woundId", woundId);
                intent.putExtra("primaryColor", primaryColor);
                intent.putExtra("sessionId", sessionId);
                intent.putExtra("userId", userId);
                intent.putExtra("token", token);
                intent.putExtra("coinType", coinType);
                intent.putExtra("woundScoreRequired", woundScoreRequired);
                intent.putExtra("whereFrom", whereFrom);
                cameraLauncher.launch(intent);
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Upload processing failed", e);
                showToast("Error processing upload");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_CODE_SYMPTOM_ACTIVITY) {
            Log.d(TAG, "onActivityResult: Received result from SymptomQuestionActivity");
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "onActivityResult: Result OK, data: " + (data != null ? data.getExtras() : "null"));
                // Create a new intent to ensure we don't have any stale data
                Intent resultIntent = new Intent();
                if (data != null && data.getExtras() != null) {
                    resultIntent.putExtras(data.getExtras());
                }
                setResult(RESULT_OK, resultIntent);
            } else {
                Log.d(TAG, "onActivityResult: Result not OK, code: " + resultCode);
                setResult(RESULT_CANCELED);
            }
            finish();
        }
    }

    private void handleUploadFailure(String message) {
        runOnUiThread(() -> showToast(message));
        if (!isDestroyed()) {
            showToast("Upload failed");
        }
        Log.e(TAG, "Upload failed: " + message);
    }

    @Override
    public void onFlatStatusChanged(boolean isFlat) {
        this.isFlat = isFlat;
        if (isDestroyed() || warningText == null || captureButton == null) return;

        runOnUiThread(() -> {
            if (isImaging && isFlat) {
                captureButton.setImageResource(R.drawable.green_camera_icon);
                warningText.setVisibility(View.GONE);
            } else {
                captureButton.setImageResource(R.drawable.camera_icon);
                warningText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showToast(String message) {
        if (!isDestroyed()) {
            runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
        }
    }

    private void updateProgressBar(int progress) {
        if (isDestroyed() || segments == null) return;

        runOnUiThread(() -> {
            try {
                int filledColor = Color.parseColor(primaryColor);
                int emptyColor = ContextCompat.getColor(this, android.R.color.darker_gray);

                for (int i = 0; i < segments.length; i++) {
                    if (segments[i] == null) continue;

                    GradientDrawable drawable = (GradientDrawable) segments[i].getBackground();
                    if (drawable == null) continue;

                    drawable.setColor(i < progress ? filledColor : emptyColor);
                }
            } catch (Exception e) {
                Log.e(TAG, "Progress bar update failed", e);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !isDestroyed() && previewView != null && camera != null) {
            // Get PreviewView bounds
            int[] previewLocation = new int[2];
            previewView.getLocationOnScreen(previewLocation);
            float touchX = event.getX();
            float touchY = event.getY();
            float relativeX = touchX - previewLocation[0];
            float relativeY = touchY - previewLocation[1];

            // Check if touch is within PreviewView bounds
            if (relativeX >= 0 && relativeX <= previewView.getWidth() && relativeY >= 0 && relativeY <= previewView.getHeight()) {
                focusAtPoint(relativeX, relativeY);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void focusAtPoint(float x, float y) {
        if (camera == null || isDestroyed() || previewView == null || focusIndicator == null) {
            return;
        }

        // Ensure PreviewView maintains original size
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        // Create metering point for camera focus
        MeteringPointFactory factory = new SurfaceOrientedMeteringPointFactory(previewView.getWidth(), previewView.getHeight());
        MeteringPoint point = factory.createPoint(x, y);

        FocusMeteringAction action = new FocusMeteringAction.Builder(point).setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS).build();

        // Lock zoom to prevent scaling
        camera.getCameraControl().setZoomRatio(1.0f);

        camera.getCameraControl().startFocusAndMetering(action).addListener(() -> {
            if (!isDestroyed()) {
                runOnUiThread(() -> {
                    // Position FocusCircle at touch point
                    focusIndicator.setPosition(x, y);
                    focusIndicator.setVisibility(View.VISIBLE);
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasPermission = CameraPermissionHelper.hasCameraPermission(this);
        updateViewVisibility();

        if (!hasPermission && !hasRequestedPermission) {
            CameraPermissionHelper.requestCameraPermission(this);
            hasRequestedPermission = true;
        }

        // Ensure PreviewView size and zoom are reset
        if (previewView != null) {
            previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        }
        if (camera != null) {
            camera.getCameraControl().setZoomRatio(1.0f);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        hasPermission = CameraPermissionHelper.hasCameraPermission(this);
        updateViewVisibility();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isCapturing.set(false);

        if (cameraProvider != null) {
            try {
                cameraProvider.unbindAll();
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding camera", e);
            }
        }

        if (mediaActionSound != null) {
            try {
                mediaActionSound.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media sound", e);
            }
        }

        if (gyroscopeChecker != null) {
            gyroscopeChecker.stopListening();
        }
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}