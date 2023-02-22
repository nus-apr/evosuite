package org.evosuite.ga.archive;

import org.evosuite.Properties;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.instrumentation.LinePool;
import org.evosuite.testcase.TestCaseMinimizer;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This Archive additionally maintains a mapping from target line goals to interesting solutions.
 *  Solutions are considered interesting if:
 *  1) They cover the target line
 *  2) They have a unique set of covered lines for the containing method (trace abstraction)
 *  3) They are short
 * Further, the archive maintains a set of partial solutions (solutions covering an uncovered goal, but
 * no target line) to crossover with the line goal solutions.
 */
public class MultiCriteriaCoverageArchive extends CoverageArchive {

    private static final long serialVersionUID = -4367448574491920497L;

    private static final Logger logger = LoggerFactory.getLogger(MultiCriteriaCoverageArchive.class);

    // Map from line goals to line coverage sets to tests
    protected final Map<LineCoverageTestFitness, Map<Set<Integer>, TestChromosome>> targetLineSolutions =  new LinkedHashMap<>();

    // Map storing partial solutions (solutions covering an uncovered goal but no line goal yet)
    protected final Map<TestFitnessFunction, TestChromosome> partialSolutions = new LinkedHashMap<>();


    public static final MultiCriteriaCoverageArchive instance = new MultiCriteriaCoverageArchive();

    public Set<LineCoverageTestFitness> getTargetLineGoals() {
        return targetLineSolutions.keySet();
    }

    @Override
    public void addTarget(TestFitnessFunction target) {
        super.addTarget(target);

        if (target instanceof LineCoverageTestFitness) {
            targetLineSolutions.put((LineCoverageTestFitness) target, new LinkedHashMap<>());
        }
    }

    @Override
    public void updateArchive(TestFitnessFunction target, TestChromosome solution, double fitnessValue) {
        // We only care about covered targets
        if (fitnessValue > 0.0) {
            return;
        }

        // Line goals are handled separately, since they are already considered as "full solutions"
        if (target instanceof LineCoverageTestFitness) {
            handleCoveredLineGoal(target, solution, fitnessValue);
            return;
        }

        // For any other class of goal, we have to check if it also covers a line goal;
        // If it does, add to coverage archive, otherwise add to map of partial solutions

        // We already have a solution for this target, no need to maintain partial solution for it
        // TODO: Should we also differentiate solutions based on the target lines/traces they cover
        if (covered.containsKey(target)) {
            return;
        }

        // Perform basic checks usually done by super
        validateSolution(target, solution, fitnessValue);

        boolean isCoveringTargetLine = getTargetLineGoals().stream().anyMatch(solution.getTestCase()::isGoalCovered);
        // This is a full solution, let super add it to the coverage archive, remove from map of partial solutions
        if (isCoveringTargetLine) {
            super.updateArchive(target, solution, fitnessValue);
            this.partialSolutions.remove(target);
            return;
        }

        // We don't have a solution for this goal yet, check if we can add/replace in map of partial solutions
        boolean isNewPartialSolution = false;
        boolean isNewPartialSolutionBetterThanCurrent = false;

        TestChromosome currentPartialSolution = this.partialSolutions.get(target);

        if (currentPartialSolution == null) {
            isNewPartialSolution = true;
        } else {
            isNewPartialSolutionBetterThanCurrent = this.isBetterThanCurrent(currentPartialSolution, solution);
        }

        if (isNewPartialSolution || isNewPartialSolutionBetterThanCurrent) {
            this.partialSolutions.put(target, solution);
        }
    }

    private void handleCoveredLineGoal(TestFitnessFunction target,
                                       TestChromosome solution,
                                       double fitnessValue) {

        // Minimize test w.r.t. covered line goals
        List<TestFitnessFunction> coveredLineGoals = solution.getTestCase().getCoveredGoals().stream()
                        .filter(LineCoverageTestFitness.class::isInstance)
                        .collect(Collectors.toList());

        // Disabling archive during minimization
        Properties.TEST_ARCHIVE = false;
        TestCaseMinimizer minimizer = new TestCaseMinimizer(null); // TODO EvoRepair: null is a bad idea
        minimizer.minimizeWithCoveredGoals(solution, coveredLineGoals);
        Properties.TEST_ARCHIVE = true;

        // Add solution to archive (or replace existing solution)
        super.updateArchive(target, solution, fitnessValue);

        // Add solution to archive of target line covering solutions
        LineCoverageTestFitness lineGoal = (LineCoverageTestFitness) target;
        ExecutionResult result = solution.getLastExecutionResult();
        if (result != null) {
            // Determine the set of method lines covered by this solution (i.e., the "trace")
            Set<Integer> methodLines = LinePool.getLines(target.getTargetClass(), target.getTargetMethod());
            Set<Integer> coveredMethodLines = result.getTrace().getCoveredLines().stream()
                    .filter(methodLines::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // Check if we already have a solution for this trace, replace if the new solution is better
            Map<Set<Integer>, TestChromosome> solutions = targetLineSolutions.get(lineGoal);
            if (solutions.containsKey(coveredMethodLines)) {
                TestChromosome currentSolution = solutions.get(coveredMethodLines);

                // New solution may replace old solution if it is shorter
                if(this.isBetterThanCurrent(currentSolution, solution)) {
                    solutions.put(coveredMethodLines, solution);
                }
            } else { // First solution for this trace, add to map
                solutions.put(coveredMethodLines, solution);
            }

        } else {
            logger.warn("Found solution with covered goal but no execution result.");
        }

    }

    private void validateSolution(TestFitnessFunction target,
                                  TestChromosome solution,
                                  double fitnessValue) {
        assert target != null;
        assert solution != null;
        assert fitnessValue >= 0.0;
        assert this.covered.containsKey(target) || this.uncovered.contains(target) : "Unknown goal: " + target;

        if (!ArchiveUtils.isCriterionEnabled(target)) {
            throw new RuntimeException(
                    "Trying to update the archive with a target of '" + target.getClass().getSimpleName()
                            + "' type, but correspondent criterion is not enabled.");
        }
    }

    public List<TestChromosome> getTargetLineSolutions() {
        List<TestChromosome> solutions = new ArrayList<>();
        for (LineCoverageTestFitness lineGoal: targetLineSolutions.keySet()) {
            solutions.addAll(targetLineSolutions.get(lineGoal).values());
        }
        return solutions;
    }

    public Map<LineCoverageTestFitness, Map<Set<Integer>, TestChromosome>> getTargetLineSolutionMap() {
        return targetLineSolutions;
    }


    public List<TestChromosome> getPartialSolutions() {
        return new ArrayList<>(partialSolutions.values());
    }

}
