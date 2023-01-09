package org.evosuite.junit.naming.methods;

import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;

import java.util.*;


public class IDTestNameGenerationStrategy implements TestNameGenerationStrategy {
    /**
     * Note: Tests can coincidentally have the same hashcode if they consist of
     * the exact same sequence of statements. Thats why we map testIds to names.
     * TODO: Do we really need a map? We could also directly return the testName based on the id.
     */
    private final Map<Integer, String> testIdToName = new HashMap<>();

    public IDTestNameGenerationStrategy(List<TestCase> testCases) {
        generateNames(testCases);
    }

    public IDTestNameGenerationStrategy(TestSuiteChromosome suite) {
        generateNames(suite.getTests());
    }

    private void generateNames(List<TestCase> testCases) {
        for (TestCase test : testCases) {
            String testName = "test" + test.getID();
            testIdToName.put(test.getID(), testName);
        }
    }

    @Override
    public String getName(TestCase test) {
        return testIdToName.get(test.getID());
    }

    public List<String> getNames() {
        return new ArrayList<>(testIdToName.values());
    }
}
