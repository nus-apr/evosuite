package org.evosuite.testcase.secondaryobjectives;

import org.evosuite.coverage.patch.PatchLineCoverageFactory;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

import java.util.Set;

public class MaximizeCoveredFixLocationsSecondaryObjective extends SecondaryObjective<TestChromosome> {

    private static final long serialVersionUID = 6763896747535071060L;

    private int getNumCoveredFixLocations(TestChromosome chromosome) {
        ExecutionResult result = chromosome.getLastExecutionResult();
        if (result != null) {
            Set<Integer> fixLocations = PatchLineCoverageFactory.getFixLocations();
            Set<Integer> coveredLines = result.getTrace().getCoveredLines();
            return (int) fixLocations.stream().filter(coveredLines::contains).count();
        } else {
            return 0;
        }
    }

    @Override
    public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
        return getNumCoveredFixLocations(chromosome2) - getNumCoveredFixLocations(chromosome1);
    }

    @Override
    public int compareGenerations(TestChromosome parent1, TestChromosome parent2, TestChromosome child1, TestChromosome child2) {
        return Math.max(getNumCoveredFixLocations(child1), getNumCoveredFixLocations(child2))
                - Math.max(getNumCoveredFixLocations(parent1), getNumCoveredFixLocations(parent2));
    }
}
