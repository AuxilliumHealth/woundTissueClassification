package com.auxilliumhealth.woundtissueclassification.Utils;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {
    public static String saveBitmapAsImage(Bitmap bitmap, String fileName) {
        File storageDir = Environment.getExternalStorageDirectory();

        if (storageDir == null || !storageDir.exists()) {
            return null; // External storage not available or not writable
        }

        File imageFile = new File(storageDir, fileName);

        try {
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            // Choose the format and quality to compress the bitmap
            Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
            int quality = 100; // 100 means max quality

            // Compress the bitmap and write it to the file
            bitmap.compress(format, quality, outputStream);

            outputStream.flush();
            outputStream.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; // Error occurred while saving the image
    }

}
