package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.patch.MethodWithOracle;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class OracleLocationGoalsSystemTest extends SystemTestBase {
    @Before
    public void enableAssertions() {
        System.setProperty("defects4j.instrumentation.enabled", "true");
    }

    @After
    public void disableAssertions() {
        System.setProperty("defects4j.instrumentation.enabled", "false");
    }

    @Test
    public void testOracleIBranchWithNSGAII() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodWithOracle.class.getCanonicalName();
        URL oracleLocations = this.getClass().getResource("methodWithOracle_oracleLocations.json");
        URL targetPatches = this.getClass().getResource("methodWithOracle_targetPatches.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateSuite", "-class", targetClass, "-criterion", "PATCHLINE:CONTEXTLINE",
                "-oracleLocations", oracleLocations.getPath(), "-targetPatches",  targetPatches.getPath()};

        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXTIME;
        Properties.SEARCH_BUDGET = 30;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().stream().map(TestFitnessFactory::getCoverageGoals).mapToInt(List::size).sum(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 13, goals);
        // TODO EvoRepair: Investigate why coverage is so low
        Assert.assertEquals("Non-optimal coverage: ", 0.75, best.getCoverage(), 0.1);
    }

    @Test
    public void testOracleIBranchWithMOSA() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodWithOracle.class.getCanonicalName();
        URL oracleLocations = this.getClass().getResource("methodWithOracle_oracleLocations.json");
        URL targetPatches = this.getClass().getResource("methodWithOracle_targetPatches.json");


        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-Dalgorithm", "MOSA", "-class", targetClass, "-criterion", "PATCHLINE:CONTEXTLINE",
                "-oracleLocations", oracleLocations.getPath(), "-targetPatches",  targetPatches.getPath()};
        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXTIME;
        Properties.SEARCH_BUDGET = 20;
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);
        int goals = TestGenerationStrategy.getFitnessFactories().stream().map(TestFitnessFactory::getCoverageGoals).mapToInt(List::size).sum(); // assuming single fitness function
        int coveredGoals = computeCoveredGoalsFromResult(result);
        Assert.assertEquals("Wrong number of goals: ", 13, goals);
        Assert.assertEquals("Non-optimal number of covered goals: ", 9, coveredGoals); // condition in L32 of oracle is always true (or false in bytecode)
    }
}
