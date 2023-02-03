package org.evosuite.patch;

public interface Patch {
    double getSizePatchFitness();

    double getWeightedFailureRateFitness();

    double getSingleTestFitness(String fullTestName);

    void clearCachedResults();
}
