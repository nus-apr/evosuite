package org.evosuite.coverage.patch.communication.json;

import java.util.Map;

public class TargetLocationFitnessMetrics {
    protected String className;
    protected int targetLine;

    protected int numCoveringTests;

    protected double minFitness;

    protected int numContexts;

    protected int numCoveredContexts;

    protected Map<String, Double> contextToMinFitness;

    protected Map<String, Integer> contextToNumCoveringTests;

    public TargetLocationFitnessMetrics() {}
    public TargetLocationFitnessMetrics(String className, int targetLine, int numCoveringTests, double minFitness,
                                        int numContexts, int numCoveredContexts,
                                        Map<String, Integer> contextToNumCoveringTests,
                                        Map<String, Double> contextToMinFitness) {

        this.className = className;
        this.targetLine = targetLine;
        this.numCoveringTests = numCoveringTests;
        this.minFitness = minFitness;
        this.numContexts = numContexts;
        this.numCoveredContexts = numCoveredContexts;
        this.contextToNumCoveringTests =  contextToNumCoveringTests;
        this.contextToMinFitness = contextToMinFitness;
    }

    public String getClassName() {
        return className;
    }

    public int getTargetLine() {
        return targetLine;
    }

    public int getNumCoveringTests() {
        return numCoveringTests;
    }

    public double getMinFitness() {
        return minFitness;
    }

    public int getNumContexts() {
        return numContexts;
    }

    public int getNumCoveredContexts() {
        return numCoveredContexts;
    }

    public Map<String, Integer> getContextToNumCoveringTests() {
        return contextToNumCoveringTests;
    }

    public Map<String, Double> getContextToMinFitness() {
        return contextToMinFitness;
    }
}
