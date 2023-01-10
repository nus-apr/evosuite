package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class Goals {
    private List<Patch> patches;
    private List<FixLocation> fixLocations;

    public Goals(){}
    public Goals(List<Patch> patches, List<FixLocation> fixLocations) {
        this.patches = patches;
        this.fixLocations = fixLocations;
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
