package com.auxilliumhealth.woundtissueclassification.Utils;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.*;

public class CalibrateFit {

    private int coinType;
    private Map<Double, Integer> lensFocusDistPixelCount;
    private String userId;
    private static final int MIN_IMAGES = 7;

    public CalibrateFit(int coinType, Map<Double, Integer> lensFocusDistPixelCount, String userId) {
        this.coinType = coinType;
        this.lensFocusDistPixelCount = lensFocusDistPixelCount;
        this.userId = userId;
    }

    private double getCoinDiameter(int coinType) {
        switch (coinType) {
            case 1: return 0.01905;
            case 2: return 0.02121;
            case 3: return 0.01791;
            case 4: return 0.02426;
            default: return -1;
        }
    }

    public CalibrationResult calibrate() throws Exception {
        double coinDiameter = getCoinDiameter(coinType);
        if (coinDiameter <= 0) {
            throw new Exception("Invalid coin type");
        }

        List<Double> distances = new ArrayList<>(lensFocusDistPixelCount.keySet());
        Collections.sort(distances);

        if (distances.size() < MIN_IMAGES) {
            throw new Exception("At least " + MIN_IMAGES + " data points are required.");
        }

        List<Integer> pixelCounts = new ArrayList<>();
        for (Double d : distances) {
            pixelCounts.add(lensFocusDistPixelCount.get(d));
        }

        double actualCoinArea = Math.PI * Math.pow(coinDiameter / 2, 2); // in m^2
        List<Double> actualAreaPerPixel = new ArrayList<>();
        List<Double> pixelPerUnit = new ArrayList<>();

        for (int count : pixelCounts) {
            double area = actualCoinArea / count;
            actualAreaPerPixel.add(area);
            pixelPerUnit.add(1.0 / Math.sqrt(area));
        }

        // Pick test point (4th index)
        int testIndex = 3;
        double testDistance = distances.get(testIndex);
        double testArea = actualAreaPerPixel.get(testIndex);
        double testPixelPerUnit = pixelPerUnit.get(testIndex);

        // Fit cubic model for area
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(3);
        WeightedObservedPoints obsArea = new WeightedObservedPoints();
        WeightedObservedPoints obsPixel = new WeightedObservedPoints();

        for (int i = 0; i < distances.size(); i++) {
            obsArea.add(distances.get(i), actualAreaPerPixel.get(i));
            obsPixel.add(distances.get(i), pixelPerUnit.get(i));
        }

        double[] areaCoeffs = fitter.fit(obsArea.toList());
        double[] pixelCoeffs = fitter.fit(obsPixel.toList());

        // Predict values at test distance
        double predictedArea = evaluateCubic(areaCoeffs, testDistance);
        double predictedPixelPerUnit = evaluateCubic(pixelCoeffs, testDistance);

        double areaError = Math.abs(predictedArea - testArea) / testArea * 100;
        double pixelError = Math.abs(predictedPixelPerUnit - testPixelPerUnit) / testPixelPerUnit * 100;

        return new CalibrationResult(areaCoeffs, pixelCoeffs, areaError, pixelError, lensFocusDistPixelCount);
    }

    private double evaluateCubic(double[] coeffs, double x) {
        return coeffs[0] + coeffs[1]*x + coeffs[2]*x*x + coeffs[3]*x*x*x;
    }

    public static class CalibrationResult {
        public double[] areaCoeffs;
        public double[] pixelPerUnitCoeffs;
        public double areaError;
        public double pixelError;
        public Map<Double, Integer> pixelCounts;

        public CalibrationResult(double[] areaCoeffs, double[] pixelPerUnitCoeffs,
                                 double areaError, double pixelError, Map<Double, Integer> pixelCounts) {
            this.areaCoeffs = areaCoeffs;
            this.pixelPerUnitCoeffs = pixelPerUnitCoeffs;
            this.areaError = areaError;
            this.pixelError = pixelError;
            this.pixelCounts = pixelCounts;
        }
    }
}

