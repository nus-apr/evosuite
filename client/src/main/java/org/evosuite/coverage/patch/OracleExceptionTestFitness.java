package org.evosuite.coverage.patch;

import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

public class OracleExceptionTestFitness extends LineCoverageTestFitness {
    private static final long serialVersionUID = 5599225319249281208L;

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
        // Super handles updateIndividual and updateArchive
        double fitness = super.getFitness(individual, result);

        if (fitness == 0.0) {
            boolean throwsOracleException = result.getAllThrownExceptions().stream()
                    .filter(RuntimeException.class::isInstance)
                    .map(Throwable::getMessage)
                    .anyMatch(msg -> msg != null && msg.equals("[Defects4J_BugReport_Violation]"));

            if (!throwsOracleException) {
                logger.error("Test covers oracle exception location but no exception was thrown!");
            }
        }

        return fitness;
    }
}
