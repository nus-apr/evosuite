package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.Properties;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.coverage.patch.communication.json.PatchValidationResult;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

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

    // TODO: Can potentially optimize request generation using JSONGenerator
    private boolean getPatchValidationResult(TestCase tc) {
        // FIXME: Inconsistent ID naming
        String testId = "test" + tc.getID();
        int patchId = targetPatch.getId();

        // Prepare request for orchestrator
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("cmd", "getPatchValidationResult");
        Map<String, Object> patchValidationData = new LinkedHashMap<>();
        patchValidationData.put("testId", testId);
        patchValidationData.put("patchId", patchId);
        msg.put("data", patchValidationData);
        PatchValidationResult validationResult = OrchestratorClient.getInstance().sendRequest(msg, new TypeReference<PatchValidationResult>() {});

        // TODO: Check that we got the correct information (testId, patchId) back

        // TODO: Empty tests cannot kill any patches
        return validationResult.getResult();
    }

    // TODO: Cache execution/kill results as this may be executed more frequently
    @Override
    public boolean isCovered(TestChromosome individual, ExecutionResult result) {
        boolean covered = getPatchValidationResult(individual.getTestCase());
        if (covered) {
            individual.getTestCase().addCoveredGoal(this);
        }
        return covered;
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
}
