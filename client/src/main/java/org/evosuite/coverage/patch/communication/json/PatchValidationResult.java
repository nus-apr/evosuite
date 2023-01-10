package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class PatchValidationResult {
    private String testName;
    private List<Integer> killedPatches;

    public PatchValidationResult() {};
    public PatchValidationResult(String testName, List<Integer> killedPatches) {
        this.testName = testName;
        this.killedPatches = killedPatches;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }
    public String getTestName() {
        return this.testName;
    }

    public void setKilledPatches(List<Integer> killedPatches) {
        this.killedPatches = killedPatches;
    }

    public List<Integer> getKilledPatches() {
        return this.killedPatches;
    }

}
