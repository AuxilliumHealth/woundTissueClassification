package com.auxilliumhealth.woundtissueclassification.Model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

public class ResultDataModel implements Parcelable {
    public float imageIndex;
    public int pixelCount;
    public File contourImage;
    public File whiteImage;
    public File originalImage;

    public ResultDataModel(float imageIndex, int pixelCount, File contourImage, File whiteImage, File originalImage) {
        this.imageIndex = imageIndex;
        this.pixelCount = pixelCount;
        this.contourImage = contourImage;
        this.whiteImage = whiteImage;
        this.originalImage = originalImage;
    }

    protected ResultDataModel(Parcel in) {
        imageIndex = in.readFloat();
        pixelCount = in.readInt();
        contourImage = new File(in.readString());
        whiteImage = new File(in.readString());
        originalImage = new File(in.readString());
    }

    public static final Creator<ResultDataModel> CREATOR = new Creator<ResultDataModel>() {
        @Override
        public ResultDataModel createFromParcel(Parcel in) {
            return new ResultDataModel(in);
        }

        @Override
        public ResultDataModel[] newArray(int size) {
            return new ResultDataModel[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(imageIndex);
        parcel.writeInt(pixelCount);
        parcel.writeString(contourImage.getAbsolutePath());
        parcel.writeString(whiteImage.getAbsolutePath());
        parcel.writeString(originalImage.getAbsolutePath());
    }

    public float getImageIndex() {
        return imageIndex;
    }

    public void setImageIndex(float imageIndex) {
        this.imageIndex = imageIndex;
    }

    public int getPixelCount() {
        return pixelCount;
    }

    public void setPixelCount(int pixelCount) {
        this.pixelCount = pixelCount;
    }

    public File getcontourImage() {
        return contourImage;
    }

    public void setcontourImage(File contourImage) {
        this.contourImage = contourImage;
    }

    public File getwhiteImage() {
        return whiteImage;
    }

    public void setwhiteImage(File whiteImage) {
        this.whiteImage = whiteImage;
    }

    public File getoriginalImage() {
        return originalImage;
    }

    public void setoriginalImage(File originalImage) {
        this.originalImage = originalImage;
    }
}

