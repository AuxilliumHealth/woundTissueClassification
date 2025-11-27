package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Intent;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.auxilliumhealth.woundtissueclassification.Model.ResultDataModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Utils.CoinDetector;
import com.auxilliumhealth.woundtissueclassification.Utils.LassoView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LassoActivity extends AppCompatActivity {

    private ImageView imageView;
    private LassoView lassoView;
    private MaterialButton saveButton, undoButton;
    private int imagePosition = -1;
    private float[] croppedCoords;
    private File originalFile;
    private String primaryColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lasso);
        if (!OpenCVLoader.initDebug()) {
            Log.e("CoinDetector", "OpenCV initialization failed.");
        } else {
            Log.d("CoinDetector", "OpenCV initialized successfully.");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.primary_text_light));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // dark icons
        }
        primaryColor = getIntent().getStringExtra("primaryColor");
        imageView = findViewById(R.id.imageView);
        lassoView = findViewById(R.id.lassoView);
        saveButton = findViewById(R.id.saveButton);
        undoButton = findViewById(R.id.undoButton);
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        MaterialToolbar materialToolbar = findViewById(R.id.material_toolbar);
        materialToolbar.setBackgroundColor(Color.parseColor(primaryColor));
        appBarLayout.setBackgroundColor(Color.parseColor(primaryColor));
        saveButton.setBackgroundColor(Color.parseColor(primaryColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor(primaryColor));
        }

        lassoView.setImageView(imageView);

        String imagePath = getIntent().getStringExtra("imagePath");
        imagePosition = getIntent().getIntExtra("position", -1);

        if (imagePath == null || imagePath.isEmpty()) {
            Toast.makeText(this, "Image path not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        originalFile = new File(imagePath);
        loadImageFromFilePath(imagePath);

        saveButton.setOnClickListener(v -> saveLassoOverlay(imagePath));
        undoButton.setOnClickListener(v -> lassoView.undo());
    }

    private void saveLassoOverlay(String imagePath) {
        Bitmap finalImage = getBitmapWithLasso();
        Bitmap croppedImage = getBitmap();

        if (finalImage == null || croppedImage == null) {
            Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Mat overlayMat = new Mat();
            Mat croppedMat = new Mat();
            Utils.bitmapToMat(finalImage, overlayMat);
            Utils.bitmapToMat(croppedImage, croppedMat);

            File cacheDir = new File(getCacheDir(), "calibration");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File overlayFile = new File(cacheDir, "overlay_" + originalFile.getName());
            File croppedFile = new File(cacheDir, "cropped_" + originalFile.getName());

            saveMatToFile(overlayMat, overlayFile);
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
                setResult(RESULT_OK, resultIntent);
                finish();

            } catch (CoinDetector.CustomError e) {
                e.printStackTrace();
                Toast.makeText(this, "Detection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                returnWithFallbackPath(imagePath);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void loadImageFromFilePath(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            finish();
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
        float scaleX = original.getWidth() / lassoView.getImageBounds().width();
        float scaleY = original.getHeight() / lassoView.getImageBounds().height();
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

        croppedCoords = new float[]{bounds.left, bounds.top, bounds.right, bounds.bottom};

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
