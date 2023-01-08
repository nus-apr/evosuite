package org.evosuite.junit.naming.methods;

import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;

import java.util.*;


public class IDTestNameGenerationStrategy implements TestNameGenerationStrategy {

    private final Map<TestCase, String> testToName = new HashMap<>();

    public IDTestNameGenerationStrategy(List<TestCase> testCases, List<ExecutionResult> results) {
        generateNames(testCases);
    }

    public IDTestNameGenerationStrategy(TestSuiteChromosome suite) {
        generateNames(suite.getTests());
    }

    private void generateNames(List<TestCase> testCases) {
        for (TestCase test : testCases) {
            String testName = "test" + test.getID();
            testToName.put(test, testName);
        }
    }

    @Override
    public String getName(TestCase test) {
        return testToName.get(test);
    }

    public List<String> getNames() {
        return new ArrayList<>(testToName.values());
    }
}
