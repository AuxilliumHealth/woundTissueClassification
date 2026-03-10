package com.auxilliumhealth.woundtissueclassification.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.auxilliumhealth.woundtissueclassification.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.auxilliumhealth.woundtissueclassification.Utils.ContourUtils;

import java.util.ArrayList;
import java.util.List;

public class WoundImageEditActivity extends AppCompatActivity {

    private static final String TAG = "WoundImageEditActivity";

    private ProgressBar progressBar;
    private MaterialTextView instructionText;
    private MaterialTextView measurementText;   // Auto area from mask
    private MaterialButton undoBtn;
    private ContourCanvasView canvasView;

    private Bitmap baseBitmap;
    private Bitmap maskBitmap;

    private String primaryColor;
    private double lensFocusDistance;
    /**
     * Raw strings from intent — parsed by ContourUtils.parseCoefficients
     */
    private String areaCoeffsStr;
    private String ppuStr;
    /**
     * Pixels per metre — set after OpenCV finishes, passed down to the canvas view.
     */
    private volatile double pixelPerUnit = 0;
    /**
     * Wound area (cm²) computed from mask pixel count.
     */
    private volatile double woundArea = 0;
    /**
     * Latest pixel distances for A→B and C→D — updated by canvas listener.
     */
    private double lastPixDistAB = 0;
    private double lastPixDistCD = 0;

    /**
     * Request code used by SymptomQuestionActivity.
     */
    public static final int REQUEST_CODE = 1001;
    /**
     * Result extra keys.
     */
    public static final String EXTRA_AREA_CM2 = "result_area_cm2";
    public static final String EXTRA_LENGTH_CM = "result_length_cm";
    public static final String EXTRA_WIDTH_CM = "result_width_cm";
    public static final String EXTRA_IMAGE_PATH = "result_image_path";
    String depth;
    String area;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wound_image_edit);

        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(this, "OpenCV failed to load", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        progressBar = findViewById(R.id.progress_bar);
        instructionText = findViewById(R.id.instruction_text);
        measurementText = findViewById(R.id.measurement_text);
        undoBtn = findViewById(R.id.btn_undo);
        MaterialButton clearBtn = findViewById(R.id.btn_clear);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        MaterialButton saveBtn = findViewById(R.id.btn_save);
        saveBtn.setEnabled(false);   // enabled only when both lines are drawn
        saveBtn.setAlpha(0.45f);
        FrameLayout canvasContainer = findViewById(R.id.canvas_container);

        // Intent data
        String croppedImagePath = getIntent().getStringExtra("croppedImagePath");
        String croppedGrabcutMask = getIntent().getStringExtra("croppedGrabcutMask");
        primaryColor = getIntent().getStringExtra("primaryColor");
        areaCoeffsStr = getIntent().getStringExtra("areaCoeffs");
        ppuStr = getIntent().getStringExtra("pixelPerUnit");
        String focusStr = getIntent().getStringExtra("lensFocusDistance");

        // area and depth are sent as Double extras from SymptomQuestionActivity
        double areaDouble  = getIntent().getDoubleExtra("area", 0.0);
        double depthDouble = getIntent().getDoubleExtra("depth", 0.0);
        area  = areaDouble  > 0 ? String.format("%.2f", areaDouble)  : "";
        depth = depthDouble > 0 ? String.format("%.2f", depthDouble) : "";

        try {
            lensFocusDistance = Double.parseDouble(focusStr != null ? focusStr : "0");
        } catch (Exception e) {
            lensFocusDistance = 0;
        }

        if (primaryColor != null) {
            try {
                int c = Color.parseColor(primaryColor);
                toolbar.setBackgroundColor(c);
                saveBtn.setBackgroundColor(c);
                undoBtn.setBackgroundColor(c);
                getWindow().setStatusBarColor(c);
            } catch (Exception ignored) {
            }
        }

        toolbar.setNavigationOnClickListener(v -> finish());
        final MaterialButton finalSaveBtn = saveBtn;
        saveBtn.setOnClickListener(v -> onDoneClicked());
        undoBtn.setOnClickListener(v -> {
            if (canvasView != null) canvasView.undoPoint();
        });
        clearBtn.setOnClickListener(v -> {
            if (canvasView != null) {
                canvasView.resetPoints();
                finalSaveBtn.setEnabled(false);
                finalSaveBtn.setAlpha(0.45f);
                updateInstructions(0);
            }
        });

        if (croppedImagePath == null || croppedGrabcutMask == null) {
            Toast.makeText(this, "Missing image data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Build the custom canvas view
        canvasView = new ContourCanvasView(this);
        canvasContainer.addView(canvasView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        final MaterialButton doneBtn = saveBtn;
        canvasView.setOnPointsChangedListener((count, pixDistAB, pixDistCD) -> {
            updateInstructions(count);
            lastPixDistAB = pixDistAB;
            lastPixDistCD = pixDistCD;

            // Enable Done only when both lines are complete
            boolean bothDone = count == 4;
            doneBtn.setEnabled(bothDone);
            doneBtn.setAlpha(bothDone ? 1f : 0.45f);

            // Live-update banner
            String lengthStr = null;
            String widthStr = null;
            if (count >= 2 && pixDistAB > 0) {
                lengthStr = pixelPerUnit > 0
                        ? String.format("%.2f cm", (pixDistAB / pixelPerUnit) * 100.0)
                        : String.format("%.0f px", pixDistAB);
            }
            if (count >= 4 && pixDistCD > 0) {
                widthStr = pixelPerUnit > 0
                        ? String.format("%.2f cm", (pixDistCD / pixelPerUnit) * 100.0)
                        : String.format("%.0f px", pixDistCD);
            }
            measurementText.setText(buildBanner(woundArea, lengthStr, widthStr));
            measurementText.setVisibility(View.VISIBLE);
        });

        loadImages(croppedImagePath, croppedGrabcutMask);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void updateInstructions(int count) {
        switch (count) {
            case 0:
                instructionText.setText("Step 1 of 2 — Tap the TOP edge of the wound to measure length");
                break;
            case 1:
                instructionText.setText("Step 1 of 2 — Tap the BOTTOM edge of the wound (opposite side)");
                break;
            case 2:
                instructionText.setText("Step 2 of 2 — Tap the LEFT edge of the wound to measure width");
                break;
            case 3:
                instructionText.setText("Step 2 of 2 — Tap the RIGHT edge of the wound (opposite side)");
                break;
            default:
                instructionText.setText("✓ Wound size marked! Press Done to save the measurements.");
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Done handler — captures canvas, returns result to caller
    // ═══════════════════════════════════════════════════════════════════════

    private void onDoneClicked() {
        if (canvasView == null) {
            finish();
            return;
        }

        // Capture the canvas view as a bitmap
        Bitmap snapshot = Bitmap.createBitmap(
                canvasView.getWidth(), canvasView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(snapshot);
        canvasView.draw(c);

        // Save bitmap to cache file
        java.io.File outFile = new java.io.File(getCacheDir(), "wound_axis_" + System.currentTimeMillis() + ".jpg");
        boolean saved = false;
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile)) {
            snapshot.compress(Bitmap.CompressFormat.JPEG, 92, fos);
            saved = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save axis image", e);
        }

        // Compute cm values
        double lengthCm = (pixelPerUnit > 0 && lastPixDistAB > 0)
                ? (lastPixDistAB / pixelPerUnit) * 100.0 : 0;
        double widthCm = (pixelPerUnit > 0 && lastPixDistCD > 0)
                ? (lastPixDistCD / pixelPerUnit) * 100.0 : 0;

        Intent result = new Intent();
        result.putExtra(EXTRA_LENGTH_CM, lengthCm);
        result.putExtra(EXTRA_WIDTH_CM, widthCm);
        result.putExtra("depth", depth);
        result.putExtra("area", area);
        if (saved) result.putExtra(EXTRA_IMAGE_PATH, outFile.getAbsolutePath());
        setResult(RESULT_OK, result);
        finish();
    }

    /**
     * Builds the measurement summary banner text.
     * Pass null for length/width strings to omit those values.
     */
    private String buildBanner(double areaCm2, String lengthStr, String widthStr) {
        StringBuilder sb = new StringBuilder();
//        sb.append(String.format("🩹 Area: %.2f cm²", areaCm2));
        if (lengthStr != null) sb.append("    ​​​   📏 Length: ").append(lengthStr);
        if (widthStr != null) sb.append("   |​​​   📐 Width:  ").append(widthStr);
        return sb.toString();
    }

    /**
     * Convenience overload used on reset (no line measurements).
     */
    private String buildBanner(double areaCm2) {
        return buildBanner(areaCm2, null, null);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Image downloading
    // ═══════════════════════════════════════════════════════════════════════

    private int baseRetryCount = 0;
    private int maskRetryCount = 0;
    private static final int MAX_RETRY = 3;

    private void loadImages(String baseUrl, String maskUrl) {
        progressBar.setVisibility(View.VISIBLE);
        baseRetryCount = 0;
        maskRetryCount = 0;
        loadBaseImage(baseUrl);
        loadMaskImage(maskUrl);
    }

    private void loadBaseImage(String url) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap res,
                            @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> t) {
                        baseBitmap = res;
                        checkAndProcess();
                    }
                    @Override public void onLoadCleared(@Nullable Drawable p) {}
                    @Override
                    public void onLoadFailed(@Nullable Drawable e) {
                        Log.w("WoundImageEdit", "Base image load failed (attempt " + (baseRetryCount+1) + "): " + url);
                        if (baseRetryCount < MAX_RETRY) {
                            baseRetryCount++;
                            new android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed(() -> loadBaseImage(url), 1000L * baseRetryCount);
                        } else {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(WoundImageEditActivity.this,
                                        "Failed to load wound image after " + MAX_RETRY + " attempts",
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
    }

    private void loadMaskImage(String url) {
        Glide.with(this)
                .asBitmap()
                .load(url)
                .skipMemoryCache(true)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap res,
                            @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> t) {
                        maskBitmap = res;
                        checkAndProcess();
                    }
                    @Override public void onLoadCleared(@Nullable Drawable p) {}
                    @Override
                    public void onLoadFailed(@Nullable Drawable e) {
                        Log.w("WoundImageEdit", "Mask image load failed (attempt " + (maskRetryCount+1) + "): " + url);
                        if (maskRetryCount < MAX_RETRY) {
                            maskRetryCount++;
                            new android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed(() -> loadMaskImage(url), 1000L * maskRetryCount);
                        } else {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(WoundImageEditActivity.this,
                                        "Failed to load mask image after " + MAX_RETRY + " attempts",
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
    }


    private synchronized void checkAndProcess() {
        if (baseBitmap != null && maskBitmap != null) new Thread(this::processWithOpenCV).start();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // OpenCV pipeline (mirrors AiProcessImageActivity logic via ContourUtils)
    // ═══════════════════════════════════════════════════════════════════════

    private void processWithOpenCV() {
        try {
            int w = baseBitmap.getWidth();
            int h = baseBitmap.getHeight();

            // ── Base image ──────────────────────────────────────────────
            Mat baseMat = new Mat();
            Utils.bitmapToMat(baseBitmap, baseMat);
            Imgproc.cvtColor(baseMat, baseMat, Imgproc.COLOR_RGBA2BGR);

            // ── Mask → binary woundMask8u ───────────────────────────────
            Bitmap scaledMask = Bitmap.createScaledBitmap(maskBitmap, w, h, false);
            Mat maskMat = new Mat();
            Utils.bitmapToMat(scaledMask, maskMat);
            Mat gray = new Mat();
            Imgproc.cvtColor(maskMat, gray, Imgproc.COLOR_RGBA2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
            Mat woundMask8u = new Mat();
            Imgproc.threshold(gray, woundMask8u, 127, 255, Imgproc.THRESH_BINARY);

            // ── 1. AREA (same as AiProcessImageActivity) ─────────────────
            int woundMaskNonZeroPixelCount = Core.countNonZero(woundMask8u);
            double[] areaCoeff = ContourUtils.parseCoefficients(areaCoeffsStr);
            double areaPerPixel = ContourUtils.calculateAreaPerPixel(lensFocusDistance, areaCoeff);
            woundArea = (areaPerPixel > 0 && woundMaskNonZeroPixelCount > 0)
                    ? Math.max(0, areaPerPixel * woundMaskNonZeroPixelCount * 10000.0)
                    : 0;

            // ── 2. CALIBRATION: pixels per metre ────────────────────────
            double[] ppuCoeff = ContourUtils.parseCoefficients(ppuStr);
            pixelPerUnit = ContourUtils.calculatePPU(lensFocusDistance, ppuCoeff);

            // ── 3. CONTOURS: draw green border + collect snapping points ──
            List<MatOfPoint> woundContours = ContourUtils.extractContours(woundMask8u);
            List<List<android.graphics.PointF>> borderPoints = new ArrayList<>();
            android.graphics.PointF maskCentroid = null;
            if (woundContours != null) {
                Imgproc.drawContours(baseMat, woundContours, -1, new Scalar(0, 255, 0), 3);
                for (MatOfPoint contour : woundContours) {
                    List<android.graphics.PointF> subList = new ArrayList<>();
                    for (org.opencv.core.Point pt : contour.toArray()) {
                        subList.add(new android.graphics.PointF(
                                (float) (pt.x / w), (float) (pt.y / h)));
                    }
                    borderPoints.add(subList);
                }

                org.opencv.imgproc.Moments moments = org.opencv.imgproc.Imgproc.moments(woundMask8u, true);
                if (moments.m00 != 0) {
                    maskCentroid = new android.graphics.PointF((float) (moments.m10 / moments.m00 / w), (float) (moments.m01 / moments.m00 / h));
                } else if (!borderPoints.isEmpty()) {
                    float minX = 1f, maxX = 0f, minY = 1f, maxY = 0f;
                    for (List<android.graphics.PointF> contour : borderPoints) {
                        for (android.graphics.PointF p : contour) {
                            if (p.x < minX) minX = p.x;
                            if (p.x > maxX) maxX = p.x;
                            if (p.y < minY) minY = p.y;
                            if (p.y > maxY) maxY = p.y;
                        }
                    }
                    maskCentroid = new android.graphics.PointF((minX + maxX) / 2, (minY + maxY) / 2);
                }
            }
            // Length/Width are calculated only when the user draws A→B / C→D interactively.


            // ── Convert result to Bitmap ──────────────────────────────
            Imgproc.cvtColor(baseMat, baseMat, Imgproc.COLOR_BGR2RGBA);
            Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(baseMat, result);
            baseMat.release();
            maskMat.release();
            gray.release();
            woundMask8u.release();

            final String bannerStr = buildBanner(woundArea);
            final int finalW = w;
            final int finalH = h;
            final List<List<android.graphics.PointF>> finalBorderPoints = borderPoints;
            final android.graphics.PointF finalCentroid = maskCentroid;

            runOnUiThread(() -> {
                // Set original dimensions FIRST so pixelDist() is correct from the first tap
                canvasView.setImagePixelDimensions(finalW, finalH);
                canvasView.setPixelPerUnit(pixelPerUnit);
                canvasView.setData(result, finalBorderPoints, finalCentroid);
                progressBar.setVisibility(View.GONE);
                updateInstructions(0);
                measurementText.setText(bannerStr);
                measurementText.setVisibility(View.VISIBLE);
            });

        } catch (Exception e) {
            Log.e(TAG, "OpenCV error", e);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    /**
     * Unused placeholder kept for call-site compatibility.
     */
    private double imageBitmapScale(Bitmap bmp) {
        return 1.0;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ContourCanvasView
    //   pts[0]=12  pts[1]=6  → yellow dashed  → Length (12→6)
    //   pts[2]=9   pts[3]=3  → green dashed   → Width  (9→3)
    // ═══════════════════════════════════════════════════════════════════════

    public static class ContourCanvasView extends View {

        private static final float SNAP_RADIUS_DP = 20f;
        // [color-start, color-end, line-color]
        private static final int[][] CONFIG = {
                {Color.RED, Color.CYAN, Color.YELLOW},
                {Color.parseColor("#FF6600"), Color.parseColor("#FF00FF"), Color.GREEN},
        };
        // Clock-face labels: 12 o'clock (top), 6 o'clock (bottom), 9 o'clock (left), 3 o'clock (right)
        private static final String[] LABELS = {"12", "6", "9", "3"};

        private Bitmap imageBitmap;
        private List<List<PointF>> borderPoints = new ArrayList<>();
        private PointF maskCentroid = null;
        /**
         * Actual image pixel width of loaded bitmap (used to convert normalised coords → real pixels).
         */
        private float imgPixelWidth = 1;
        /**
         * True original pixel dimensions of the captured image — used by pixelDist()
         * so that pixel distances are in the same space pixelPerUnit was calibrated for.
         * These are set explicitly from processWithOpenCV() and are NOT affected by
         * whatever resolution Glide happened to download the image at.
         */
        private float imgOriginalWidth  = 1;
        private float imgOriginalHeight = 1;
        /**
         * Pixels per metre — used to draw real-world measurement on each line.
         */
        private double pixelPerUnit = 0;

        private final PointF[] pts = new PointF[4];

        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint centroidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private OnPointsChangedListener listener;

        /**
         * count = number of pts set (0–4), abPx = pixel distance A→B, cdPx = pixel distance C→D.
         */
        public interface OnPointsChangedListener {
            void onPointsChanged(int count, double abPx, double cdPx);
        }

        public ContourCanvasView(Context ctx) {
            super(ctx);
            float[] dash = {20f, 10f};
            dotPaint.setStyle(Paint.Style.FILL);
            ringPaint.setStyle(Paint.Style.FILL);
            ringPaint.setColor(Color.WHITE);
            line1Paint.setStyle(Paint.Style.STROKE);
            line1Paint.setStrokeWidth(4f);
            line1Paint.setColor(Color.YELLOW);
            line1Paint.setPathEffect(new DashPathEffect(dash, 0));
            line2Paint.setStyle(Paint.Style.STROKE);
            line2Paint.setStrokeWidth(4f);
            line2Paint.setColor(Color.GREEN);
            line2Paint.setPathEffect(new DashPathEffect(dash, 0));
            labelPaint.setColor(Color.WHITE);
            labelPaint.setTextSize(36f);
            labelPaint.setTextAlign(Paint.Align.CENTER);
            labelPaint.setShadowLayer(5f, 0, 0, Color.BLACK);
            // Centroid marker
            centroidPaint.setStyle(Paint.Style.FILL);
            centroidPaint.setColor(Color.parseColor("#FF4081"));
            centroidPaint.setShadowLayer(6f, 0, 0, Color.BLACK);
        }

        public void setPixelPerUnit(double ppu) {
            this.pixelPerUnit = ppu;
            invalidate();
        }

        /**
         * Must be called with the true original pixel dimensions of the wound image
         * (i.e. baseBitmap.getWidth/Height from processWithOpenCV BEFORE any scaling).
         * This ensures pixelDist() works in the same pixel space as pixelPerUnit.
         */
        public void setImagePixelDimensions(int originalWidth, int originalHeight) {
            this.imgOriginalWidth  = originalWidth;
            this.imgOriginalHeight = originalHeight;
        }

        public void setOnPointsChangedListener(OnPointsChangedListener l) {
            this.listener = l;
        }

        public void setData(Bitmap bmp, List<List<PointF>> points, PointF centroid) {
            imageBitmap = bmp;
            borderPoints = points;
            maskCentroid = centroid;
            imgPixelWidth = bmp.getWidth();
            // imgOriginalWidth/Height are set separately via setImagePixelDimensions()
            // If not yet set, default to the bitmap dimensions as a safe fallback
            if (imgOriginalWidth <= 1) imgOriginalWidth  = bmp.getWidth();
            if (imgOriginalHeight <= 1) imgOriginalHeight = bmp.getHeight();
            invalidate();
        }

        public void resetPoints() {
            for (int i = 0; i < 4; i++) pts[i] = null;
            invalidate();
            notifyListener();
        }

        /**
         * Removes the last placed point (most recent tap).
         */
        public void undoPoint() {
            int n = 0;
            for (PointF p : pts) if (p != null) n++;
            if (n > 0) {
                pts[n - 1] = null;
                invalidate();
                notifyListener();
            }
        }

        // ── Coordinate helpers ───────────────────────────────────────────

        /**
         * Returns [scale, offsetX, offsetY] for fit-center letterbox.
         */
        private float[] so() {
            if (imageBitmap == null) return new float[]{1, 0, 0};
            float s = Math.min(getWidth() / (float) imageBitmap.getWidth(),
                    getHeight() / (float) imageBitmap.getHeight());
            return new float[]{s,
                    (getWidth() - imageBitmap.getWidth() * s) / 2f,
                    (getHeight() - imageBitmap.getHeight() * s) / 2f};
        }

        /**
         * Normalised image-space → view-space
         */
        private PointF toView(PointF n, float s, float ox, float oy) {
            return new PointF(n.x * imageBitmap.getWidth() * s + ox,
                    n.y * imageBitmap.getHeight() * s + oy);
        }

        /**
         * Pixel distance between two normalised points, in the ORIGINAL image's pixel space.
         * This keeps the result consistent with pixelPerUnit which was calibrated
         * against the original capture resolution.
         */
        private double pixelDist(PointF a, PointF b) {
            if (a == null || b == null) return 0;
            double dx = (a.x - b.x) * imgOriginalWidth;
            double dy = (a.y - b.y) * imgOriginalHeight;
            return Math.sqrt(dx * dx + dy * dy);
        }

        // ── Drawing ──────────────────────────────────────────────────────

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (imageBitmap == null) return;
            float[] so = so();
            float sc = so[0], ox = so[1], oy = so[2];

            canvas.save();
            canvas.translate(ox, oy);
            canvas.scale(sc, sc);
            canvas.drawBitmap(imageBitmap, 0, 0, null);
            canvas.restore();

            drawPair(canvas, 0, 1, line1Paint, sc, ox, oy);   // 12→6 yellow
            drawPair(canvas, 2, 3, line2Paint, sc, ox, oy);   // 9→3  green
        }

        private void drawPair(Canvas canvas, int ia, int ib, Paint lp, float s, float ox, float oy) {
            if (pts[ia] == null) return;
            PointF va = toView(pts[ia], s, ox, oy);
            drawDot(canvas, va.x, va.y, CONFIG[ia / 2][0], LABELS[ia]);
            if (pts[ib] != null) {
                PointF vb = toView(pts[ib], s, ox, oy);
                drawDot(canvas, vb.x, vb.y, CONFIG[ia / 2][1], LABELS[ib]);
                canvas.drawLine(va.x, va.y, vb.x, vb.y, lp);
            }
        }

        private void drawDot(Canvas c, float x, float y, int color, String lbl) {
            c.drawCircle(x, y, 22f, ringPaint);
            dotPaint.setColor(color);
            c.drawCircle(x, y, 14f, dotPaint);
            c.drawText(lbl, x, y - 30f, labelPaint);
        }

        // ── Touch ────────────────────────────────────────────────────────

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_DOWN || imageBitmap == null)
                return super.onTouchEvent(e);
            int n = setCount();
            if (n >= 4) return true;

            float[] so = so();
            float sc = so[0], ox = so[1], oy = so[2];
            float iw = imageBitmap.getWidth(), ih = imageBitmap.getHeight();

            float nx = (e.getX() - ox) / (iw * sc);
            float ny = (e.getY() - oy) / (ih * sc);
            if (nx < 0 || nx > 1 || ny < 0 || ny > 1) return true;

            float snapR = (SNAP_RADIUS_DP * getResources().getDisplayMetrics().density) / (iw * sc);
            PointF snapped = nearest(nx, ny, snapR);
            if (snapped == null) {
                Toast.makeText(getContext(), "Tap closer to the green contour border", Toast.LENGTH_SHORT).show();
                return true;
            }

            pts[n] = snapped;

            // Auto fill the rest if this is the first point (12 o'clock)
            if (n == 0 && maskCentroid != null) {
                autoFillClockFace(snapped);
            }

            invalidate();
            notifyListener();
            return true;
        }

        private void autoFillClockFace(PointF p12) {
            if (maskCentroid == null || borderPoints == null || borderPoints.isEmpty()) return;

            float iw = imageBitmap != null ? imageBitmap.getWidth()  : 1f;
            float ih = imageBitmap != null ? imageBitmap.getHeight() : 1f;

            // ── Compute the 12→6 axis in PIXEL space ──────────────────────────
            // Normalised delta → pixel delta
            float dx_px = (p12.x - maskCentroid.x) * iw;
            float dy_px = (p12.y - maskCentroid.y) * ih;
            float dlen  = (float) Math.sqrt(dx_px * dx_px + dy_px * dy_px);
            if (dlen == 0) return;

            // Unit direction toward 12 o'clock, in pixel space
            float ux_px = dx_px / dlen;
            float uy_px = dy_px / dlen;

            // ── 6 o'clock: exact opposite in pixel space ──────────────────────
            // Convert pixel-space direction back to normalised-space direction for
            // rayContourIntersection (which works in normalised coords and
            // normalises the vector internally, so only proportions matter).
            pts[1] = rayContourIntersection(-ux_px / iw, -uy_px / ih);

            // ── 9 and 3 o'clock: perpendicular to 12→6 IN PIXEL SPACE ─────────
            // Pixel-space perpendiculars: (-uy_px, ux_px)  and  (uy_px, -ux_px)
            // Divided by image dimensions → normalised-space direction for ray cast
            pts[2] = rayContourIntersection( uy_px / iw, -ux_px / ih);   // 3 o'clock (right)
            pts[3] = rayContourIntersection(-uy_px / iw,  ux_px / ih);   // 9 o'clock (left)
        }

        /**
         * Casts a ray from maskCentroid in direction (vx, vy) and returns the EXACT
         * point where the ray crosses the contour polygon boundary edge.
         * This guarantees the point lies precisely on the contour line (not just near it),
         * so lines 12–6 and 9–3 are perfectly straight and perpendicular.
         */
        private PointF rayContourIntersection(float vx, float vy) {
            if (borderPoints == null || borderPoints.isEmpty() || maskCentroid == null) return null;

            float len = (float) Math.sqrt(vx * vx + vy * vy);
            if (len == 0) return null;
            final float nx = vx / len, ny = vy / len; // normalized ray direction
            final float cx = maskCentroid.x, cy = maskCentroid.y;

            PointF best = null;
            float bestT = Float.MAX_VALUE;

            for (List<PointF> contour : borderPoints) {
                int n = contour.size();
                for (int i = 0; i < n; i++) {
                    PointF a = contour.get(i);
                    PointF b = contour.get((i + 1) % n);

                    // Solve: C + t*(nx,ny) = A + s*(B-A)  for t>0, 0<=s<=1
                    float bax = b.x - a.x;
                    float bay = b.y - a.y;
                    float denom = nx * bay - ny * bax;
                    if (Math.abs(denom) < 1e-9f) continue; // parallel

                    float acx = a.x - cx, acy = a.y - cy;
                    float t = (acx * bay - acy * bax) / denom;
                    float s = (acx * ny - acy * nx) / denom;

                    if (t > 1e-6f && s >= 0f && s <= 1f && t < bestT) {
                        bestT = t;
                        best = new PointF(cx + t * nx, cy + t * ny);
                    }
                }
            }

            // Fallback: nearest contour point in that half-plane if polygon intersection fails
            if (best == null) {
                float bestDot = -Float.MAX_VALUE;
                for (List<PointF> contour : borderPoints) {
                    for (PointF p : contour) {
                        float px = p.x - cx, py = p.y - cy;
                        float dot = px * nx + py * ny;
                        if (dot > bestDot) {
                            bestDot = dot;
                            best = p;
                        }
                    }
                }
            }
            return best;
        }

        private void notifyListener() {
            if (listener == null) return;
            listener.onPointsChanged(setCount(), pixelDist(pts[0], pts[1]), pixelDist(pts[2], pts[3]));
        }

        private int setCount() {
            int n = 0;
            for (PointF p : pts) if (p != null) n++;
            return n;
        }

        private PointF nearest(float nx, float ny, float snapR) {
            PointF best = null;
            float bd = Float.MAX_VALUE;
            for (List<PointF> contour : borderPoints) {
                for (PointF p : contour) {
                    float dx = p.x - nx, dy = p.y - ny, d = dx * dx + dy * dy;
                    if (d < bd) {
                        bd = d;
                        best = p;
                    }
                }
            }
            return (best != null && bd <= snapR * snapR) ? best : null;
        }
    }
}
