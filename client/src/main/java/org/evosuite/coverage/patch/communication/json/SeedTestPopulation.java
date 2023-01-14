package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class SeedTestPopulation {
    private String serializedSuite;

    private String testPrefix;
    private List<SeedTest> tests;

    public SeedTestPopulation() {}
    public SeedTestPopulation(String serializedSuite, String testPrefix, List<SeedTest> tests) {
        this.serializedSuite = serializedSuite;
        this.testPrefix = testPrefix;
        this.tests = tests;
    }

    public String getSerializedSuite() {
        return this.serializedSuite;
    }

    public void setSerializedSuite(String serializedSuite) {
        this.serializedSuite = serializedSuite;
    }

    public String getTestPrefix() {
        return testPrefix;
    }

    public void setTestPrefix(String testPrefix) {
        this.testPrefix = testPrefix;
    }

    public List<SeedTest> getTests() {
        return tests;
    }

    public void setTests(List<SeedTest> tests) {
        this.tests = tests;
    }
}
