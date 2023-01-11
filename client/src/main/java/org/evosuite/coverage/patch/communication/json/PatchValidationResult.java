package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class PatchValidationResult {
    private String testName;
    private List<String> killedPatches;

    public PatchValidationResult() {};
    public PatchValidationResult(String testName, List<String> killedPatches) {
        this.testName = testName;
        this.killedPatches = killedPatches;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }
    public String getTestName() {
        return this.testName;
    }

    public void setKilledPatches(List<String> killedPatches) {
        this.killedPatches = killedPatches;
    }

    public List<String> getKilledPatches() {
        return this.killedPatches;
    }

}
