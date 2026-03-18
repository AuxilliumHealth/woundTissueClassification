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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class GyroscopeChecker implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private boolean isGyroAvailable = false;
    private boolean isFlat = false;
    private OnFlatStatusChangeListener listener;

    private float[] gravityMap = new float[3];
    private float[] magneticMap = new float[3];
    private float currentAzimuth = 0f;

    // Interface to send flat status updates
    public interface OnFlatStatusChangeListener {
        void onFlatStatusChanged(boolean isFlat);
        void onAngleChanged(float azimuth);
    }

    // Constructor with listener
    public GyroscopeChecker(Context context, OnFlatStatusChangeListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            isGyroAvailable = (gyroscope != null);
        }
    }

    public boolean isGyroscopeAvailable() {
        return isGyroAvailable;
    }

    public void startListening() {
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
    }

    public float getCurrentAzimuthDegrees() {
        return currentAzimuth;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityMap, 0, event.values.length);

            float x = event.values[0]; // Left-Right tilt
            float y = event.values[1]; // Front-Back tilt
            float z = event.values[2]; // Gravity (should be ~9.8 when flat)

            // Calculate exact angles in degrees
            double pitchAngle = Math.atan2(y, Math.sqrt(x * x + z * z)) * 180 / Math.PI;
            double rollAngle = Math.atan2(-x, z) * 180 / Math.PI;

            // Check if phone is flat
            boolean newFlatStatus = Math.abs(x) < 1.5 && Math.abs(y) < 1.5 && Math.abs(z - 9.8) < 1.5;

            if (newFlatStatus != isFlat) { // Only update if status changes
                isFlat = newFlatStatus;
                
                // Notify the activity
                if (listener != null) {
                    listener.onFlatStatusChanged(isFlat);
                }
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magneticMap, 0, event.values.length);
        }

        // Calculate compass rotation if we have both sensor updates
        if (gravityMap != null && magneticMap != null) {
            float[] rotationMatrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityMap, magneticMap);
            if (success) {
                float[] orientationAngles = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationAngles);

                // Azimuth is orientationAngles[0]
                float azimuthInRadians = orientationAngles[0];
                float azimuthInDegrees = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;
                
                if (Math.abs(azimuthInDegrees - currentAzimuth) > 1.0f) {
                    currentAzimuth = azimuthInDegrees;
                    if (listener != null) {
                        listener.onAngleChanged(currentAzimuth);
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this use case
    }
}
