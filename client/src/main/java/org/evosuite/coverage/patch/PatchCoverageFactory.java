package org.evosuite.coverage.patch;

import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PatchCoverageFactory extends AbstractFitnessFactory<PatchCoverageTestFitness> {
    private static final Logger logger = LoggerFactory.getLogger(PatchCoverageFactory.class);

    @Override
    public List<PatchCoverageTestFitness> getCoverageGoals() {
        List<PatchCoverageTestFitness> goals = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (Patch p : PatchPool.getInstance().getPatchPool()) {
            goals.add(new PatchCoverageTestFitness(p));
        }

        goalComputationTime = System.currentTimeMillis() - start;
        return goals;
    }
}
