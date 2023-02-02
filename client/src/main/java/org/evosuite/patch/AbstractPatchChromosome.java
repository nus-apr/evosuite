package org.evosuite.patch;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;

public abstract class AbstractPatchChromosome<E extends AbstractPatchChromosome<E>>
    extends Chromosome<E> implements Patch {

    private boolean hasSizePatchFitness = false;

    /**
     * Adds a fitness function with an associated fitness value, coverage value,
     * and number of covered goals.
     *
     * @param ff              a fitness function
     * @param fitnessValue    the fitness value for {@code ff}
     * @param coverage        the coverage value for {@code ff}
     * @param numCoveredGoals the number of covered goals for {@code ff}
     */
    @Override
    public void addFitness(FitnessFunction<E> ff, double fitnessValue, double coverage, int numCoveredGoals) {
        super.addFitness(ff, fitnessValue, coverage, numCoveredGoals);
        if (ff instanceof SizePatchFitness) {
            hasSizePatchFitness = true;
        }
    }

    /**
     * Secondary Objectives are specific to chromosome types
     *
     * @param o a {@link Chromosome} object.
     * @return a int.
     */
    @Override
    public int compareSecondaryObjective(E o) {
        return hasSizePatchFitness ? 0 : Double.compare(getSizePatchFitness(), o.getSizePatchFitness());
    }
}
