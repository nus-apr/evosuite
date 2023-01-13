package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class SeedTestPopulation {
    private String serializedSuite;
    private List<SeedTest> tests;

    public SeedTestPopulation() {}
    public SeedTestPopulation(String serializedSuite, List<SeedTest> tests) {
        this.serializedSuite = serializedSuite;
        this.tests = tests;
    }

    public String getSerializedSuite() {
        return this.serializedSuite;
    }

    public void setSerializedSuite(String serializedSuite) {
        this.serializedSuite = serializedSuite;
    }

    public List<SeedTest> getTests() {
        return tests;
    }

    public void setTests(List<SeedTest> tests) {
        this.tests = tests;
    }
}
