package com.auxilliumhealth.woundtissueclassification.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.auxilliumhealth.woundtissueclassification.Model.ResultDataModel;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CoinDetector {

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("CoinDetector", "OpenCV initialization failed.");
        } else {
            Log.d("CoinDetector", "OpenCV initialized successfully.");
        }
    }

    public List<ResultDataModel> detectCoinsExtractPixelCounts(Context context, ArrayList<float[]> croppedCoords, ArrayList<String> filePaths) throws CustomError {

        if (filePaths == null || filePaths.isEmpty()) {
            throw new CustomError("No image paths provided");
        }
        if (croppedCoords == null || croppedCoords.size() != filePaths.size()) {
            throw new CustomError("Cropped coordinates count must match image paths count");
        }

        List<ResultDataModel> resultDataList = new ArrayList<>();
        File cacheDir = new File(context.getCacheDir(), "calibration");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        for (int i = 0; i < filePaths.size(); i++) {

            String originalFilePath = filePaths.get(i);
            float[] crop = croppedCoords.get(i);

            try {
                File file = new File(originalFilePath);
                String filename = file.getName().toLowerCase();

                // Supported formats
                if (!(filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp"))) {
                    throw new CustomError("Unsupported file format: " + filename);
                }

                Bitmap fullBitmap = BitmapFactory.decodeFile(originalFilePath);
                if (fullBitmap == null) {
                    throw new CustomError("Bitmap could not be decoded: " + filename);
                }

                // Crop coordinates
                float left = Math.max(0, crop[0]);
                float top = Math.max(0, crop[1]);
                float right = Math.min(crop[2], fullBitmap.getWidth());
                float bottom = Math.min(crop[3], fullBitmap.getHeight());

                int width = (int) (right - left);
                int height = (int) (bottom - top);

                if (width <= 0 || height <= 0) {
                    throw new CustomError("Invalid crop region for image: " + filename);
                }

                // Crop bitmap
                Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, (int) left, (int) top, width, height);
                Mat croppedMat = new Mat();
                Utils.bitmapToMat(croppedBitmap, croppedMat);

                // Convert to grayscale
                Mat gray = new Mat();
                Imgproc.cvtColor(croppedMat, gray, Imgproc.COLOR_BGR2GRAY);

                // Canny edge detection
                Mat edges = new Mat();
                Imgproc.Canny(gray, edges, 80, 200);

                // Dilate (strengthen edges)
                Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
                Mat edgesDilated = new Mat();
                Imgproc.dilate(edges, edgesDilated, kernel);

                // Find contours
                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(edgesDilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                // Filter small contours
                List<MatOfPoint> validContours = new ArrayList<>();
                for (MatOfPoint contour : contours) {
                    if (Imgproc.contourArea(contour) > 100) {
                        validContours.add(contour);
                    }
                }

                // No contour → return original
                if (validContours.isEmpty()) {
                    File outputFile = new File(cacheDir, "original_" + file.getName());
                    saveBitmapToFile(fullBitmap, outputFile);
                    resultDataList.add(new ResultDataModel(0f, 0, null, null, outputFile));
                    continue;
                }

                // Get the largest contour (outer coin)
                MatOfPoint largestContour = validContours.stream().max(Comparator.comparingDouble(Imgproc::contourArea)).orElse(validContours.get(0));

                // Fill contour to count pixels
                Mat filledContour = Mat.zeros(gray.size(), CvType.CV_8UC1);
                Imgproc.drawContours(filledContour, List.of(largestContour), -1, new Scalar(255), -1);
                int pixelCount = Core.countNonZero(filledContour);

                // Convert full image to Mat (for drawing)
                Mat originalMat = new Mat();
                Utils.bitmapToMat(fullBitmap, originalMat);

                // Translate contour points from cropped → full image coordinates
                Point[] translatedPoints = largestContour.toArray();
                for (Point p : translatedPoints) {
                    p.x += left;
                    p.y += top;
                }

                MatOfPoint translatedContour = new MatOfPoint(translatedPoints);

                // Draw contour on full image
                Imgproc.drawContours(originalMat, List.of(translatedContour), -1, new Scalar(0, 255, 0), 3);

                // Save output files
                String timestamp = String.valueOf(System.currentTimeMillis());
                File filledFile = new File(cacheDir, "filled_" + timestamp + "_" + file.getName());
                File combinedFile = new File(cacheDir, "combined_" + timestamp + "_" + file.getName());

                saveMatToFile(filledContour, filledFile);
                saveMatToFile(originalMat, combinedFile);

                // Add result
                resultDataList.add(new ResultDataModel(0f, pixelCount, combinedFile, filledFile, file));

            } catch (Exception e) {

                Log.e("CoinDetector", "Error processing image: " + originalFilePath, e);

                try {
                    File outputFile = new File(cacheDir, "error_" + System.currentTimeMillis() + "_" + new File(originalFilePath).getName());
                    saveBitmapToFile(BitmapFactory.decodeFile(originalFilePath), outputFile);
                    resultDataList.add(new ResultDataModel(0f, 0, null, null, outputFile));

                } catch (Exception ioException) {
                    throw new CustomError("Processing failed for image: " + originalFilePath);
                }
            }
        }

        return resultDataList;
    }

    private void saveBitmapToFile(Bitmap bitmap, File outputFile) throws IOException {
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            Log.d("CoinDetector", "Saved original image to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("CoinDetector", "Failed to save original image: " + outputFile.getAbsolutePath(), e);
            throw e;
        }
    }

    private void saveMatToFile(Mat mat, File outputFile) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            Log.d("CoinDetector", "Saved image to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("CoinDetector", "Failed to save image: " + outputFile.getAbsolutePath(), e);
            throw e;
        }
    }

    public ResultDataModel detectCoinsExtractPixelCounts(Context context, float[] croppedCoords, String originalFilePath) throws CustomError {

        if (originalFilePath == null || originalFilePath.isEmpty()) {
            throw new CustomError("No image path provided");
        }
//        Log.d("detectCoins", "croppedCoords = " + Arrays.toString(croppedCoords));
        File file = new File(originalFilePath);
        String filename = file.getName().toLowerCase();

        if (!(filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp"))) {
            throw new CustomError("Unsupported file format: " + filename);
        }

        Bitmap fullBitmap = BitmapFactory.decodeFile(originalFilePath);
        if (fullBitmap == null) {
            throw new CustomError("Bitmap could not be decoded: " + filename);
        }


        File cacheDir = new File(context.getCacheDir(), "calibration");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        try {
            // Crop region
            float left = Math.max(0, croppedCoords[0]);
            float top = Math.max(0, croppedCoords[1]);
            float right = Math.min(croppedCoords[2], fullBitmap.getWidth());
            float bottom = Math.min(croppedCoords[3], fullBitmap.getHeight());

            int width = (int) (right - left);
            int height = (int) (bottom - top);

            if (width <= 0 || height <= 0) {
                throw new CustomError("Invalid crop region");
            }

            // Crop the image
            Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, (int) left, (int) top, width, height);
            Mat croppedMat = new Mat();
            Utils.bitmapToMat(croppedBitmap, croppedMat);

            // Convert to grayscale
            Mat gray = new Mat();
            Imgproc.cvtColor(croppedMat, gray, Imgproc.COLOR_BGR2GRAY);

            // Canny edge detection (Python equivalent)
            Mat edges = new Mat();
            Imgproc.Canny(gray, edges, 80, 200);

            // Dilate edges to strengthen contour detection
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Mat edgesDilated = new Mat();
            Imgproc.dilate(edges, edgesDilated, kernel);

            // Find contours
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(edgesDilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Log.d("CoinDetector", "Contours found: " + contours.size());

            // Filter small contours
            List<MatOfPoint> validContours = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                if (Imgproc.contourArea(contour) > 50) {
                    validContours.add(contour);
                }
            }

            if (validContours.isEmpty()) {
                File outputFile = new File(cacheDir, "original_" + file.getName());
                saveBitmapToFile(fullBitmap, outputFile);
                return new ResultDataModel(0f, 0, null, null, outputFile);
            }

            // Largest contour = coin boundary
            MatOfPoint largestContour = validContours.stream().max(Comparator.comparingDouble(Imgproc::contourArea)).orElse(null);

            if (largestContour == null) {
                File outputFile = new File(cacheDir, "original_" + file.getName());
                saveBitmapToFile(fullBitmap, outputFile);
                return new ResultDataModel(0f, 0, null, null, outputFile);
            }

            // Fill contour to count pixel area
            Mat filledContour = Mat.zeros(gray.size(), CvType.CV_8UC1);
            Imgproc.drawContours(filledContour, List.of(largestContour), -1, new Scalar(255), -1);

            int pixelCount = Core.countNonZero(filledContour);

            // Draw contour on original image (translate back to full bitmap coordinates)
            Mat originalMat = new Mat();
            Utils.bitmapToMat(fullBitmap, originalMat);

            Point[] translatedPoints = largestContour.toArray();
            for (Point p : translatedPoints) {
                p.x += left;
                p.y += top;
            }

            MatOfPoint translatedContour = new MatOfPoint(translatedPoints);
            Imgproc.drawContours(originalMat, List.of(translatedContour), -1, new Scalar(0, 255, 0), 5);

            // Save files
            File filledFile = new File(cacheDir, "filled_" + file.getName());
            File combinedFile = new File(cacheDir, "combined_" + file.getName());

            saveMatToFile(filledContour, filledFile);
            saveMatToFile(originalMat, combinedFile);

            return new ResultDataModel(0f, pixelCount, combinedFile, filledFile, file);

        } catch (Exception e) {

            Log.e("CoinDetector", "Error processing: " + originalFilePath, e);

            try {
                File outputFile = new File(cacheDir, "original_" + file.getName());
                saveBitmapToFile(fullBitmap, outputFile);
                return new ResultDataModel(0f, 0, null, null, outputFile);

            } catch (IOException ioException) {
                throw new CustomError("Processing failed and couldn't save original");
            }
        }
    }

    public static class CustomError extends Exception {
        public CustomError(String message) {
            super(message);
        }
    }
}