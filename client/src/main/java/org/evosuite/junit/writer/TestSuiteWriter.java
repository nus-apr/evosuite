/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */

package org.evosuite.junit.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.evosuite.Properties;
import org.evosuite.Properties.Criterion;
import org.evosuite.Properties.OutputGranularity;
import org.evosuite.TimeController;
import org.evosuite.coverage.dataflow.DefUseCoverageTestFitness;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.ContextLineTestFitness;
import org.evosuite.coverage.patch.OracleExceptionTestFitness;
import org.evosuite.coverage.patch.communication.json.OracleExceptionFitnessMetrics;
import org.evosuite.coverage.patch.communication.json.TargetLocationFitnessMetrics;
import org.evosuite.ga.archive.Archive;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.junit.UnitTestAdapter;
import org.evosuite.junit.naming.methods.CoverageGoalTestNameGenerationStrategy;
import org.evosuite.junit.naming.methods.IDTestNameGenerationStrategy;
import org.evosuite.junit.naming.methods.NumberedTestNameGenerationStrategy;
import org.evosuite.junit.naming.methods.TestNameGenerationStrategy;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.result.TestGenerationResultBuilder;
import org.evosuite.runtime.*;
import org.evosuite.runtime.testdata.EnvironmentDataList;
import org.evosuite.testcase.*;
import org.evosuite.testcase.execution.CodeUnderTestException;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.ArrayUtil;
import org.evosuite.utils.FileIOUtils;
import org.evosuite.utils.LoggingUtils;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.util.*;

import static org.evosuite.junit.writer.TestSuiteWriterUtils.*;

/**
 * Class used to generate the source code of the JUnit test cases.
 * <p/>
 * <p/>
 * NOTE: a test case should only access to the following packages
 * <ul>
 * <li> Java API
 * <li> Junit
 * <li> org.evosuite.runtime.*
 *
 * @author Gordon Fraser
 */
public class TestSuiteWriter implements Opcodes {

    /**
     * Constant <code>logger</code>
     */
    private final static Logger logger = LoggerFactory.getLogger(TestSuiteWriter.class);

    public static final String NOT_GENERATED_TEST_NAME = "notGeneratedAnyTest";

    protected TestCaseExecutor executor = TestCaseExecutor.getInstance();

    protected List<TestCase> testCases = new ArrayList<>();

    protected Map<Integer, String> testComment = new HashMap<>();

    private final UnitTestAdapter adapter = TestSuiteWriterUtils.getAdapter();

    private final TestCodeVisitor visitor = new TestCodeVisitor();

    private final static String NEWLINE = java.lang.System.getProperty("line.separator");

    private TestNameGenerationStrategy nameGenerator = null;

    /**
     * Add test to suite. If the test is a prefix of an existing test, just keep
     * existing test. If an existing test is a prefix of the test, replace the
     * existing test.
     *
     * @param test a {@link org.evosuite.testcase.TestCase} object.
     * @return Index of the test case
     */
    public int insertTest(TestCase test) {
        if (Properties.CALL_PROBABILITY <= 0) {
            for (int i = 0; i < testCases.size(); i++) {
                if (test.isPrefix(testCases.get(i))) {
                    // It's shorter than an existing one
                    // test_cases.set(i, test);
                    logger.info("This is a prefix of an existing test");
                    testCases.get(i).addAssertions(test);
                    return i;
                } else {
                    // Already have that one...
                    if (testCases.get(i).isPrefix(test)) {
                        test.addAssertions(testCases.get(i));
                        testCases.set(i, test);
                        logger.info("We have a prefix of this one");
                        return i;
                    }
                }
            }
        }
        logger.info("Adding new test case:");
        if (logger.isDebugEnabled()) {
            logger.debug(test.toCode());
        }
        testCases.add(test);
        return testCases.size() - 1;
    }

    /**
     * <p>
     * insertTest
     * </p>
     *
     * @param test    a {@link org.evosuite.testcase.TestCase} object.
     * @param comment a {@link java.lang.String} object.
     * @return a int.
     */
    public int insertTest(TestCase test, String comment) {
        int id = insertTest(test);
        if (testComment.containsKey(id)) {
            if (!testComment.get(id).contains(comment))
                testComment.put(id, testComment.get(id) + NEWLINE + METHOD_SPACE + "//"
                        + comment);
        } else
            testComment.put(id, comment);
        return id;
    }

    /**
     * <p>
     * insertTests
     * </p>
     *
     * @param tests a {@link java.util.List} object.
     */
    public void insertTests(List<TestCase> tests) {
        for (TestCase test : tests)
            insertTest(test);
    }

    /**
     * <p>
     * insertTests
     * </p>
     *
     * @param tests a {@link java.util.List} object.
     */
    public void insertAllTests(List<TestCase> tests) {
        testCases.addAll(tests);
    }

    /**
     * Get all test cases
     *
     * @return a {@link java.util.List} object.
     */
    public List<TestCase> getTestCases() {
        return testCases;
    }

    /**
     * Create JUnit test suite for class
     *
     * @param name      Name of the class
     * @param directory Output directory
     */
    public List<File> writeTestSuite(String name, String directory, List<ExecutionResult> cachedResults) throws IllegalArgumentException {

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Empty test class name");
        }
        if (!name.endsWith("Test")) {
            /*
             * This is VERY important, as otherwise tests can get ignored by "mvn test"
             */
            throw new IllegalArgumentException("Test classes should have name ending with 'Test'. Invalid input name: " + name);
        }

        List<File> generated = new ArrayList<>();
        String dir = TestSuiteWriterUtils.makeDirectory(directory);
        String content = "";

        // Execute all tests
        executor.newObservers();
        LoopCounter.getInstance().setActive(true); //be sure it is active here, as JUnit checks might have left it to false

        List<ExecutionResult> results = new ArrayList<>();
        for (TestCase test : testCases) {
            boolean added = false;
            if (!TimeController.getInstance().hasTimeToExecuteATestCase()) {
                logger.info("Using cached result");
                for (ExecutionResult result : cachedResults) {
                    if (result != null && result.test == test) {
                        results.add(result);
                        added = true;
                        break;
                    }
                }
            }
            if (!added) {
                ExecutionResult result = runTest(test);
                results.add(result);
            }
        }

        if (Properties.TEST_NAMING_STRATEGY == Properties.TestNamingStrategy.NUMBERED) {
            nameGenerator = new NumberedTestNameGenerationStrategy(testCases, results);
        } else if (Properties.TEST_NAMING_STRATEGY == Properties.TestNamingStrategy.COVERAGE) {
            nameGenerator = new CoverageGoalTestNameGenerationStrategy(testCases, results);
        } else if (Properties.TEST_NAMING_STRATEGY == Properties.TestNamingStrategy.ID) {
            nameGenerator = new IDTestNameGenerationStrategy(testCases);
        } else {
            throw new RuntimeException("Unsupported naming strategy: " + Properties.TEST_NAMING_STRATEGY);
        }

        // Avoid downcasts that could break
        removeUnnecessaryDownCasts(results);

        // Sometimes some timeouts lead to assertions being attached to statements
        // related to exceptions. This is not currently handled, so as a workaround
        // let's try to remove any remaining assertions. TODO: Better solution
        removeAssertionsAfterException(results);


        if (Properties.OUTPUT_GRANULARITY == OutputGranularity.MERGED || testCases.size() == 0) {
            File file = new File(dir + "/" + name + ".java");
            //executor.newObservers();
            content = getUnitTestsAllInSameFile(name, results);
            FileIOUtils.writeFile(content, file);
            generated.add(file);
        } else {
            for (int i = 0; i < testCases.size(); i++) {
                File file = new File(dir + "/" + name + "_" + i + ".java"); // e.g., dir/Foo_ESTest_0.java
                //executor.newObservers();
                String testCode = getOneUnitTestInAFile(name, i, results);
                FileIOUtils.writeFile(testCode, file);
                content += testCode;
                generated.add(file);
            }
        }

        if (Properties.TEST_SCAFFOLDING && !Properties.NO_RUNTIME_DEPENDENCY) {
            String scaffoldingName = Scaffolding.getFileName(name);
            File file = new File(dir + "/" + scaffoldingName + ".java");
            String scaffoldingContent = Scaffolding.getScaffoldingFileContent(name, results,
                    TestSuiteWriterUtils.hasAnySecurityException(results));
            FileIOUtils.writeFile(scaffoldingContent, file);
            generated.add(file);
            content += scaffoldingContent;
        }

        writeCoveredGoalsFile();

        TestGenerationResultBuilder.getInstance().setTestSuiteCode(content);
        return generated;
    }


    public void writeTargetLocationStats(TestSuiteChromosome testSuite,
                                         TestGenerationResult<TestChromosome> result,
                                         String outputPath) {

        if (!Properties.SERIALIZE_GA && Properties.EVOREPAIR_TEST_GENERATION) {
            return;
        }

        LoggingUtils.getEvoLogger().info("* Writing target location stats to {}", outputPath);

        GeneticAlgorithm<?> ga = result.getGeneticAlgorithm();

        /*
         * Step 1: Setup
         * For each goal, compute the number of covering tests and min fitness of closest not covering tests.
         */

        // Mapping between fitness class to fitness functions to number of covering test cases
        Map<Class<?>, Map<TestFitnessFunction, Integer>> goalToNumCoveringTests = new LinkedHashMap<>();

        // Mapping between fitness class to fitness functions to minimal fitness value
        Map<Class<?>, Map<TestFitnessFunction, Double>> goalToMinFitness = new LinkedHashMap<>();

        // Compute values for maps
        computeNumCoveringTestsAndMinFitness((List<TestFitnessFunction>) ga.getFitnessFunctions(),
                testSuite.getTestChromosomes(), goalToNumCoveringTests, goalToMinFitness);

        try {
            /*
             * Step 2: Write out basic summary stats
             * For each class of fitness function, write out summarized coverage stats.
             */
            File outputDir = new File(outputPath);
            if (!outputDir.exists()) { // should already be created
                logger.warn("Error while writing statistics: dir {} does not exist.", outputDir);
                return;
            }
            writeCoverageSummary(outputDir, goalToNumCoveringTests);

            /*
             * Step 3: Compute target goal mappings to compute stats
             * To compute target location specific stats, we need the following mappings:
             * 1) Target location to corresponding context goals
             * 2) Context goal description to simple identifier (to make stats more readable)
             * 3) Simple context goal identifier to complete description (to retrieve the full context description from an id)
             */

            // Mapping from context goal to number of tests that cover it and min fitness of tests not yet covering it
            Map <TestFitnessFunction, Integer> contextGoalsToNumCoveringTests = goalToNumCoveringTests.getOrDefault(ContextLineTestFitness.class, Collections.emptyMap());
            Map <TestFitnessFunction, Double> contextGoalsToMinFitness = goalToMinFitness.getOrDefault(ContextLineTestFitness.class, Collections.emptyMap());

            // TODO EvoRepair: At this point, we should just output the target line stats, no need to do any further computations
            if (contextGoalsToNumCoveringTests.isEmpty()) {
                logger.warn("No registered ContextLineGoals. No context line stats will be produced.");
            }

            // Mapping from target location goal to context goals
            Map <TestFitnessFunction, Set<TestFitnessFunction>> targetGoalToContextGoalMap = new LinkedHashMap<>();

            // Mapping between context branch IDs and more readable names
            Map <TestFitnessFunction, String> contextToIdMap = new LinkedHashMap<>();
            Map <String, String> contextIdToNameMap = new LinkedHashMap<>();

            int contextBranchID = 0;
            for (TestFitnessFunction fitnessFunction : contextGoalsToNumCoveringTests.keySet()) {
                // Assign IDs to contexts
                int id = contextBranchID++;
                contextToIdMap.put(fitnessFunction, "Context-" + id);
                contextIdToNameMap.put("Context-" + id, fitnessFunction.toString());

                // Assign context goal to target location goal
                ContextLineTestFitness contextGoal = (ContextLineTestFitness) fitnessFunction;
                LineCoverageTestFitness lineGoal = contextGoal.getLineGoal();
                if (!targetGoalToContextGoalMap.containsKey(lineGoal)) {
                    targetGoalToContextGoalMap.put(lineGoal, new LinkedHashSet<>());
                }
                targetGoalToContextGoalMap.get(lineGoal).add(contextGoal);
            }

            /*
             * Step 4: Compute stats for target location goals
             * For all target location goals (fix location + oracle exception), we compute the following stats:
             * 1) The number of tests in the test suite covering the goal
             * 2) The (minimum) fitness of the next closest test to cover this goal
             * 3) The number of contexts this goal can be covered from
             * 4) The number of contexts that have been covered by a test
             * 5) For each context, the number of tests that cover it
             * 6) For each context, the (minimum) fitness of the next closest test to cover it
             *
             * For oracle exception targets, we additionally determine the following stats:
             * 7) The number of tests that trigger the exception and also cover a fix location
             * 8) The (minimum) distance to any fix location out of all tests that trigger the exception but cover no fix location
             * 9) For each context, the number of tests that trigger the exception in this context and also cover a fix location
             * 10) For each context, The (minimum) distance to any fix location for all tests that trigger the exception in this context
             */

            // Number of covering tests and minimum fitness for fix location and oracle exception goals
            Map<TestFitnessFunction, Integer>  targetLocationGoalToNumCoveringTests = new LinkedHashMap<>();
            targetLocationGoalToNumCoveringTests.putAll(goalToNumCoveringTests.getOrDefault(LineCoverageTestFitness.class, Collections.emptyMap()));
            targetLocationGoalToNumCoveringTests.putAll(goalToNumCoveringTests.getOrDefault(OracleExceptionTestFitness.class, Collections.emptyMap()));

            Map<TestFitnessFunction, Double>  targetLocationGoalToMinFitness = new LinkedHashMap<>();
            targetLocationGoalToMinFitness.putAll(goalToMinFitness.getOrDefault(LineCoverageTestFitness.class, Collections.emptyMap()));
            targetLocationGoalToMinFitness.putAll(goalToMinFitness.getOrDefault(OracleExceptionTestFitness.class, Collections.emptyMap()));

            // Stats for fix location and oracle exception goals
            List<TargetLocationFitnessMetrics> fixLocationMetrics = new ArrayList<>();
            List<OracleExceptionFitnessMetrics> oracleExceptionMetrics = new ArrayList<>();

            // For each target location (fix location, oracle exception) goal, determine context goal stats
            for (TestFitnessFunction fitnessFunction : targetLocationGoalToNumCoveringTests.keySet()) {
                // Determine context goals
                LineCoverageTestFitness targetLocationGoal = (LineCoverageTestFitness) fitnessFunction; // Could also be OracleExceptionTestFitness

                // Get stats for context goals
                Map<String, Integer> contextGoalToNumCoveringTests = new LinkedHashMap<>();
                Map<String, Double> contextGoalToMinFitness = new LinkedHashMap<>();

                Set<TestFitnessFunction> contextGoals = targetGoalToContextGoalMap.getOrDefault(targetLocationGoal, Collections.emptySet());

                int numTotalContexts = contextGoals.size();
                int numCoveredContexts = 0;

                for (TestFitnessFunction contextGoal : contextGoals) {
                    int numCoveringTests = contextGoalsToNumCoveringTests.get(contextGoal);
                    if (numCoveringTests > 0) {
                        numCoveredContexts++;
                    }
                    contextGoalToMinFitness.put(contextToIdMap.get(contextGoal), contextGoalsToMinFitness.get(contextGoal));
                    contextGoalToNumCoveringTests.put(contextToIdMap.get(contextGoal), numCoveringTests);
                }

                String className = targetLocationGoal.getClassName();
                int lineNumber = targetLocationGoal.getLine();
                int numCoveringTests = targetLocationGoalToNumCoveringTests.get(targetLocationGoal);
                double minFitness = targetLocationGoalToMinFitness.get(targetLocationGoal);

                if (targetLocationGoal instanceof OracleExceptionTestFitness) {
                    int numFixLocationCoveringTests = getNumFixLocationCoveringTestsForGoal(targetLocationGoal,
                            Archive.getMultiCriteriaArchive().getFixLocationGoals(), testSuite.getTestChromosomes());

                    double minFixLocationFitness = getMinFixLocationFitnessForGoal(targetLocationGoal,
                            Archive.getMultiCriteriaArchive().getFixLocationGoals(), testSuite.getTestChromosomes());

                    Map<String, Integer> contextToNumFixLocationCoveringTests = new LinkedHashMap<>();
                    Map<String, Double> contextToMinFixLocationFitness = new LinkedHashMap<>();

                    for (TestFitnessFunction contextGoal : contextGoals) {
                        int numContextFixLocationCoveringTests = getNumFixLocationCoveringTestsForGoal(contextGoal,
                                Archive.getMultiCriteriaArchive().getFixLocationGoals(), testSuite.getTestChromosomes());
                        contextToNumFixLocationCoveringTests.put(contextToIdMap.get(contextGoal), numContextFixLocationCoveringTests);

                        double minContextFixLocationFitness = getMinFixLocationFitnessForGoal(contextGoal,
                                Archive.getMultiCriteriaArchive().getFixLocationGoals(), testSuite.getTestChromosomes());
                        contextToMinFixLocationFitness.put(contextToIdMap.get(contextGoal), minContextFixLocationFitness);
                    }

                    OracleExceptionFitnessMetrics metrics = new OracleExceptionFitnessMetrics(className, lineNumber, numCoveringTests,
                            minFitness, numTotalContexts, numCoveredContexts, contextGoalToNumCoveringTests, contextGoalToMinFitness,
                            numFixLocationCoveringTests, minFixLocationFitness, contextToNumFixLocationCoveringTests, contextToMinFixLocationFitness
                            );
                    oracleExceptionMetrics.add(metrics);

                } else {
                    TargetLocationFitnessMetrics metrics = new TargetLocationFitnessMetrics(className, lineNumber, numCoveringTests,
                            minFitness, numTotalContexts, numCoveredContexts, contextGoalToNumCoveringTests, contextGoalToMinFitness);
                    fixLocationMetrics.add(metrics);
                }
            }

            writeTargetLocationSummary(outputDir, fixLocationMetrics, oracleExceptionMetrics, contextIdToNameMap);
        } catch (IOException e) {
            logger.warn("Error while writing statistics: " + e.getMessage());
        }
    }

    /**
     * Compute minimum fitness and number of covering tests for each fitness function
     * @param fitnessFunctions List of all fitness functions
     * @param tests List of all tests
     * @param goalToNumCoveringTests (empty) mapping from classes of fitness functions to fitness functions to number of covering tests
     * @param goalToMinFitness (empty) mapping from classes of fitness functions to fitness functions to min fitness values
     */
    private void computeNumCoveringTestsAndMinFitness(List<TestFitnessFunction> fitnessFunctions, List<TestChromosome> tests,
                                                      Map<Class<?>, Map<TestFitnessFunction, Integer>> goalToNumCoveringTests,
                                                      Map<Class<?>, Map<TestFitnessFunction, Double>> goalToMinFitness) {

        for (TestFitnessFunction ff : fitnessFunctions) {
            // How many tests already cover this goal?
            if (!goalToNumCoveringTests.containsKey(ff.getClass())) {
                goalToNumCoveringTests.put(ff.getClass(), new LinkedHashMap<>());
            }

            int numCoveringSolutions = (int) tests.stream().mapToDouble(t -> t.getFitness(ff)).filter(f -> f == 0.0).count();
            goalToNumCoveringTests.get(ff.getClass()).put(ff, numCoveringSolutions);


            // How close is the next test to cover the goal? I.e., only consider remaining tests that don't already cover the goal
            if (!goalToMinFitness.containsKey(ff.getClass())) {
                goalToMinFitness.put(ff.getClass(), new LinkedHashMap<>());
            }
            // If all tests cover this goal, return 0.0
            double minFitness = tests.stream().mapToDouble(t -> t.getFitness(ff)).filter(f -> f > 0.0).min().orElse(0.0);
            goalToMinFitness.get(ff.getClass()).put(ff, minFitness);
        }
    }


    private int getNumFixLocationCoveringTestsForGoal(TestFitnessFunction goal, Collection<LineCoverageTestFitness> fixLocationGoals, List<TestChromosome> tests) {
        return (int) tests.stream().filter(t -> t.getFitness(goal) == 0.0 && fixLocationGoals.stream().anyMatch(f -> t.getFitness(f) == 0.0)).count();
    }

    private double getMinFixLocationFitnessForGoal(TestFitnessFunction goal, Collection<LineCoverageTestFitness> fixLocationGoals, List<TestChromosome> tests) {
        return tests.stream()
                .filter(t -> t.getFitness(goal) == 0.0) // All tests covering this goal
                .mapToDouble(t -> fixLocationGoals.stream().mapToDouble(t::getFitness).min().orElse(0.0)) // Min fix location distance
                .filter(fitness -> fitness > 0.0) // Filter out fitness values of tests covering a fix location
                .min().orElse(0.0); // Minimum distance (or 0 if all tests cover a fix location)
    }

    private void writeCoverageSummary(File outputDir, Map<Class<?>, Map<TestFitnessFunction, Integer>> goalToNumCoveringTests) throws IOException {
        // General coverage stats
        File f_stats = new File(outputDir.getAbsolutePath() + File.separator + "coverage_stats.csv");
        BufferedWriter out_coverage = new BufferedWriter(new FileWriter(f_stats));
        out_coverage.write("CRITERION, GOALS, COVERED, UNCOVERED, NUM_COVERING_TESTS" + "\n");

        StringBuilder sb_coverage = new StringBuilder();

        // Write out general stats
        for (Class<?> fitnessClass : goalToNumCoveringTests.keySet()) {
            int covered = 0;
            int totalCoveringTests = 0;
            int uncovered = 0;
            for (TestFitnessFunction fitnessFunction : goalToNumCoveringTests.get(fitnessClass).keySet()) {
                double numCoveringTests = goalToNumCoveringTests.get(fitnessClass).get(fitnessFunction);
                totalCoveringTests += numCoveringTests;
                if (numCoveringTests > 0) {
                    covered++;
                } else {
                    uncovered++;
                }
            }
            sb_coverage.append(fitnessClass.toString());
            sb_coverage.append(",");
            sb_coverage.append(covered + uncovered);
            sb_coverage.append(",");
            sb_coverage.append(covered);
            sb_coverage.append(",");
            sb_coverage.append(uncovered);
            sb_coverage.append(",");
            sb_coverage.append(totalCoveringTests);
            sb_coverage.append("\n");
        }

        out_coverage.write(sb_coverage.toString());
        out_coverage.close();
    }

    private void writeTargetLocationSummary(File outputDir, List<TargetLocationFitnessMetrics> fixLocationMetrics,
                                            List<OracleExceptionFitnessMetrics> oracleExceptionMetrics,
                                            Map <String, String> contextIdToNameMap) throws IOException {

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        File f_fixLocationStats = new File(outputDir.getAbsolutePath() + File.separator + "fixLocation_stats.json");
        mapper.writeValue(f_fixLocationStats, fixLocationMetrics);

        File f_oracleLocationStats = new File(outputDir.getAbsolutePath() + File.separator + "oracleLocation_stats.json");
        mapper.writeValue(f_oracleLocationStats, oracleExceptionMetrics);

        File f_contextIdToNameMap = new File(outputDir.getAbsolutePath() + File.separator + "contextIdToFullNameMap.json");
        mapper.writeValue(f_contextIdToNameMap, contextIdToNameMap);
    }


    /**
     * Create JUnit test suite for class. Customized version of {@link TestSuiteWriter#writeTestSuite(String, String, List)}
     *
     * @param name      Name of the class
     * @param directory Output directory
     */
    public List<File> writeValidationTestSuite(String name, String directory, List<ExecutionResult> cachedResults, TestNameGenerationStrategy nameGen) throws IllegalArgumentException {

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Empty test class name");
        }
        if (!name.endsWith("Test")) {
            /*
             * This is VERY important, as otherwise tests can get ignored by "mvn test"
             */
            throw new IllegalArgumentException("Test classes should have name ending with 'Test'. Invalid input name: " + name);
        }

        List<File> generated = new ArrayList<>();
        String dir = TestSuiteWriterUtils.makeDirectory(directory);
        String content = "";

        // The default test suite writer would execute the test suite again, we use cached results by default
        // TODO: Investigate why the last execution result of a test case may be null
        // Execute all tests
        executor.newObservers();
        LoopCounter.getInstance().setActive(true); //be sure it is active here, as JUnit checks might have left it to false

        // TODO: Tests and results should be in the same order, no need to iterate over both lists every time
        List<ExecutionResult> results = new ArrayList<>();
        for (TestCase test : testCases) {
            boolean added = false;
            /**
             * TODO: We must execute the test if the corresponding result is null, otherwise
             *       the following methods depending on the results will break. Alternatively,
             *       we need to add null-checks to these methods.
             */

            if (!TimeController.getInstance().hasTimeToExecuteATestCase()) {
                logger.info("Using cached result");
                for (ExecutionResult result : cachedResults) {
                    if (result != null && result.test == test) {
                        results.add(result);
                        added = true;
                        break;
                    }
                }
            }
            if (!added) {
                ExecutionResult result = runTest(test);
                results.add(result);
            }
        }



        nameGenerator = nameGen;

        // Avoid downcasts that could break
        removeUnnecessaryDownCasts(results);

        // Sometimes some timeouts lead to assertions being attached to statements
        // related to exceptions. This is not currently handled, so as a workaround
        // let's try to remove any remaining assertions. TODO: Better solution
        removeAssertionsAfterException(results);

        // Write all test cases into single file
        File testSuitefile = new File(dir + "/" + name + ".java");
        //executor.newObservers();
        content = getUnitTestsAllInSameFile(name, results);
        FileIOUtils.writeFile(content, testSuitefile);
        generated.add(testSuitefile);

        if (Properties.TEST_SCAFFOLDING && !Properties.NO_RUNTIME_DEPENDENCY) {
            String scaffoldingName = Scaffolding.getFileName(name);
            File scaffoldingFile = new File(dir + "/" + scaffoldingName + ".java");
            String scaffoldingContent = Scaffolding.getScaffoldingFileContent(name, results,
                    TestSuiteWriterUtils.hasAnySecurityException(results));
            FileIOUtils.writeFile(scaffoldingContent, scaffoldingFile);
            generated.add(scaffoldingFile);
            content += scaffoldingContent;
        }

        // No need to do this while still evolving tests
        // writeCoveredGoalsFile();

        // TODO: Is this necessary?
        // TestGenerationResultBuilder.getInstance().setTestSuiteCode(content);
        return generated;
    }

    /**
     * To avoid having completely empty test classes, a no-op test is created
     *
     * @return
     */
    private String getEmptyTest() {
        StringBuilder bd = new StringBuilder();
        bd.append(METHOD_SPACE);
        bd.append("@Test\n");
        bd.append(METHOD_SPACE);
        bd.append("public void " + NOT_GENERATED_TEST_NAME + "() {\n");
        bd.append(BLOCK_SPACE);
        bd.append("// EvoSuite did not generate any tests\n");
        bd.append(METHOD_SPACE);
        bd.append("}\n");
        return bd.toString();
    }

    private void removeUnnecessaryDownCasts(List<ExecutionResult> results) {
        for (ExecutionResult result : results) {
            if (result.test instanceof DefaultTestCase) {
                ((DefaultTestCase) result.test).removeDownCasts();
            }
        }
    }

    private void removeAssertionsAfterException(List<ExecutionResult> results) {
        for (ExecutionResult result : results) {
            if (result.noThrownExceptions())
                continue;
            int exceptionPosition = result.getFirstPositionOfThrownException();
            // TODO: Not clear how that can happen...
            if (result.test.size() > exceptionPosition)
                result.test.getStatement(exceptionPosition).removeAssertions();
        }
    }

    private String getDefects4JInstrumentationCode() {
        StringBuilder builder = new StringBuilder();
        builder.append(NEWLINE);
        builder.append("@Before");
        builder.append(NEWLINE);
        builder.append("public void enableInstrumentation() {");
        builder.append("System.setProperty(\"defects4j.instrumentation.enabled\", \"true\");");
        builder.append("}");
        builder.append(NEWLINE);
        builder.append("@After");
        builder.append(NEWLINE);
        builder.append("public void disableInstrumentation() {");
        builder.append("System.setProperty(\"defects4j.instrumentation.enabled\", \"false\");");
        builder.append("}");
        builder.append(NEWLINE);
        return builder.toString();
    }


    /**
     * Create JUnit file for given class name
     *
     * @param name Name of the class file
     * @return String representation of JUnit test file
     */
    private String getUnitTestsAllInSameFile(String name, List<ExecutionResult> results) {

        /*
         * if there was any security exception, then we need to scaffold the
         * test cases with a sandbox
         */
        boolean wasSecurityException = TestSuiteWriterUtils.hasAnySecurityException(results);

        StringBuilder builder = new StringBuilder();

        builder.append(getHeader(name, name, results));

        if (!Properties.TEST_SCAFFOLDING && !Properties.NO_RUNTIME_DEPENDENCY) {
            builder.append(new Scaffolding().getBeforeAndAfterMethods(name, wasSecurityException, results));
        }

        builder.append(getDefects4JInstrumentationCode());

        if (testCases.isEmpty()) {
            builder.append(getEmptyTest());
        } else {
            for (int i = 0; i < testCases.size(); i++) {
                builder.append(testToString(i, i, results.get(i)));
            }
        }
        builder.append(getFooter());

        return builder.toString();
    }

    /**
     * Create JUnit file for given class name
     *
     * @param name   Name of the class file
     * @param testId a int.
     * @return String representation of JUnit test file
     */
    private String getOneUnitTestInAFile(String name, int testId, List<ExecutionResult> results) {

        boolean wasSecurityException = results.get(testId).hasSecurityException();

        StringBuilder builder = new StringBuilder();

        builder.append(getHeader(name + "_" + testId, name, results));

        if (!Properties.TEST_SCAFFOLDING) {
            builder.append(new Scaffolding().getBeforeAndAfterMethods(name + "_" + testId, wasSecurityException, results));
        }

        builder.append(testToString(testId, testId, results.get(testId)));
        builder.append(getFooter());

        return builder.toString();
    }

    /**
     * <p>
     * runTest
     * </p>
     *
     * @param test a {@link org.evosuite.testcase.TestCase} object.
     * @return a {@link org.evosuite.testcase.execution.ExecutionResult} object.
     */
    protected ExecutionResult runTest(TestCase test) {

        ExecutionResult result = new ExecutionResult(test, null);

        try {
            logger.debug("Executing test");
            result = executor.execute(test);
        } catch (Exception e) {
            throw new Error(e);
        }

        return result;
    }


    // -----------------------------------------------------------
    // --------------   code generation methods ------------------
    // -----------------------------------------------------------


    /**
     * Determine packages that need to be imported in the JUnit file
     *
     * @param results a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    protected String getImports(List<ExecutionResult> results) {
        StringBuilder builder = new StringBuilder();
        Set<Class<?>> imports = new HashSet<>();
        Set<Class<?>> accessedClasses = new HashSet<>();
        boolean wasSecurityException = TestSuiteWriterUtils.hasAnySecurityException(results);
        boolean hasException = false;

        for (ExecutionResult result : results) {
            visitor.clearExceptions();
            visitor.setExceptions(result.exposeExceptionMapping());
            result.test.accept(visitor);
            imports.addAll(visitor.getImports());
            accessedClasses.addAll(result.test.getAccessedClasses());
            if (!hasException)
                hasException = !result.noThrownExceptions();
        }
        visitor.clearExceptions();

        if (doesUseMocks(results)) {
            String mockito = Mockito.class.getCanonicalName();
            builder.append("import static " + mockito + ".*;" + NEWLINE);
            // MockitoExtension is now deprecated
            //String extension = MockitoExtension.class.getCanonicalName();
            //builder.append("import static "+extension+".*;"+NEWLINE);
            imports.add(ViolatedAssumptionAnswer.class);
        }

        if (hasException && !Properties.NO_RUNTIME_DEPENDENCY) {
            builder.append("import static " + EvoAssertions.class.getCanonicalName() + ".*;" + NEWLINE);
        }

        if (Properties.RESET_STANDARD_STREAMS) {
            imports.add(PrintStream.class);
            imports.add(DebugGraphics.class);
        }

        if (TestSuiteWriterUtils.needToUseAgent() && !Properties.NO_RUNTIME_DEPENDENCY) {
            imports.add(EvoRunnerParameters.class);
            if (Properties.TEST_FORMAT == Properties.OutputFormat.JUNIT5) {
                imports.add(EvoRunnerJUnit5.class);
                imports.add(RegisterExtension.class);
            } else {
                imports.add(RunWith.class);
                imports.add(EvoRunner.class);
            }
        }

        Set<String> importNames = new HashSet<>();
        for (Class<?> imp : imports) {
            while (imp.isArray())
                imp = imp.getComponentType();
            if (imp.isPrimitive())
                continue;
            if (imp.getName().startsWith("java.lang")) {
                String name = imp.getName().replace("java.lang.", "");
                if (!name.contains("."))
                    continue;
            }
            if (!imp.getName().contains("."))
                continue;
            // TODO: Check for anonymous type?
            if (imp.getName().contains("$"))
                importNames.add(imp.getName().replace("$", "."));
            else
                importNames.add(imp.getName());
        }

        for (Class<?> klass : EnvironmentDataList.getListOfClasses()) {
            //TODO: not paramount, but best if could check if actually used in the test suite
            if (accessedClasses.contains(klass))
                importNames.add(klass.getCanonicalName());
        }

        if (wasSecurityException) {
            //Add import info for EvoSuite classes used in the generated test suite
            importNames.add(java.util.concurrent.ExecutorService.class.getCanonicalName());
            importNames.add(java.util.concurrent.Executors.class.getCanonicalName());
            importNames.add(java.util.concurrent.Future.class.getCanonicalName());
            importNames.add(java.util.concurrent.TimeUnit.class.getCanonicalName());
        }

        if (!Properties.TEST_SCAFFOLDING && !Properties.NO_RUNTIME_DEPENDENCY) {
            importNames.addAll(Scaffolding.getScaffoldingImports(wasSecurityException, results));
        }

        // If a CodeUnderTestException happens, the test will be chopped before that exception
        // but it would still be in the imports
        importNames.remove(CodeUnderTestException.class.getCanonicalName());

        // Imports for custom defects4j instrumentation
        importNames.add("org.junit.Before");
        importNames.add("org.junit.After");
        importNames.add("java.lang.System");


        List<String> importsSorted = new ArrayList<>(importNames);

        Collections.sort(importsSorted);
        for (String imp : importsSorted) {
            builder.append("import ");
            builder.append(imp);
            builder.append(";");
            builder.append(NEWLINE);
        }

        builder.append(NEWLINE);

        return builder.toString();
    }


    /**
     * JUnit file header
     *
     * @param test_name        a {@link java.lang.String} object.
     * @param scaffolding_name a {@link java.lang.String} object.
     * @param results          a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    protected String getHeader(String test_name, String scaffolding_name, List<ExecutionResult> results) {
        StringBuilder builder = new StringBuilder();
        builder.append("/*");
        builder.append(NEWLINE);
        builder.append(" * This file was automatically generated by EvoSuite");
        builder.append(NEWLINE);
        builder.append(" * " + new Date());
        builder.append(NEWLINE);
        builder.append(" */");
        builder.append(NEWLINE);
        builder.append(NEWLINE);

        if (!Properties.CLASS_PREFIX.equals("")) {
            builder.append("package ");
            builder.append(Properties.CLASS_PREFIX);
            builder.append(";");
            builder.append(NEWLINE);
        }
        builder.append(NEWLINE);

        builder.append(adapter.getImports());
        builder.append(getImports(results));

        if (TestSuiteWriterUtils.needToUseAgent() && !Properties.NO_RUNTIME_DEPENDENCY) {
            builder.append(getRunner());
        }

        builder.append(adapter.getClassDefinition(test_name));

        if (Properties.TEST_SCAFFOLDING && !Properties.NO_RUNTIME_DEPENDENCY) {
            builder.append(" extends ").append(Scaffolding.getFileName(scaffolding_name));
        }

        builder.append(" {");
        builder.append(NEWLINE);
        if (Properties.TEST_FORMAT == Properties.OutputFormat.JUNIT5) {
            builder.append("@RegisterExtension").append(NEWLINE);
            builder.append(METHOD_SPACE).append("static EvoRunnerJUnit5 runner = new EvoRunnerJUnit5(").append(test_name).append(".class);").append(NEWLINE);
        }
        return builder.toString();
    }

    private Object getRunner() {


        String s = Properties.TEST_FORMAT == Properties.OutputFormat.JUNIT5 ? "@EvoRunnerParameters("
                : "@RunWith(EvoRunner.class) @EvoRunnerParameters(";
        List<String> list = new ArrayList<>();

        if (Properties.REPLACE_CALLS) {
            list.add("mockJVMNonDeterminism = true");
        }

        if (Properties.VIRTUAL_FS) {
            list.add("useVFS = true");
        }

        if (Properties.VIRTUAL_NET) {
            list.add("useVNET = true");
        }

        if (Properties.RESET_STATIC_FIELDS) {
            list.add("resetStaticState = true");
        }

        if (Properties.USE_SEPARATE_CLASSLOADER) {
            list.add("separateClassLoader = true");
        }

        if (Properties.REPLACE_GUI) {
            list.add("mockGUI = true");
        }

        if (!list.isEmpty()) {
            s += list.get(0);

            for (int i = 1; i < list.size(); i++) {
                s += ", " + list.get(i);
            }
        }

        s += ") " + NEWLINE;

        return s;
    }

    /**
     * JUnit file footer
     *
     * @return a {@link java.lang.String} object.
     */
    protected String getFooter() {
        return "}" + NEWLINE;
    }


    /**
     * Convert one test case to a Java method
     *
     * @param id     Index of the test case
     * @param result a {@link org.evosuite.testcase.execution.ExecutionResult} object.
     * @return String representation of test case
     */
    protected String testToString(int number, int id, ExecutionResult result) {

        boolean wasSecurityException = result.hasSecurityException();

        String testInfo = getInformation(id);

        StringBuilder builder = new StringBuilder();
        builder.append(NEWLINE);
        if (Properties.TEST_COMMENTS || testComment.containsKey(id)) {
            builder.append(METHOD_SPACE);
            builder.append("//");
            builder.append(testInfo);
            builder.append(NEWLINE);
        }

        // Get the test method name generated in TestNameGenerator
        String methodName = nameGenerator.getName(testCases.get(id));
        if (methodName == null) {
            // if TestNameGenerator did not generate a name, fall back to original naming
            methodName = TestSuiteWriterUtils.getNameOfTest(testCases, number);
        }
        builder.append(adapter.getMethodDefinition(methodName));

        /*
         * A test case might throw a lot of different kinds of exceptions.
         * These might come from SUT, and might also come from the framework itself (eg, see ExecutorService.submit).
         * Regardless of whether they are declared or not, an exception that propagates to the JUnit framework will
         * result in a failure for the test case. However, there might be some checked exceptions, and for those
         * we need to declare them in the signature with "throws". So, the easiest (but still correct) option
         * is to just declare once to throw any generic Exception, and be done with it once and for all
         */
        builder.append(" throws Throwable ");
        builder.append(" {");
        builder.append(NEWLINE);

        // ---------   start with the body -------------------------
        String CODE_SPACE = INNER_BLOCK_SPACE;

        // No code after an exception should be printed as it would break compilability
        TestCase test = testCases.get(id);

        Integer pos = result.getFirstPositionOfThrownException();
        if (pos != null) {
            if (result.getExceptionThrownAtPosition(pos) instanceof CodeUnderTestException) {
                test.chop(pos);
            } else {
                test.chop(pos + 1);
            }
        }

        if (wasSecurityException) {
            builder.append(BLOCK_SPACE);
            builder.append("Future<?> future = " + Scaffolding.EXECUTOR_SERVICE
                    + ".submit(new Runnable(){ ");
            builder.append(NEWLINE);
            builder.append(INNER_BLOCK_SPACE);
            builder.append(INNER_BLOCK_SPACE);
            builder.append("@Override public void run() { ");
            builder.append(NEWLINE);
            Set<Class<?>> exceptions = test.getDeclaredExceptions();
            if (!exceptions.isEmpty()) {
                builder.append(INNER_INNER_BLOCK_SPACE);
                builder.append("try {");
                builder.append(NEWLINE);
            }
            CODE_SPACE = INNER_INNER_INNER_BLOCK_SPACE;
        }

        for (String line : adapter.getTestString(id, test,
                result.exposeExceptionMapping(), visitor).split("\\r?\\n")) {
            builder.append(CODE_SPACE);
            builder.append(line);
            builder.append(NEWLINE);
        }

        if (wasSecurityException) {
            Set<Class<?>> exceptions = test.getDeclaredExceptions();
            if (!exceptions.isEmpty()) {
                builder.append(INNER_INNER_BLOCK_SPACE);
                builder.append("} catch(Throwable t) {");
                builder.append(NEWLINE);
                builder.append(INNER_INNER_INNER_BLOCK_SPACE);
                builder.append("  // Need to catch declared exceptions");
                builder.append(NEWLINE);
                builder.append(INNER_INNER_BLOCK_SPACE);
                builder.append("}");
                builder.append(NEWLINE);
            }

            builder.append(INNER_BLOCK_SPACE);
            builder.append("} "); //closing run(){
            builder.append(NEWLINE);
            builder.append(BLOCK_SPACE);
            builder.append("});"); //closing submit
            builder.append(NEWLINE);

            long time = Properties.TIMEOUT + 1000; // we add one second just to be sure, that to avoid issues with test cases taking exactly TIMEOUT ms
            builder.append(BLOCK_SPACE);
            builder.append("future.get(" + time + ", TimeUnit.MILLISECONDS);");
            builder.append(NEWLINE);
        }

        // ---------   end of the body ----------------------------

        builder.append(METHOD_SPACE);
        builder.append("}");
        builder.append(NEWLINE);

        String testCode = builder.toString();
        TestGenerationResultBuilder.getInstance().setTestCase(methodName, testCode, test,
                testInfo, result);
        return testCode;
    }

    /**
     * When writing out the JUnit test file, each test can have a text comment
     *
     * @param num Index of test case
     * @return Comment for test case
     */
    protected String getInformation(int num) {

        if (testComment.containsKey(num)) {
            String comment = testComment.get(num);
            if (!comment.endsWith("\n"))
                comment = comment + NEWLINE;
            return comment;
        }

        TestCase test = testCases.get(num);
        Set<TestFitnessFunction> coveredGoals = test.getCoveredGoals();

        StringBuilder builder = new StringBuilder();
        builder.append("Test case number: " + num);

        if (!coveredGoals.isEmpty()) {
            builder.append(NEWLINE);
            builder.append("  /*");
            builder.append(NEWLINE);
            builder.append("   * ");
            builder.append(coveredGoals.size() + " covered goal");
            if (coveredGoals.size() != 1)
                builder.append("s");
            builder.append(":");
            int nr = 1;
            for (TestFitnessFunction goal : coveredGoals) {
                builder.append(NEWLINE);
                builder.append("   * Goal " + nr + ". " + goal.toString());
                // TODO only for debugging purposes
                if (ArrayUtil.contains(Properties.CRITERION, Criterion.DEFUSE)
                        && (goal instanceof DefUseCoverageTestFitness)) {
                    DefUseCoverageTestFitness duGoal = (DefUseCoverageTestFitness) goal;
                    if (duGoal.getCoveringTrace() != null) {
                        String traceInformation = duGoal.getCoveringTrace().toDefUseTraceInformation(duGoal.getGoalVariable(),
                                duGoal.getCoveringObjectId());
                        traceInformation = traceInformation.replaceAll("\n", "");
                        builder.append(NEWLINE);
                        builder.append("     * DUTrace: " + traceInformation);
                    }
                }
                nr++;
            }

            builder.append(NEWLINE);
            builder.append("   */");
            builder.append(NEWLINE);
        }

        return builder.toString();
    }

    private void writeCoveredGoalsFile() {
        if (Properties.WRITE_COVERED_GOALS_FILE) {
            StringBuilder builder = new StringBuilder();
            File file = new File(Properties.COVERED_GOALS_FILE);
            for (int i = 0; i < testCases.size(); i++) {
                TestCase test = testCases.get(i);
                String generatedName = nameGenerator.getName(test);
                String testName = (generatedName != null) ? generatedName : TestSuiteWriterUtils.getNameOfTest(testCases, i);
                Set<TestFitnessFunction> coveredGoals = test.getCoveredGoals();
                for (TestFitnessFunction goal : coveredGoals) {
                    builder.append(testName + "," + goal.toString() + NEWLINE);
                }
            }
            FileIOUtils.writeFile(builder.toString(), file);
        }
    }
}
