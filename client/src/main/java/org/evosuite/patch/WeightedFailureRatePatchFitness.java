package org.evosuite.patch;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;

public class WeightedFailureRatePatchFitness<T extends AbstractPatchChromosome<T>> extends FitnessFunction<T> {
    /**
     * Calculate and set fitness function #TODO the 'set fitness' part should be
     * done by some abstract super class of all FitnessFunctions
     *
     * @param individual a {@link Chromosome} object.
     * @return new fitness
     */
    @Override
    public double getFitness(T individual) {
        double fitness = individual.getWeightedFailureRateFitness();
        individual.setFitness(this, fitness);
        return fitness;
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
