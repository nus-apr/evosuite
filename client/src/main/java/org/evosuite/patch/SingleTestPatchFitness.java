package org.evosuite.patch;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;

public class SingleTestPatchFitness<T extends AbstractPatchChromosome<T>> extends FitnessFunction<T> {

    final String fullTestName;
    final double weight;

    public SingleTestPatchFitness(String fullTestName, double weight) {
        this.fullTestName = fullTestName;
        this.weight = weight;
    }

    /**
     * Calculate and set fitness function #TODO the 'set fitness' part should be
     * done by some abstract super class of all FitnessFunctions
     *
     * @param individual a {@link Chromosome} object.
     * @return new fitness
     */
    @Override
    public double getFitness(T individual) {
        double rawFitness = individual.getSingleTestFitness(fullTestName);
        if (rawFitness == Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        }
        return rawFitness * getWeight();
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

    public String getFullTestName() {
        return fullTestName;
    }

    public double getWeight() {
        return weight;
    }
}
