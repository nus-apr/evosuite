package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.Properties;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.junit.naming.methods.IDTestNameGenerationStrategy;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

/*
 * TODO: When asking the orchestrator for patch mutation score, send test data as file location rather than source code.
 */

public class PatchPool {
    private static final Logger logger = LoggerFactory.getLogger(PatchPool.class);

    private final Set<Patch> patches = new LinkedHashSet<>();
    private static PatchPool instance = null;

    public PatchPool() {
        try {
            initPatchPool();
        } catch (IOException e) {
            throw new RuntimeException("Error while receiving patch pool: " + e.getMessage());
        }
    }

    public static PatchPool getInstance() {
        if (instance == null) {
            instance = new PatchPool();
        }
        return instance;
    }

    /**
     * Requests the patch pool from the orchestrator.
     * @throws IOException
     */
    public void initPatchPool() throws IOException {
        if (!patches.isEmpty()) {
            logger.warn("Patch pool not empty.");
        }

        // Request patch pool from orchestrator through comm server
        List<Patch> result = OrchestratorClient.getInstance().sendRequest("getPatchPool", new TypeReference<List<Patch>>() {});

        // Add received patches to the patch pool
        logger.info("Received patch pool from orchestrator of size: " + result.size());
        patches.addAll(result);
    }

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
        String testDir = "evorepair-populations"; // TODO: make configurable
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
        OrchestratorClient.getInstance().sendRequest(msg, new TypeReference<Object>() {});
    }

    public Set<Patch> getPatchPool() {
        return patches;
    }
}
