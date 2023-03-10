package org.evosuite.coverage.patch;

import org.evosuite.coverage.line.LineCoverageSuiteFitness;

public class FixLocationCoverageSuiteFitness extends LineCoverageSuiteFitness {

    private static final long serialVersionUID = -5181376681187629308L;

    public FixLocationCoverageSuiteFitness() {
        super(new FixLocationCoverageFactory().getCoverageGoals());
    }
}
