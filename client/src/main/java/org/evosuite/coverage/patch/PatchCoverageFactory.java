package org.evosuite.coverage.patch;

import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PatchCoverageFactory extends AbstractFitnessFactory<PatchCoverageTestFitness> {
    private static final Logger logger = LoggerFactory.getLogger(PatchCoverageFactory.class);

    public PatchCoverageFactory() {
        /*
        if (Properties.EVOREPAIR_SEED_KILL_MATRIX != null) {
            LoggingUtils.getEvoLogger().info("[EvoRepair] Loading seed kill matrix from file.");
            File killMatrixFile = new File(Properties.EVOREPAIR_SEED_KILL_MATRIX);
            PatchCoverageTestFitness.clearKillMatrix();
            PatchCoverageTestFitness.loadKillMatrix(killMatrixFile);
        }

         */
    }

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

    public List<PatchCoverageTestFitness> getCoverageGoals(List<Patch> patches) {
        List<PatchCoverageTestFitness> goals = new ArrayList<>();

        for (Patch p : patches) {
            goals.add(new PatchCoverageTestFitness(p));
        }

        return goals;
    }
}
