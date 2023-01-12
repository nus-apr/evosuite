package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.coverage.patch.communication.json.PatchValidationResult;
import org.evosuite.coverage.patch.communication.json.SinglePatchValidationResult;
import org.evosuite.ga.archive.Archive;
import org.evosuite.runtime.util.Inputs;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.utils.DebuggingObjectOutputStream;

import java.io.*;
import java.util.*;

public class PatchCoverageTestFitness extends TestFitnessFunction {
    // Note: If we never clear this map we can keep track of all killed patches (incl. those not part of the pool anymore)
    private static final Map<String, Set<String>> killMatrix = new LinkedHashMap<>();
    private final Patch targetPatch;

    public PatchCoverageTestFitness(Patch targetPatch) {
        this.targetPatch = Objects.requireNonNull(targetPatch, "targetPatch cannot be null");
    }

    public Patch getTargetPatch() {
        return targetPatch;
    }

    public static boolean updateKillMatrix(List<PatchValidationResult> results) {
        boolean updated = false;
        for (PatchValidationResult result : results) {
            String testName = result.getTestName();
            if (!result.getKilledPatches().isEmpty()) {
                updated = true;
            } else {
                continue;
            }

            if (!killMatrix.containsKey(testName)) {
                killMatrix.put(testName, new LinkedHashSet<>(result.getKilledPatches()));
            } else {
                killMatrix.get(testName).addAll(result.getKilledPatches());
            }
        }
        return updated;
    }

    public static Map<String, Set<String>> getKillMatrix() {
        return killMatrix;
    }

    public static void clearKillMatrix() {
        killMatrix.clear();
    }

    public static boolean saveKillMatrix(File target) {
        File parent = target.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (ObjectOutputStream out = new DebuggingObjectOutputStream(new FileOutputStream(target))) {
            out.writeObject(killMatrix);

            out.flush();
            out.close();
        } catch (IOException e) {
            logger.error("Failed to open/handle " + target.getAbsolutePath() + " for writing: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static void loadKillMatrix(String target) throws IllegalArgumentException {
        loadKillMatrix(new File(target));
    }

    public static void loadKillMatrix(File target) throws IllegalArgumentException {
        Inputs.checkNull(target);

        if (!killMatrix.keySet().isEmpty()) {
            throw new RuntimeException("Trying to reload an already populated kill matrix.");
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(target))) {

            try {
                Object obj = in.readObject();
                if (obj != null) {
                    if (obj instanceof LinkedHashMap) {
                        //this check might fail if old version is used, and EvoSuite got updated
                        Map<String, Set<String>> loadedKillMap = (Map) obj;
                        for (String s : loadedKillMap.keySet()) {
                            killMatrix.put(s, loadedKillMap.get(s));
                        }
                    }
                } else {
                    throw new RuntimeException("Unable to load kill matrix.");
                }
            } catch (EOFException e) {
                //fine
            } catch (Exception e) {
                logger.warn("Problems when reading a serialized kill matrix from " + target.getAbsolutePath() + " : " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            logger.warn("Cannot load tests because file does not exist: " + target.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to open/handle " + target.getAbsolutePath() + " for reading: " + e.getMessage());
        }
    }


    // TODO: Implement fitness as covering patch locations + killing it
    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        boolean isCovered = isCovered(individual.getTestCase());
        double fitness = isCovered ? 0.0 : 1.0;

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
