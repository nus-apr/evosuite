package org.evosuite.coverage.patch;

import org.evosuite.coverage.line.LineCoverageTestFitness;

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
}
