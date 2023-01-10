package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.MethodReturnsPrimitive;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.patch.PatchLineCoverageFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

import static org.evosuite.Properties.Criterion.BRANCH;
import static org.evosuite.Properties.Criterion.PATCHLINE;

public class TargetLinesSystemTest extends SystemTestBase {
    @Test
    public void testLoadJSON() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        URL resource = this.getClass().getResource("testTargetLines.json");
        String[] command = new String[] {"-targetLines", resource.getPath(), "-class", targetClass };
        Object result = evosuite.parseCommandLine(command);

        Assert.assertArrayEquals(PatchLineCoverageFactory.getTargetLinesForClass("some.package.name.Class1"), new int[] {1,2,3});
        Assert.assertArrayEquals(PatchLineCoverageFactory.getTargetLinesForClass("some.package.name.Class2"), new int[] {4,5,6,7,8,9});
    }

    @Test
    public void testPatchLineFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        URL resource = this.getClass().getResource("testPatchLineFitness.json");

        String[] command = new String[] {"-generateSuite", "-targetLines", resource.getPath(), "-class", targetClass };
        Properties.ALGORITHM = Properties.Algorithm.MONOTONIC_GA;
        Properties.CRITERION = new Properties.Criterion[]{
                PATCHLINE
        };

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 3, goals);
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);

    }

    @Test
    public void testMOSAPatchLineFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        URL resource = this.getClass().getResource("testPatchLineFitness.json");

        String[] command = new String[] {"-generateMOSuite", "-targetLines", resource.getPath(), "-class", targetClass };
        Properties.ASSERTIONS = false;
        Properties.ALGORITHM = Properties.Algorithm.MOSAPATCH;
        Properties.CRITERION = new Properties.Criterion[]{
                PATCHLINE, BRANCH
        };
        Properties.MINIMIZE = false;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 3, goals);
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);
        LoggingUtils.getEvoLogger().info("hi");

    }
}
