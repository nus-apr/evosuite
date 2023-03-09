package org.evosuite.testcase.secondaryobjectives;

import org.evosuite.ga.SecondaryObjective;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

public class MaximizeTriggeredOracleSecondaryObjective extends SecondaryObjective<TestChromosome> {

    private static final long serialVersionUID = -2800785990113173551L;

    public int getNumOracleTriggers(TestChromosome chromosome) {
        ExecutionResult result = chromosome.getLastExecutionResult();
        if (result != null) {
            return result.hasOracleException() ? 1 : 0;
        } else {
            return 0;
        }
    }

    @Override
    public int compareChromosomes(TestChromosome chromosome1, TestChromosome chromosome2) {
        return getNumOracleTriggers(chromosome2) - getNumOracleTriggers(chromosome1);
    }

    @Override
    public int compareGenerations(TestChromosome parent1, TestChromosome parent2, TestChromosome child1, TestChromosome child2) {
        return (getNumOracleTriggers(child1) + getNumOracleTriggers(child2))
                - (getNumOracleTriggers(parent1) + getNumOracleTriggers(parent2));
    }
}
