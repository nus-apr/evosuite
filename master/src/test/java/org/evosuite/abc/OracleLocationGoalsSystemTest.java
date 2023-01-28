package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.MethodWithOracleAnnotation;
import org.evosuite.EvoSuite;
import org.evosuite.SystemTestBase;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class OracleLocationGoalsSystemTest extends SystemTestBase {

    @Test
    public void testOracleIBranchWithNSGAII() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodWithOracleAnnotation.class.getCanonicalName();
        URL oracleLocations = this.getClass().getResource("MethodWithOracleAnnotation_oracleLocations.json");
        URL targetPatches = this.getClass().getResource("empty_patch_population.json");


        String[] command = new String[] {"-evorepair", "testgen", "-generateSuite", "-class", targetClass, "-criterion", "IBRANCH",
                "-oracleLocations", oracleLocations.getPath(), "-targetPatches",  targetPatches.getPath()};
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 4, goals);
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);
    }

    @Test
    public void testOracleIBranchWithMOSA() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodWithOracleAnnotation.class.getCanonicalName();
        URL oracleLocations = this.getClass().getResource("MethodWithOracleAnnotation_oracleLocations.json");
        URL targetPatches = this.getClass().getResource("empty_patch_population.json");


        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-class", targetClass, "-criterion", "IBRANCH",
                "-oracleLocations", oracleLocations.getPath(), "-targetPatches",  targetPatches.getPath()};

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 4, goals);
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);
    }
}
