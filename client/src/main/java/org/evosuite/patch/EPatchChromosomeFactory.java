package org.evosuite.patch;

import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.msu.cse.repair.core.parser.ExtendedModificationPoint;
import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.ec.problems.ArjaEProblem;
import us.msu.cse.repair.ec.representation.ArjaDecisionVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class EPatchChromosomeFactory implements ChromosomeFactory<EPatchChromosome> {

    private static final Logger logger = LoggerFactory.getLogger(EPatchChromosomeFactory.class);

    ArjaEProblem problem;
    int[] numberOfAvailableManipulations;
    int[] numberOfReplaceIngredients;
    int[] numberOfInsertIngredients;
    double[] probabilities;

    double initRatioOfPerfect;
    double initRatioOfFame;

    double mutationProbability_;

    public EPatchChromosomeFactory(ArjaEProblem problem, double initRatioOfPerfect, double initRatioOfFame,
                                   double mutationProbability) {
        this.problem = problem;
        this.numberOfAvailableManipulations = EPatchChromosome.getNumberOfAvailableManipulations(problem);
        this.numberOfReplaceIngredients = EPatchChromosome.getNumberOfReplaceIngredients(problem);
        this.numberOfInsertIngredients = EPatchChromosome.getNumberOfInsertIngredients(problem);

        this.initRatioOfPerfect = initRatioOfPerfect;
        this.initRatioOfFame = initRatioOfFame;

        this.mutationProbability_ = mutationProbability;

        List<ExtendedModificationPoint> modificationPoints = problem.getExtendedModificationPoints();
        int size = modificationPoints.size();
        this.probabilities = new double[size];

        String strategy = problem.getInitializationStrategy();
        if (strategy.equals("Prior")) {
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

    public EPatchChromosomeFactory(ArjaEProblem problem, double mutationProbability) {
        this(problem, 0, 0, mutationProbability);
    }

    /**
     * Generates a new chromosome.
     *
     * @return the newly generated chromosome
     */
    @Override
    public EPatchChromosome getChromosome() {
        int size = this.problem.getExtendedModificationPoints().size();

        BitSet bits = new BitSet(size);
        for (int i = 0; i < size; i++) {
            if (Randomness.nextDouble() < probabilities[i]) {
                bits.set(i);
            }
        }

        int[] array = new int[3 * size];
        for (int i = 0; i < size; i++) {
            array[i] = Randomness.nextInt(0, this.numberOfAvailableManipulations[i]);
            array[size + i] = Randomness.nextInt(0, Math.max(this.numberOfReplaceIngredients[i], 1));
            array[size * 2 + i] = Randomness.nextInt(0, Math.max(this.numberOfInsertIngredients[i], 1));
        }

        return new EPatchChromosome(bits, array, this.problem, this.numberOfAvailableManipulations,
                this.numberOfReplaceIngredients, this.numberOfInsertIngredients, this.mutationProbability_);
    }

    /**
     *
     * @param populationSize size of the whole population.
     */
    public List<EPatchChromosome> getSeedPopulation(int populationSize) {
        List<EPatchChromosome> seedPopulation = new ArrayList<>();

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
            logger.info("{} perfect patches are used as seeds", i);
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
            logger.info("{} hall-of-fame patches are used as seeds", i);
        }

        return seedPopulation;
    }

    public List<EPatchChromosome> getSeeds() {
        List<EPatchChromosome> seeds = new ArrayList<>();
        seeds.addAll(problem.getPerfectDecisionVariables()
                            .stream()
                            .map(this::wrapArjaDecisionVariable)
                            .collect(Collectors.toList()));
        seeds.addAll(problem.getFameDecisionVariables()
                            .stream()
                            .map(this::wrapArjaDecisionVariable)
                            .collect(Collectors.toList()));
        return seeds;
    }

    private EPatchChromosome wrapArjaDecisionVariable(ArjaDecisionVariable var) {
        BitSet bits = (BitSet) var.getBits().clone();

        int[] origArray = var.getArray();
        int[] array = Arrays.copyOf(origArray, origArray.length);

        return new EPatchChromosome(bits, array, problem, numberOfAvailableManipulations,
                                    numberOfReplaceIngredients, numberOfInsertIngredients,
                                    this.mutationProbability_);
    }
}
