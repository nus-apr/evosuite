package org.evosuite.patch;

import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.operators.crossover.CrossOverFunction;

public class PatchCrossOver extends CrossOverFunction<PatchChromosome> {

    /**
     * Replace parents with crossed over individuals
     *
     * @param parent1 a {@link Chromosome} object.
     * @param parent2 a {@link Chromosome} object.
     * @throws ConstructionFailedException if any.
     */
    @Override
    public void crossOver(PatchChromosome parent1, PatchChromosome parent2) throws ConstructionFailedException {

    }
}
