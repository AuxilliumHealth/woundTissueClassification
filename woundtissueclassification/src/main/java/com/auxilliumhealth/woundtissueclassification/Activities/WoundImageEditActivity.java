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

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import com.auxilliumhealth.woundtissueclassification.Utils.ContourUtils;

import java.util.ArrayList;
import java.util.List;

public class WoundImageEditActivity extends AppCompatActivity {

    private static final String TAG = "WoundImageEditActivity";
    
    public enum InteractiveMode {
        NONE, ZOOM_PAN, LASSO, EDIT, MEASURE
    }
    
    public enum EditMode {
        ADD(Color.BLUE), REMOVE(Color.RED);
        private final int color;
        EditMode(int color) { this.color = color; }
        public int getColor() { return color; }
    }

    private ProgressBar progressBar;
    private MaterialTextView instructionText;
    private MaterialTextView measurementText;   // Auto area from mask
    private MaterialButton undoBtn;
    private ContourCanvasView canvasView;
    
    private InteractiveMode currentMode = InteractiveMode.MEASURE;
    private EditMode currentEditMode = EditMode.ADD;

    private Bitmap baseBitmap;
    private Bitmap maskBitmap;

    private static class HistoryState {
        Bitmap displayBitmap;
        List<List<android.graphics.PointF>> borderPoints;
        android.graphics.PointF[] measurementPoints;
        double area;
        
        HistoryState(Bitmap b, List<List<android.graphics.PointF>> pts, android.graphics.PointF[] mPts, double a) {
            this.displayBitmap = b;
            this.borderPoints = new ArrayList<>();
            for (List<android.graphics.PointF> lp : pts) this.borderPoints.add(new ArrayList<>(lp));
            this.measurementPoints = mPts.clone();
            this.area = a;
        }
    }
    private final java.util.Stack<HistoryState> history = new java.util.Stack<>();

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
                saveBtn.setBackgroundTintList(ColorStateList.valueOf(c));
                undoBtn.setBackgroundTintList(ColorStateList.valueOf(c));
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    android.view.Window window = getWindow();
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    window.setStatusBarColor(c);
                    
                    // Toggle dark icons if primary color is light (API 23+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        double luminance = (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 255.0;
                        int flags = window.getDecorView().getSystemUiVisibility();
                        if (luminance > 0.5) {
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        } else {
                            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        }
                        window.getDecorView().setSystemUiVisibility(flags);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        toolbar.inflateMenu(R.menu.menu_image_edit);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear) {
                saveHistory();
                canvasView.clear();
                saveBtn.setEnabled(false);
                saveBtn.setAlpha(0.45f);
                return true;
            }
            return false;
        });
        toolbar.setNavigationOnClickListener(v -> finish());
        final MaterialButton finalSaveBtn = saveBtn;
        saveBtn.setOnClickListener(v -> onDoneClicked());
        undoBtn.setOnClickListener(v -> performUndo());
        clearBtn.setOnClickListener(v -> {
            if (canvasView != null) {
                saveHistory();
                canvasView.clear();
                finalSaveBtn.setEnabled(false);
                finalSaveBtn.setAlpha(0.45f);
                updateInstructions(0);
            }
        });

        setupToolButtons();

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

        // Initialize history after first process
        undoBtn.setEnabled(false);
        undoBtn.setAlpha(0.45f);

        loadImages(croppedImagePath, croppedGrabcutMask);
    }
    
    private void saveHistory() {
        if (canvasView == null) return;
        history.push(new HistoryState(canvasView.imageBitmap, canvasView.borderPoints, canvasView.pts, woundArea));
        if (history.size() > 20) history.remove(0); // limit history
        undoBtn.setEnabled(true);
        undoBtn.setAlpha(1.0f);
    }
    
    private void performUndo() {
        if (currentMode == InteractiveMode.MEASURE) {
            canvasView.undo();
            return;
        }
        
        if (!history.isEmpty()) {
            HistoryState state = history.pop();
            woundArea = state.area;
            canvasView.updateData(state.displayBitmap, state.borderPoints);
            System.arraycopy(state.measurementPoints, 0, canvasView.pts, 0, 4);
            canvasView.invalidate();
            
            // Re-render banner with current distances
            int count = canvasView.setCount();
            String lengthStr = null, widthStr = null;
            if (count >= 2) {
                double d = canvasView.pixelDist(canvasView.pts[0], canvasView.pts[1]);
                lengthStr = pixelPerUnit > 0 ? String.format("%.2f cm", (d / pixelPerUnit) * 100.0) : null;
            }
            if (count >= 4) {
                double d = canvasView.pixelDist(canvasView.pts[2], canvasView.pts[3]);
                widthStr = pixelPerUnit > 0 ? String.format("%.2f cm", (d / pixelPerUnit) * 100.0) : null;
            }
            measurementText.setText(buildBanner(woundArea, lengthStr, widthStr));
            canvasView.notifyListener();
        }
        
        if (history.isEmpty()) {
            undoBtn.setEnabled(false);
            undoBtn.setAlpha(0.45f);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void updateInstructions(int count) {
        if (currentMode != InteractiveMode.MEASURE) {
            switch (currentMode) {
                case ZOOM_PAN: instructionText.setText("Pinch to zoom and Drag to pan"); break;
                case LASSO: instructionText.setText("Draw a contour around the wound"); break;
                case EDIT: instructionText.setText(currentEditMode == EditMode.ADD ? "Draw to ADD area" : "Draw to REMOVE area"); break;
                default: instructionText.setText("Select a tool to edit or measure"); break;
            }
            return;
        }

        switch (count) {
            case 0:
                instructionText.setText("Step 1 of 2 — Tap the TOP edge to measure length. (Long press point to remove)");
                break;
            case 1:
                instructionText.setText("Step 1 of 2 — Tap the BOTTOM edge. (Drag points to adjust)");
                break;
            case 2:
                instructionText.setText("Step 2 of 2 — Tap the LEFT edge of the wound to measure width");
                break;
            case 3:
                instructionText.setText("Step 2 of 2 — Tap the RIGHT edge of the wound (opposite side)");
                break;
            case 4:
                instructionText.setText("✓ Wound size marked! Press Done to save measurements.");
                break;
            default:
                if (currentMode == InteractiveMode.LASSO) {
                    instructionText.setText("Lasso Mode: Outline the wound to detect its edges.");
                } else if (currentMode == InteractiveMode.EDIT) {
                    instructionText.setText(currentEditMode == EditMode.ADD ? "Add Mode: Outline area to ADD to the wound." : "Remove Mode: Outline area to REMOVE from the wound.");
                } else if (currentMode == InteractiveMode.ZOOM_PAN) {
                    instructionText.setText("Zoom Mode: Use two fingers to pinch or drag the image.");
                } else {
                    instructionText.setText("✓ Ready! Use tools below to refine or Done to finish.");
                }
                break;
        }
    }
    
    private void setupToolButtons() {
        findViewById(R.id.tool_zoom).setOnClickListener(v -> setMode(InteractiveMode.ZOOM_PAN));
        findViewById(R.id.tool_lasso).setOnClickListener(v -> setMode(InteractiveMode.LASSO));
        findViewById(R.id.tool_expand).setOnClickListener(v -> expandOrShrink(true));
        findViewById(R.id.tool_shrink).setOnClickListener(v -> expandOrShrink(false));
        findViewById(R.id.tool_add).setOnClickListener(v -> {
            currentEditMode = EditMode.ADD;
            setMode(InteractiveMode.EDIT);
        });
        findViewById(R.id.tool_remove).setOnClickListener(v -> {
            currentEditMode = EditMode.REMOVE;
            setMode(InteractiveMode.EDIT);
        });
        findViewById(R.id.tool_measure).setOnClickListener(v -> setMode(InteractiveMode.MEASURE));
        
        // Initial state
        setMode(InteractiveMode.MEASURE);
    }
    
    private void setMode(InteractiveMode mode) {
        currentMode = mode;
        if (canvasView != null) {
            canvasView.setMode(mode, currentEditMode);
        }
        updateInstructions(canvasView != null ? canvasView.setCount() : 0);
        
        // Update UI highlights
        int highlight = Color.parseColor("#44FFFFFF");
        findViewById(R.id.tool_zoom).setBackgroundColor(mode == InteractiveMode.ZOOM_PAN ? highlight : Color.TRANSPARENT);
        findViewById(R.id.tool_lasso).setBackgroundColor(mode == InteractiveMode.LASSO ? highlight : Color.TRANSPARENT);
        findViewById(R.id.tool_add).setBackgroundColor(mode == InteractiveMode.EDIT && currentEditMode == EditMode.ADD ? highlight : Color.TRANSPARENT);
        findViewById(R.id.tool_remove).setBackgroundColor(mode == InteractiveMode.EDIT && currentEditMode == EditMode.REMOVE ? highlight : Color.TRANSPARENT);
        findViewById(R.id.tool_measure).setBackgroundColor(mode == InteractiveMode.MEASURE ? highlight : Color.TRANSPARENT);
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
        result.putExtra("area", String.format("%.2f", woundArea));
        result.putExtra(EXTRA_AREA_CM2, woundArea);
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
    
    // ── OpenCV Editing Tools ───────────────────────────────────────────

    private void autoEdgeDetection(Mat seedMask, boolean mergeWithExisting) {
        if (baseBitmap == null) return;
        Mat fullExistingMask = canvasView.getCurrentMask();
        Mat focusMask = seedMask != null ? seedMask.clone() : (fullExistingMask != null ? fullExistingMask.clone() : null);
        
        if (focusMask == null || Core.countNonZero(focusMask) == 0) {
            Toast.makeText(this, "Please draw an area first", Toast.LENGTH_SHORT).show();
            if (focusMask != null) focusMask.release();
            if (fullExistingMask != null) fullExistingMask.release();
            return;
        }
        
        if (seedMask == null) saveHistory();
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                Mat baseMat = new Mat();
                Utils.bitmapToMat(baseBitmap, baseMat);
                // OpenCV GrabCut works best with BGR (3 channels)
                Imgproc.cvtColor(baseMat, baseMat, Imgproc.COLOR_RGBA2BGR);

                // Enhancement: Apply CLAHE to the L channel of Lab space for better contrast
                Mat lab = new Mat();
                Imgproc.cvtColor(baseMat, lab, Imgproc.COLOR_BGR2Lab);
                List<Mat> labChannels = new ArrayList<>();
                Core.split(lab, labChannels);
                CLAHE clahe = Imgproc.createCLAHE(4.0, new Size(8, 8));
                clahe.apply(labChannels.get(0), labChannels.get(0));
                Core.merge(labChannels, lab);
                Imgproc.cvtColor(lab, baseMat, Imgproc.COLOR_Lab2BGR);
                lab.release();
                for (Mat m : labChannels) m.release();

                // Strict Initialization: Everything is Background (GC_BGD)
                Mat grabcutMask = new Mat(baseMat.size(), CvType.CV_8UC1, new Scalar(Imgproc.GC_BGD));
                
                // Mark ONLY the user's lasso area as Probable Foreground (GC_PR_FGD)
                // This forces the algorithm to find the wound INSIDE the drawn area
                // It can shrink to fit the real wound but cannot expand outside your drawing
                grabcutMask.setTo(new Scalar(Imgproc.GC_PR_FGD), focusMask);

                // Define focal area (Bounding Box)
                org.opencv.core.Rect rect = Imgproc.boundingRect(focusMask);
                int pad = 5; // Very small padding for background stats
                rect.x = Math.max(0, rect.x - pad);
                rect.y = Math.max(0, rect.y - pad);
                rect.width = Math.min(baseMat.cols() - rect.x, rect.width + pad * 2);
                rect.height = Math.min(baseMat.rows() - rect.y, rect.height + pad * 2);

                Mat bgModel = new Mat();
                Mat fgModel = new Mat();
                Imgproc.grabCut(baseMat, grabcutMask, rect, bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_MASK);

                Mat resultMask = new Mat();
                // Extract foreground (Definite=1 or Probable=3)
                Core.compare(grabcutMask, new Scalar(Imgproc.GC_PR_FGD), resultMask, Core.CMP_EQ);
                Mat definiteFgd = new Mat();
                Core.compare(grabcutMask, new Scalar(Imgproc.GC_FGD), definiteFgd, Core.CMP_EQ);
                Core.bitwise_or(resultMask, definiteFgd, resultMask);
                
                // FINAL GUARANTEE: Intersect with user's original selection
                // This ensures that even if GrabCut tried to expand, we cut it back to the lasso
                Core.bitwise_and(resultMask, focusMask, resultMask);
                
                // Keep all separate wound components found within the user's focus area
                List<MatOfPoint> contours = new ArrayList<>();
                Imgproc.findContours(resultMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                if (!contours.isEmpty()) {
                    resultMask.setTo(new Scalar(0));
                    Imgproc.drawContours(resultMask, contours, -1, new Scalar(255), -1);
                }

                // Final merging logic for Add mode
                if (mergeWithExisting) {
                    Mat existing = canvasView.getCurrentMask();
                    if (existing != null) {
                        Core.bitwise_or(existing, resultMask, resultMask);
                        existing.release();
                    }
                }

                updateMaskAndRecontour(resultMask);
                
                baseMat.release();
                bgModel.release();
                fgModel.release();
                grabcutMask.release();
                resultMask.release();
                definiteFgd.release();
                focusMask.release();
                if (fullExistingMask != null) fullExistingMask.release();
            } catch (Exception e) {
                Log.e(TAG, "AutoEdgeDetection failed", e);
            } finally {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }

    private void expandOrShrink(boolean expand) {
        if (baseBitmap == null) return;
        Mat currentMask = canvasView.getCurrentMask();
        if (currentMask == null || Core.countNonZero(currentMask) == 0) return;

        saveHistory();
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(7, 7));
                Mat processedMask = new Mat();
                if (expand) {
                    Imgproc.dilate(currentMask, processedMask, kernel);
                } else {
                    Imgproc.erode(currentMask, processedMask, kernel);
                }
                
                updateMaskAndRecontour(processedMask);
                processedMask.release();
                kernel.release();
                currentMask.release();
            } catch (Exception e) {
                Log.e(TAG, "Expand/Shrink failed", e);
            } finally {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        }).start();
    }
    
    /**
     * Clears existing contours and resets the canvas to the clean base image.
     * Called when starting a new manual lasso.
     */
    public void prepareForLasso() {
        if (baseBitmap == null || canvasView == null) return;
        if (currentMode == InteractiveMode.LASSO) {
            canvasView.updateData(baseBitmap, new ArrayList<>());
            measurementText.setText(buildBanner(0));
        }
    }

    private void updateMaskAndRecontour(Mat newMask8u) {
        if (baseBitmap == null) return;
        
        // Handle component filtering
        List<MatOfPoint> woundContours = ContourUtils.extractContours(newMask8u);
        if (woundContours != null && woundContours.size() > 1) {
            // Sort by area descending
            woundContours.sort((o1, o2) -> Double.compare(Imgproc.contourArea(o2), Imgproc.contourArea(o1)));
            
            // If we are REMOVING, we strictly keep only the largest piece to prevent splitting.
            // In ADD or LASSO mode, we allow multiple pieces (for disconnected wounds or building).
            if (currentMode == InteractiveMode.EDIT && currentEditMode == EditMode.REMOVE) {
                newMask8u.setTo(new Scalar(0));
                Imgproc.drawContours(newMask8u, woundContours, 0, new Scalar(255), -1);
                while (woundContours.size() > 1) {
                    woundContours.remove(1).release();
                }
            } else {
                // For Add/Lasso, we only discard tiny noise (less than 1% of the largest component)
                double maxArea = Imgproc.contourArea(woundContours.get(0));
                for (int i = woundContours.size() - 1; i >= 1; i--) {
                    if (Imgproc.contourArea(woundContours.get(i)) < maxArea * 0.01) {
                        woundContours.remove(i).release();
                    }
                }
                // Refresh mask to match filtered contours
                newMask8u.setTo(new Scalar(0));
                Imgproc.drawContours(newMask8u, woundContours, -1, new Scalar(255), -1);
            }
        }

        // 1. Recalculate area for the PRIMARY piece only
        int nonZero = Core.countNonZero(newMask8u);
        double[] areaCoeff = ContourUtils.parseCoefficients(areaCoeffsStr);
        double areaPerPixel = ContourUtils.calculateAreaPerPixel(lensFocusDistance, areaCoeff);
        woundArea = (areaPerPixel > 0 && nonZero > 0)
                ? Math.max(0, areaPerPixel * nonZero * 10000.0)
                : 0;

        // 2. Extract refined border points for drawing
        List<List<android.graphics.PointF>> borderPoints = new ArrayList<>();
        int w = baseBitmap.getWidth();
        int h = baseBitmap.getHeight();
        
        Mat baseMat = new Mat();
        Utils.bitmapToMat(baseBitmap, baseMat);
        
        if (woundContours != null) {
            Imgproc.drawContours(baseMat, woundContours, -1, new Scalar(0, 255, 0, 255), 3);
            for (MatOfPoint contour : woundContours) {
                List<android.graphics.PointF> subList = new ArrayList<>();
                for (org.opencv.core.Point pt : contour.toArray()) {
                    subList.add(new android.graphics.PointF((float) (pt.x / w), (float) (pt.y / h)));
                }
                borderPoints.add(subList);
            }
        }

        Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(baseMat, result);
        baseMat.release();

        final String bannerStr = buildBanner(woundArea);
        runOnUiThread(() -> {
            canvasView.updateData(result, borderPoints);
            measurementText.setText(bannerStr);
        });
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
        private static final int[][] CONFIG = {
                {Color.RED, Color.CYAN, Color.YELLOW},
                {Color.parseColor("#FF6600"), Color.parseColor("#FF00FF"), Color.GREEN},
        };
        private static final String[] LABELS = {"12", "6", "9", "3"};

        private InteractiveMode currentMode = InteractiveMode.MEASURE;
        private EditMode currentEditMode = EditMode.ADD;

        private Bitmap imageBitmap;
        private List<List<PointF>> borderPoints = new ArrayList<>();
        private PointF maskCentroid = null;
        private float imgOriginalWidth = 1;
        private float imgOriginalHeight = 1;
        private double pixelPerUnit = 0;

        // Zoom / Pan
        private final Matrix matrix = new Matrix();
        private final Matrix inverseMatrix = new Matrix();
        private final float[] matrixValues = new float[9];
        private final ScaleGestureDetector scaleDetector;
        private final GestureDetector gestureDetector;

        // Lasso / Edit
        private final Path currentLassoPath = new Path();
        private final List<PointF> lassoPoints = new ArrayList<>();
        private final Paint lassoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Measure Points
        private final PointF[] pts = new PointF[4];
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private OnPointsChangedListener listener;

        public interface OnPointsChangedListener {
            void onPointsChanged(int count, double abPx, double cdPx);
        }

        public ContourCanvasView(Context ctx) {
            super(ctx);
            setupPaints();

            scaleDetector = new ScaleGestureDetector(ctx, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) {
                    if (currentMode == InteractiveMode.ZOOM_PAN) {
                        float s = d.getScaleFactor();
                        matrix.postScale(s, s, d.getFocusX(), d.getFocusY());
                        invalidate();
                    }
                    return true;
                }
            });

            gestureDetector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                    if (currentMode == InteractiveMode.ZOOM_PAN) {
                        matrix.postTranslate(-dx, -dy);
                        invalidate();
                    }
                    return true;
                }
                
                @Override
                public void onLongPress(MotionEvent e) {
                    if (currentMode == InteractiveMode.MEASURE) {
                        float x = e.getX(), y = e.getY();
                        float screenRadius = 50f * getResources().getDisplayMetrics().density;
                        for (int i = 0; i < 4; i++) {
                            if (pts[i] != null) {
                                PointF v = toView(pts[i]);
                                float dx = x - v.x;
                                float dy = y - v.y;
                                if (Math.sqrt(dx*dx + dy*dy) < screenRadius) {
                                    pts[i] = null;
                                    invalidate();
                                    notifyListener();
                                    return;
                                }
                            }
                        }
                    }
                }
            });
        }

        private void setupPaints() {
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

            lassoPaint.setStyle(Paint.Style.STROKE);
            lassoPaint.setStrokeWidth(5f);
            lassoPaint.setColor(Color.BLUE);
            
            overlayPaint.setStyle(Paint.Style.FILL);
            overlayPaint.setColor(0x444444FF);
        }

        public void setMode(InteractiveMode mode, EditMode editMode) {
            this.currentMode = mode;
            this.currentEditMode = editMode;
            if (mode == InteractiveMode.EDIT) {
                lassoPaint.setColor(editMode.getColor());
                overlayPaint.setColor(editMode == EditMode.ADD ? 0x444444FF : 0x44FF4444);
            } else {
                lassoPaint.setColor(Color.BLUE);
                overlayPaint.setColor(0x444444FF);
            }
            invalidate();
        }

        public void setPixelPerUnit(double ppu) { this.pixelPerUnit = ppu; invalidate(); }
        public void setImagePixelDimensions(int w, int h) { this.imgOriginalWidth = w; this.imgOriginalHeight = h; }
        public void setOnPointsChangedListener(OnPointsChangedListener l) { this.listener = l; }

        public void setData(Bitmap bmp, List<List<PointF>> points, PointF centroid) {
            imageBitmap = bmp;
            borderPoints = points;
            maskCentroid = centroid;
            configureInitialMatrix();
            invalidate();
        }

        public void updateData(Bitmap bmp, List<List<PointF>> points) {
            imageBitmap = bmp;
            borderPoints = points;
            currentLassoPath.reset();
            lassoPoints.clear();
            invalidate();
        }

        private void configureInitialMatrix() {
            if (imageBitmap == null) return;
            matrix.reset();
            float s = Math.min((float) getWidth() / imageBitmap.getWidth(), (float) getHeight() / imageBitmap.getHeight());
            float ox = (getWidth() - imageBitmap.getWidth() * s) / 2f;
            float oy = (getHeight() - imageBitmap.getHeight() * s) / 2f;
            matrix.postScale(s, s);
            matrix.postTranslate(ox, oy);
        }

        public void clear() {
            for (int i = 0; i < 4; i++) pts[i] = null;
            lassoPoints.clear();
            currentLassoPath.reset();
            invalidate();
            notifyListener();
        }

        public void undo() {
            if (currentMode == InteractiveMode.MEASURE) {
                int n = setCount();
                if (n > 0) { pts[n - 1] = null; invalidate(); notifyListener(); }
            } else {
                currentLassoPath.reset();
                lassoPoints.clear();
                invalidate();
            }
        }

        public Mat getCurrentMask() {
            if (imageBitmap == null) return null;
            int w = (int) imgOriginalWidth;
            int h = (int) imgOriginalHeight;
            Mat mask = Mat.zeros(h, w, CvType.CV_8UC1);
            
            if (!borderPoints.isEmpty()) {
                List<MatOfPoint> contours = new ArrayList<>();
                for (List<PointF> poly : borderPoints) {
                    Point[] ptsArr = new Point[poly.size()];
                    for (int i = 0; i < poly.size(); i++) {
                        ptsArr[i] = new Point(poly.get(i).x * w, poly.get(i).y * h);
                    }
                    contours.add(new MatOfPoint(ptsArr));
                }
                Imgproc.fillPoly(mask, contours, new Scalar(255));
            }
            return mask;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (imageBitmap == null) return;

            canvas.save();
            canvas.concat(matrix);
            canvas.drawBitmap(imageBitmap, 0, 0, null);
            canvas.restore();

            // Draw Measure Points/Lines
            if (currentMode == InteractiveMode.MEASURE || currentMode == InteractiveMode.NONE) {
                drawPair(canvas, 0, 1, line1Paint);
                drawPair(canvas, 2, 3, line2Paint);
            }

            // Draw Lasso
            if ((currentMode == InteractiveMode.LASSO || currentMode == InteractiveMode.EDIT) && !currentLassoPath.isEmpty()) {
                canvas.drawPath(currentLassoPath, lassoPaint);
            }
        }

        private void drawPair(Canvas canvas, int ia, int ib, Paint lp) {
            if (pts[ia] == null) return;
            PointF va = toView(pts[ia]);
            drawDot(canvas, va.x, va.y, CONFIG[ia / 2][0], LABELS[ia]);
            if (pts[ib] != null) {
                PointF vb = toView(pts[ib]);
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

        private PointF toView(PointF n) {
            float[] p = {n.x * imageBitmap.getWidth(), n.y * imageBitmap.getHeight()};
            matrix.mapPoints(p);
            return new PointF(p[0], p[1]);
        }

        private PointF toNormalised(float vx, float vy) {
            matrix.invert(inverseMatrix);
            float[] p = {vx, vy};
            inverseMatrix.mapPoints(p);
            // Clamp to 0..1 range to be safe for OpenCV downstream
            float nx = Math.max(0f, Math.min(1f, p[0] / imageBitmap.getWidth()));
            float ny = Math.max(0f, Math.min(1f, p[1] / imageBitmap.getHeight()));
            return new PointF(nx, ny);
        }

        private int draggedPointIndex = -1;

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (imageBitmap == null) return false;
            
            float x = event.getX(), y = event.getY();

            // Restrict drawing to the visible image bounds (view coordinates)
            if (currentMode == InteractiveMode.LASSO || currentMode == InteractiveMode.EDIT) {
                float[] rect = {0, 0, imageBitmap.getWidth(), imageBitmap.getHeight()};
                matrix.mapPoints(rect);
                float left = Math.min(rect[0], rect[2]), right = Math.max(rect[0], rect[2]);
                float top = Math.min(rect[1], rect[3]), bottom = Math.max(rect[1], rect[3]);
                x = Math.max(left, Math.min(x, right));
                y = Math.max(top, Math.min(y, bottom));
            }
            
            if (currentMode == InteractiveMode.ZOOM_PAN) {
                scaleDetector.onTouchEvent(event);
                gestureDetector.onTouchEvent(event);
                return true;
            }

            if (currentMode == InteractiveMode.LASSO || currentMode == InteractiveMode.EDIT) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Context downCtx = getContext();
                        if (currentMode == InteractiveMode.LASSO && downCtx instanceof WoundImageEditActivity) {
                            ((WoundImageEditActivity) downCtx).prepareForLasso();
                        }
                        currentLassoPath.reset();
                        currentLassoPath.moveTo(x, y);
                        lassoPoints.clear();
                        lassoPoints.add(toNormalised(x, y));
                        break;
                    case MotionEvent.ACTION_MOVE:
                        currentLassoPath.lineTo(x, y);
                        lassoPoints.add(toNormalised(x, y));
                        break;
                    case MotionEvent.ACTION_UP:
                        currentLassoPath.close();
                        processCompletedLasso();
                        // Path and points are now cleared inside act.updateData/autoEdgeDetection
                        break;
                }
                invalidate();
                return true;
            }

            if (currentMode == InteractiveMode.MEASURE) {
                float screenRadius = 40f * getResources().getDisplayMetrics().density;
                
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Check if we are tapping an existing point to drag or delete
                    for (int i = 0; i < 4; i++) {
                        if (pts[i] != null) {
                            PointF v = toView(pts[i]);
                            float dx = x - v.x;
                            float dy = y - v.y;
                            if (Math.sqrt(dx*dx + dy*dy) < screenRadius) {
                                draggedPointIndex = i;
                                return true;
                            }
                        }
                    }
                    
                    int n = setCount();
                    if (n >= 4) return true;

                    PointF norm = toNormalised(x, y);
                    float snapR = (SNAP_RADIUS_DP * getResources().getDisplayMetrics().density);
                    matrix.getValues(matrixValues);
                    float currentScale = matrixValues[Matrix.MSCALE_X];
                    PointF snapped = nearest(norm.x, norm.y, snapR / (currentScale * imageBitmap.getWidth()));
                    
                    if (snapped == null) {
                        Toast.makeText(getContext(), "Tap closer to the green contour border", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    pts[n] = snapped;
                    if (n == 0 && maskCentroid != null) autoFillClockFace(snapped);
                    invalidate();
                    notifyListener();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (draggedPointIndex != -1) {
                        PointF norm = toNormalised(x, y);
                        // Optional: snap while dragging
                        pts[draggedPointIndex] = norm;
                        invalidate();
                        notifyListener();
                        return true;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    draggedPointIndex = -1;
                    return true;
                }
            }

            return super.onTouchEvent(event);
        }

        private void processCompletedLasso() {
            // Convert lasso points to a mask and update borderPoints
            int w = (int) imgOriginalWidth;
            int h = (int) imgOriginalHeight;
            Mat lassoMask = Mat.zeros(h, w, CvType.CV_8UC1);
            Point[] ptsArr = new Point[lassoPoints.size()];
            for (int i = 0; i < lassoPoints.size(); i++) {
                ptsArr[i] = new Point(lassoPoints.get(i).x * w, lassoPoints.get(i).y * h);
            }
            List<MatOfPoint> contours = new ArrayList<>();
            contours.add(new MatOfPoint(ptsArr));
            Imgproc.fillPoly(lassoMask, contours, new Scalar(255));

            Mat currentMask = getCurrentMask();
            if (currentMode == InteractiveMode.LASSO) {
                // Trigger auto-snap after lasso is done
                Context ctx = getContext();
                if (ctx instanceof WoundImageEditActivity) {
                    WoundImageEditActivity act = (WoundImageEditActivity) ctx;
                    act.saveHistory();
                    act.autoEdgeDetection(lassoMask, false);
                }
            } else if (currentMode == InteractiveMode.EDIT) {
                if (currentEditMode == EditMode.ADD) {
                    // Reverted ADD to manual operation for reliability
                    Core.bitwise_or(currentMask, lassoMask, currentMask);
                    updateMaskAndRecontourProxy(currentMask);
                } else {
                    // REMOVE remains manual for precision
                    Mat invLasso = new Mat();
                    Core.bitwise_not(lassoMask, invLasso);
                    Core.bitwise_and(currentMask, invLasso, currentMask);
                    invLasso.release();
                    updateMaskAndRecontourProxy(currentMask);
                }
            }
            lassoMask.release();
            currentMask.release();
        }

        private void updateMaskAndRecontourProxy(Mat mask) {
            Context ctx = getContext();
            if (ctx instanceof WoundImageEditActivity) {
                WoundImageEditActivity act = (WoundImageEditActivity) ctx;
                act.saveHistory();
                act.updateMaskAndRecontour(mask);
            }
        }

        private void autoFillClockFace(PointF p12) {
            if (maskCentroid == null || borderPoints == null || borderPoints.isEmpty()) return;
            float dx = (p12.x - maskCentroid.x) * imgOriginalWidth;
            float dy = (p12.y - maskCentroid.y) * imgOriginalHeight;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len == 0) return;
            float ux = dx / len, uy = dy / len;

            pts[1] = rayContourIntersection(-ux / imgOriginalWidth, -uy / imgOriginalHeight);
            pts[2] = rayContourIntersection(uy / imgOriginalWidth, -ux / imgOriginalHeight);
            pts[3] = rayContourIntersection(-uy / imgOriginalWidth, ux / imgOriginalHeight);
        }

        private PointF rayContourIntersection(float vx, float vy) {
            if (maskCentroid == null) return null;
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            if (len == 0) return null;
            float nx = vx / len, ny = vy / len;
            float cx = maskCentroid.x, cy = maskCentroid.y;
            PointF best = null;
            float bestT = Float.MAX_VALUE;

            for (List<PointF> contour : borderPoints) {
                int n = contour.size();
                for (int i = 0; i < n; i++) {
                    PointF a = contour.get(i), b = contour.get((i + 1) % n);
                    float bax = b.x - a.x, bay = b.y - a.y;
                    float denom = nx * bay - ny * bax;
                    if (Math.abs(denom) < 1e-9f) continue;
                    float acx = a.x - cx, acy = a.y - cy;
                    float t = (acx * bay - acy * bax) / denom;
                    float s = (acx * ny - acy * nx) / denom;
                    if (t > 1e-6f && s >= 0f && s <= 1f && t < bestT) {
                        bestT = t;
                        best = new PointF(cx + t * nx, cy + t * ny);
                    }
                }
            }
            return best;
        }

        private void notifyListener() {
            if (listener != null) listener.onPointsChanged(setCount(), pixelDist(pts[0], pts[1]), pixelDist(pts[2], pts[3]));
        }

        public int setCount() {
            int n = 0;
            for (PointF p : pts) if (p != null) n++;
            return n;
        }

        private double pixelDist(PointF a, PointF b) {
            if (a == null || b == null) return 0;
            double dx = (a.x - b.x) * imgOriginalWidth;
            double dy = (a.y - b.y) * imgOriginalHeight;
            return Math.sqrt(dx * dx + dy * dy);
        }

        private PointF nearest(float nx, float ny, float snapR) {
            PointF best = null;
            float bd = Float.MAX_VALUE;
            for (List<PointF> contour : borderPoints) {
                for (PointF p : contour) {
                    float dx = p.x - nx, dy = p.y - ny, d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d < bd) { bd = d; best = p; }
                }
            }
            return (best != null && bd <= snapR) ? best : null;
        }
    }
}
