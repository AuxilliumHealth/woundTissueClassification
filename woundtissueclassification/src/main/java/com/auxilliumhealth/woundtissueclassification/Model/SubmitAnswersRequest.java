package com.auxilliumhealth.woundtissueclassification.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SubmitAnswersRequest {
    @SerializedName("userId")
    private String userId;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("answers")
    private List<Answer> answers;

    public SubmitAnswersRequest(String userId, String sessionId, List<Answer> answers) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.answers = answers;
    }

    public static class Answer {
        @SerializedName("questionId")
        private int questionId;

        @SerializedName("optionId")
        private int optionId;

        public Answer(int questionId, int optionId) {
            this.questionId = questionId;
            this.optionId = optionId;
        }
    }
}