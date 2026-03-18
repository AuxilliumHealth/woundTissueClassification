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
package com.auxilliumhealth.woundtissueclassification.Utils;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.calib3d.StereoSGBM;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Processor for measuring wound depth using calibrated stereo images.
 * Implements the 10-step process for wound depth estimation.
 */
public class WoundDepthProcessor {
    private static final String TAG = "WoundDepthProcessor";
    
    // Physical coin diameter (standard reference, e.g., 1-cent coin ~19.05mm)
    private static final double REAL_COIN_DIAMETER_MM = 19.05; 
    
    private double mmPerPixel = 0.0;
    private double baselineMm = 0.0;
    private double focalLengthPx = 0.0;

    public static class DepthResult {
        public double maxDepthMm;
        public Bitmap visualization;
        public Rect woundRect;

        public DepthResult(double maxDepthMm, Bitmap visualization, Rect woundRect) {
            this.maxDepthMm = maxDepthMm;
            this.visualization = visualization;
            this.woundRect = woundRect;
        }
    }

    public WoundDepthProcessor(double baselineMm, double focalLengthPx) {
        this.baselineMm = baselineMm;
        this.focalLengthPx = focalLengthPx;
    }

    /**
     * Main processing entry point with an optional predefined mask.
     * 
     * @param leftBitmap Left stereo image
     * @param rightBitmap Right stereo image
     * @param maskMat Optional predefined mask Mat (CV_8UC1)
     * @return DepthResult containing measured depth and visualization
     */
    public DepthResult process(Bitmap leftBitmap, Bitmap rightBitmap, Mat maskMat) {
        Mat leftMat = new Mat();
        Mat rightMat = new Mat();
        Utils.bitmapToMat(leftBitmap, leftMat);
        Utils.bitmapToMat(rightBitmap, rightMat);

        // 1 & 2. Detect coin and measure diameter
        double coinDiameterPx = detectCoinDiameter(leftMat);
        
        // 3. Compute pixel-to-mm scale
        if (coinDiameterPx > 0) {
            mmPerPixel = REAL_COIN_DIAMETER_MM / coinDiameterPx;
            Log.d(TAG, "Scale: " + mmPerPixel + " mm/px");
        } else {
            Log.e(TAG, "Coin not detected. Using fallback scale.");
            mmPerPixel = 0.1; // Fallback
        }

        // 4. Generate disparity map
        Mat disparity = generateDisparityMap(leftMat, rightMat);

        // 5. Convert to depth map
        Mat depthMap = convertToDepthMap(disparity);

        // 6. Segment wound region (Using provided mask if available)
        Rect woundRect;
        if (maskMat != null && !maskMat.empty()) {
            woundRect = segmentWoundWithMask(maskMat);
        } else {
            woundRect = segmentWound(leftMat);
        }
        
        // 7, 8, & 9. Identify skin reference and calculate depth
        double maxDepthMm = calculateWoundDepth(depthMap, woundRect);

        // 10. Visualize and Output
        Bitmap viz = visualizeResults(leftMat, woundRect, maxDepthMm);

        return new DepthResult(maxDepthMm, viz, woundRect);
    }

    private Rect segmentWoundWithMask(Mat mask) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) {
            return new Rect(0, 0, mask.cols(), mask.rows());
        }

        MatOfPoint largest = Collections.max(contours, Comparator.comparingDouble(Imgproc::contourArea));
        return Imgproc.boundingRect(largest);
    }

    /**
     * Step 1 & 2: Hough Circle detection for the calibration coin.
     */
    private double detectCoinDiameter(Mat img) {
        Mat gray = new Mat();
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(9, 9), 2, 2);

        Mat circles = new Mat();
        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1, 
                gray.rows() / 8, 100, 30, 20, 100);

        if (circles.cols() > 0) {
            double[] data = circles.get(0, 0);
            return data[2] * 2; // Radius * 2
        }
        return 0;
    }

    /**
     * Step 4: Disparity Map using Semi-Global Block Matching.
     */
    private Mat generateDisparityMap(Mat left, Mat right) {
        Mat leftGray = new Mat();
        Mat rightGray = new Mat();
        Imgproc.cvtColor(left, leftGray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.cvtColor(right, rightGray, Imgproc.COLOR_RGBA2GRAY);

        // SGBM Parameters optimized for mobile
        StereoSGBM sgbm = StereoSGBM.create(0, 64, 11);
        sgbm.setP1(8 * 11 * 11);
        sgbm.setP2(32 * 11 * 11);
        sgbm.setMode(StereoSGBM.MODE_SGBM);

        Mat disparity = new Mat();
        sgbm.compute(leftGray, rightGray, disparity);
        
        // Normalize for visualization and processing
        Mat disp8 = new Mat();
        disparity.convertTo(disp8, CvType.CV_8U, 255.0 / (64 * 16));
        return disparity;
    }

    /**
     * Step 5: Convert disparity to depth based on focal length and baseline.
     * depth = (f * B) / disparity
     */
    private Mat convertToDepthMap(Mat disparity) {
        Mat depthMap = new Mat(disparity.size(), CvType.CV_32F);
        float baseline = (float) baselineMm;
        float focal = (float) focalLengthPx;

        for (int y = 0; y < disparity.rows(); y++) {
            for (int x = 0; x < disparity.cols(); x++) {
                short[] dispVal = new short[1];
                disparity.get(y, x, dispVal);
                float val = (float) dispVal[0] / 16.0f; // SGBM returns 16x disparity

                if (val > 0) {
                    float depth = (focal * baseline) / val;
                    depthMap.put(y, x, depth);
                } else {
                    depthMap.put(y, x, 0.0f);
                }
            }
        }
        return depthMap;
    }

    /**
     * Step 6: Basic wound segmentation based on color and contrast (Saliency).
     */
    private Rect segmentWound(Mat img) {
        Mat hsv = new Mat();
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(hsv, hsv, Imgproc.COLOR_RGB2HSV);

        // Typical wound color range (Reddish/Pink)
        Mat mask1 = new Mat();
        Mat mask2 = new Mat();
        Core.inRange(hsv, new Scalar(0, 50, 50), new Scalar(15, 255, 255), mask1);
        Core.inRange(hsv, new Scalar(160, 50, 50), new Scalar(180, 255, 255), mask2);
        
        Mat combinedMask = new Mat();
        Core.bitwise_or(mask1, mask2, combinedMask);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(combinedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) {
            // Fallback: center region
            return new Rect(img.cols() / 4, img.rows() / 4, img.cols() / 2, img.rows() / 2);
        }

        MatOfPoint largest = Collections.max(contours, Comparator.comparingDouble(Imgproc::contourArea));
        return Imgproc.boundingRect(largest);
    }

    /**
     * Step 7, 8, & 9: Surface reference and max depth calculation.
     */
    private double calculateWoundDepth(Mat depthMap, Rect woundRect) {
        // Sample skin around the wound (50px padding)
        Rect skinRect = new Rect(
                Math.max(0, woundRect.x - 50),
                Math.max(0, woundRect.y - 50),
                Math.min(depthMap.cols() - 1, woundRect.width + 100),
                Math.min(depthMap.rows() - 1, woundRect.height + 100)
        );

        // Average skin depth (Reference Plane)
        List<Float> skinDepths = new ArrayList<>();
        for (int y = skinRect.y; y < skinRect.y + skinRect.height; y++) {
            for (int x = skinRect.x; x < skinRect.x + skinRect.width; x++) {
                if (!woundRect.contains(new Point(x, y))) {
                    float[] d = new float[1];
                    depthMap.get(y, x, d);
                    if (d[0] > 0) skinDepths.add(d[0]);
                }
            }
        }

        if (skinDepths.isEmpty()) return 0.0;
        
        Collections.sort(skinDepths);
        float medianSkinDepth = skinDepths.get(skinDepths.size() / 2);

        // Find max depth inside the wound relative to skin plane
        float maxRelDepth = 0;
        for (int y = woundRect.y; y < woundRect.y + woundRect.height; y++) {
            for (int x = woundRect.x; x < woundRect.x + woundRect.width; x++) {
                float[] d = new float[1];
                depthMap.get(y, x, d);
                if (d[0] > medianSkinDepth) {
                    maxRelDepth = Math.max(maxRelDepth, d[0] - medianSkinDepth);
                }
            }
        }

        return maxRelDepth; // Return depth in mm
    }

    /**
     * Step 10: Visualization.
     */
    private Bitmap visualizeResults(Mat img, Rect woundRect, double depth) {
        Mat result = img.clone();
        
        // Draw wound bounding box
        Imgproc.rectangle(result, woundRect, new Scalar(0, 255, 0), 4);
        
        // Add text label
        String label = String.format("Max Depth: %.2f mm", depth);
        Imgproc.putText(result, label, new Point(woundRect.x, woundRect.y - 10), 
                Imgproc.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0, 255, 0), 3);

        Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bitmap);
        return bitmap;
    }
}
