package com.auxilliumhealth.woundtissueclassification.Utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GyroscopeChecker implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private boolean isGyroAvailable = false;
    private boolean isFlat = false;
    private OnFlatStatusChangeListener listener;

    // Interface to send flat status updates
    public interface OnFlatStatusChangeListener {
        void onFlatStatusChanged(boolean isFlat);
    }

    // Constructor with listener
    public GyroscopeChecker(Context context, OnFlatStatusChangeListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            isGyroAvailable = (gyroscope != null);
        }
    }

    public boolean isGyroscopeAvailable() {
        return isGyroAvailable;
    }

    public void startListening() {
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0]; // Left-Right tilt
            float y = event.values[1]; // Front-Back tilt
            float z = event.values[2]; // Gravity (should be ~9.8 when flat)

            // Check if phone is flat
            boolean newFlatStatus = Math.abs(x) < 1.5 && Math.abs(y) < 1.5 && Math.abs(z - 9.8) < 1.5;

            if (newFlatStatus != isFlat) { // Only update if status changes
                isFlat = newFlatStatus;
//                Log.d("GyroscopeChecker", isFlat ? "Phone is FLAT." : "Phone is NOT flat.");

                // Notify the activity
                if (listener != null) {
                    listener.onFlatStatusChanged(isFlat);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this use case
    }
}
