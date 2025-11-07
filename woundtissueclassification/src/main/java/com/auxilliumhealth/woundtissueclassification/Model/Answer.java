package com.auxilliumhealth.woundtissueclassification.Model;


public class Answer {
    public int questionId;
    public int optionId;

    public Answer(int questionId, int optionId) {
        this.questionId = questionId;
        this.optionId = optionId;
    }
}
