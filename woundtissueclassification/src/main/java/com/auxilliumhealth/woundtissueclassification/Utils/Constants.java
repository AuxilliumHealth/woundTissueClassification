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

import android.os.Environment;

public class Constants {
    public static final String API_FAILURE = "Something went wrong,please try again";
    public static final int[] WOUND_BOUNDING_BOX_INPUT_SIZE = new int[]{320,320};
    public static final String WOUND_BOUNDING_BOX_OUTPUT_IMAGE_NAME = "BoundingBox_output.png";
    public static final float WOUND_BOUNDING_BOX_THRESHOLD = 0.7f;
    public static final String FILE_NAME =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/Auxilliumhealth/";

}
