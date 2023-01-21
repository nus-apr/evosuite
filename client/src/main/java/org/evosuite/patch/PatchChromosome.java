package org.evosuite.patch;

import com.google.gson.Gson;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.localsearch.LocalSearchObjective;

import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.msu.cse.repair.core.AbstractRepairProblem;
import us.msu.cse.repair.core.filterrules.MIFilterRule;
import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.core.testexecutors.ExternalTestExecutor;
import us.msu.cse.repair.core.testexecutors.ITestExecutor;
import us.msu.cse.repair.core.util.IO;
import us.msu.cse.repair.ec.problems.ArjaProblem;
import us.msu.cse.repair.ec.representation.ArjaSolutionSummary;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PatchChromosome extends Chromosome<PatchChromosome> {

    private static final Logger logger = LoggerFactory.getLogger(PatchChromosome.class);

    BitSet bits;

    int[] array;

    ArjaProblem problem;

    int[] numberOfAvailableManipulations;
    int[] numberOfIngredients;

    private Boolean isUndesirable = null;
    private Double weightedFailureRate = null;
    private Double numberOfEdits = null;
    private List<Integer> locations = null;
    private Map<String, String> modifiedJavaSources = null;

    public PatchChromosome(BitSet bits, int[] array, ArjaProblem problem,
                           int[] numberOfAvailableManipulations, int[] numberOfIngredients) {
        List<ModificationPoint> modificationPoints = problem.getModificationPoints();

        int size = modificationPoints.size();

        if (array.length != 2 * size) {
            throw new IllegalArgumentException();
        }
        if (numberOfAvailableManipulations.length != size) {
            throw new IllegalArgumentException();
        }
        if (numberOfIngredients.length != size) {
            throw new IllegalArgumentException();
        }

        this.bits = bits;
        this.array = array;
        this.problem = problem;
        this.numberOfAvailableManipulations = numberOfAvailableManipulations;
        this.numberOfIngredients = numberOfIngredients;
    }

    public static int[] getNumberOfAvailableManipulations(AbstractRepairProblem problem) {
        int size = problem.getModificationPoints().size();

        int[] numberOfAvailableManipulations = new int[size];

        List<List<String>> availableManipulations = problem.getAvailableManipulations();

        for (int i = 0; i < size; i++) {
            numberOfAvailableManipulations[i] = availableManipulations.get(i).size();
        }

        return numberOfAvailableManipulations;
    }

    public static int[] getNumberOfIngredients(AbstractRepairProblem problem) {
        List<ModificationPoint> modificationPoints = problem.getModificationPoints();

        int size = modificationPoints.size();

        int[] numberOfIngredients = new int[size];

        for (int i = 0; i < size; i++) {
            numberOfIngredients[i] = modificationPoints.get(i).getIngredients().size();
        }

        return numberOfIngredients;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Create a deep copy of the chromosome
     */
    @Override
    public PatchChromosome clone() {
        return new PatchChromosome((BitSet) bits.clone(), Arrays.copyOf(array, array.length), problem,
                                   numberOfAvailableManipulations, numberOfIngredients);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatchChromosome that = (PatchChromosome) o;
        return bits.equals(that.bits) && Arrays.equals(array, that.array) && problem == that.problem;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(bits);
        result = 31 * result + Arrays.hashCode(array);
        return result;
    }

    /**
     * Secondary Objectives are specific to chromosome types
     *
     * @param o a {@link Chromosome} object.
     * @return a int.
     */
    @Override
    public int compareSecondaryObjective(PatchChromosome o) {
        return 0;
    }

    /**
     * Apply mutation
     */
    @Override
    public void mutate() {
        // BitFlipUniformMutation of Arja
        int size = problem.getNumberOfModificationPoints();
        double probability = 1.0 / size;

        for (int i = 0; i < size; i++) {
            if (Randomness.nextDouble() < probability) {
                bits.flip(i);
            }
        }

        for (int i = 0; i < size; i++) {
            if (Randomness.nextDouble() < probability) {
                array[i] = Randomness.nextInt(0, numberOfAvailableManipulations[i]);
            }
        }

        for (int i = 0; i < size; i++) {
            if (Randomness.nextDouble() < probability) {
                array[size + i] = Randomness.nextInt(0, numberOfIngredients[i]);
            }
        }
    }

    /**
     * Single point cross over
     *
     * @param other     a {@link Chromosome} object.
     * @param position1 a int.
     * @param position2 a int.
     * @throws ConstructionFailedException if any.
     */
    @Override
    public void crossOver(PatchChromosome other, int position1, int position2) throws ConstructionFailedException {
        throw new UnsupportedOperationException("Single point crossover is undefined for patch chromosomes");
    }

    /**
     * Apply the local search
     *
     * @param objective a {@link LocalSearchObjective}
     *                  object.
     */
    @Override
    public boolean localSearch(LocalSearchObjective<PatchChromosome> objective) {
        throw new UnsupportedOperationException("Local search not supported for patch chromosomes");
    }

    /**
     * Return length of individual
     *
     * @return a int.
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * <p>
     * Returns the runtime type of the implementor (a.k.a. "self-type"). This method must only be
     * implemented in concrete, non-abstract subclasses by returning a reference to {@code this},
     * and nothing else. Returning a reference to any other runtime type other than {@code this}
     * breaks the contract.
     * </p>
     * <p>
     * In other words, every concrete subclass {@code Foo} that implements the interface {@code
     * SelfTyped} must implement this method as follows:
     * <pre>{@code
     * public class Foo implements SelfTyped<Foo> {
     *     @Override
     *     public Foo self() {
     *         return this;
     *     }
     * }
     * }</pre>
     * </p>
     *
     * @return a reference to the self-type
     */
    @Override
    public PatchChromosome self() {
        return this;
    }

    private void precomputeFitnesses() throws Exception {
        isUndesirable = false;

        int size = problem.getNumberOfModificationPoints();
        Map<String, ASTRewrite> astRewriters = new LinkedHashMap<>();

        Map<Integer, Double> selectedMP = new LinkedHashMap<>();

        List<ModificationPoint> modificationPoints = problem.getModificationPoints();
        List<List<String>> availableManipulations = problem.getAvailableManipulations();
        boolean miFilterRule = problem.getMiFilterRule();

        for (int i = 0; i < size; i++) {
            if (bits.get(i)) {
                double suspValue = modificationPoints.get(i).getSuspValue();
                if (miFilterRule) {
                    String manipName = availableManipulations.get(i).get(array[i]);
                    ModificationPoint mp = modificationPoints.get(i);

                    Statement seed = null;
                    if (!mp.getIngredients().isEmpty()) {
                        seed = mp.getIngredients().get(array[i + size]);
                    }
                    int index = MIFilterRule.canFiltered(manipName, seed, modificationPoints.get(i));
                    if (index == -1) {
                        selectedMP.put(i, suspValue);
                    } else if (index < mp.getIngredients().size()) {
                        array[i + size] = index;
                        selectedMP.put(i, suspValue);
                    } else {
                        bits.set(i, false);
                    }
                } else {
                    selectedMP.put(i, suspValue);
                }
            }
        }

        if (selectedMP.isEmpty()) {
            isUndesirable = true;
            return;
        }

        int numberOfEdits = selectedMP.size();
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(selectedMP.entrySet());

        Integer maxNumberOfEdits = problem.getMaxNumberOfEdits();
        if (maxNumberOfEdits != null && selectedMP.size() > maxNumberOfEdits) {
            list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

            numberOfEdits = maxNumberOfEdits;
        }

        for (int i = 0; i < numberOfEdits; i++) {
            problem.manipulateOneModificationPoint(list.get(i).getKey(), size, array, astRewriters);
        }

        for (int i = numberOfEdits; i < selectedMP.size(); i++) {
            bits.set(list.get(i).getKey(), false);
        }

        locations = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry: list) {
            locations.add(entry.getKey());
        }

        modifiedJavaSources = problem.getModifiedJavaSources(astRewriters);
        Map<String, JavaFileObject> compiledClasses = problem.getCompiledClassesForTestExecution(modifiedJavaSources);

        boolean status = false;
        if (compiledClasses != null) {
            this.numberOfEdits = (double) numberOfEdits;

            ITestExecutor testExecutor = problem.getTestExecutor(compiledClasses, problem.getSamplePositiveTests());

            if (problem.useDefects4JInstrumentation) {
                ((ExternalTestExecutor) testExecutor).enableDefects4jInstrumentation();
            }

            status = testExecutor.runTests();

            Double percentage = problem.getPercentage();
            if (status && percentage != null && percentage < 1) {
                testExecutor = problem.getTestExecutor(compiledClasses, problem.getPositiveTests());

                if (problem.useDefects4JInstrumentation) {
                    ((ExternalTestExecutor) testExecutor).enableDefects4jInstrumentation();
                }

                status = testExecutor.runTests();

            }

            if (!testExecutor.isIOExceptional() && !testExecutor.isTimeout()) {
                weightedFailureRate = problem.getWeight() * testExecutor.getRatioOfFailuresInPositive()
                                      + testExecutor.getRatioOfFailuresInNegative();

                Set<String> failures = testExecutor.getFailedTests().keySet();
                if (failures.isEmpty()) {
                    save();
                } else if (failures.stream().noneMatch(problem::isUserTest)) {
                    ArjaSolutionSummary summary = new ArjaSolutionSummary(bits, array, problem);
                    problem.appendToHallOfFameOut(summary, ArjaProblem.getGlobalID());
                    if (problem.getFameOutputRoot() != null) {
                        saveAsFame();
                    }
                }
            } else {
                isUndesirable = true;
            }
        } else {
            isUndesirable = true;
        }

        if (status) {
            logger.info("found a plausible patch");
            save();
        }
    }

    public double getSizePatchFitness() {
        if (isUndesirable == null) {
            try {
                precomputeFitnesses();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return isUndesirable ? Double.MAX_VALUE : numberOfEdits;
    }

    public double getWeightedFailureRateFitness() {
        if (isUndesirable == null) {
            try {
                precomputeFitnesses();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return isUndesirable ? Double.MAX_VALUE : weightedFailureRate;
    }

    public void save() {
        List<Integer> opList = new ArrayList<>();
        List<Integer> locList = new ArrayList<>();
        List<Integer> ingredList = new ArrayList<>();

        int size = array.length / 2;

        for (int i = 0; i < numberOfEdits; i++) {
            int loc = locations.get(i);
            int op = array[loc];
            int ingred = array[loc + size];
            opList.add(op);
            locList.add(loc);
            ingredList.add(ingred);
        }

        ArjaSolutionSummary summary = new ArjaSolutionSummary(bits, array, problem);

        try {
            if (problem.addTestAdequatePatch(opList, locList, ingredList)) {
                if (problem.getDiffFormat()) {
                    try {
                        String patchOutputRoot = problem.getPatchOutputRoot();
                        int globalId = AbstractRepairProblem.getGlobalID();

                        IO.savePatch(modifiedJavaSources, problem.getSrcJavaDir(),
                                     patchOutputRoot, globalId);

                        PrintStream ps = new PrintStream(
                                Files.newOutputStream(Paths.get(patchOutputRoot, "Patch_" + globalId, "summary")));
                        ps.println(new Gson().toJson(summary));
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                problem.saveTestAdequatePatch(opList, locList, ingredList);
                AbstractRepairProblem.increaseGlobalID();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void saveAsFame() {
        List<Integer> opList = new ArrayList<>();
        List<Integer> locList = new ArrayList<>();
        List<Integer> ingredList = new ArrayList<>();

        int size = array.length / 2;

        for (int i = 0; i < numberOfEdits; i++) {
            int loc = locations.get(i);
            int op = array[loc];
            int ingred = array[loc + size];
            opList.add(op);
            locList.add(loc);
            ingredList.add(ingred);
        }

        ArjaSolutionSummary summary = new ArjaSolutionSummary(bits, array, problem);

        try {
            if (problem.addFamePatch(opList, locList, ingredList)) {
                if (problem.getDiffFormat()) {
                    try {
                        String fameOutputRoot = problem.getFameOutputRoot();
                        int globalId = AbstractRepairProblem.getGlobalID();

                        IO.savePatch(modifiedJavaSources, problem.getSrcJavaDir(),
                                     fameOutputRoot, globalId);

                        PrintStream ps = new PrintStream(
                                Files.newOutputStream(Paths.get(fameOutputRoot, "Patch_" + globalId, "summary")));
                        ps.println(new Gson().toJson(summary));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                problem.saveFamePatch(opList, locList, ingredList);
                AbstractRepairProblem.increaseGlobalID();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
