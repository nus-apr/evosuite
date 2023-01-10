package org.evosuite.patch;

import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.utils.Randomness;

import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.ec.problems.ArjaProblem;
import us.msu.cse.repair.ec.representation.ArjaDecisionVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

public class PatchChromosomeFactory implements ChromosomeFactory<PatchChromosome> {

    ArjaProblem problem;
    int[] numberOfAvailableManipulations;
    int[] numberOfIngredients;
    double[] probabilities;

    double initRatioOfPerfect;
    double initRatioOfFame;

    public PatchChromosomeFactory(ArjaProblem problem, double initRatioOfPerfect, double initRatioOfFame) {
        this.problem = problem;
        this.numberOfAvailableManipulations = PatchChromosome.getNumberOfAvailableManipulations(problem);
        this.numberOfIngredients = PatchChromosome.getNumberOfIngredients(problem);

        this.initRatioOfPerfect = initRatioOfPerfect;
        this.initRatioOfFame = initRatioOfFame;

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

    public PatchChromosomeFactory(ArjaProblem problem) {
        this(problem, 0, 0);
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

    /**
     *
     * @param populationSize size of the whole population.
     */
    public List<PatchChromosome> getSeedPopulation(int populationSize) {
        List<PatchChromosome> seedPopulation = new ArrayList<>();

        Set<ArjaDecisionVariable> perfectDecisionVars = problem.getPerfectDecisionVariables();
        if (initRatioOfPerfect != 0) {
            if (perfectDecisionVars == null) {
                throw new RuntimeException("missing perfect seeds (option perfectPath)");
            }

            int numPerfect = (int) Math.round(initRatioOfPerfect * populationSize);
            int i = 0;
            for (ArjaDecisionVariable var: perfectDecisionVars) {
                seedPopulation.add(wrapArjaDecisionVariable(var));

                i++;
                if (i >= numPerfect) {
                    break;
                }
            }
        }

        Set<ArjaDecisionVariable> fameDecisionVars = problem.getFameDecisionVariables();
        if (initRatioOfFame != 0) {
            if (fameDecisionVars == null) {
                throw new RuntimeException("missing hall of fame seeds (option hallOfFameInPath");
            }

            int numFame = (int) Math.round(initRatioOfFame * populationSize);
            int i = 0;
            for (ArjaDecisionVariable var: fameDecisionVars) {
                seedPopulation.add(wrapArjaDecisionVariable(var));

                i++;
                if (i >= numFame) {
                    break;
                }
            }
        }

        return seedPopulation;
    }

    private PatchChromosome wrapArjaDecisionVariable(ArjaDecisionVariable var) {
        BitSet bits = (BitSet) var.getBits().clone();

        int[] origArray = var.getArray();
        int[] array = Arrays.copyOf(origArray, origArray.length);

        return new PatchChromosome(bits, array, problem, numberOfAvailableManipulations, numberOfIngredients);
    }
}
