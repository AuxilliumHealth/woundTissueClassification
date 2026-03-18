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

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for wound contour operations.
 * Ported from the local-model project (AiProcessImageActivity reference).
 */
public class ContourUtils {

    // ── Result data classes ───────────────────────────────────────────────────

    public static class FurthestPointsResult {
        public Point p1;
        public Point p2;
        public double distance;

        public FurthestPointsResult(Point p1, Point p2, double distance) {
            this.p1 = p1;
            this.p2 = p2;
            this.distance = distance;
        }
    }

    public static class WidthResult {
        public double maxWidth;
        public Point side1Point;
        public Point side2Point;

        public WidthResult(double maxWidth, Point side1Point, Point side2Point) {
            this.maxWidth = maxWidth;
            this.side1Point = side1Point;
            this.side2Point = side2Point;
        }
    }

    // ── Calibration ───────────────────────────────────────────────────────────

    /**
     * Cubic polynomial:  a3·d³ + a2·d² + a1·d + a0
     */
    public static double cubicModel(double d, double a3, double a2, double a1, double a0) {
        return a3 * Math.pow(d, 3) + a2 * Math.pow(d, 2) + a1 * d + a0;
    }

    /**
     * Pixels per metre as a function of focus distance.
     * Requires exactly 4 coefficients in order [a3, a2, a1, a0].
     */
    public static double calculatePPU(double focusDistance, double[] coeffs) {
        if (focusDistance == 0 || coeffs == null || coeffs.length != 4) return 0;
        return cubicModel(focusDistance, coeffs[0], coeffs[1], coeffs[2], coeffs[3]);
    }

    /**
     * Area per pixel (m²) as a function of focus distance.
     * Requires exactly 4 coefficients in order [a3, a2, a1, a0].
     */
    public static double calculateAreaPerPixel(double focusDistance, double[] coeffs) {
        if (focusDistance == 0 || coeffs == null || coeffs.length != 4) return 0;
        return cubicModel(focusDistance, coeffs[0], coeffs[1], coeffs[2], coeffs[3]);
    }

    /**
     * Parse "[a3, a2, a1, a0]" or "a3,a2,a1,a0" into double[4].
     * Returns null if the string is invalid or doesn't have exactly 4 values.
     */
    public static double[] parseCoefficients(String s) {
        if (s == null || s.isEmpty()) return null;
        s = s.replace("[", "").replace("]", "").trim();
        String[] parts = s.split(",");
        if (parts.length != 4) return null;
        double[] out = new double[4];
        for (int i = 0; i < 4; i++) {
            try { out[i] = Double.parseDouble(parts[i].trim()); }
            catch (NumberFormatException e) { return null; }
        }
        return out;
    }

    // ── Contour extraction ────────────────────────────────────────────────────

    /**
     * Finds external contours in a binary mask.
     * Returns null if the mask is empty or no contours are found.
     */
    public static List<MatOfPoint> extractContours(Mat binaryMask) {
        if (binaryMask == null || binaryMask.empty()) {
            Log.e("ContourUtils", "Empty mask passed to extractContours");
            return null;
        }
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binaryMask, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.isEmpty()) {
            Log.i("ContourUtils", "No contours found");
            return null;
        }
        return contours;
    }

    /**
     * Merges all points from a list of contours into a single MatOfPoint2f.
     */
    public static MatOfPoint2f preprocessContours(List<MatOfPoint> contours) {
        if (contours == null || contours.isEmpty()) return null;
        List<Point> all = new ArrayList<>();
        for (MatOfPoint c : contours) all.addAll(c.toList());
        MatOfPoint2f mat = new MatOfPoint2f();
        mat.fromList(all);
        return mat;
    }

    // ── Geometry ──────────────────────────────────────────────────────────────

    /**
     * Finds the two farthest points in a MatOfPoint2f (O(n²)).
     */
    public static FurthestPointsResult findFurthestPoints(MatOfPoint2f pts) {
        if (pts == null || pts.empty()) return null;
        Point[] points = pts.toArray();
        double maxDist = -1;
        Point p1 = null, p2 = null;
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                double dx = points[i].x - points[j].x;
                double dy = points[i].y - points[j].y;
                double d  = Math.sqrt(dx*dx + dy*dy);
                if (d > maxDist) { maxDist = d; p1 = points[i]; p2 = points[j]; }
            }
        }
        return (p1 != null) ? new FurthestPointsResult(p1, p2, maxDist) : null;
    }

    /**
     * Finds the maximum perpendicular width to the line (lineStart → lineEnd)
     * by scanning along the axis and measuring how far each perpendicular
     * scan stays inside the binary mask.
     * Mirrors ContourUtils.findMaxPerpendicularWidthCorrected from the local-model.
     */
    public static WidthResult findMaxPerpendicularWidthCorrected(Point lineStart, Point lineEnd, Mat mask) {
        double dx  = lineEnd.x - lineStart.x;
        double dy  = lineEnd.y - lineStart.y;
        double len = Math.sqrt(dx*dx + dy*dy);
        if (len == 0) return new WidthResult(0, null, null);

        double ux = -dy / len;  // perpendicular unit vector x
        double uy =  dx / len;  // perpendicular unit vector y

        double maxW   = 0;
        Point bestS   = null;
        Point bestE   = null;

        // Scan 5 %→95 % of the length axis to avoid endpoint artefacts
        for (double t = 0.05; t <= 0.95; t += 0.01) {
            Point pBase = new Point(lineStart.x + t * dx, lineStart.y + t * dy);

            // Expand positive direction
            double dP = 0;
            while (true) {
                int tx = (int)(pBase.x + ux * (dP + 1));
                int ty = (int)(pBase.y + uy * (dP + 1));
                if (tx < 0 || tx >= mask.cols() || ty < 0 || ty >= mask.rows()
                        || mask.get(ty, tx)[0] == 0) break;
                dP++;
                if (dP > mask.cols()) break;
            }

            // Expand negative direction
            double dM = 0;
            while (true) {
                int tx = (int)(pBase.x - ux * (dM + 1));
                int ty = (int)(pBase.y - uy * (dM + 1));
                if (tx < 0 || tx >= mask.cols() || ty < 0 || ty >= mask.rows()
                        || mask.get(ty, tx)[0] == 0) break;
                dM++;
                if (dM > mask.cols()) break;
            }

            if (dP + dM > maxW) {
                maxW  = dP + dM;
                bestS = new Point(pBase.x + ux * dP, pBase.y + uy * dP);
                bestE = new Point(pBase.x - ux * dM, pBase.y - uy * dM);
            }
        }
        return new WidthResult(maxW, bestS, bestE);
    }
}
