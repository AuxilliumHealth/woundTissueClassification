package com.auxilliumhealth.woundtissueclassification.Utils;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

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

    public String getLogicalCameraId() {
        return logicalCameraId;
    }

    public List<String> getPhysicalCameraIds() {
        return physicalCameraIds;
    }
}
