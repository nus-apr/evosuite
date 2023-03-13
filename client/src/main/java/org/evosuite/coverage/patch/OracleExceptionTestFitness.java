package org.evosuite.coverage.patch;

import org.evosuite.Properties;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;


public class OracleExceptionTestFitness extends LineCoverageTestFitness {
    private static final long serialVersionUID = 5599225319249281208L;

    private boolean blind = false;

    /**
     * Constructor - fitness is specific to a method
     *
     * @param className  the class name
     * @param methodName the method name
     * @param line
     * @throws IllegalArgumentException
     */
    public OracleExceptionTestFitness(String className, String methodName, Integer line) {
        super(className, methodName, line);
    }

    public OracleExceptionTestFitness(String className, String methodName, Integer line, boolean blind) {
        super(className, methodName, line);
        this.blind = blind;
        if (blind && !branchFitnesses.isEmpty()) {
            logger.error("Created blind OracleExceptionGoal even though the target location has been instrumented.");
            this.blind = false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Calculate fitness
     *
     * @param individual a {@link org.evosuite.testcase.ExecutableChromosome} object.
     * @param result     a {@link org.evosuite.testcase.execution.ExecutionResult} object.
     * @return a double.
     */
    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        double fitness;

        if (blind) {
            fitness = result.hasOracleException() ? 0.0 : 1.0;
            updateIndividual(individual, fitness);

            if (fitness == 0.0) {
                individual.getTestCase().addCoveredGoal(this);
            }

            if (Properties.TEST_ARCHIVE) {
                Archive.getArchiveInstance().updateArchive(this, individual, fitness);
            }

        } else {
            // Super handles updateIndividual and updateArchive
            fitness = super.getFitness(individual, result);

            if (fitness == 0.0) {
                if (!result.hasOracleException()) {
                    logger.error("Test covers oracle exception location but no exception was thrown!");
                }
            }
        }

        return fitness;
    }
}
