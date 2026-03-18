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
package com.auxilliumhealth.woundtissueclassification.Manager;


import android.content.Context;
import android.graphics.Bitmap;

import com.auxilliumhealth.woundtissueclassification.Utils.Constants;
import com.auxilliumhealth.woundtissueclassification.Utils.FileUtil;
import com.auxilliumhealth.woundtissueclassification.Model.WoundBoundingBox;


public class WoundBoundingBoxManager {
    private  String sessionId;
    private String imageDirectory;
    private WoundBoundingBox woundBoundingBox;
    private Context context;

    public WoundBoundingBoxManager(Context context) {
        this.context = context;
    }

    public void init(String imagePath, String sessionId) {
        this.imageDirectory = imagePath ;
        this.sessionId = sessionId;
        woundBoundingBox = new WoundBoundingBox(this.context, this.imageDirectory, Constants.WOUND_BOUNDING_BOX_INPUT_SIZE, Constants.WOUND_BOUNDING_BOX_THRESHOLD);
    }

    public void process() {
        woundBoundingBox.run();
    }

    public Bitmap getOutputBitmap() {
        return woundBoundingBox.getOutputBitmap();
    }


    public float[] getWoundCoordinates() {
        return woundBoundingBox.getWoundLocations();
    }

}

