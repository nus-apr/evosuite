package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.Properties;
import org.evosuite.ga.archive.Archive;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

import java.io.File;
import java.util.*;

public class PatchCoverageTestFitness extends TestFitnessFunction {

    private final Patch targetPatch;

    public PatchCoverageTestFitness(Patch targetPatch) {
        this.targetPatch = Objects.requireNonNull(targetPatch, "targetPatch cannot be null");
    }

    public Patch getTargetPatch() {
        return targetPatch;
    }

    // TODO: Implement fitness as covering patch locations + killing it
    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        boolean isCovered = isCovered(individual.getTestCase());
        double fitness = isCovered ? 0.0 : 1.0;

        updateIndividual(individual, fitness);

        if (fitness == 0.0) {
            individual.getTestCase().addCoveredGoal(this);
        }

        // TODO: why is this necessary?
        if (Properties.TEST_ARCHIVE) {
            Archive.getArchiveInstance().updateArchive(this, individual, fitness);
        }

        return fitness;
    }

    // TODO: Add class/method info to patch
    @Override
    public int compareTo(TestFitnessFunction other) {
        if (other instanceof PatchCoverageTestFitness) {
            return Integer.compare(targetPatch.getId(), ((PatchCoverageTestFitness) other).targetPatch.getId());
        }
        return 0;
    }

    // TODO: request generation can potentially be optimized using JsonGenerator
    // TODO: Avoid writing out the same test suite multiple times
    private PatchValidationResult getPatchValidationResult(TestCase tc, ExecutionResult cachedResult) {
        // Write out test suite
        TestSuiteWriter suiteWriter = new TestSuiteWriter();
        suiteWriter.insertTest(tc);

        String name = Properties.TARGET_CLASS.substring(Properties.TARGET_CLASS.lastIndexOf(".") + 1);
        String testDir = "evosuite-patch-tests"; // TODO: make configurable
        String id = String.valueOf(tc.hashCode());
        String suffix = Properties.JUNIT_SUFFIX;

        List<File> tests = suiteWriter.writeTestCase(name + id + suffix, testDir, cachedResult);

        // Generate request for orchestrator
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("cmd", 2); // TODO: Refactor commands to enum/interface
        Map<String, Object> patchInfo = new LinkedHashMap<>();
        patchInfo.put("patchId", targetPatch.getId());
        //patchInfo.put("testSuiteDir", testDir);
        //patchInfo.put("testName", name + suffix);
        patchInfo.put("testFilePath", tests.get(0).getAbsolutePath());
        if (Properties.TEST_SCAFFOLDING && !Properties.NO_RUNTIME_DEPENDENCY) {
            patchInfo.put("testScaffoldingFilePath", tests.get(1).getAbsolutePath());
        }
        request.put("data", patchInfo);

        return OrchestratorClient.getInstance().sendRequest(request, new TypeReference<PatchValidationResult>() {});
    }

    // TODO: Cache execution/kill results as this may be executed more frequently
    @Override
    public boolean isCovered(ExecutionResult result) {
        return getPatchValidationResult(result.test, result).isKilled;
    }

    @Override
    public String getTargetClass() {
        // TODO: Get from patch
        return null;
    }

    @Override
    public String getTargetMethod() {
        // TODO: Get from patch
        return null;
    }

    @Override // TODO: Implement
    public String toString() {
        return super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PatchCoverageTestFitness that = (PatchCoverageTestFitness) o;

        return targetPatch.equals(that.targetPatch);
    }

    @Override
    public int hashCode() {
        return targetPatch.hashCode();
    }

    public static class PatchValidationResult {
        private int patchId;
        private boolean isKilled; // TODO: if possible, change to fitness

        public PatchValidationResult() {}
        public PatchValidationResult(int patchId, boolean isKilled) {
            this.patchId = patchId;
            this.isKilled = isKilled;
        }

        public void setPatchId(int patchId) {
            this.patchId = patchId;
        }

        public void setKilled(boolean isKilled) {
            this.isKilled = isKilled;
        }
    }


}
