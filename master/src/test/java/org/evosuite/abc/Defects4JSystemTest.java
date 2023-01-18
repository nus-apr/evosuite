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


    /**
     * CLI command: java -jar master/target/evosuite-master-1.2.0.jar -class org.apache.commons.math3.distribution.HypergeometricDistribution
     * -generateMOSuite -evorepair testgen -targetPatches math2_patches.json
     * -projectCP /home/lam/workspace/nus-apr/evoRepair/test/math_2_instr/target/classes -Dminimize=false -Dassertions=false
     */
    @Test
    public void testMath2() {

        EvoSuite evosuite = new EvoSuite();

        URL resource = this.getClass().getResource("math2_patches.json");

        String targetClass = "org.apache.commons.math3.distribution.HypergeometricDistribution";
        String projectCP = "/home/lam/workspace/nus-apr/evoRepair/test/math_2_instr/target/classes";
        //Properties.JUNIT_SUFFIX = "_Debug_ESTest";
        //Properties.JUNIT_TESTS = true; // Write test suite to out.
        String[] command = new String[] {"-generateMOSuite", "-evorepair", "testgen", "-targetPatches", resource.getPath(), "-class", targetClass,
        "-projectCP", projectCP};


        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<TestSuiteChromosome> ga = getGAFromResult(result);
    }
}
