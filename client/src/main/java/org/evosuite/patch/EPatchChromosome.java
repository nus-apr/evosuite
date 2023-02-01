package org.evosuite.patch;

import com.google.gson.Gson;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.localsearch.LocalSearchObjective;

import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.ExtendedOptionsHelper;
import us.msu.cse.repair.algorithms.arja.Arja;
import us.msu.cse.repair.core.AbstractRepairProblem;
import us.msu.cse.repair.core.filterrules.MIFilterRule;
import us.msu.cse.repair.core.parser.ExtendedModificationPoint;
import us.msu.cse.repair.core.parser.ModificationPoint;
import us.msu.cse.repair.core.testexecutors.ExternalTestExecutor;
import us.msu.cse.repair.core.testexecutors.ITestExecutor;
import us.msu.cse.repair.core.util.IO;
import us.msu.cse.repair.ec.problems.ArjaEProblem;
import us.msu.cse.repair.ec.representation.ArjaSolutionSummary;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class EPatchChromosome extends Chromosome<EPatchChromosome>  {

    private static final Logger logger = LoggerFactory.getLogger(EPatchChromosome.class);

    BitSet bits;

    int[] array;

    ArjaEProblem problem;

    int[] numberOfAvailableManipulations;
    int[] numberOfReplaceIngredients;
    int[] numberOfInsertIngredients;
    final private double[] ratios;

    private double mutationProbability_;

    private Boolean isUndesirable = null;
    private Double weightedFailureRate = null;
    private Double numberOfEdits = null;
    private List<Integer> locations = null;
    private Map<String, String> modifiedJavaSources = null;
    private Set<String> failedTests = null;

    public EPatchChromosome(BitSet bits, int[] array, ArjaEProblem problem,
                            int[] numberOfAvailableManipulations, int[] numberOfReplaceIngredients,
                            int[] numberOfInsertIngredients, double mutationProbability) {
        List<ModificationPoint> modificationPoints = problem.getModificationPoints();

        int size = modificationPoints.size();

        if (array.length != 3 * size) {
            throw new IllegalArgumentException();
        }
        if (numberOfAvailableManipulations.length != size) {
            throw new IllegalArgumentException();
        }
        if (numberOfReplaceIngredients.length != size) {
            throw new IllegalArgumentException();
        }
        if (numberOfInsertIngredients.length != size) {
            throw new IllegalArgumentException();
        }

        this.bits = bits;
        this.array = array;
        this.problem = problem;
        this.numberOfAvailableManipulations = numberOfAvailableManipulations;
        this.numberOfReplaceIngredients = numberOfReplaceIngredients;
        this.numberOfInsertIngredients = numberOfInsertIngredients;

        ratios = new double[size];
        double sum = 0;
        for (int i = 0; i < size; i++) {
            double susp = modificationPoints.get(i).getSuspValue();
            ratios[i] = susp + (i > 0 ? ratios[i - 1] : 0);
            sum += susp;
        }

        for (int i = 0; i < size; i++) {
            ratios[i] /= sum;
        }

        mutationProbability_ = mutationProbability;
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

    public static int[] getNumberOfReplaceIngredients(ArjaEProblem problem) {
        List<ExtendedModificationPoint> modificationPoints = problem.getExtendedModificationPoints();

        int size = modificationPoints.size();

        int[] numberOfIngredients = new int[size];

        for (int i = 0; i < size; i++) {
            numberOfIngredients[i] = modificationPoints.get(i).getReplaceIngredients().size();
        }

        return numberOfIngredients;
    }

    public static int[] getNumberOfInsertIngredients(ArjaEProblem problem) {
        List<ExtendedModificationPoint> modificationPoints = problem.getExtendedModificationPoints();

        int size = modificationPoints.size();

        int[] numberOfIngredients = new int[size];

        for (int i = 0; i < size; i++) {
            numberOfIngredients[i] = modificationPoints.get(i).getInsertIngredients().size();
        }

        return numberOfIngredients;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Create a deep copy of the chromosome
     */
    @Override
    public EPatchChromosome clone() {
        return new EPatchChromosome((BitSet) bits.clone(), Arrays.copyOf(array, array.length), problem,
                numberOfAvailableManipulations, numberOfReplaceIngredients, numberOfInsertIngredients,
                mutationProbability_);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EPatchChromosome that = (EPatchChromosome) o;
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
    public int compareSecondaryObjective(EPatchChromosome o) {
        return 0;
    }

    /**
     * Apply mutation
     */
    @Override
    public void mutate() {
        int k = getMutatingLocation();

        int size = problem.getNumberOfModificationPoints();

        if (Randomness.nextDouble() < mutationProbability_) {
            bits.flip(k);

            array[k] = Randomness.nextInt(0, numberOfAvailableManipulations[k]);
            array[k + size] = Randomness.nextInt(0, Math.max(numberOfReplaceIngredients[k], 1));
            array[k + size * 2] = Randomness.nextInt(0, Math.max(numberOfInsertIngredients[k], 1));
        } else {
            int u = Randomness.nextInt(0, 3);
            switch (u) {
                case 0:
                    bits.flip(k);
                    break;
                case 1:
                    array[k] = Randomness.nextInt(0, Math.max(numberOfAvailableManipulations[k], 1));
                    break;
                case 2:
                    if (array[k] == 1) {
                        array[k + size * 2] = Randomness.nextInt(0, Math.max(numberOfInsertIngredients[k], 1));
                    } else {
                        array[k + size] = Randomness.nextInt(0, Math.max(numberOfReplaceIngredients[k], 1));
                    }
                    break;
                default:
                    throw new RuntimeException(String.format("unexpected random number: %d", u));
            }
        }
    }

    int getMutatingLocation() {
        double rnd = Randomness.nextDouble();

        for (int i = 0; i < ratios.length; i++) {
            if (rnd < ratios[i])
                return i;
        }

        return 0;
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
    public void crossOver(EPatchChromosome other, int position1, int position2) throws ConstructionFailedException {
        throw new UnsupportedOperationException("Single point crossover is undefined for patch chromosomes");
    }

    /**
     * Apply the local search
     *
     * @param objective a {@link LocalSearchObjective}
     *                  object.
     */
    @Override
    public boolean localSearch(LocalSearchObjective<EPatchChromosome> objective) {
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
    public EPatchChromosome self() {
        return this;
    }

    private void precomputeFitnesses() throws Exception {
        isUndesirable = false;

        int size = problem.getNumberOfModificationPoints();
        Map<String, ASTRewrite> astRewriters = new LinkedHashMap<>();

        Map<Integer, Double> selectedMP = new LinkedHashMap<>();

        List<ModificationPoint> modificationPoints = problem.getModificationPoints();
        List<List<String>> availableManipulations = problem.getAvailableManipulations();

        for (int i = 0; i < size; i++) {
            if (bits.get(i)) {
                double suspValue = modificationPoints.get(i).getSuspValue();
                selectedMP.put(i, suspValue);
            }
        }

        if (selectedMP.isEmpty()) {
            isUndesirable = true;
            return;
        }

        int numberOfEdits = selectedMP.size();
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(selectedMP.entrySet());

        for (int i = 0; i < numberOfEdits; i++) {
            problem.manipulateOneModificationPoint(list.get(i).getKey(), size, array, astRewriters);
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

//            if (problem.useDefects4JInstrumentation) {
//                ((ExternalTestExecutor) testExecutor).enableDefects4jInstrumentation();
//            }

            status = testExecutor.runTests();

            Double percentage = problem.getPercentage();
            if (status && percentage != null && percentage < 1) {
                testExecutor = problem.getTestExecutor(compiledClasses, problem.getPositiveTests());

//                if (problem.useDefects4JInstrumentation) {
//                    ((ExternalTestExecutor) testExecutor).enableDefects4jInstrumentation();
//                }

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
                    problem.appendToHallOfFameOut(summary, ArjaEProblem.getGlobalID());
                    if (problem.getFameOutputRoot() != null) {
                        failedTests = failures;
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

        int size = array.length / 3;

        List<List<String>> availableManipulations = problem.getAvailableManipulations();

        for (int i = 0; i < numberOfEdits; i++) {
            int loc = locations.get(i);
            int op = array[loc];

            String manipName = availableManipulations.get(loc).get(op);

            int ingred;
            if (manipName.equalsIgnoreCase("Replace"))
                ingred = array[loc + size];
            else
                ingred = array[loc +  2 * size];

            opList.add(op);
            locList.add(loc);
            ingredList.add(ingred);
        }

        ArjaSolutionSummary summary = new ArjaSolutionSummary(bits, array, problem);

        try {
            if (problem.addTestAdequatePatch(opList, locList, ingredList)) {
//                if (problem.getDiffFormat()) {
//                    try {
//                        String patchOutputRoot = problem.getPatchOutputRoot();
//                        int globalId = AbstractRepairProblem.getGlobalID();
//
//                        IO.savePatch(modifiedJavaSources, problem.getSrcJavaDir(),
//                                patchOutputRoot, globalId);
//
//                        PrintStream ps = new PrintStream(
//                                Files.newOutputStream(Paths.get(patchOutputRoot, "Patch_" + globalId, "summary")));
//                        ps.println(new Gson().toJson(summary));
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
                problem.saveTestAdequatePatch(opList, locList, ingredList, modifiedJavaSources, new Gson().toJson(summary));
//                AbstractRepairProblem.increaseGlobalID();
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

        int size = array.length / 3;

        List<List<String>> availableManipulations = problem.getAvailableManipulations();

        for (int i = 0; i < numberOfEdits; i++) {
            int loc = locations.get(i);
            int op = array[loc];

            String manipName = availableManipulations.get(loc).get(op);

            int ingred;
            if (manipName.equalsIgnoreCase("Replace"))
                ingred = array[loc + size];
            else
                ingred = array[loc +  2 * size];

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
                                Files.newOutputStream(
                                        Paths.get(fameOutputRoot, "Patch_" + globalId, "failed_tests")));
                        ps.print(String.join("\n", failedTests));

                        ps = new PrintStream(
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
