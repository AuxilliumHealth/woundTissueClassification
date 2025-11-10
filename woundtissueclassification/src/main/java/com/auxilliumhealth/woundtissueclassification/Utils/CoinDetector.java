package com.auxilliumhealth.woundtissueclassification.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
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
        File cacheDir = new File(context.getCacheDir(), "wound_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        for (int i = 0; i < filePaths.size(); i++) {
            String originalFilePath = filePaths.get(i);
            float[] crop = croppedCoords.get(i);

            try {
                File file = new File(originalFilePath);
                String filename = file.getName().toLowerCase();
                if (!(filename.endsWith(".png") || filename.endsWith(".jpg") ||
                        filename.endsWith(".jpeg") || filename.endsWith(".bmp"))) {
                    throw new CustomError("Unsupported file format: " + filename);
                }

                Bitmap fullBitmap = BitmapFactory.decodeFile(originalFilePath);
                if (fullBitmap == null) {
                    throw new CustomError("Bitmap could not be decoded: " + filename);
                }

                float left = Math.max(0, crop[0]);
                float top = Math.max(0, crop[1]);
                float right = Math.min(crop[2], fullBitmap.getWidth());
                float bottom = Math.min(crop[3], fullBitmap.getHeight());

                int width = (int)(right - left);
                int height = (int)(bottom - top);

                if (width <= 0 || height <= 0) {
                    throw new CustomError("Invalid crop region for image: " + filename);
                }

                Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, (int)left, (int)top, width, height);
                Mat croppedMat = new Mat();
                Utils.bitmapToMat(croppedBitmap, croppedMat);

                Mat gray = new Mat();
                Imgproc.cvtColor(croppedMat, gray, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

                Mat binary = new Mat();
                Imgproc.adaptiveThreshold(gray, binary, 255,
                        Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 10);

                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                List<MatOfPoint> validContours = new ArrayList<>();
                for (MatOfPoint contour : contours) {
                    if (Imgproc.contourArea(contour) > 100) {
                        validContours.add(contour);
                    }
                }

                if (validContours.isEmpty()) {
                    File outputFile = new File(cacheDir, "original_" + file.getName());
                    saveBitmapToFile(fullBitmap, outputFile);
                    resultDataList.add(new ResultDataModel(0f, 0, null, null, outputFile));
                    continue;
                }

                MatOfPoint largestContour = validContours.stream()
                        .max(Comparator.comparingDouble(Imgproc::contourArea))
                        .orElse(validContours.get(0));

                Mat filledContour = Mat.zeros(gray.size(), CvType.CV_8UC1);
                Imgproc.drawContours(filledContour, List.of(largestContour), -1, new Scalar(255), -1);
                int pixelCount = Core.countNonZero(filledContour);

                Mat originalMat = new Mat();
                Utils.bitmapToMat(fullBitmap, originalMat);

                Point[] translatedPoints = largestContour.toArray();
                for (Point p : translatedPoints) {
                    p.x += left;
                    p.y += top;
                }

                MatOfPoint translatedContour = new MatOfPoint(translatedPoints);
                Imgproc.drawContours(originalMat, List.of(translatedContour), -1, new Scalar(0, 255, 0), 3);

                String timestamp = String.valueOf(System.currentTimeMillis());
                File filledFile = new File(cacheDir, "filled_" + timestamp + "_" + file.getName());
                File combinedFile = new File(cacheDir, "combined_" + timestamp + "_" + file.getName());

                saveMatToFile(filledContour, filledFile);
                saveMatToFile(originalMat, combinedFile);

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

    public ResultDataModel detectCoinsExtractPixelCounts(float[] croppedCoords, String originalFilePath) throws CustomError {
        if (originalFilePath == null || originalFilePath.isEmpty()) {
            throw new CustomError("No image path provided");
        }

        File file = new File(originalFilePath);
        String filename = file.getName().toLowerCase();

        if (!(filename.endsWith(".png") || filename.endsWith(".jpg") ||
                filename.endsWith(".jpeg") || filename.endsWith(".bmp"))) {
            throw new CustomError("Unsupported file format: " + filename);
        }

        Bitmap fullBitmap = BitmapFactory.decodeFile(originalFilePath);
        if (fullBitmap == null) {
            throw new CustomError("Bitmap could not be decoded: " + filename);
        }

        File cacheDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "wound_images");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        try {
            float left = Math.max(0, croppedCoords[0]);
            float top = Math.max(0, croppedCoords[1]);
            float right = Math.min(croppedCoords[2], fullBitmap.getWidth());
            float bottom = Math.min(croppedCoords[3], fullBitmap.getHeight());

            int width = (int)(right - left);
            int height = (int)(bottom - top);

            if (width <= 0 || height <= 0) {
                throw new CustomError("Invalid crop region");
            }

            Bitmap croppedBitmap = Bitmap.createBitmap(fullBitmap, (int)left, (int)top, width, height);
            Mat croppedMat = new Mat();
            Utils.bitmapToMat(croppedBitmap, croppedMat);

            // Enhanced preprocessing
            Mat gray = new Mat();
            Imgproc.cvtColor(croppedMat, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

            Mat binary = new Mat();
            Imgproc.adaptiveThreshold(gray, binary, 255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
            // Fallback to Otsu's thresholding if adaptive fails
            if (Core.countNonZero(binary) == 0 || Core.countNonZero(binary) == binary.total()) {
                Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
            }

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Log.d("CoinDetector", "Total contours found: " + contours.size());

            List<MatOfPoint> validContours = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > 50) { // Reduced threshold to capture smaller coins
                    validContours.add(contour);
                }
            }

            Log.d("CoinDetector", "Valid contours after filtering: " + validContours.size());

            if (validContours.isEmpty()) {
                File outputFile = new File(cacheDir, "original_" + file.getName());
                saveBitmapToFile(fullBitmap, outputFile);
                return new ResultDataModel(0f, 0, null, null, outputFile);
            }

            MatOfPoint largestContour = validContours.stream()
                    .max(Comparator.comparingDouble(Imgproc::contourArea))
                    .orElse(null);

            if (largestContour == null) {
                File outputFile = new File(cacheDir, "original_" + file.getName());
                saveBitmapToFile(fullBitmap, outputFile);
                return new ResultDataModel(0f, 0, null, null, outputFile);
            }

            // Dilate contour to ensure entire coin is covered
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
            Mat dilatedContour = new Mat();
            Imgproc.dilate(Mat.zeros(gray.size(), CvType.CV_8UC1), dilatedContour, kernel);
            Imgproc.drawContours(dilatedContour, List.of(largestContour), -1, new Scalar(255), -1);
            int pixelCount = Core.countNonZero(dilatedContour);

            Mat originalMat = new Mat();
            Utils.bitmapToMat(fullBitmap, originalMat);

            Point[] translatedPoints = largestContour.toArray();
            for (Point p : translatedPoints) {
                p.x += left;
                p.y += top;
            }

            MatOfPoint translatedContour = new MatOfPoint(translatedPoints);
            Imgproc.drawContours(originalMat, List.of(translatedContour), -1, new Scalar(0, 255, 0), 5); // Thicker line

            File filledFile = new File(cacheDir, "filled_" + file.getName());
            File combinedFile = new File(cacheDir, "combined_" + file.getName());

            saveMatToFile(dilatedContour, filledFile);
            saveMatToFile(originalMat, combinedFile);

            return new ResultDataModel(0f, pixelCount, combinedFile, filledFile, file);

        } catch (Exception e) {
            Log.e("CoinDetector", "Error processing image: " + originalFilePath, e);
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