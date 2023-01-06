package org.evosuite.patch;

import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.utils.Randomness;

import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.ec.problems.ArjaProblem;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class PatchChromosomeFactory implements ChromosomeFactory<PatchChromosome> {

    ArjaProblem problem;
    int[] numberOfAvailableManipulations;
    int[] numberOfIngredients;
    double[] probabilities;

    public PatchChromosomeFactory(ArjaProblem problem) {
        this.problem = problem;
        this.numberOfAvailableManipulations = PatchChromosome.getNumberOfAvailableManipulations(problem);
        this.numberOfIngredients = PatchChromosome.getNumberOfIngredients(problem);

        int size = problem.getNumberOfModificationPoints();
        this.probabilities = new double[size];

        String strategy = problem.getInitializationStrategy();
        if (strategy.equals("Prior")) {
            List<ModificationPoint> modificationPoints = problem.getModificationPoints();

            double mu = problem.getMu();

            for (int i = 0; i < size; i++) {
                probabilities[i] = modificationPoints.get(i).getSuspValue() * mu;
            }
        } else if (strategy.equals("Random")) {
            Arrays.fill(probabilities, 0.5);
        } else {
            throw new RuntimeException("Undefined initialization strategy: " + strategy);
        }
    }

    /**
     * Generates a new chromosome.
     *
     * @return the newly generated chromosome
     */
    @Override
    public PatchChromosome getChromosome() {
        int size = this.problem.getNumberOfModificationPoints();

        BitSet bits = new BitSet(size);
        for (int i = 0; i < size; i++) {
            if (Randomness.nextDouble() < probabilities[i]) {
                bits.set(i);
            }
        }

        int[] array = new int[2 * size];
        for (int i = 0; i < size; i++) {
            array[i] = Randomness.nextInt(0, this.numberOfAvailableManipulations[i]);
            array[size + i] = Randomness.nextInt(0, this.numberOfIngredients[i]);
        }

        return new PatchChromosome(bits, array, this.problem, this.numberOfAvailableManipulations,
                                   this.numberOfIngredients);
    }
}
