package org.evosuite.testsuite.secondaryobjectives;

import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;

public class MaximizeTriggeredOracleSecondaryObjective extends SecondaryObjective<TestSuiteChromosome> {

    private static final long serialVersionUID = 4331589926389178555L;

    public int getNumOracleTriggers(TestSuiteChromosome chromosome) {
        int sum = 0;
        for (TestChromosome test: chromosome.getTestChromosomes()) {
            ExecutionResult result = test.getLastExecutionResult();
            if (result != null) {
                sum += (int) result.getAllThrownExceptions().stream()
                        .filter(RuntimeException.class::isInstance)
                        .map(Throwable::getMessage)
                        .filter(msg -> msg.equals("[Defects4J_BugReport_Violation]"))
                        .count();
            }
        }
        return sum;
    }

    @Override
    public int compareChromosomes(TestSuiteChromosome chromosome1, TestSuiteChromosome chromosome2) {
        return getNumOracleTriggers(chromosome2) - getNumOracleTriggers(chromosome1);
    }

    @Override
    public int compareGenerations(TestSuiteChromosome parent1, TestSuiteChromosome parent2, TestSuiteChromosome child1, TestSuiteChromosome child2) {
        return Math.max(getNumOracleTriggers(child1), getNumOracleTriggers(child2))
                - Math.max(getNumOracleTriggers(parent1), getNumOracleTriggers(parent2));
    }
}
