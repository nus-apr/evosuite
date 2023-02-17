package org.evosuite.ga.archive;

import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.instrumentation.LinePool;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This Archive additionally maintains a mapping from target line goals to interesting solutions.
 * Solutions are considered interesting if:
 * 1) They cover the target line
 * 2) They have a unique set of covered lines for the containing method (trace abstraction)
 * 3) They are short
 */
public class CoverageWithLineArchive extends CoverageArchive {

    private static final long serialVersionUID = -4367448574491920497L;

    private static final Logger logger = LoggerFactory.getLogger(CoverageWithLineArchive.class);

    // Map from line goals to line coverage sets to tests
    protected final Map<LineCoverageTestFitness, Map<Set<Integer>, TestChromosome>> targetLineSolutions =  new LinkedHashMap<>();

    public static final CoverageWithLineArchive instance = new CoverageWithLineArchive();

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
        super.updateArchive(target, solution, fitnessValue);

        if (fitnessValue > 0.0 || !(target instanceof LineCoverageTestFitness)) { // We only care about covered line goals
            return;
        }

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

    public List<TestChromosome> getTargetLineSolutions() {
        List<TestChromosome> solutions = new ArrayList<>();
        for (LineCoverageTestFitness lineGoal: targetLineSolutions.keySet()) {
            solutions.addAll(targetLineSolutions.get(lineGoal).values());
        }
        return solutions;
    }

}
