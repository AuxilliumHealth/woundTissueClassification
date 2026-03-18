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

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

public class BlurDetector {
    private static final String TAG = "BlurDetector";
    private static final double BLUR_THRESHOLD = 50.0; // Adjusted threshold for wound imaging clarity

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed.");
        }
    }

    /**
     * Detects if the given bitmap is blurry using Laplacian variance.
     * @param bitmap The bitmap to check.
     * @return true if blurry, false otherwise.
     */
    public static boolean isBlurry(Bitmap bitmap) {
        if (bitmap == null) return true;

        try {
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);

            Mat gray = new Mat();
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);

            Mat laplacian = new Mat();
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F);

            MatOfDouble mean = new MatOfDouble();
            MatOfDouble stdDev = new MatOfDouble();
            Core.meanStdDev(laplacian, mean, stdDev);

            double variance = Math.pow(stdDev.get(0, 0)[0], 2);
            Log.d(TAG, "Blur variance: " + variance);

            // Clean up
            mat.release();
            gray.release();
            laplacian.release();

            return variance < BLUR_THRESHOLD;
        } catch (Exception e) {
            Log.e(TAG, "Error detecting blur", e);
            return false; // Assume not blurry on error to avoid blocking user unfairly
        }
    }
}
