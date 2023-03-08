package org.evosuite.testsuite.secondaryobjectives;

import org.evosuite.coverage.patch.FixLocationCoverageFactory;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;

import java.util.Set;

public class MaximizeCoveredFixLocationsSecondaryObjective extends SecondaryObjective<TestSuiteChromosome>  {
    private static final long serialVersionUID = -4865334831581659209L;

    private int getNumCoveredFixLocations(TestSuiteChromosome chromosome) {
        int sum = 0;
        Set<Integer> fixLocations = FixLocationCoverageFactory.getFixLocations();
        for (TestChromosome test: chromosome.getTestChromosomes()) {
            ExecutionResult result = test.getLastExecutionResult();
            if (result != null) {
                Set<Integer> coveredLines = result.getTrace().getCoveredLines();
                sum += (int) fixLocations.stream().filter(coveredLines::contains).count();
            }
        }
        return sum;
    }

    @Override
    public int compareChromosomes(TestSuiteChromosome chromosome1, TestSuiteChromosome chromosome2) {
        return getNumCoveredFixLocations(chromosome2) - getNumCoveredFixLocations(chromosome1);
    }

    @Override
    public int compareGenerations(TestSuiteChromosome parent1, TestSuiteChromosome parent2, TestSuiteChromosome child1, TestSuiteChromosome child2) {
        return Math.max(getNumCoveredFixLocations(child1), getNumCoveredFixLocations(child2))
                - Math.max(getNumCoveredFixLocations(parent1), getNumCoveredFixLocations(parent2));
    }
}
