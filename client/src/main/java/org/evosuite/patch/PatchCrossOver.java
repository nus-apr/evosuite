package org.evosuite.patch;

import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.utils.Randomness;

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
        // HUXSinglePointCrossover in Arja
        int size = parent1.problem.getNumberOfModificationPoints();

        for (int i = 0; i < size; i++) {
            if (parent1.bits.get(i) != parent2.bits.get(i)) {
                if (Randomness.nextDouble() < 0.5) {
                    // swap two bits
                    parent1.bits.flip(i);
                    parent2.bits.flip(i);
                }
            }
        }

        int arrayCrossOverPoint1 = Randomness.nextInt(0, size);
        for (int i = arrayCrossOverPoint1; i < size; i++) {
            int tmp = parent1.array[i];
            parent1.array[i] = parent2.array[i];
            parent2.array[i] = tmp;
        }

        int doubleSize = size + size;
        int arrayCrossOverPoint2 = Randomness.nextInt(size + 1, doubleSize);
        for (int i = arrayCrossOverPoint2; i < doubleSize; i++) {
            int tmp = parent1.array[i];
            parent1.array[i] = parent2.array[i];
            parent2.array[i] = tmp;
        }
    }
}
