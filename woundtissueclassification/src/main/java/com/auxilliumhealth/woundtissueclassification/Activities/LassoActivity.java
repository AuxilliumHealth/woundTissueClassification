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
package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.auxilliumhealth.woundtissueclassification.Model.ResultDataModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Utils.CoinDetector;
import com.auxilliumhealth.woundtissueclassification.Utils.LassoView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LassoActivity extends AppCompatActivity {

    private ImageView imageView;
    private LassoView lassoView;
    private MaterialButton saveButton, clearButton;
    private CircularProgressIndicator loadingProgress;
    private int imagePosition = -1;
    private float[] croppedCoords;
    private File originalFile;
    private String primaryColor,whereFrom;
    private String lensFocusDistance, woundId, sessionId, coinType, userId, token;
    private boolean woundScoreRequired = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lasso);
        
        if (!OpenCVLoader.initDebug()) {
            Log.e("CoinDetector", "OpenCV initialization failed.");
        }

        initializeColorAndStatusbar();

        imageView = findViewById(R.id.imageView);
        lassoView = findViewById(R.id.lassoView);
        saveButton = findViewById(R.id.saveButton);
        clearButton = findViewById(R.id.clearButton);
        
        initializeUIText();

        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        MaterialToolbar materialToolbar = findViewById(R.id.material_toolbar);
        
        if (primaryColor != null) {
            try {
                int color = Color.parseColor(primaryColor);
                materialToolbar.setBackgroundColor(color);
                appBarLayout.setBackgroundColor(color);
                saveButton.setRippleColorResource(android.R.color.white);
                saveButton.setBackgroundTintList(ColorStateList.valueOf(color));
            } catch (IllegalArgumentException e) {
                Log.e("LassoActivity", "Invalid primary color: " + primaryColor);
            }
        }

        materialToolbar.setNavigationOnClickListener(v -> finish());
        
        lassoView.setImageView(imageView);

        String imagePath = getIntent().getStringExtra("imagePath");
        imagePosition = getIntent().getIntExtra("position", -1);
        whereFrom=getIntent().getStringExtra("whereFrom");
        lensFocusDistance = getIntent().getStringExtra("lensFocusDistance");
        woundId = getIntent().getStringExtra("woundId");
        sessionId = getIntent().getStringExtra("sessionId");
        userId = getIntent().getStringExtra("userId");
        token = getIntent().getStringExtra("token");
        coinType = getIntent().getStringExtra("coinType");
        woundScoreRequired = getIntent().getBooleanExtra("woundScoreRequired", true);



        if (imagePath == null || imagePath.isEmpty()) {
            Toast.makeText(this, "Image path not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        originalFile = new File(imagePath);
        loadingProgress = findViewById(R.id.loadingProgress);
        loadImage(imagePath);

        saveButton.setOnClickListener(v -> saveLassoOverlay(imagePath));
        clearButton.setOnClickListener(v -> lassoView.clear());

        // Initial state
        saveButton.setEnabled(false);
        saveButton.setAlpha(0.6f);

        lassoView.setOnPathChangedListener(hasPath -> {
            saveButton.setEnabled(hasPath);
            saveButton.setAlpha(hasPath ? 1.0f : 0.6f);
            
            if (hasPath) {
                clearButton.setVisibility(View.VISIBLE);
            } else {
                // Keep them visible but maybe we can hide them or just keep as is
            }
        });
    }

    private void initializeUIText() {
        MaterialToolbar materialToolbar = findViewById(R.id.material_toolbar);
        TextView instructionText = findViewById(R.id.instructionText);
        
        if ("Calibration".equalsIgnoreCase(whereFrom)) {
            materialToolbar.setTitle("Coin Calibration");
            instructionText.setText("Draw around the coin area");
            saveButton.setText("Done");
        } else {
            materialToolbar.setTitle("Mark Wound Region");
            instructionText.setText("Draw around the wound area");
            saveButton.setText("Done");
        }
    }

    private void initializeColorAndStatusbar() {
        primaryColor = getIntent().getStringExtra("primaryColor");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            if (primaryColor != null) {
                window.setStatusBarColor(Color.parseColor(primaryColor));
            } else {
                window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.black));
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Since we have a dark background/primary color usually, 
            // we don't necessarily want light status bar icons.
            // But let's follow the previous logic if it was intended.
            // getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void saveLassoOverlay(String imagePath) {
        Bitmap finalImage = getBitmapWithLasso();
        Bitmap croppedImage = getBitmap();

        if (finalImage == null || croppedImage == null) {
            Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File cacheDir = new File(getCacheDir(), "lasso_processing");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File overlayFile = new File(cacheDir, "overlay_" + originalFile.getName());
            
            // Save the overlay (image with lasso drawn)
            try (FileOutputStream out = new FileOutputStream(overlayFile)) {
                finalImage.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }

            if (!"Calibration".equalsIgnoreCase(whereFrom)) {
                // Non-calibration mode: Return coordinates and overlay path only
                Intent resultIntent = new Intent();
                resultIntent.putExtra("overlayPath", overlayFile.getAbsolutePath());
                resultIntent.putExtra("imagePath", imagePath);
                resultIntent.putExtra("position", imagePosition);
                resultIntent.putExtra("coordinates", croppedCoords);
                resultIntent.putExtra("lensFocusDistance", lensFocusDistance);
                resultIntent.putExtra("woundId", woundId);
                resultIntent.putExtra("sessionId", sessionId);
                resultIntent.putExtra("userId", userId);
                resultIntent.putExtra("token", token);
                resultIntent.putExtra("coinType", coinType);
                resultIntent.putExtra("woundScoreRequired", woundScoreRequired);
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }

            // Calibration mode: Run CoinDetector
            Mat overlayMat = new Mat();
            Mat croppedMat = new Mat();
            Utils.bitmapToMat(finalImage, overlayMat);
            Utils.bitmapToMat(croppedImage, croppedMat);

            File calibCacheDir = new File(getCacheDir(), "calibration");
            if (!calibCacheDir.exists()) {
                calibCacheDir.mkdirs();
            }
            File croppedFile = new File(calibCacheDir, "cropped_" + originalFile.getName());
            saveMatToFile(croppedMat, croppedFile);

            try {
                if (croppedCoords == null) {
                    Log.e("LassoActivity", "croppedCoords is null");
                    throw new CoinDetector.CustomError("No coordinates found.");
                }

                CoinDetector coinDetector = new CoinDetector();
                ResultDataModel result = coinDetector.detectCoinsExtractPixelCounts(LassoActivity.this, croppedCoords, imagePath);

                Intent resultIntent = new Intent();
                if (result == null || result.getcontourImage() == null || result.getPixelCount() == 0) {
                    resultIntent.putExtra("overlayPath", imagePath);
                    resultIntent.putExtra("pixelCount", 0);
                    Log.w("LassoActivity", "Result or contour image is null.");
                } else {
                    resultIntent.putExtra("overlayPath", result.getcontourImage().getAbsolutePath());
                    resultIntent.putExtra("pixelCount", result.getPixelCount());
                    Log.d("LassoActivity", "Result: " + result.getPixelCount());
                }

                resultIntent.putExtra("position", imagePosition);
                resultIntent.putExtra("coordinates", croppedCoords);
                setResult(RESULT_OK, resultIntent);
                finish();

            } catch (CoinDetector.CustomError e) {
                e.printStackTrace();
                Toast.makeText(this, "Unable to process calibration. Please try again.", Toast.LENGTH_SHORT).show();
                returnWithFallbackPath(imagePath);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save information. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void returnWithFallbackPath(String imagePath) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("overlayPath", imagePath);
        resultIntent.putExtra("position", imagePosition);
        resultIntent.putExtra("pixelCount", 0);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void saveMatToFile(Mat mat, File outputFile) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Log.d("LassoActivity", "Saved image to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("LassoActivity", "Failed to save image: " + outputFile.getAbsolutePath(), e);
            throw e;
        }
    }

    private void loadImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return;

        if (imagePath.startsWith("http")) {
            loadingProgress.setVisibility(View.VISIBLE);
            
            // For remote URLs, we use Glide to load it into the ImageView
            // We also attempt to get the file so downstream logic (CoinDetector) works
            Glide.with(this)
                .asBitmap()
                .load(imagePath)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        runOnUiThread(() -> {
                            loadingProgress.setVisibility(View.GONE);
                            Toast.makeText(LassoActivity.this, "Failed to load remote image", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        runOnUiThread(() -> {
                            loadingProgress.setVisibility(View.GONE);
                            imageView.setImageBitmap(resource);
                            
                            // Essential: trigger bounds calculation once image is actually set
                            imageView.post(() -> {
                                lassoView.calculateImageBounds();
                            });

                            // Try to get a local file for this URL
                            new Thread(() -> {
                                try {
                                    File cacheFile = Glide.with(LassoActivity.this)
                                        .asFile()
                                        .load(imagePath)
                                        .submit()
                                        .get();
                                    
                                    // Copy to a permanent location in our cache for originalFile
                                    File internalCache = new File(getCacheDir(), "original_images");
                                    if (!internalCache.exists()) internalCache.mkdirs();
                                    File localCopy = new File(internalCache, "remote_" + System.currentTimeMillis() + ".jpg");
                                    
                                    try (InputStream in = new FileInputStream(cacheFile);
                                         OutputStream out = new FileOutputStream(localCopy)) {
                                        byte[] buf = new byte[8192];
                                        int read;
                                        while ((read = in.read(buf)) > 0) {
                                            out.write(buf, 0, read);
                                        }
                                    }
                                    
                                    originalFile = localCopy;
                                    Log.d("LassoActivity", "Remote image cached locally at: " + originalFile.getAbsolutePath());
                                } catch (Exception e) {
                                    Log.e("LassoActivity", "Error caching remote image", e);
                                }
                            }).start();
                        });
                        return true;
                    }
                })
                .into(imageView);
        } else {
            // Local file logic
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                originalFile = new File(imagePath);
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private Bitmap getBitmapWithLasso() {
        Bitmap original = getOriginalBitmapFromDrawable();
        if (original == null) return null;

        Bitmap result = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(original, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setAntiAlias(true);

        Matrix matrix = new Matrix();
        float scaleX = original.getWidth() / lassoView.getImageBounds().width();
        float scaleY = original.getHeight() / lassoView.getImageBounds().height();
        matrix.postScale(scaleX, scaleY);
        matrix.postTranslate(-lassoView.getImageBounds().left * scaleX, -lassoView.getImageBounds().top * scaleY);

        for (Path path : lassoView.getAllPaths()) {
            Path scaledPath = new Path(path);
            scaledPath.transform(matrix);
            canvas.drawPath(scaledPath, paint);
        }

        return result;
    }

    private Bitmap getBitmap() {
        Bitmap original = getOriginalBitmapFromDrawable();
        if (original == null || lassoView.getAllPaths().isEmpty()) return null;

        Matrix matrix = new Matrix();
        float scaleX = (float) original.getWidth() / lassoView.getImageBounds().width();
        float scaleY = (float) original.getHeight() / lassoView.getImageBounds().height();
        matrix.postScale(scaleX, scaleY);
        matrix.postTranslate(-lassoView.getImageBounds().left * scaleX, -lassoView.getImageBounds().top * scaleY);

        Path path = new Path(lassoView.getAllPaths().get(0));
        path.transform(matrix);
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);

        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        float halfSize = Math.max(bounds.width(), bounds.height()) / 2f;

        Rect cropRect = new Rect(Math.max(0, (int) (centerX - halfSize)), Math.max(0, (int) (centerY - halfSize)), Math.min(original.getWidth(), (int) (centerX + halfSize)), Math.min(original.getHeight(), (int) (centerY + halfSize)));

        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
            Log.e("LassoActivity", "Invalid crop dimensions: " + cropRect);
            return null;
        }

        // Calculate exact bounding box for the server (in original bitmap pixels first)
        float xmin = bounds.left;
        float ymin = bounds.top;
        float xmax = bounds.right;
        float ymax = bounds.bottom;

        // Scale coordinates from downsampled bitmap pixels to original file pixels
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);
        
        int fullWidth = options.outWidth;
        int fullHeight = options.outHeight;
        
        if (fullWidth > 0 && fullHeight > 0) {
            float fileScaleX = (float) fullWidth / original.getWidth();
            float fileScaleY = (float) fullHeight / original.getHeight();
            
            croppedCoords = new float[]{
                    xmin * fileScaleX,
                    ymin * fileScaleY,
                    xmax * fileScaleX,
                    ymax * fileScaleY
            };
            Log.d("LassoActivity", String.format("Scaled coords to file (%dx%d): [%.2f, %.2f, %.2f, %.2f]", 
                fullWidth, fullHeight, croppedCoords[0], croppedCoords[1], croppedCoords[2], croppedCoords[3]));
        } else {
            // Fallback if file reading fails
            croppedCoords = new float[]{xmin, ymin, xmax, ymax};
        }

        return Bitmap.createBitmap(original, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
    }

    private Bitmap getOriginalBitmapFromDrawable() {
        Drawable drawable = imageView.getDrawable();
        if (drawable == null) return null;

        try {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            Log.e("LassoActivity", "Drawable to Bitmap conversion failed", e);
            return null;
        }
    }
}
