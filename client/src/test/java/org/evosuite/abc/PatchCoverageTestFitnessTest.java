package org.evosuite.abc;

import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.coverage.patch.PatchCoverageTestFitness;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.execution.ExecutionResult;
import org.junit.Test;

public class PatchCoverageTestFitnessTest {

    // TODO: Implement
    @Test
    public void testPatchValidation() {
        Patch targetPatch = new Patch(0);
        PatchCoverageTestFitness testFitness = new PatchCoverageTestFitness(targetPatch);

        TestCase tc = new DefaultTestCase();
        ExecutionResult executionResult = new ExecutionResult(tc);
        testFitness.isCovered(executionResult);
    }
}
