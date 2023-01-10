package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class PatchValidationSummary {
    private List<PatchValidationResult> results;
    private List<Patch> patches;
    private List<FixLocation> fixLocations;
    public PatchValidationSummary() {}
    public PatchValidationSummary(List<PatchValidationResult> results, List<Patch> patches, List<FixLocation> fixLocations) {
        this.results = results;
        this.patches = patches;
        this.fixLocations = fixLocations;
    }

    public List<PatchValidationResult> getResults() {
        return this.results;
    }
    public void setResults(List<PatchValidationResult> results) {
        this.results = results;
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
