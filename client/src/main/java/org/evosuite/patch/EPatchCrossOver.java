package org.evosuite.patch;

import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.utils.Randomness;

public class EPatchCrossOver extends CrossOverFunction<EPatchChromosome> {
    @Override
    public void crossOver(EPatchChromosome parent1, EPatchChromosome parent2) throws ConstructionFailedException {
        // PureHuxCrossOver2 in Arja-E
        int size = parent1.problem.getNumberOfModificationPoints();
        int arraySize = size * 3;

        for (int i = 0; i < arraySize; i++) {
            if (Randomness.nextDouble() < 0.5) {
                int tmp = parent1.array[i];
                parent1.array[i] = parent2.array[i];
                parent2.array[i] = tmp;
            }
        }

        for (int i = 0; i < size; i++) {
            if (parent1.bits.get(i) != parent2.bits.get(i)) {
                if (Randomness.nextDouble() < 0.5) {
                    // swap two bits
                    parent1.bits.flip(i);
                    parent2.bits.flip(i);
                }
            }
        }
    }
}
