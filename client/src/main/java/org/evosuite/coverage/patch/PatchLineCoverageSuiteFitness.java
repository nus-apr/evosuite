package org.evosuite.coverage.patch;

import org.evosuite.coverage.line.LineCoverageSuiteFitness;

public class PatchLineCoverageSuiteFitness extends LineCoverageSuiteFitness {

    public PatchLineCoverageSuiteFitness() {
        super(new PatchLineCoverageFactory().getCoverageGoals());
    }
}
