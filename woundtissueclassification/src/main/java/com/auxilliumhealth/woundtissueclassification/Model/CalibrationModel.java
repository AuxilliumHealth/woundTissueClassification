package com.auxilliumhealth.woundtissueclassification.Model;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CalibrationModel {
    @SerializedName("area_coeffs_cubic")
    @Expose
    private List<Double> areaCoeffsCubic;
    @SerializedName("area_error")
    @Expose
    private Double areaError;
    @SerializedName("pixel_error")
    @Expose
    private Double pixelError;
    @SerializedName("pixel_per_unit_coeffs_cubic")
    @Expose
    private List<Double> pixelPerUnitCoeffsCubic;

    public List<Double> getAreaCoeffsCubic() {
        return areaCoeffsCubic;
    }

    public void setAreaCoeffsCubic(List<Double> areaCoeffsCubic) {
        this.areaCoeffsCubic = areaCoeffsCubic;
    }

    public Double getAreaError() {
        return areaError;
    }

    public void setAreaError(Double areaError) {
        this.areaError = areaError;
    }

    public Double getPixelError() {
        return pixelError;
    }

    public void setPixelError(Double pixelError) {
        this.pixelError = pixelError;
    }

    public List<Double> getPixelPerUnitCoeffsCubic() {
        return pixelPerUnitCoeffsCubic;
    }

    public void setPixelPerUnitCoeffsCubic(List<Double> pixelPerUnitCoeffsCubic) {
        this.pixelPerUnitCoeffsCubic = pixelPerUnitCoeffsCubic;
    }
}

