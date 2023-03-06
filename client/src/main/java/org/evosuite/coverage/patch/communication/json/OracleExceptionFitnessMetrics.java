package org.evosuite.coverage.patch.communication.json;

import java.util.Map;

public class OracleExceptionFitnessMetrics extends TargetLocationFitnessMetrics {
    private int numFixLocationCoveringTests;

    private double minFixLocationFitness;

    protected Map<String, Integer> contextToNumFixLocationCoveringTests;

    protected Map<String, Double> contextToMinFixLocationFitness;

    public OracleExceptionFitnessMetrics() {}

    public OracleExceptionFitnessMetrics(String className, int targetLine, int numCoveringTests, double minFitness,
                                         int numContexts, int numCoveredContexts,
                                         Map<String, Integer> contextToNumCoveringTests,
                                         Map<String, Double> contextToMinFitness,
                                         int numFixLocationCoveringTests,
                                         double minFixLocationFitness,
                                         Map<String, Integer> contextToNumFixLocationCoveringTests,
                                         Map<String, Double> contextToMinFixLocationFitness
                                         ) {

        super(className, targetLine, numCoveringTests, minFitness, numContexts, numCoveredContexts, contextToNumCoveringTests, contextToMinFitness);
        this.numFixLocationCoveringTests = numFixLocationCoveringTests;
        this.minFixLocationFitness = minFixLocationFitness;
        this.contextToNumFixLocationCoveringTests = contextToNumFixLocationCoveringTests;
        this.contextToMinFixLocationFitness = contextToMinFixLocationFitness;
    }

    public int getNumFixLocationCoveringTests() {
        return numFixLocationCoveringTests;
    }

    public double getMinFixLocationFitness() {
        return minFixLocationFitness;
    }

    public Map<String, Integer> getContextToNumFixLocationCoveringTests() {
        return contextToNumFixLocationCoveringTests;
    }

    public Map<String, Double> getContextToMinFixLocationFitness() {
        return contextToMinFixLocationFitness;
    }
}
