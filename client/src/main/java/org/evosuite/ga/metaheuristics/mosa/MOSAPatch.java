package org.evosuite.ga.metaheuristics.mosa;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.Properties;
import org.evosuite.coverage.FitnessFunctions;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.coverage.patch.communication.json.JsonFilePath;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.junit.naming.methods.IDTestNameGenerationStrategy;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.utils.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class MOSAPatch extends MOSA {

    private static final long serialVersionUID = 4922357906751423327L;

    private static final Logger logger = LoggerFactory.getLogger(MOSAPatch.class);

    private final Map<Properties.Criterion, List<TestFitnessFunction>> mappedFitnessFunctions;

    private boolean fixLocationsCovered = false;

    public MOSAPatch(ChromosomeFactory<TestChromosome> factory) {
        super(factory);

        if (!ArrayUtil.contains(Properties.CRITERION, Properties.Criterion.PATCHLINE)) {
            throw new RuntimeException("Criterion PATCHLINE not enabled.");
        }

        this.mappedFitnessFunctions = new LinkedHashMap<>();
        for (Properties.Criterion criterion : Properties.CRITERION) {
            List<TestFitnessFunction> criterionFitnessFunctions = new ArrayList<>();
            Class<?> testFit = FitnessFunctions.getTestFitnessFunctionClass(criterion);
            for (TestFitnessFunction f : fitnessFunctions) {
                if (testFit.isInstance(f)) {
                    criterionFitnessFunctions.add(f);
                }
            }
            this.mappedFitnessFunctions.put(criterion, criterionFitnessFunctions);
        }
    }

    // TODO: When do we actually stop?

    @Override
    public boolean isFinished() {
        return getAge() > 10;
    }


    @Override
    protected List<TestChromosome> breedNextGeneration() {
        // First breed without evaluation
        List<TestChromosome> offspringPopulation = breedNextGenerationWithoutEvaluation();

        // Previous population did not cover all fix locations, but offspring population might do
        if(!fixLocationsCovered) {
            postCalculateFitness(offspringPopulation);
            updateFixLocationsCovered();
        }

        /**
         * If all fix locations have been covered, we can notify the orchestrator and ask for results
         * We require:
         * - Current set of goals: Patch pool, fix locations
         * - Patch validation results w.r.t. updated goals
         *
         * Workflow:
         * 0. Perform selection based on original goals!!!
         * 1. Update goals: Here and in Archive
         * 2. Clear covered goals of each input of the previous population, since we recompute fitness w.r.t. new goals
         * 3. Reset coverage archive
         * 4. Evaluate previous population + offspring population w.r.t. patches and fix locations
         */
        if(fixLocationsCovered) {
            List<TestChromosome> union = new ArrayList<>();
            union.addAll(this.population);
            union.addAll(offspringPopulation);
            sendTestPopulationToOrchestrator(union, getAge());

            // TODO: Update goals and archive
            updateFixLocationsCovered(); // We might need to continue covering new fix locations first
        }

        // Then evaluate
        postCalculateFitness(population);
        return offspringPopulation;
    }

    @Override
    protected void calculateFitness(TestChromosome tc) {
        // If all fix locations have been covered, we can start computing patch mutation score
        if (fixLocationsCovered) {
            this.fitnessFunctions.forEach(fitnessFunction -> fitnessFunction.getFitness(tc));
        } else {
            // Otherwise, compute fix location fitness only
            this.mappedFitnessFunctions.get(Properties.Criterion.PATCHLINE).forEach(fitnessFunction -> fitnessFunction.getFitness(tc));
        }
        // if one of the coverage criterion is Criterion.EXCEPTION, then we have to analyse the results
        // of the execution to look for generated exceptions
        /*
        if (ArrayUtil.contains(Properties.CRITERION, Properties.Criterion.EXCEPTION)) {
            ExceptionCoverageSuiteFitness.calculateExceptionInfo(
                    Collections.singletonList(c.getLastExecutionResult()),
                    new HashMap<>(), new HashMap<>(), new HashMap<>(), new ExceptionCoverageSuiteFitness());
        }

         */

        this.notifyEvaluation(tc);
        // update the time needed to reach the max coverage
        this.budgetMonitor.checkMaxCoverage(this.getNumberOfCoveredGoals());

    }

    // Checks if any of the uncovered goals correspond to fix locations
    private void updateFixLocationsCovered() {
        Set<TestFitnessFunction> uncoveredGoals = super.getUncoveredGoals();
        fixLocationsCovered = uncoveredGoals.stream().noneMatch(LineCoverageTestFitness.class::isInstance);
    }

    // Filter out patch mutation goals before all fix locations have been covered
    // TODO: Redundant computations, optimize
    @Override
    protected Set<TestFitnessFunction> getUncoveredGoals() {
        if (fixLocationsCovered) {
            return super.getUncoveredGoals();
        } else {
            return super.getUncoveredGoals().stream()
                    .filter(LineCoverageTestFitness.class::isInstance)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
    /**
     * Cannot override because it is used as an stopping criterion.
    @Override
    protected int getNumberOfUncoveredGoals() {
        if (fixLocationsCovered) {
            return super.getNumberOfUncoveredGoals();
        } else {
            return (int) super.getUncoveredGoals().stream()
                    .filter(LineCoverageTestFitness.class::isInstance)
                    .count();
        }
    }
    */

    // TODO: Request generation can potentially be optimized using JsonGenerator
    public void sendTestPopulationToOrchestrator(List<TestChromosome> population, int generation) {
        List<TestCase> tests = population.stream()
                .map(TestChromosome::getTestCase)
                .collect(toList());

        List<ExecutionResult> results = population.stream()
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
        msg.put("cmd", "updateTestPopulation");
        Map<String, Object> populationInfo = new LinkedHashMap<>();
        populationInfo.put("generation", generation);
        populationInfo.put("tests", nameGenerator.getNames());
        populationInfo.put("classname", name + generation + suffix);
        populationInfo.put("testSuitePath", generatedTests.get(0).getAbsolutePath());
        if (Properties.TEST_SCAFFOLDING && !Properties.NO_RUNTIME_DEPENDENCY) {
            populationInfo.put("testScaffoldingPath", generatedTests.get(1).getAbsolutePath());
        }
        msg.put("data", populationInfo);

        // TODO: Parse response
        // 1. Process patch validation results for PatchCoverageGoals to Cover
        // 2. Update goals
        OrchestratorClient client = OrchestratorClient.getInstance();
        JsonFilePath response = client.sendFileRequest(msg, new TypeReference<JsonFilePath>() {});
    }

}
