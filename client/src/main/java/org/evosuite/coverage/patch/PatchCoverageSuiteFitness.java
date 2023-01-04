package org.evosuite.coverage.patch;

import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatchCoverageSuiteFitness extends TestSuiteFitnessFunction {

    // TODO: Can the patch pool change during evolution?
    private final Set<PatchCoverageTestFitness> allPatches = new HashSet<>();

    public PatchCoverageSuiteFitness() {
        allPatches.addAll(new PatchCoverageFactory().getCoverageGoals());
    }

    @Override
    public double getFitness(TestSuiteChromosome suite) {
        double fitness = 0.0;

        // TODO: Optimize, check for timeouts (skip tests)
        List<ExecutionResult> results = runTestSuite(suite);
        Set<PatchCoverageTestFitness> killedPatches = new HashSet<>();
        for(PatchCoverageTestFitness goal : allPatches) {
            for(ExecutionResult result : results) {
                if(goal.isCovered(result)) {
                    killedPatches.add(goal);
                    break;
                }
            }
        }

        fitness = allPatches.size() - killedPatches.size();

        updateIndividual(suite, fitness);
        suite.setNumOfCoveredGoals(this, killedPatches.size());
        if (!allPatches.isEmpty())
            suite.setCoverage(this, (double) killedPatches.size() / (double) allPatches.size());
        else
            suite.setCoverage(this, 1.0);
        return fitness;
    }
}
