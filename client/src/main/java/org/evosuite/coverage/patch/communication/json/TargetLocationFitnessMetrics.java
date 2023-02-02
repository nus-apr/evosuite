package org.evosuite.coverage.patch.communication.json;

import java.util.Map;

public class TargetLocationFitnessMetrics {
    private String className;
    private int targetLine;

    private double minFitness;

    private int numCoveringTests;

    private int numTotalContexts;

    private int numCoveredContexts;

    private Map<String, Double> contextMinFitnessMap;

    private Map<String, Integer> contextCoveringTestsMap;

    public TargetLocationFitnessMetrics() {}
    public TargetLocationFitnessMetrics(String className, int targetLine, double minFitness, int numCoveringTests,
        int numTotalContexts, int numCoveredContexts, Map<String, Double> contextMinFitnessMap, Map<String, Integer> contextCoveringTestsMap) {

        this.className = className;
        this.targetLine = targetLine;
        this.minFitness = minFitness;
        this.numCoveringTests = numCoveringTests;
        this.numTotalContexts = numTotalContexts;
        this.numCoveredContexts = numCoveredContexts;
        this.contextMinFitnessMap = contextMinFitnessMap;
        this.contextCoveringTestsMap =  contextCoveringTestsMap;
    }

    public String getClassName() {
        return className;
    }

    public int getTargetLine() {
        return targetLine;
    }

    public double getMinFitness() {
        return minFitness;
    }

    public int getNumCoveringTests() {
        return numCoveringTests;
    }

    public int getNumTotalContexts() {
        return numTotalContexts;
    }

    public int getNumCoveredContexts() {
        return numCoveredContexts;
    }

    public Map<String, Double> getContextMinFitnessMap() {
        return contextMinFitnessMap;
    }

    public Map<String, Integer> getContextCoveringTestsMap() {
        return contextCoveringTestsMap;
    }
}
