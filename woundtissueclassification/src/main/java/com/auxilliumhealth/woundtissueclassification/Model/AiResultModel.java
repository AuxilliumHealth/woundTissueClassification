package com.auxilliumhealth.woundtissueclassification.Model;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AiResultModel {


    @SerializedName("Result")
    @Expose
    private Result result;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }



    public class Result {

        @SerializedName("CallusPercent")
        @Expose
        private String callusPercent;
        @SerializedName("CroppedImagePath")
        @Expose
        private String croppedImagePath;
        @SerializedName("imgPath")
        @Expose
        private String imgPath;
        @SerializedName("DisplayImagePath")
        @Expose
        private String displayImagePath;
        @SerializedName("ErythemaPercent")
        @Expose
        private String erythemaPercent;
        @SerializedName("EscharPercent")
        @Expose
        private String escharPercent;
        @SerializedName("GranulationPercent")
        @Expose
        private String granulationPercent;
        @SerializedName("MacerationPercent")
        @Expose
        private String macerationPercent;
        @SerializedName("NormalPercent")
        @Expose
        private String normalPercent;
        @SerializedName("NormalTissuePercent")
        @Expose
        private String normalTissuePercent;
        @SerializedName("PeriWoundTissueOverlayImagePath")
        @Expose
        private String periWoundTissueOverlayImagePath;
        @SerializedName("Post_Processing")
        @Expose
        private String postProcessing;
        @SerializedName("SloughPercent")
        @Expose
        private String sloughPercent;
        @SerializedName("WoundArea")
        @Expose
        private String woundArea;
        @SerializedName("WoundLength")
        @Expose
        private String woundLength;
        @SerializedName("WoundPeriwoundOverlayImagePath")
        @Expose
        private String woundPeriwoundOverlayImagePath;
        @SerializedName("WoundTissueOverlayImagePath")
        @Expose
        private String woundTissueOverlayImagePath;
        @SerializedName("WoundWidth")
        @Expose
        private String woundWidth;

        public String getCallusPercent() {
            return callusPercent;
        }

        public void setCallusPercent(String callusPercent) {
            this.callusPercent = callusPercent;
        }

        public String getCroppedImagePath() {
            return croppedImagePath;
        }

        public void setCroppedImagePath(String croppedImagePath) {
            this.croppedImagePath = croppedImagePath;
        }

        public String getDisplayImagePath() {
            return displayImagePath;
        }

        public void setDisplayImagePath(String displayImagePath) {
            this.displayImagePath = displayImagePath;
        }

        public String getErythemaPercent() {
            return erythemaPercent;
        }

        public void setErythemaPercent(String erythemaPercent) {
            this.erythemaPercent = erythemaPercent;
        }

        public String getEscharPercent() {
            return escharPercent;
        }

        public void setEscharPercent(String escharPercent) {
            this.escharPercent = escharPercent;
        }

        public String getGranulationPercent() {
            return granulationPercent;
        }

        public void setGranulationPercent(String granulationPercent) {
            this.granulationPercent = granulationPercent;
        }

        public String getMacerationPercent() {
            return macerationPercent;
        }

        public void setMacerationPercent(String macerationPercent) {
            this.macerationPercent = macerationPercent;
        }

        public String getNormalPercent() {
            return normalPercent;
        }

        public void setNormalPercent(String normalPercent) {
            this.normalPercent = normalPercent;
        }

        public String getNormalTissuePercent() {
            return normalTissuePercent;
        }

        public void setNormalTissuePercent(String normalTissuePercent) {
            this.normalTissuePercent = normalTissuePercent;
        }

        public String getPeriWoundTissueOverlayImagePath() {
            return periWoundTissueOverlayImagePath;
        }

        public void setPeriWoundTissueOverlayImagePath(String periWoundTissueOverlayImagePath) {
            this.periWoundTissueOverlayImagePath = periWoundTissueOverlayImagePath;
        }

        public String getPostProcessing() {
            return postProcessing;
        }

        public void setPostProcessing(String postProcessing) {
            this.postProcessing = postProcessing;
        }

        public String getSloughPercent() {
            return sloughPercent;
        }

        public void setSloughPercent(String sloughPercent) {
            this.sloughPercent = sloughPercent;
        }

        public String getWoundArea() {
            return woundArea;
        }

        public void setWoundArea(String woundArea) {
            this.woundArea = woundArea;
        }

        public String getWoundLength() {
            return woundLength;
        }

        public void setWoundLength(String woundLength) {
            this.woundLength = woundLength;
        }

        public String getWoundPeriwoundOverlayImagePath() {
            return woundPeriwoundOverlayImagePath;
        }

        public void setWoundPeriwoundOverlayImagePath(String woundPeriwoundOverlayImagePath) {
            this.woundPeriwoundOverlayImagePath = woundPeriwoundOverlayImagePath;
        }

        public String getWoundTissueOverlayImagePath() {
            return woundTissueOverlayImagePath;
        }

        public void setWoundTissueOverlayImagePath(String woundTissueOverlayImagePath) {
            this.woundTissueOverlayImagePath = woundTissueOverlayImagePath;
        }

        public String getWoundWidth() {
            return woundWidth;
        }

        public void setWoundWidth(String woundWidth) {
            this.woundWidth = woundWidth;
        }

        public String getImgPath() {
            return imgPath;
        }

        public void setImgPath(String imgPath) {
            this.imgPath = imgPath;
        }
    }
}
