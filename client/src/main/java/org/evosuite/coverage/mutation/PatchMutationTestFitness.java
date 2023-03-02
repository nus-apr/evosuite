package org.evosuite.coverage.mutation;

import org.evosuite.Properties;
import org.evosuite.assertion.AssertionTraceObserver;
import org.evosuite.coverage.TestCoverageGoal;
import org.evosuite.coverage.patch.OracleExceptionFactory;
import org.evosuite.coverage.patch.OracleExceptionTestFitness;
import org.evosuite.ga.archive.Archive;
import org.evosuite.ga.stoppingconditions.MaxStatementsStoppingCondition;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.TestCaseExecutor;

import java.util.List;

/**
 * This fitness function considers a mutant to be killed if we observe exceptional behavior (i.e., custom exception)
 */
public class PatchMutationTestFitness extends StrongMutationTestFitness {
    private static final long serialVersionUID = 7149790438450400843L;

    private static List<OracleExceptionTestFitness> oracleGoals = new OracleExceptionFactory().getCoverageGoals();
    /**
     * <p>
     * Constructor for MutationTestFitness.
     * </p>
     *
     * @param mutation a {@link Mutation} object.
     */
    public PatchMutationTestFitness(Mutation mutation) {
        super(mutation);
    }

    // Check if execution of the mutant resulted in an oracle exception...
    protected MutationExecutionResult getMutationResult(TestChromosome individual,
                                                        ExecutionResult originalResult,
                                                        ExecutionResult mutationResult) {

        MutationExecutionResult result = new MutationExecutionResult();

        // Check default mutant killing conditions
        boolean timeout = TestCoverageGoal.hasTimeout(mutationResult);
        boolean exceptions = !mutationResult.noThrownExceptions();
        boolean assertions = getNumAssertions(originalResult, mutationResult) > 0;

        boolean oracleException = mutationResult.getAllThrownExceptions().stream()
                .filter(RuntimeException.class::isInstance)
                .map(Throwable::getMessage)
                .anyMatch(msg -> msg != null && msg.equals("[Defects4J_BugReport_Violation]"));

        // Rather than computing impact (coverage difference), we compute the distance to any oracle location
        double minFitness = getOracleExceptionDistance(individual, mutationResult);
        result.setImpact(minFitness);

        if (oracleException) {
            result.setHasException(true);
        } else if (timeout || exceptions || assertions) {
            logger.warn("Test case kills mutant, but without triggering the oracle (distance: {})", minFitness);
        }

        return result;
    }

    private double getOracleExceptionDistance(TestChromosome individual, ExecutionResult mutationResult) {
        return oracleGoals.stream().mapToDouble(o -> o.getFitness(individual, mutationResult)).min().orElse(1.0);
    }

    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        // impact -> oracle distance (0..2)

        // If not touched, fitness = branchcoveragefitnesses + 2

        // If executed, fitness = normalize(constraint distance) + asserted_yes_no

        // If infected, check impact?

        double fitness = 0.0;

        double executionDistance = diameter;

        // Get control flow distance
        if (!result.getTrace().getTouchedMutants().contains(mutation.getId()))
            executionDistance = normalize(getExecutionDistance(result));
        else
            executionDistance = 0.0;

        double infectionDistance = 1.0;

        double oracleExceptionDistance = 2.0; // used to be impactDistance, can range between 0 and 2.0

        boolean exceptionCase = false;

        // If executed...but not with reflection
        if (executionDistance <= 0 && !result.calledReflection()) {
            // Add infection distance
            assert (result.getTrace() != null);
            // assert (result.getTrace().mutantDistances != null);
            assert (result.getTrace().getTouchedMutants().contains(mutation.getId()));
            infectionDistance = normalize(result.getTrace().getMutationDistance(mutation.getId()));
            logger.debug("Infection distance for mutation = " + infectionDistance);

            // If infected check if it is also killed
            if (infectionDistance <= 0) {

                // If the trace was generated without observers, we need to re-execute
                ensureExecutionResultHasTraces(individual, result);

                logger.debug("Running test on mutant " + mutation.getId());
                MutationExecutionResult mutationResult = individual.getLastExecutionResult(mutation);

                if (mutationResult == null) {
                    ExecutionResult exResult = runTest(individual.getTestCase(), mutation);
                    mutationResult = getMutationResult(individual, result, exResult);
                    individual.setLastExecutionResult(mutationResult, mutation);
                }

                if (mutationResult.hasException()) {
                    logger.debug("Mutant raises exception");

                    // We don't care if the original program also throws the exception
                    //if (result.noThrownExceptions()) {
                        fitness = 0.0; // Exception difference
                        exceptionCase = true;
                    //}
                }

                if (!exceptionCase) {
                    oracleExceptionDistance = mutationResult.getImpact();
                    if (oracleExceptionDistance == 0.0) {
                        logger.warn("Test case has zero oracle exception distance but did not throw an exception.");
                    }
                }
            }
        }

        if (!exceptionCase)
            fitness = oracleExceptionDistance + infectionDistance + executionDistance;

        updateIndividual(individual, fitness);
        if (fitness == 0.0) {
            individual.getTestCase().addCoveredGoal(this);
            //assert(isCovered(individual, result));
        }
        assert (fitness >= 0.0);
        assert (fitness <= executionDistance + 3.0);

        if (Properties.TEST_ARCHIVE) {
            Archive.getArchiveInstance().updateArchive(this, individual, fitness);
        }

        return fitness;
    }

    @Override
    public String toString() {
        return "Patch " + mutation.toString();
    }

}
