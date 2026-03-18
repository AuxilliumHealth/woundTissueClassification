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
package com.auxilliumhealth.woundtissueclassification.Model;

import java.util.List;

public class Question {
    public int questionId;
    public String question;
    public List<Option> options;
    public String imageResource; // Add this field

    public static class Option {
        public int optionId;
        public String option;
        public String emoji; // For emoji support in sub-options
        public SubOption subOption;
    }

    public static class SubOption {
        public String subQuestion;
        public List<Option> subOptions;
    }
}



