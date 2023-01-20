/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 * <p>
 * This file is part of EvoSuite.
 * <p>
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 * <p>
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.ga.metaheuristics.mosa;

import org.evosuite.Properties;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.ga.archive.Archive;
import org.evosuite.ga.metaheuristics.TestSuiteAdapter;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An adapter that allows all variants of the MOSA algorithm to be used in such contexts where
 * {@code TestSuiteChromosome}s are expected instead of {@code TestChromosome}s.
 */
public class MOSATestSuiteAdapter extends TestSuiteAdapter<AbstractMOSA> {
    private static final long serialVersionUID = 1556980428376303737L;

    public MOSATestSuiteAdapter(final AbstractMOSA algorithm) {
        super(algorithm);
        algorithm.setAdapter(this);
    }

    /*
     * Some methods of the super class (i.e., {@link org.evosuite.ga.metaheuristics.GeneticAlgorithm}
     * class) require a {@link org.evosuite.testsuite.TestSuiteChromosome} object. However, MOSA
     * evolves {@link org.evosuite.testsuite.TestChromosome} objects. Therefore, we must override
     * those methods and create a {@link org.evosuite.testsuite.TestSuiteChromosome} object with all
     * the evolved {@link org.evosuite.testsuite.TestChromosome} objects (either in the population or
     * in the {@link org.evosuite.ga.archive.Archive}).
     *
     * The following code has been copied from org.evosuite.ga.metaheuristics.mosa.AbstractMOSA
     * and retrofitted in a more type-safe fashion.
     */

    @Override
    public List<TestSuiteChromosome> getBestIndividuals() {
        // get final test suite (i.e., non dominated solutions in Archive)
        TestSuiteChromosome bestTestCases = Archive.getArchiveInstance().mergeArchiveAndSolution(new TestSuiteChromosome());
        if (bestTestCases.getTestChromosomes().isEmpty()) {
            for (TestChromosome test : getAlgorithm().getBestIndividuals()) {
                bestTestCases.addTest(test);
            }
        }

        // compute overall fitness and coverage
        this.computeCoverageAndFitness(bestTestCases);

        return Collections.singletonList(bestTestCases);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is used by the Progress Monitor at the and of each generation to show the total coverage reached by the algorithm.
     * Since the Progress Monitor requires a {@link TestSuiteChromosome} object, this method artificially creates
     * a {@link TestSuiteChromosome} object as the union of all solutions stored in the {@link
     * Archive}.</p>
     *
     * <p>The coverage score of the {@link TestSuiteChromosome} object is given by the percentage of targets marked
     * as covered in the archive.</p>
     *
     * @return a {@link TestSuiteChromosome} object to be consumable by the Progress Monitor.
     */
    @Override
    public TestSuiteChromosome getBestIndividual() {
        TestSuiteChromosome best = getAlgorithm().generateSuite();
        if (best.getTestChromosomes().isEmpty()) {
            for (TestChromosome test : getAlgorithm().getBestIndividuals()) {
                best.addTest(test);
            }
            for (TestSuiteFitnessFunction suiteFitness : getAlgorithm().suiteFitnessFunctions.keySet()) {
                best.setCoverage(suiteFitness, 0.0);
                best.setFitness(suiteFitness, 1.0);
            }
            return best;
        }

        // compute overall fitness and coverage
        this.computeCoverageAndFitness(best);

        return best;
    }

    @Override
    public TestSuiteChromosome getBestIndividual(int size) {
        TestSuiteChromosome best = getBestIndividual();
        Set<TestChromosome> bestTests = new LinkedHashSet<>(best.getTestChromosomes());

        // Add next (non-deminated) best tests
        getAlgorithm().getBestIndividuals().stream()
                    .filter(t -> !bestTests.contains(t))
                    .forEach(best::addTestChromosome);

        // Fill up with remaining good tests (that at least cover some fix locations and dont have timeouts)
        //getAlgorithm().getPopulation()
        int remaining = Math.max(0, Properties.EVOREPAIR_NUM_TESTS - best.size());

        if (remaining > 0) {
            calculateFitnessAndSortPopulation();
            getAlgorithm().getPopulation().stream()
                    .filter(t -> !bestTests.contains(t)) // Don't add duplicate tests
                    .filter(t -> !t.getLastExecutionResult().hasTimeout()) // Don't add tests with timeouts
                    .filter(t -> t.getTestCase().getCoveredGoals().stream().anyMatch(LineCoverageTestFitness.class::isInstance)) // Only add tests that cover at least one fix location
                    .limit(remaining) // Only add remaining number of tests
                    .forEach(best::addTestChromosome);
        }
        return best;
    }

    protected void computeCoverageAndFitness(TestSuiteChromosome suite) {
        for (Map.Entry<TestSuiteFitnessFunction, Class<?>> entry : getAlgorithm().suiteFitnessFunctions
                .entrySet()) {
            TestSuiteFitnessFunction suiteFitnessFunction = entry.getKey();
            Class<?> testFitnessFunction = entry.getValue();

            int numberCoveredTargets =
                    Archive.getArchiveInstance().getNumberOfCoveredTargets(testFitnessFunction);
            int numberUncoveredTargets =
                    Archive.getArchiveInstance().getNumberOfUncoveredTargets(testFitnessFunction);
            int totalNumberTargets = numberCoveredTargets + numberUncoveredTargets;

            double coverage = totalNumberTargets == 0 ? 1.0
                    : ((double) numberCoveredTargets) / ((double) totalNumberTargets);

            suite.setFitness(suiteFitnessFunction, numberUncoveredTargets);
            suite.setCoverage(suiteFitnessFunction, coverage);
            suite.setNumOfCoveredGoals(suiteFitnessFunction, numberCoveredTargets);
            suite.setNumOfNotCoveredGoals(suiteFitnessFunction, numberUncoveredTargets);
        }
    }

    void applyLocalSearch(final TestSuiteChromosome testSuite) {
        population = new LinkedList<>();
        population.add(testSuite);
        super.applyLocalSearch();
        population = null;
    }
}
