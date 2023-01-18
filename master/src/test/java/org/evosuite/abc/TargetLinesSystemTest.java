package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.MethodReturnsPrimitive;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.coverage.patch.PatchLineCoverageFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.evosuite.Properties.Criterion.*;

public class TargetLinesSystemTest extends SystemTestBase {
    @Test
    public void testLoadJSON() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        Properties.ALGORITHM = Properties.Algorithm.MOSA;
        Properties.CRITERION = new Properties.Criterion[]{
                PATCHLINE
        };

        URL resource = this.getClass().getResource("patch_population.json");
        String[] command = new String[] {"-generateMOSuite", "-evorepair", "testgen", "-targetPatches", resource.getPath(), "-class", targetClass };
        Object result = evosuite.parseCommandLine(command);

        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList(25,29,42)), PatchLineCoverageFactory.getTargetLinesForClass(targetClass));

        Assert.assertEquals(PatchLineCoverageFactory.getTargetLineWeight(targetClass, 25), 1.0, 0.01);
        Assert.assertEquals(PatchLineCoverageFactory.getTargetLineWeight(targetClass, 29), 2.0/3, 0.01);
        Assert.assertEquals(PatchLineCoverageFactory.getTargetLineWeight(targetClass, 42), 1.0/3, 0.01);
    }

    @Test
    public void testPatchLineFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-generateSuite", "-evorepair", "testgen", "-targetPatches", resource.getPath(), "-class", targetClass};
        Properties.ALGORITHM = Properties.Algorithm.MONOTONIC_GA;
        Properties.CRITERION = new Properties.Criterion[]{
                PATCHLINE
        };

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .map(TestFitnessFactory::getCoverageGoals)
                .mapToInt(List::size).sum();
        Assert.assertEquals("Wrong number of goals: ", 7, goals);
        Assert.assertEquals("Non-optimal fitness: ", 3.0, best.getFitness(), 0.01);

    }

    @Test
    public void testMOSAPatchLineFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-generateMOSuite", "-evorepair", "testgen", "-targetPatches", resource.getPath(), "-class", targetClass };
        Properties.ASSERTIONS = false;
        Properties.ALGORITHM = Properties.Algorithm.MOSA;
        Properties.CRITERION = new Properties.Criterion[]{
                PATCHLINE, PATCH,
        };
        Properties.MINIMIZE = false;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 3, goals);
        Assert.assertEquals("Non-optimal fitness: ", 3.0, best.getFitness(), 0.01); // patches cannot be killed
        LoggingUtils.getEvoLogger().info("hi");

    }
}
