package org.evosuite.testcase.secondaryobjectives;

import org.evosuite.coverage.patch.SeedHandler;
import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;

import java.util.Set;

/**
 * Prefers inputs that are not initial seed inputs or an extension of them
 */
public class NoSeedSecondaryObjective extends SecondaryObjective<TestChromosome> {


    private static final long serialVersionUID = 4922063465295883170L;

    @Override
    public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
        Set<TestChromosome> seeds = SeedHandler.getInstance().getSeedTests();
        boolean firstSeed = seeds.contains(chromosome1);
        boolean secondSeed = seeds.contains(chromosome2);

        // Both tests are either seeds or no seeds
        if (firstSeed == secondSeed) {
            return 0;
        }

        if (firstSeed) { // Only first test is a seed
            // Prefer second test if it is not simply an extension of the seed, otherwise prefer (shorter) seed
            if (chromosome1.getTestCase().isPrefix(chromosome2.getTestCase())) {
                return -1;
            } else {
                return +1;
            }
        } else { // Only second test is a seed
            // Prefer first test only if it is not simply an extension  of the second
            if (chromosome2.getTestCase().isPrefix(chromosome1.getTestCase())) {
                return +1;
            } else {
                return -1;
            }
        }
    }

    @Override
    public int compareGenerations(TestChromosome parent1, TestChromosome parent2, TestChromosome child1, TestChromosome child2) {
        // this function is not used
        throw new RuntimeException(
                "compareGenerations function of " + NoSeedSecondaryObjective.class.getCanonicalName()
                        + " has not been implemented yet");
    }
}
