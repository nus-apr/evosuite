package org.evosuite.coverage.mutation;

import org.evosuite.Properties;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

/**
 * This fitness function considers a mutant to be killed if we observe exceptional behavior (i.e., custom exception)
 */
public class StrongPatchMutationTestFitness extends StrongMutationTestFitness {
    private static final long serialVersionUID = 7149790438450400843L;


    /**
     * <p>
     * Constructor for MutationTestFitness.
     * </p>
     *
     * @param mutation a {@link Mutation} object.
     */
    public StrongPatchMutationTestFitness(Mutation mutation, boolean strong, boolean patchStrong) {
        super(mutation, strong, patchStrong);
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

        boolean oracleExceptionCase = false;

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

                if (mutationResult.hasOracleException()) {
                    logger.debug("Mutant raises exception");

                    // We don't care if the original program also throws the exception, this mutant is considered as killed
                    fitness = 0.0;
                    oracleExceptionCase = true;
                }

                if (!oracleExceptionCase) {
                    oracleExceptionDistance = mutationResult.getOracleExceptionDistance();
                    if (oracleExceptionDistance == 0.0) {
                        logger.warn("Test case has zero oracle exception distance but did not throw an exception.");
                    }
                }
            }
        }

        if (!oracleExceptionCase)
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
