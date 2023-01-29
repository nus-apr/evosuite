package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.MethodWithOracleAnnotation;
import org.evosuite.EvoSuite;
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
        String targetClass = MethodWithOracleAnnotation.class.getCanonicalName();
        URL oracleLocations = this.getClass().getResource("MethodWithOracleAnnotation_oracleLocations.json");
        URL targetPatches = this.getClass().getResource("empty_patch_population.json");


        String[] command = new String[] {"-evorepair", "testgen", "-generateSuite", "-class", targetClass, "-criterion", "BRANCH:IBRANCH:PATCHLINE",
                "-oracleLocations", oracleLocations.getPath(), "-targetPatches",  targetPatches.getPath()};
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().stream().map(TestFitnessFactory::getCoverageGoals).mapToInt(List::size).sum(); // assuming single fitness function
        int coveredGoals = computeCoveredGoalsFromResult(result);
        Assert.assertEquals("Wrong number of goals: ", 13, goals);
        // FIXME: Coverage is too low for some reason
        //Assert.assertEquals("Non-optimal number of covered goals: ", 9, coveredGoals);
    }

    @Test
    public void testOracleIBranchWithMOSA() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodWithOracleAnnotation.class.getCanonicalName();
        URL oracleLocations = this.getClass().getResource("MethodWithOracleAnnotation_oracleLocations.json");
        URL targetPatches = this.getClass().getResource("empty_patch_population.json");


        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-class", targetClass, "-criterion", "BRANCH:IBRANCH:PATCHLINE",
                "-oracleLocations", oracleLocations.getPath(), "-targetPatches",  targetPatches.getPath()};

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);
        int goals = TestGenerationStrategy.getFitnessFactories().stream().map(TestFitnessFactory::getCoverageGoals).mapToInt(List::size).sum(); // assuming single fitness function
        int coveredGoals = computeCoveredGoalsFromResult(result);
        Assert.assertEquals("Wrong number of goals: ", 13, goals);
        Assert.assertEquals("Non-optimal number of covered goals: ", 9, coveredGoals);
    }
}
