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
package com.auxilliumhealth.woundtissueclassification.Model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.util.Log;

import com.auxilliumhealth.woundtissueclassification.ml.WoundteleCoinDetectionModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.Arrays;

public class WoundBoundingBox {
    private String TAG = "WoundBoundingBox";
    private Bitmap outputBitmap;
    private Bitmap inputBitmap;
    private Context context;
    private int width , height;
    private float[] woundLocations;
    private int[] originalInputSize;
    private float postProcessingThreshold = 0.5f;
    private float BoundingBoxInreaseThreaseHold = 0.1f;

    public WoundBoundingBox(Context context, String inputFilePath , int[] inputSize, float postProcessingThreshold) {
        this.context = context;
        this.height = inputSize[0]; // should be 320
        this.width = inputSize[1];  // should be 320
        this.originalInputSize = inputSize;
        loadInput(inputFilePath);
        this.postProcessingThreshold = postProcessingThreshold;
    }

    public void run() {

        try {
            WoundteleCoinDetectionModel model = WoundteleCoinDetectionModel.newInstance(this.context);

            TensorImage normalizedInputImageTensor = new TensorImage(DataType.UINT8);
            normalizedInputImageTensor.load(inputBitmap);


            // Model Inference
            WoundteleCoinDetectionModel.Outputs outputs = model.process(normalizedInputImageTensor);

            TensorBuffer locations = outputs.getLocationsAsTensorBuffer();
            TensorBuffer scores = outputs.getScoresAsTensorBuffer();


            float[] locationsArray = locations.getFloatArray();
            float[][][] outputBoundingBoxes = reshape(locationsArray, 1, 40, 4);
            float[] outputScores = scores.getFloatArray();

            int maxIndex = 0;
            for (int i = 0; i < outputScores.length; i++) {
                if (outputScores[i] > outputScores[maxIndex]) {
                    maxIndex = i;
                }
            }

            drawBoundingBoxes(this.inputBitmap, outputBoundingBoxes, outputScores, postProcessingThreshold, BoundingBoxInreaseThreaseHold);

            model.close();
        } catch (IOException e) {
            Log.e(TAG, "Model loading or inference failed: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage(), e);
        }
    }

    public float[][][] reshape(float[] array, int dim1, int dim2, int dim3) {
        float[][][] reshapedArray = new float[dim1][dim2][dim3];
        int index = 0;
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                for (int k = 0; k < dim3; k++) {
                    reshapedArray[i][j][k] = array[index++];
                }
            }
        }
        return reshapedArray;
    }

    public void drawBoundingBoxes(Bitmap bitmap, float[][][] outputBoundingBoxes, float[] outputScores, float THRESHOLD, float BoundingBoxInreaseThreaseHold) {
        bitmap = Bitmap.createScaledBitmap(bitmap, originalInputSize[1], originalInputSize[0], false);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(30.0f);
        paint.setColor(Color.RED);

        for (int i = 0; i < outputBoundingBoxes[0].length; i++) {
            float score = outputScores[i];
            if (score > THRESHOLD) {
                float[] box = outputBoundingBoxes[0][i];

                float left = box[1] * bitmap.getWidth();
                float top = box[0] * bitmap.getHeight();
                float right = box[3] * bitmap.getWidth();
                float bottom = box[2] * bitmap.getHeight();

                float boxWidth = right - left;
                float boxHeight = bottom - top;

                left -= BoundingBoxInreaseThreaseHold * boxWidth;
                top -= BoundingBoxInreaseThreaseHold * boxHeight;
                right += BoundingBoxInreaseThreaseHold * boxWidth;
                bottom += BoundingBoxInreaseThreaseHold * boxHeight;

                left = Math.max(0, Math.min(left, bitmap.getWidth() - 1));
                top = Math.max(0, Math.min(top, bitmap.getHeight() - 1));
                right = Math.max(0, Math.min(right, bitmap.getWidth() - 1));
                bottom = Math.max(0, Math.min(bottom, bitmap.getHeight() - 1));

                woundLocations = new float[]{left, top, right, bottom};
                canvas.drawRect(left, top, right, bottom, paint);
            }
        }

        this.outputBitmap = bitmap;
    }

    private void loadInput(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap originalBitmap = BitmapFactory.decodeFile(filePath, options);

        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

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
                    break;
            }

            Bitmap rotatedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
            originalInputSize = new int[]{rotatedBitmap.getHeight(), rotatedBitmap.getWidth()};
            this.inputBitmap = Bitmap.createScaledBitmap(rotatedBitmap, this.width, this.height, true);

        } catch (IOException e) {
            Log.e(TAG, "loadInput: Failed to read EXIF or rotate bitmap", e);
        }
    }

    public Bitmap getOutputBitmap() {
        return this.outputBitmap;
    }

    public float[] getWoundLocations() {
        return this.woundLocations;
    }
}
