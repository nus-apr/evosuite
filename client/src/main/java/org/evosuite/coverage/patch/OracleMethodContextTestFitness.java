package org.evosuite.coverage.patch;

import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.ibranch.IBranchTestFitness;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.setup.CallContext;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;

public class OracleMethodContextTestFitness extends IBranchTestFitness {

    private static final long serialVersionUID = 2910701883821162539L;

    public OracleMethodContextTestFitness(BranchCoverageGoal branch, CallContext context, LineCoverageTestFitness violationTarget) {
        super(branch, context);
        if (branch.getBranch() != null) {
            throw new IllegalArgumentException("OracleMethodContext goal can only be applied to root branches.");
        }
    }

    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        double fitness = 1.0;
        double contextFitness = super.getFitness(individual, result);


        // Distance to covering the fault is line distance to thrown exception (if an exception should be thrown!
        return fitness;

    }
}
