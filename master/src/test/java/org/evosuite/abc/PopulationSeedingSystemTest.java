package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.ComplexConstraints;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteSerialization;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.evosuite.Properties.Criterion.*;

public class PopulationSeedingSystemTest extends SystemTestBase {

    private EvoSuite evosuite;
    String targetClass = ComplexConstraints.class.getCanonicalName();

    @Before
    public void setup() {
        evosuite = new EvoSuite();
        Properties.ASSERTIONS = false;
        Properties.ALGORITHM = Properties.Algorithm.MOSA;
        Properties.CRITERION = new Properties.Criterion[]{
                BRANCH, LINE
        };
    }

    @Test
    public void testWriteSeeds() {
        String[] command = new String[] {"-generateMOSuite", "-class", targetClass };
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);

        // EvoSuite is deterministic, should stop at exactly 18 generations
        Assert.assertEquals(18, ga.getAge());
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();

        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                        .mapToInt(f -> f.getCoverageGoals().size()).sum();
        Assert.assertEquals("Wrong number of goals: ", 12, goals);
        Assert.assertEquals("Non-optimal coverage: ", 12, best.getCoveredGoals().size(), 0.01);

        File populationFile = new File("src/test/resources/org/evosuite/abc/population.tmp");
        TestSuiteSerialization.saveTests(best, populationFile);
        System.out.println("-----------");
        System.out.println(best);
        System.out.println("-----------");

    }

    @Test
    public void testLoadSeeds() {
        String[] command = new String[] {"-generateMOSuite", "-class", targetClass,
                "-Dseed_population=src/test/resources/org/evosuite/abc/population.tmp"};

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);

        // Since we use a full-coverage suite as seed, evolution should stop right after population initizalization
        Assert.assertEquals(0, ga.getAge());
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .mapToInt(f -> f.getCoverageGoals().size()).sum();
        Assert.assertEquals("Wrong number of goals: ", 12, goals);
        Assert.assertEquals("Non-optimal coverage: ", 12, best.getCoveredGoals().size(), 0.01);
        System.out.println(best);
    }
}
