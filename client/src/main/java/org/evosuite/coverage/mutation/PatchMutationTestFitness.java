package org.evosuite.coverage.mutation;

import org.evosuite.Properties;
import org.evosuite.assertion.AssertionTraceObserver;
import org.evosuite.coverage.TestCoverageGoal;
import org.evosuite.ga.archive.Archive;
import org.evosuite.ga.stoppingconditions.MaxStatementsStoppingCondition;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.TestCaseExecutor;

/**
 * This fitness function considers a mutant to be killed if we observe exceptional behavior (i.e., custom exception)
 */
public class PatchMutationTestFitness extends StrongMutationTestFitness {
    private static final long serialVersionUID = 7149790438450400843L;

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

    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        // impact (0..1)
        // asserted: 0/1
        //

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

        double impactDistance = 1.0;

        boolean exceptionCase = false;

        // If executed...but not with reflection
        if (executionDistance <= 0 && !result.calledReflection()) {
            // Add infection distance
            assert (result.getTrace() != null);
            // assert (result.getTrace().mutantDistances != null);
            assert (result.getTrace().getTouchedMutants().contains(mutation.getId()));
            infectionDistance = normalize(result.getTrace().getMutationDistance(mutation.getId()));
            logger.debug("Infection distance for mutation = " + infectionDistance);

            // Don't re-execute on the mutant if we believe the mutant causes timeouts
            if (MutationTimeoutStoppingCondition.isDisabled(mutation) && infectionDistance <= 0) {
                impactDistance = 0.0;
            }
            // If infected check if it is also killed
            else if (infectionDistance <= 0) {

                // If the trace was generated without observers, we need to re-execute
                ensureExecutionResultHasTraces(individual, result);

                logger.debug("Running test on mutant " + mutation.getId());
                MutationExecutionResult mutationResult = individual.getLastExecutionResult(mutation);

                if (mutationResult == null) {
                    ExecutionResult exResult = runTest(individual.getTestCase(), mutation);
                    mutationResult = getMutationResult(result, exResult);
                    individual.setLastExecutionResult(mutationResult, mutation);
                }
                if (mutationResult.hasTimeout()) {
                    logger.debug("Found timeout in mutant!");
                    MutationTimeoutStoppingCondition.timeOut(mutation);
                    fitness = 0.0; // Timeout = dead
                    exceptionCase = true;
                }

                if (mutationResult.hasException()) {
                    logger.debug("Mutant raises exception");
                    if (result.noThrownExceptions()) {
                        fitness = 0.0; // Exception difference
                        exceptionCase = true;
                    }
                }

                if (mutationResult.getNumAssertions() == 0) {
                    double impact = mutationResult.getImpact();
                    logger.debug("Impact is " + impact + " (" + (1.0 / (1.0 + impact))
                            + ")");
                    impactDistance = 1.0 / (1.0 + impact);
                } else {
                    logger.debug("Assertions: " + mutationResult.getNumAssertions());
                    impactDistance = 0.0;
                }
                logger.debug("Impact distance for mutation = " + fitness);

            }
        }

        // Note EvoRepair: Add min distance to any oracle exception


        if (!exceptionCase)
            fitness = impactDistance + infectionDistance + executionDistance;
        logger.debug("Individual fitness: " + impactDistance + " + " + infectionDistance
                + " + " + executionDistance + " = " + fitness);
        //if (fitness == 0.0) {
        //	assert (getNumAssertions(individual.getLastExecutionResult(),
        //	                         individual.getLastExecutionResult(mutation)) > 0);
        //}

        updateIndividual(individual, fitness);
        if (fitness == 0.0) {
            individual.getTestCase().addCoveredGoal(this);
            //assert(isCovered(individual, result));
        }
        assert (fitness >= 0.0);
        assert (fitness <= executionDistance + 2.0);

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
