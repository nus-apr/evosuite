package org.evosuite.coverage.patch.communication.json;

import java.util.List;

/**
 * (Unused, kept for reference only)
 * Type representation of the patch pool to receive from the orchestrator.
 */

public class PatchPoolType {
    private List<Patch> patches;

    public PatchPoolType() {}
    public PatchPoolType(List<Patch> patches) {this.patches = patches;}
    public void setPatches(List<Patch> patches) {this.patches = patches;}
    public List<Patch> getPatches() {return patches;}

}