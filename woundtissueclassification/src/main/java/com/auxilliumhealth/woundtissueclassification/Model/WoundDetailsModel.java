package com.auxilliumhealth.woundtissueclassification.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WoundDetailsModel {


        @SerializedName("aiModelData")
        @Expose
        private AiModelDataModel aiModelData;
        @SerializedName("WoundScore")
        @Expose
        private String woundScore;
        @SerializedName("imageUrl")
        @Expose
        private String imageUrl;
        @SerializedName("sessionId")
        @Expose
        private String sessionId;
        @SerializedName("DateTime")
        @Expose
        private String dateTime;
        @SerializedName("userId")
        @Expose
        private String userId;
        @SerializedName("woundId")
        @Expose
        private String woundId;

        @SerializedName("answers")
        @Expose
        private List<Answer> answers;

        public AiModelDataModel getAiModelData() {
            return aiModelData;
        }

        public void setAiModelData(AiModelDataModel aiModelData) {
            this.aiModelData = aiModelData;
        }

        public String getWoundScore() {
            return woundScore;
        }

        public void setWoundScore(String woundScore) {
            this.woundScore = woundScore;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getDateTime() {
            return dateTime;
        }

        public void setDateTime(String dateTime) {
            this.dateTime = dateTime;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getWoundId() {
            return woundId;
        }

        public void setWoundId(String woundId) {
            this.woundId = woundId;
        }

        public List<Answer> getAnswers() {
            return answers;
        }

        public void setAnswers(List<Answer> answers) {
            this.answers = answers;
        }


public class AiModelDataModel {
    @SerializedName("ErythemaPercent")
    @Expose
    private Float erythemaPercent;
    @SerializedName("MacerationPercent")
    @Expose
    private Float macerationPercent;
    @SerializedName("Post_Processing")
    @Expose
    private String postProcessing;
    @SerializedName("WoundLength")
    @Expose
    private Float woundLength;
    @SerializedName("NormalPercent")
    @Expose
    private Float normalPercent;
    @SerializedName("DisplayImagePath")
    @Expose
    private String displayImagePath;
    @SerializedName("CallusPercent")
    @Expose
    private Float callusPercent;
    @SerializedName("NormalTissuePercent")
    @Expose
    private Float normalTissuePercent;
    @SerializedName("WoundWidth")
    @Expose
    private Float woundWidth;
    @SerializedName("lens_focal_distance")
    @Expose
    private Float lensFocalDistance;
    @SerializedName("SloughPercent")
    @Expose
    private Float sloughPercent;
    @SerializedName("WoundArea")
    @Expose
    private Float woundArea;
    @SerializedName("WoundDepth")
    @Expose
    private Float woundDepth;
    @SerializedName("WoundTissueOverlayImagePath")
    @Expose
    private String woundTissueOverlayImagePath;
    @SerializedName("PeriWoundTissueOverlayImagePath")
    @Expose
    private String periWoundTissueOverlayImagePath;
    @SerializedName("WoundPeriwoundOverlayImagePath")
    @Expose
    private String woundPeriwoundOverlayImagePath;
    @SerializedName("area_coeffs")
    @Expose
    private List<Object> areaCoeffs;
    @SerializedName("GranulationPercent")
    @Expose
    private Float granulationPercent;
    @SerializedName("CroppedImagePath")
    @Expose
    private String croppedImagePath;
    @SerializedName("EscharPercent")
    @Expose
    private Float escharPercent;
    @SerializedName("pixel_per_unit_coeffs")
    @Expose
    private List<Object> pixelPerUnitCoeffs;

    @SerializedName("WoundMeasurementOverlayImagePath")
    @Expose
    private String woundMeasurementOverlayImagePath;

    @SerializedName("ClockwiseMappingVisualizationImagePath")
    @Expose
    private String ClockwiseMappingVisualizationImagePath;

    public String getClockwiseMappingVisualizationImagePath() {
        return ClockwiseMappingVisualizationImagePath;
    }

    public String getWoundMeasurementOverlayImagePath() {
        return woundMeasurementOverlayImagePath;
    }

    public void setWoundMeasurementOverlayImagePath(String woundMeasurementOverlayImagePath) {
        this.woundMeasurementOverlayImagePath = woundMeasurementOverlayImagePath;
    }

    public Float getErythemaPercent() {
        return erythemaPercent;
    }

    public void setErythemaPercent(Float erythemaPercent) {
        this.erythemaPercent = erythemaPercent;
    }

    public Float getMacerationPercent() {
        return macerationPercent;
    }

    public void setMacerationPercent(Float macerationPercent) {
        this.macerationPercent = macerationPercent;
    }

    public String getPostProcessing() {
        return postProcessing;
    }

    public void setPostProcessing(String postProcessing) {
        this.postProcessing = postProcessing;
    }

    public Float getWoundLength() {
        return woundLength;
    }

    public void setWoundLength(Float woundLength) {
        this.woundLength = woundLength;
    }

    public Float getNormalPercent() {
        return normalPercent;
    }

    public void setNormalPercent(Float normalPercent) {
        this.normalPercent = normalPercent;
    }

    public String getDisplayImagePath() {
        return displayImagePath;
    }

    public void setDisplayImagePath(String displayImagePath) {
        this.displayImagePath = displayImagePath;
    }

    public Float getCallusPercent() {
        return callusPercent;
    }

    public void setCallusPercent(Float callusPercent) {
        this.callusPercent = callusPercent;
    }

    public Float getNormalTissuePercent() {
        return normalTissuePercent;
    }

    public void setNormalTissuePercent(Float normalTissuePercent) {
        this.normalTissuePercent = normalTissuePercent;
    }

    public Float getWoundWidth() {
        return woundWidth;
    }

    public void setWoundWidth(Float woundWidth) {
        this.woundWidth = woundWidth;
    }

    public Float getLensFocalDistance() {
        return lensFocalDistance;
    }

    public void setLensFocalDistance(Float lensFocalDistance) {
        this.lensFocalDistance = lensFocalDistance;
    }

    public Float getSloughPercent() {
        return sloughPercent;
    }

    public void setSloughPercent(Float sloughPercent) {
        this.sloughPercent = sloughPercent;
    }

    public Float getWoundArea() {
        return woundArea;
    }

    public void setWoundArea(Float woundArea) {
        this.woundArea = woundArea;
    }

    public Float getWoundDepth() {
        return woundDepth;
    }

    public void setWoundDepth(Float woundDepth) {
        this.woundDepth = woundDepth;
    }

    public String getWoundTissueOverlayImagePath() {
        return woundTissueOverlayImagePath;
    }

    public void setWoundTissueOverlayImagePath(String woundTissueOverlayImagePath) {
        this.woundTissueOverlayImagePath = woundTissueOverlayImagePath;
    }

    public String getPeriWoundTissueOverlayImagePath() {
        return periWoundTissueOverlayImagePath;
    }

    public void setPeriWoundTissueOverlayImagePath(String periWoundTissueOverlayImagePath) {
        this.periWoundTissueOverlayImagePath = periWoundTissueOverlayImagePath;
    }

    public String getWoundPeriwoundOverlayImagePath() {
        return woundPeriwoundOverlayImagePath;
    }

    public void setWoundPeriwoundOverlayImagePath(String woundPeriwoundOverlayImagePath) {
        this.woundPeriwoundOverlayImagePath = woundPeriwoundOverlayImagePath;
    }

    public List<Object> getAreaCoeffs() {
        return areaCoeffs;
    }

    public void setAreaCoeffs(List<Object> areaCoeffs) {
        this.areaCoeffs = areaCoeffs;
    }

    public Float getGranulationPercent() {
        return granulationPercent;
    }

    public void setGranulationPercent(Float granulationPercent) {
        this.granulationPercent = granulationPercent;
    }

    public String getCroppedImagePath() {
        return croppedImagePath;
    }

    public void setCroppedImagePath(String croppedImagePath) {
        this.croppedImagePath = croppedImagePath;
    }

    public Float getEscharPercent() {
        return escharPercent;
    }

    public void setEscharPercent(Float escharPercent) {
        this.escharPercent = escharPercent;
    }

    public List<Object> getPixelPerUnitCoeffs() {
        return pixelPerUnitCoeffs;
    }

    public void setPixelPerUnitCoeffs(List<Object> pixelPerUnitCoeffs) {
        this.pixelPerUnitCoeffs = pixelPerUnitCoeffs;
    }

}

public class Answer {

    @SerializedName("optionId")
    @Expose
    private Float optionId;
    @SerializedName("questionId")
    @Expose
    private Float questionId;
    @SerializedName("optionText")
    @Expose
    private String optionText;
    @SerializedName("questionText")
    @Expose
    private String questionText;

    public Float getOptionId() {
        return optionId;
    }

    public void setOptionId(Float optionId) {
        this.optionId = optionId;
    }

    public Float getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Float questionId) {
        this.questionId = questionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

}

}