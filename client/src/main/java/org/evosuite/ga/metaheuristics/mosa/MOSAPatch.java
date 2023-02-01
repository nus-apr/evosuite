package org.evosuite.ga.metaheuristics.mosa;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.Properties;
import org.evosuite.coverage.exception.ExceptionCoverageSuiteFitness;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.PatchCoverageFactory;
import org.evosuite.coverage.patch.PatchCoverageTestFitness;
import org.evosuite.coverage.patch.PatchLineCoverageFactory;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.coverage.patch.communication.json.JsonFilePath;
import org.evosuite.coverage.patch.communication.json.PatchValidationSummary;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.archive.Archive;
import org.evosuite.junit.naming.methods.IDTestNameGenerationStrategy;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.testcase.DefaultTestCase;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.utils.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class MOSAPatch extends MOSA {

    private static final long serialVersionUID = 4922357906751423327L;

    private static final Logger logger = LoggerFactory.getLogger(MOSAPatch.class);

    // Goals to use once patch kill results become available
    private final Set<TestFitnessFunction> goalsForNextIteration;

    public MOSAPatch(ChromosomeFactory<TestChromosome> factory) {
        super(factory);

        if (!ArrayUtil.contains(Properties.CRITERION, Properties.Criterion.PATCHLINE)) {
            throw new RuntimeException("Criterion PATCHLINE not enabled.");
        }

        this.goalsForNextIteration = new LinkedHashSet<>();
    }

    // NOTE: This should only be called before the search is started in order to properly init goals
    @Override
    public void addFitnessFunction(final FitnessFunction<TestChromosome> function) {
        if (function instanceof TestFitnessFunction) {
        //    if (false) {
                // Patch coverage goals will be added once we have started killing the first set of patches
                goalsForNextIteration.add((TestFitnessFunction) function);
        //    } else {
                // Initial goals that can serve as guidance before any patches have been killed
                fitnessFunctions.add((TestFitnessFunction) function);
        //    }
        } else {
            throw new IllegalArgumentException("Only TestFitnessFunctions are supported");
        }
    }

    // TODO: When do we actually want to stop?
    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    protected List<TestChromosome> breedNextGeneration() {
        // First breed without evaluation, TODO: here can potentially breed + evaluate
        List<TestChromosome> offspringPopulation = breedNextGenerationWithoutEvaluation();
        postCalculateFitness(offspringPopulation);

        /**
         * If all fix locations have been covered, we can notify the orchestrator and ask for patch validation results.
         * Then, we check if any patches have been killed by the current population:
         * If yes:
         * - We update the current goals to reflect the previous patch pool (target lines + killed patches)
         * - This includes updating the fitness functions, resetting the archive, and recomputing fitness/goals for tests
         * - Finally, we "enqueue" the next set of goals that reflect the updated patch pool, which are selected once new patches have been killed
         * If not, we continue selection/evolution based on the current fitness values.
         */
        if(isPopulationAdequate()) {
            List<TestChromosome> union = new ArrayList<>();
            union.addAll(this.population);
            union.addAll(offspringPopulation);
            sendPopulationToOrchestratorAndUpdateGoals(union, getAge());
            // Evaluate union w.r.t. previous goals (patches killed etc.)
            postCalculateFitness(union);
        }
        return offspringPopulation;
    }

    @Override
    protected void calculateFitness(TestChromosome tc) {
        // If all fix locations have been covered, we can start computing patch mutation score
        if (isPopulationAdequate()) {
            this.fitnessFunctions.forEach(fitnessFunction -> fitnessFunction.getFitness(tc));
        } else {
            // Otherwise, compute fix location fitness only
            this.fitnessFunctions.stream()
                    .filter(LineCoverageTestFitness.class::isInstance)
                    .forEach(fitnessFunction -> fitnessFunction.getFitness(tc));
        }

        // if one of the coverage criterion is Criterion.EXCEPTION, then we have to analyse the results
        // of the execution to look for generated exceptions

        if (ArrayUtil.contains(Properties.CRITERION, Properties.Criterion.EXCEPTION)) {
            ExceptionCoverageSuiteFitness.calculateExceptionInfo(
                    Collections.singletonList(tc.getLastExecutionResult()),
                    new HashMap<>(), new HashMap<>(), new HashMap<>(), new ExceptionCoverageSuiteFitness());
        }

        this.notifyEvaluation(tc);
        // update the time needed to reach the max coverage
        this.budgetMonitor.checkMaxCoverage(this.getNumberOfCoveredGoals());

    }

    // Whether enough goals have been covered to send the current population to the orchestrator
    private boolean isPopulationAdequate() {
        return fixLocationsCovered();
    }

    // Checks if any of the uncovered goals correspond to fix locations
    private boolean fixLocationsCovered() {
        Set<TestFitnessFunction> uncoveredGoals = super.getUncoveredGoals();
        return uncoveredGoals.stream().noneMatch(LineCoverageTestFitness.class::isInstance);
    }

    // Since this method is only used as a stopping criterion, simply return 1 to never stop
    @Override
    protected int getNumberOfUncoveredGoals() {
        return 1;
    }

    // TODO: Request generation can potentially be optimized using JsonGenerator
    public void sendPopulationToOrchestratorAndUpdateGoals(List<TestChromosome> population, int generation) {
        // Filter population to only fix-location covering tests
        List<TestChromosome> reachingTests = population.stream()
                .filter(t -> (t.getTestCase()).getCoveredGoals().stream().anyMatch(LineCoverageTestFitness.class::isInstance))
                .collect(toList());

        List<TestCase> tests = reachingTests.stream()
                .map(TestChromosome::getTestCase)
                .collect(toList());

        List<ExecutionResult> results = reachingTests.stream()
                .map(TestChromosome::getLastExecutionResult)
                .collect(toList());

        TestSuiteWriter suiteWriter = new TestSuiteWriter();
        suiteWriter.insertAllTests(tests);

        String name = Properties.TARGET_CLASS.substring(Properties.TARGET_CLASS.lastIndexOf(".") + 1);
        String testDir = "test-populations"; // TODO: make configurable
        String suffix = Properties.JUNIT_SUFFIX;

        IDTestNameGenerationStrategy nameGenerator = new IDTestNameGenerationStrategy(tests);

        List<File> generatedTests = suiteWriter.writeValidationTestSuite(name + generation + suffix, testDir, results, nameGenerator);

        // Generate JSON message for orchestrator
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("cmd", "getKillMatrixAndNewGoals");
        Map<String, Object> populationInfo = new LinkedHashMap<>();
        populationInfo.put("generation", generation);
        populationInfo.put("tests", nameGenerator.getNames());
        populationInfo.put("classname", Properties.TARGET_CLASS + generation + suffix);
        populationInfo.put("testSuitePath", generatedTests.get(0).getAbsolutePath());
        if (Properties.TEST_SCAFFOLDING && !Properties.NO_RUNTIME_DEPENDENCY) {
            populationInfo.put("testScaffoldingPath", generatedTests.get(1).getAbsolutePath());
        }
        msg.put("data", populationInfo);

        OrchestratorClient client = OrchestratorClient.getInstance();
        JsonFilePath response = client.sendFileRequest(msg, new TypeReference<JsonFilePath>() {});
        PatchValidationSummary summary = client.getJSONReplyFromFile(response.getPath(),
                "getKillMatrixAndNewGoals", new TypeReference<PatchValidationSummary>() {});

        // Update patch kill matrix
        // Note: Changes in patch goals can only happen if patches have been killed
        //boolean updated = PatchCoverageTestFitness.updateKillMatrix(summary.getKillMatrix());
        boolean updated = false;

        if (updated) {
            // TODO: Better to check if the new patch pool is different from the previous one
            if(!summary.getPatches().isEmpty()) {
                throw new RuntimeException("Patches have been killed but no update in patch pool.");
            }

            // We have successfully killed patches and can start evolving using these patch kill scores
            // Update set of goals (patches killed in current iteration), enqueue new goals for next iteration
            // TODO: Keep patch goals throughout evolution?
            if (!goalsForNextIteration.isEmpty()) {
                // Clear everything we know and update w.r.t. new goals
                Archive.getArchiveInstance().reset();
                this.fitnessFunctions.clear();

                // Here, we add the patches for which we know the patch kill results
                Archive.getArchiveInstance().addTargets(goalsForNextIteration);
                this.fitnessFunctions.addAll(goalsForNextIteration);

                // Updated fix locations will serve as guidance towards the next set of patches
                // TODO: Make this iterate through properties and instantiate factories from FitnessFunctions.java
                List<LineCoverageTestFitness> lineGoals = new PatchLineCoverageFactory().getCoverageGoals(summary.getFixLocations());
                //Archive.getArchiveInstance().addTargets(lineGoals); FIXME EvoRepair: LineCoverageTestFitness vs. TestFitnessFunction
                this.fitnessFunctions.addAll(lineGoals);

                population.stream().forEach(testChromosome -> {
                    testChromosome.setChanged(true);
                    testChromosome.getTestCase().clearCoveredGoals();
                    calculateFitness(testChromosome); // evaluate w.r.t. fix location distance + killed patches
                });

                // Clean up
                goalsForNextIteration.clear();
            }

            // Save/"enqueue" patch coverage goals for next iteration
            goalsForNextIteration.addAll(new PatchCoverageFactory().getCoverageGoals(summary.getPatches()));
        }
    }
}
