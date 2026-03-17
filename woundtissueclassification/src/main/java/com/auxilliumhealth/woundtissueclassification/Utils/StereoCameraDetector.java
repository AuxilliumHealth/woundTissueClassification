package com.auxilliumhealth.woundtissueclassification.Utils;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StereoCameraDetector {
    private static final String TAG = "StereoCameraDetector";
    private CameraManager cameraManager;
    private String logicalCameraId = null;
    private List<String> physicalCameraIds = new ArrayList<>();

    public StereoCameraDetector(Context context) {
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public boolean isStereoSupported() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(cameraId);

                // We only care about back-facing cameras
                Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                if (facing == null || facing != CameraCharacteristics.LENS_FACING_BACK) continue;

                int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                boolean isLogicalMultiCamera = false;
                if (capabilities != null) {
                    for (int cap : capabilities) {
                        if (cap == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) {
                            isLogicalMultiCamera = true;
                            break;
                        }
                    }
                }

                if (isLogicalMultiCamera) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        Set<String> physicalIds = chars.getPhysicalCameraIds();
                        // We need at least 2 physical cameras to do stereo
                        if (physicalIds != null && physicalIds.size() >= 2) {
                            logicalCameraId = cameraId;
                            physicalCameraIds.addAll(physicalIds);
                            return true; // Compatible multi-camera device found
                        }
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera Access Exception while checking stereo support", e);
        }
        return false;
    }

    public double getBaselineMm() {
        if (physicalCameraIds.size() < 2) return 0.0;
        try {
            CameraCharacteristics c1 = cameraManager.getCameraCharacteristics(physicalCameraIds.get(0));
            CameraCharacteristics c2 = cameraManager.getCameraCharacteristics(physicalCameraIds.get(1));
            
            float[] t1 = c1.get(CameraCharacteristics.LENS_POSE_TRANSLATION);
            float[] t2 = c2.get(CameraCharacteristics.LENS_POSE_TRANSLATION);
            
            if (t1 != null && t2 != null) {
                // Euclidean distance between camera centers (Android returns translation in meters)
                double distMeters = Math.sqrt(Math.pow(t1[0] - t2[0], 2) + 
                                            Math.pow(t1[1] - t2[1], 2) + 
                                            Math.pow(t1[2] - t2[2], 2));
                return distMeters * 1000.0; // Return in mm
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting baseline", e);
        }
        return 0.0;
    }

    public double getFocalLengthPx(int imageWidth) {
        if (physicalCameraIds.isEmpty()) return 0.0;
        try {
            CameraCharacteristics chars = cameraManager.getCameraCharacteristics(physicalCameraIds.get(0));
            float[] focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            SizeF sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            
            if (focalLengths != null && focalLengths.length > 0 && sensorSize != null) {
                float fMm = focalLengths[0];
                float wMm = sensorSize.getWidth();
                // f_px = (f_mm * image_width_px) / sensor_width_mm
                return (fMm * imageWidth) / wMm;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting focal length", e);
        }
        return 0.0;
    }

    public String getLogicalCameraId() {
        return logicalCameraId;
    }

    public List<String> getPhysicalCameraIds() {
        return physicalCameraIds;
    }
}
