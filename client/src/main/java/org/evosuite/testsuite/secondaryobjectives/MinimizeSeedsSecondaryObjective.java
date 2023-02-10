package org.evosuite.testsuite.secondaryobjectives;

import org.evosuite.coverage.patch.SeedHandler;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;

import java.util.Set;

/**
 * Minimize the number of reused seeds in the test suite
 */
public class MinimizeSeedsSecondaryObjective extends SecondaryObjective<TestSuiteChromosome> {

    private static final long serialVersionUID = 5333289992630805338L;

    private int getNumSeeds(TestSuiteChromosome chromosome) {
        Set<TestChromosome> seedTests = SeedHandler.getInstance().getSeedTests();
        return (int) chromosome.getTestChromosomes().stream().filter(seedTests::contains).count();
    }

    @Override
    public int compareChromosomes(TestSuiteChromosome chromosome1, TestSuiteChromosome chromosome2) {
        return getNumSeeds(chromosome1) - getNumSeeds(chromosome2);
    }

    @Override
    public int compareGenerations(TestSuiteChromosome parent1, TestSuiteChromosome parent2, TestSuiteChromosome child1, TestSuiteChromosome child2) {
        return Math.min(getNumSeeds(parent1), getNumSeeds(parent2))
                - Math.min(getNumSeeds(child1), getNumSeeds(child2));
    }
}
