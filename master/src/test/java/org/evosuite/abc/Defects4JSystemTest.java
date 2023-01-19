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
     * CLI command: java -jar master/target/evosuite-master-1.2.0.jar -class org.apache.commons.math3.distribution.HypergeometricDistribution -generateMOSuite -evorepair testgen -targetPatches math2_patches.json -projectCP /home/lam/workspace/nus-apr/evoRepair/test/math_2_instr/target/classes -Dminimize=false -Dassertions=false
     */
    @Test
    public void testMath2() {

        EvoSuite evosuite = new EvoSuite();

        URL resource = this.getClass().getResource("math2_patches.json");

        String targetClass = "org.apache.commons.math3.distribution.HypergeometricDistribution";
        String projectCP = "/home/lam/workspace/nus-apr/evoRepair/test/math_2_instr/target/classes";
        Properties.JUNIT_SUFFIX = "_Debug_ESTest";
        Properties.JUNIT_TESTS = true; // Write test suite to out.
        String[] command = new String[] {"-generateMOSuite", "-evorepair", "testgen", "-targetPatches", resource.getPath(), "-class", targetClass,
        "-projectCP", projectCP};


        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<TestSuiteChromosome> ga = getGAFromResult(result);
    }

    /**
     * FIX: Target line goals for private methods
     * CLI cmd: java -jar master/target/evosuite-master-1.2.0.jar -class org.apache.commons.math.optimization.fitting.GaussianFitter -generateMOSuite -evorepair testgen -targetPatches ../evorepair-experiments/evorepair/C1-evorepair-defects4j-Math-58-6c757b79/output/math_58-math_58-230118_170804/gen-test/target_patches_gen1.json -projectCP ../defects4j-instrumented/instrumented-archives/math_58/target/classes -Dminimize=false -Dassertions=false
     */
    @Test
    public void testMath58() {

        EvoSuite evosuite = new EvoSuite();

        URL resource = this.getClass().getResource("math2_patches.json");

        String targetClass = "org.apache.commons.math.optimization.fitting.GaussianFitter";

        // TODO EvoRepair: Avoid using hardcoded paths
        String targetPatches = "/home/lam/workspace/nus-apr/evorepair-experiments/evorepair/C1-evorepair-defects4j-Math-58-6c757b79/output/math_58-math_58-230118_170804/gen-test/target_patches_gen1.json";
        String projectCP = "/home/lam/workspace/nus-apr/defects4j-instrumented/instrumented-archives/math_58/target/classes";
        //Properties.JUNIT_SUFFIX = "_Debug_ESTest";
        //Properties.JUNIT_TESTS = true; // Write test suite to out.
        String[] command = new String[] {"-generateMOSuite", "-evorepair", "testgen", "-targetPatches", targetPatches, "-class", targetClass,
                "-projectCP", projectCP};

        //This is necessary to cover all fix locations, don't uncomment if using additional goals (e.g., patch mutation or patch coverage)
        //Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXTIME;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<TestSuiteChromosome> ga = getGAFromResult(result);
    }
}
