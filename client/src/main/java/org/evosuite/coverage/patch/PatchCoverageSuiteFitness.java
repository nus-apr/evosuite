package org.evosuite.coverage.patch;

import org.evosuite.Properties;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatchCoverageSuiteFitness extends TestSuiteFitnessFunction {

    // TODO: Can the patch pool change during evolution?
    private final Set<PatchCoverageTestFitness> patchGoals = new HashSet<>();
    private final int totalGoals;

    public PatchCoverageSuiteFitness() {
        this(new PatchCoverageFactory().getCoverageGoals());
    }

    public PatchCoverageSuiteFitness(List<PatchCoverageTestFitness> goals) {
        totalGoals = goals.size();
        for (PatchCoverageTestFitness goal : goals) {
            patchGoals.add(goal);
            if (Properties.TEST_ARCHIVE) {
                Archive.getArchiveInstance().addTarget(goal);
            }
        }
    }

    @Override
    public double getFitness(TestSuiteChromosome suite) {
        double fitness = 0.0;

        // TODO: Optimize, check for timeouts (skip tests)
        List<ExecutionResult> results = runTestSuite(suite);
        Set<PatchCoverageTestFitness> killedPatches = new HashSet<>();
        for(PatchCoverageTestFitness goal : patchGoals) {
            for(ExecutionResult result : results) {
                if(goal.isCovered(result)) {
                    killedPatches.add(goal);
                    break;
                }
            }
        }

        int coverage = killedPatches.size();
        fitness = totalGoals - coverage;

        // Penalize fitness if the test suite times out
        for (ExecutionResult result : results) {
            if (result.hasTimeout() || result.hasTestException()) {
                fitness = totalGoals;
                break;
            }
        }

        if (totalGoals > 0) {
            suite.setCoverage(this, (double) coverage / (double) totalGoals);
        } else {
            suite.setCoverage(this, 1.0);
        }

        suite.setNumOfCoveredGoals(this, coverage);
        suite.setNumOfNotCoveredGoals(this, totalGoals - coverage);

        updateIndividual(suite, fitness);

        return fitness;
    }

    @Override
    public boolean updateCoveredGoals() {
        if (!Properties.TEST_ARCHIVE) {
            return false;
        }
        return true;
    }
}
