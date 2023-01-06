package org.evosuite.patch;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;

public class WeightedFailureRatePatchFitness extends FitnessFunction<PatchChromosome> {
    /**
     * Calculate and set fitness function #TODO the 'set fitness' part should be
     * done by some abstract super class of all FitnessFunctions
     *
     * @param individual a {@link Chromosome} object.
     * @return new fitness
     */
    @Override
    public double getFitness(PatchChromosome individual) {
        return 0;
    }

    /**
     * Do we need to maximize, or minimize this function?
     *
     * @return a boolean.
     */
    @Override
    public boolean isMaximizationFunction() {
        return false;
    }
}
