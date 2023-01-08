package org.evosuite.coverage.patch.communication.json;

public class PatchValidationResult {
    private String testId;
    private int patchId;
    private boolean result; // TODO: if possible, change to fitness

    public PatchValidationResult() {}
    public PatchValidationResult(String testId, int patchId, boolean result) {
        this.testId = testId;
        this.patchId = patchId;
        this.result = result;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestId() {
        return testId;
    }

    public void setPatchId(int patchId) {
        this.patchId = patchId;
    }

    public int getPatchId() {
        return patchId;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public boolean getResult() {
        return result;
    }
}