package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.MethodReturnsPrimitive;
import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.junit.naming.methods.IDTestNameGenerationStrategy;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class PatchValidationSystemTest extends SystemTestBase {
    private static final boolean USE_PYTHON_SERVER = false;

    @Test
    public void testWriteValidationTestSuite() {
        try {
            // Start separate server thread
            if (!USE_PYTHON_SERVER) {
                ServerRunnable sr = new ServerRunnable(7777);
                Thread serverThread = new Thread(sr);
                serverThread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to start server: " + e);
        } finally {
        }

        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        URL resource = this.getClass().getResource("patch_population.json");

        String clazz = "org.apache.commons.math3.distribution.HypergeometricDistribution";
        String projectCP = "/home/lam/workspace/nus-apr/evoRepair/test/math_2_instr/target/classes";
        String base_dir = "/home/lam/workspace/nus-apr/evoRepair/output/math_2_buggy-instr-230111_222410/gen-test/gen1/org.apache.commons.math3.distribution.HypergeometricDistribution";
        String targetLinesFilePath = "/home/lam/workspace/nus-apr/evoRepair/output/math_2_buggy-instr-230111_222410/init_locations.json";
        String port = "59359";
        Properties.JUNIT_SUFFIX = "_gen_2_ESTest";

        //String[] command = new String[] {"-generateMOSuite", "-targetLines", resource.getPath(),"-port", "7777", "-class", targetClass };
        String[] command = new String[] {"-generateMOSuite", "-evorepair", "testgen", "-targetPatches", resource.getPath(), "-port", port, "-class", targetClass,
        "-projectCP", projectCP, "-base_dir", base_dir};

        Properties.ASSERTIONS = false;
        Properties.ALGORITHM = Properties.Algorithm.MOSAPATCH;
        Properties.CRITERION = new Properties.Criterion[]{
                Properties.Criterion.PATCH, Properties.Criterion.PATCHLINE
        };
        Properties.TIMEOUT=100_000;
        Properties.GLOBAL_TIMEOUT=100_000;
        Properties.MOCK_IF_NO_GENERATOR = false;
        // FIXME: Test suite minimization seems to break the number of covered goals in the stats

        // Debugging
        Properties.MINIMIZE = false;
        Properties.EXTRA_TIMEOUT=Integer.MAX_VALUE;
        Properties.MINIMIZATION_TIMEOUT=Integer.MAX_VALUE;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<TestSuiteChromosome> ga = getGAFromResult(result);
        /**
         * MOSATestSuiteAdapter returns the population of TestCaseChromosomes
         * as a singleton list of TestSuiteChromosomes.
         */
        /*
        List<TestSuiteChromosome> testSuites = ga.getPopulation();
        Assert.assertEquals(testSuites.size(), 1);
        TestSuiteChromosome population = testSuites.get(0);

        List<TestCase> tests = population.getTests();
        List<ExecutionResult> results = population.getLastExecutionResults();

        TestSuiteWriter suiteWriter = new TestSuiteWriter();
        suiteWriter.insertAllTests(tests);

        String name = "TestPopulation";
        String testDir = "testWriteValidationTestSuite";
        String suffix = Properties.JUNIT_SUFFIX;

        IDTestNameGenerationStrategy nameGenerator = new IDTestNameGenerationStrategy(tests);
        //List<File> generatedTests = suiteWriter.writeValidationTestSuite(name + suffix, testDir, results, nameGenerator);


         */
    }
}
