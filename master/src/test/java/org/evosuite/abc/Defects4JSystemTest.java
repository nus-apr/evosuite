package org.evosuite.abc;

import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Test;

import java.net.URL;

import static org.evosuite.Properties.Criterion.*;

public class Defects4JSystemTest extends SystemTestBase {

    @Test
    public void testMath2() {

        EvoSuite evosuite = new EvoSuite();

        URL resource = this.getClass().getResource("patch_population.json");

        String clazz = "org.apache.commons.math3.distribution.HypergeometricDistribution";
        //String clazz = "org.apache.commons.math3.distribution.HypergeometricDistribution";
        String projectCP = "/home/lam/workspace/nus-apr/evoRepair/test/math_2_instr/target/classes";
        Properties.JUNIT_SUFFIX = "_gen_2_ESTest";

        //String[] command = new String[] {"-generateMOSuite", "-targetLines", resource.getPath(),"-port", "7777", "-class", targetClass };
        //String[] command = new String[] {"-generateMOSuite", "-evorepair", "testgen", "-targetPatches", resource.getPath(), "-port", port, "-class", targetClass,
        //"-projectCP", projectCP, "-base_dir", base_dir};

        Properties.ASSERTIONS = false;
        Properties.ALGORITHM = Properties.Algorithm.MOSAPATCH;
        Properties.CRITERION = new Properties.Criterion[]{
                PATCH, PATCHLINE
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
    }
}
