package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class PatchValidationSummary {
    private List<PatchValidationResult> killMatrix;
    private List<Patch> patches;
    private List<FixLocation> fixLocations;
    public PatchValidationSummary() {}
    public PatchValidationSummary(List<PatchValidationResult> killMatrix, List<Patch> patches, List<FixLocation> fixLocations) {
        this.killMatrix = killMatrix;
        this.patches = patches;
        this.fixLocations = fixLocations;
    }

    public List<PatchValidationResult> getKillMatrix() {
        return this.killMatrix;
    }
    public void setResults(List<PatchValidationResult> killMatrix) {
        this.killMatrix = killMatrix;
    }

    public List<Patch> getPatches() {
        return this.patches;
    }
    public void setPatches(List<Patch> patches) {
        this.patches = patches;
    }

    public List<FixLocation> getFixLocations() {
        return this.fixLocations;
    }
    public void setFixLocations(List<FixLocation> fixLocations) {
        this.fixLocations = fixLocations;
    }


}
