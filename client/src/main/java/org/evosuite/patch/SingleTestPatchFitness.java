package org.evosuite.patch;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;

import java.util.Objects;

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

        double fitness;

        if (rawFitness == Double.MAX_VALUE) {
            fitness = Double.MAX_VALUE;
        } else {
            fitness = rawFitness * getWeight();

            if (fitness == 0) {
                SingleTestArchive.getInstance().updateArchive(this, individual);
            }
        }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SingleTestPatchFitness<?> that = (SingleTestPatchFitness<?>) o;
        return Double.compare(that.getWeight(), getWeight()) == 0 && getFullTestName().equals(that.getFullTestName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFullTestName(), getWeight());
    }

    @Override
    public String toString() {
        return String.format("SingleTestPatchFitness[%s,%.1f]", getFullTestName(), getWeight());
    }

    public String getFullTestName() {
        return fullTestName;
    }

    public double getWeight() {
        return weight;
    }
}
