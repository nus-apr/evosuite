package org.evosuite.coverage.patch;

import org.evosuite.coverage.line.LineCoverageSuiteFitness;

import java.util.ArrayList;

public class OracleExceptionSuiteFitness extends LineCoverageSuiteFitness {
    private static final long serialVersionUID = -7926786427491062259L;

    public OracleExceptionSuiteFitness() {
        super(new ArrayList<>(new OracleExceptionFactory().getCoverageGoals()));
    }
}
