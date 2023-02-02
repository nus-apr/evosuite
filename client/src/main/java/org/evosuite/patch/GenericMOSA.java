package org.evosuite.patch;

import org.evosuite.ClientProcess;
import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.comparators.OnlyCrowdingComparator;
import org.evosuite.ga.operators.ranking.CrowdingDistance;
import org.evosuite.ga.operators.selection.BestKSelection;
import org.evosuite.ga.operators.selection.RandomKSelection;
import org.evosuite.ga.operators.selection.RankSelection;
import org.evosuite.ga.operators.selection.SelectionFunction;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientNodeLocal;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;

import java.util.List;
import java.util.Set;

public abstract class GenericMOSA<T extends Chromosome<T>> extends GenericAbstractMOSA<T> {

    private static final Logger logger = LoggerFactory.getLogger(GenericMOSA.class);

    /**
     * Crowding distance measure to use
     */
    protected CrowdingDistance<T> distance = new CrowdingDistance<>();

    /**
     * Constructor.
     *
     * @param factory a {@link ChromosomeFactory} object
     */
    public GenericMOSA(ChromosomeFactory<T> factory) {
        super(factory);
    }

    /**
     * Generate one new generation
     */
    @Override
    protected void evolve() {
        List<T> offspringPopulation = this.breedNextGeneration();

        // Create the union of parents and offSpring
        List<T> union = new ArrayList<>();
        union.addAll(this.population);
        union.addAll(offspringPopulation);

        Set<FitnessFunction<T>> uncoveredGoals = this.getUncoveredGoals();

        // Ranking the union

        logger.debug("Union Size =" + union.size());
        // Ranking the union using the best rank algorithm (modified version of the non dominated sorting algorithm)
        this.rankingFunction.computeRankingAssignment(union, uncoveredGoals);

        int remain = this.population.size();
        int index = 0;
        List<T> front = null;
        this.population.clear();

        // Obtain the next front
        front = this.rankingFunction.getSubfront(index);

        while ((remain > 0) && (remain >= front.size()) && !front.isEmpty()) {
            // Assign crowding distance to individuals
            this.distance.fastEpsilonDominanceAssignment(front, uncoveredGoals);
            // Add the individuals of this front
            this.population.addAll(front);

            // Decrement remain
            remain = remain - front.size();

            // Obtain the next front
            index++;
            if (remain > 0) {
                front = this.rankingFunction.getSubfront(index);
            }
        }

        // Remain is less than front(index).size, insert only the best one
        if (remain > 0 && !front.isEmpty()) { // front contains individuals to insert
            this.distance.fastEpsilonDominanceAssignment(front, uncoveredGoals);
            front.sort(new OnlyCrowdingComparator<>());
            for (int k = 0; k < remain; k++) {
                this.population.add(front.get(k));
            }

            remain = 0;
        }

        this.currentIteration++;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Generate solution
     */
    @Override
    public void generateSolution() {
        logger.info("executing generateSolution function");

        // keep track of covered goals
        this.fitnessFunctions.forEach(this::addUncoveredGoal);

        // initialize population
        if (this.population.isEmpty()) {
            this.initializePopulation();
        }

        // Calculate dominance ranks and crowding distance
        this.rankingFunction.computeRankingAssignment(this.population, this.getUncoveredGoals());
        for (int i = 0; i < this.rankingFunction.getNumberOfSubfronts(); i++) {
            this.distance.fastEpsilonDominanceAssignment(this.rankingFunction.getSubfront(i), this.getUncoveredGoals());
        }

        final ClientNodeLocal<TestChromosome> clientNode =
                ClientServices.<TestChromosome>getInstance().getClientNode();

//        Listener<Set<TestChromosome>> listener = null;
//        if (Properties.NUM_PARALLEL_CLIENTS > 1) {
//            listener = event -> immigrants.add(new LinkedList<>(event));
//            clientNode.addListener(listener);
//        }

        // TODO add here dynamic stopping condition
        while (!this.isFinished() && this.getNumberOfUncoveredGoals() > 0) {
            this.evolve();
            this.notifyIteration();
        }

//        if (Properties.NUM_PARALLEL_CLIENTS > 1) {
//            clientNode.deleteListener(listener);
//
//            if (ClientProcess.DEFAULT_CLIENT_NAME.equals(ClientProcess.getIdentifier())) {
//                //collect all end result test cases
//                Set<Set<TestChromosome>> collectedSolutions = clientNode.getBestSolutions();
//
//                logger.debug(ClientProcess.DEFAULT_CLIENT_NAME + ": Received " + collectedSolutions.size() + " solution sets");
//                for (Set<TestChromosome> solution : collectedSolutions) {
//                    for (TestChromosome t : solution) {
//                        this.calculateFitness(t);
//                    }
//                }
//            } else {
//                //send end result test cases to Client-0
//                Set<TestChromosome> solutionsSet = new HashSet<>(getSolutions());
//                logger.debug(ClientProcess.getPrettyPrintIdentifier() + "Sending " + solutionsSet.size()
//                        + " solutions to " + ClientProcess.DEFAULT_CLIENT_NAME);
//                clientNode.sendBestSolution(solutionsSet);
//            }
//        }

        // storing the time needed to reach the maximum coverage
//        clientNode.trackOutputVariable(RuntimeVariable.Time2MaxCoverage,
//                this.budgetMonitor.getTime2MaxCoverage());
        this.notifySearchFinished();
    }
}
