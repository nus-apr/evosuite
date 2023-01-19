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

        URL resource = this.getClass().getResource("patch_population.json");
        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-criterion", "PATCHLINE", "-targetPatches", resource.getPath(), "-class", targetClass };
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

        String[] command = new String[] {"-evorepair", "testgen", "-generateSuite", "-criterion", "PATCHLINE", "-targetPatches", resource.getPath(), "-class", targetClass};
        Properties.ALGORITHM = Properties.Algorithm.MONOTONIC_GA;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .map(TestFitnessFactory::getCoverageGoals)
                .mapToInt(List::size).sum();
        Assert.assertEquals("Wrong number of goals: ", 3, goals);
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);

    }

    @Test
    public void testMOSAPatchLineFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-criterion", "PATCHLINE", "-targetPatches", resource.getPath(), "-class", targetClass };

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 3, goals);

        // MOSATestSuiteAdapter.getBestIndividuals:95 sets the suite fitness to 1.0 for some reason, even if all goals have been covered
        // TODO EvoRepair: investigate
        Assert.assertEquals("Non-optimal fitness: ", 1.0, best.getFitness(), 0.01);
    }
}
