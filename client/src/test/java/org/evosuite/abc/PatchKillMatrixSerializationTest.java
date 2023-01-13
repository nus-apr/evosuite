package org.evosuite.abc;

import org.evosuite.coverage.patch.PatchCoverageTestFitness;
import org.evosuite.coverage.patch.communication.json.PatchValidationResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.*;

public class PatchKillMatrixSerializationTest {
    /**
     * FIXME: Unused, may want to remove entirely
    @Test
    public void testSerialization() {
        Map<String, Set<String>> originalMap = new LinkedHashMap<>();
        originalMap.put("test1", new LinkedHashSet<>(Arrays.asList("patch1")));
        originalMap.put("test2", new LinkedHashSet<>(Arrays.asList("patch2", "patch3")));
        originalMap.put("test3", new LinkedHashSet<>(Arrays.asList("patch1", "patch3")));
        originalMap.put("test4", new LinkedHashSet<>(Arrays.asList("patch1", "patch2", "patch3", "patch4")));

        // Convert map to list of patch validation results ("kill matrix"), save results
        List<PatchValidationResult> results = new ArrayList<>();
        for (String test : originalMap.keySet()) {
            results.add(new PatchValidationResult(test, new ArrayList<>(originalMap.get(test))));
        }
        PatchCoverageTestFitness.updateKillMatrix(results);

        // Serialize
        File killMatrixFile = new File("src/test/resources/org/evosuite/abc/killMatrix.tmp");
        PatchCoverageTestFitness.saveKillMatrix(killMatrixFile);

        // Deserialize
        PatchCoverageTestFitness.clearKillMatrix();
        File loadedKillMatrixFile = new File("src/test/resources/org/evosuite/abc/killMatrix.tmp");
        PatchCoverageTestFitness.loadKillMatrix(loadedKillMatrixFile);

        Assert.assertEquals(originalMap, PatchCoverageTestFitness.getKillMatrix());
        System.out.println(PatchCoverageTestFitness.getKillMatrix().toString());

    }

     */
}
