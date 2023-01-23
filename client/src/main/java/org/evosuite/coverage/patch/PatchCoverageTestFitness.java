package org.evosuite.coverage.patch;

import org.evosuite.Properties;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

import java.util.*;

public class PatchCoverageTestFitness extends TestFitnessFunction {
    // TODO EvoRepair: We probably only need the number of killed patches
    // Note: If we never clear this map we can keep track of all killed patches (incl. those not part of the pool anymore)
    private static final Map<Integer, Set<String>> killMatrix = new LinkedHashMap<>();
    private static int minKills = -1;
    private static int maxKills = -1;
    private final Patch targetPatch;
    private final Set<TestFitnessFunction> fixLocationGoals = new LinkedHashSet<>();

    public PatchCoverageTestFitness(Patch targetPatch) {
        this.targetPatch = Objects.requireNonNull(targetPatch, "targetPatch cannot be null");
        this.fixLocationGoals.addAll(new PatchLineCoverageFactory().getCoverageGoals(targetPatch.getFixLocations()));
    }

    public Patch getTargetPatch() {
        return targetPatch;
    }

    // TODO: Optimize
    public static void setKillMatrix(Map<Integer, Set<String>> newKillMatrix) {
        killMatrix.clear();
        for (Integer test: newKillMatrix.keySet()) {
            killMatrix.put(test, newKillMatrix.get(test));

            int numKills = newKillMatrix.get(test).size();
            if (minKills == -1 || numKills < minKills) {
                minKills = numKills;
            }
            if (maxKills == -1 || numKills > maxKills) {
                maxKills = numKills;
            }
        }
    }

    public static double getNormalizedKillScore(int testId, double lower, double upper) {
        if (!killMatrix.containsKey("testId")) {
            return upper;
        }
        double killScore = killMatrix.get(testId).size();

        // First normalize to [0,1]
        if (maxKills != minKills) {
            killScore = (killScore - minKills) / (maxKills-minKills);
        } else {
            killScore = 1.0;
        }

        // Now scale to [lower, upper]
        double range = upper - lower;
        killScore = (killScore * range) + lower;
        return killScore;
    }

    public static Map<Integer, Set<String>> getKillMatrix() {
        return killMatrix;
    }

    public static void clearKillMatrix() {
        killMatrix.clear();
    }

    private double getNormalizedFixLocationFitness(TestChromosome tc, double lower, double upper) {
        int numFixLocations = fixLocationGoals.size();

        // Normalize overall fitness between [0, 1]
        double fixLocationFitness = normalize(fixLocationGoals.stream()
                .mapToDouble(goal -> goal.getFitness(tc))
                .sum());

        // Then scale to [lower, upper]
        double range = upper - lower;
        fixLocationFitness = (fixLocationFitness * range) + lower;

        return fixLocationFitness;

    }

    /*
     * Reaching fix locations = 0.5
     * Number of killed patches in the past = 0.4
     */
    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        //boolean isCovered = isCovered(individual.getTestCase());
        //double fitness = isCovered ? 0.0 : 1.0;
        double fitness = 1.0;
        // Normalize kill score between 0 and 0.4, set fitness between 1 and 0.6
        fitness = fitness - PatchCoverageTestFitness.getNormalizedKillScore(individual.getTestCase().getID(), 0, 0.4);

        // Normalized fix location fitness between 0 and 0.5
        fitness = fitness - getNormalizedFixLocationFitness(individual, 0, 0.5);

        // Penalize tests with timeouts or exceptions
        if (result.hasTimeout() || result.hasTestException()) {
            fitness = 1.0;
        }

        updateIndividual(individual, fitness);

        if (fitness == 0.0) {
            individual.getTestCase().addCoveredGoal(this);
        }

        if (Properties.TEST_ARCHIVE) {
            Archive.getArchiveInstance().updateArchive(this, individual, fitness);
        }

        return fitness;
    }

    // TODO: Add class/method info to patch
    @Override
    public int compareTo(TestFitnessFunction other) {
        if (other instanceof PatchCoverageTestFitness) {
            return targetPatch.getIndex().compareTo(((PatchCoverageTestFitness) other).targetPatch.getIndex());
        }
        return 0;
    }

    // TODO: Cache execution/kill results as this may be executed more frequently
    @Override
    public boolean isCovered(TestChromosome individual, ExecutionResult result) {
        String testName = "test" + individual.getTestCase().getID(); // TODO: Optimize
        String patchIndex = targetPatch.getIndex();
        boolean covered = false;

        if (PatchCoverageTestFitness.killMatrix.containsKey(testName)) {
            if (PatchCoverageTestFitness.killMatrix.get(testName).contains(patchIndex)) {
                covered = true;
            }
        }

        if (covered) {
            individual.getTestCase().addCoveredGoal(this);
        }
        return covered;
    }

    @Override
    public String getTargetClass() {
        // TODO: Get from patch
        return null;
    }

    @Override
    public String getTargetMethod() {
        // TODO: Get from patch
        return null;
    }

    @Override // TODO: Implement
    public String toString() {
        return super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PatchCoverageTestFitness that = (PatchCoverageTestFitness) o;

        return targetPatch.equals(that.targetPatch);
    }

    @Override
    public int hashCode() {
        return targetPatch.hashCode();
    }
}
