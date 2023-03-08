package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.patch.ClassWithInnerClass;
import com.examples.with.different.packagename.coverage.MethodReturnsPrimitive;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.coverage.patch.PatchPool;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;


public class TargetLinesSystemTest extends SystemTestBase {
    @Test
    public void testLoadJSON() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        URL resource = this.getClass().getResource("patch_population.json");
        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-criterion", "FIXLOCATION", "-targetPatches", resource.getPath(), "-class", targetClass };
        Object result = evosuite.parseCommandLine(command);

        PatchPool patchPool = PatchPool.getInstance();

        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList(25, 29, 41, 42, 43)), patchPool.getFixLocationsForClass(targetClass, false));

        Assert.assertEquals(patchPool.getFixLocationWeight(targetClass, 25), 3.0/4, 0.01);
        Assert.assertEquals(patchPool.getFixLocationWeight(targetClass, 29), 2.0/3, 0.01);
        Assert.assertEquals(patchPool.getFixLocationWeight(targetClass, 41), 1.0/2, 0.01);
        Assert.assertEquals(patchPool.getFixLocationWeight(targetClass, 42), 1.0/2, 0.01);
        Assert.assertEquals(patchPool.getFixLocationWeight(targetClass, 43), 1.0/2, 0.01);
    }

    @Test
    public void testFixLocationFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateSuite", "-criterion", "FIXLOCATION", "-targetPatches", resource.getPath(), "-class", targetClass};

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .map(TestFitnessFactory::getCoverageGoals)
                .mapToInt(List::size).sum();
        Assert.assertEquals("Wrong number of goals: ", 5, goals);
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);

    }

    @Test
    public void testMOSAfixLocationFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-criterion", "FIXLOCATION", "-targetPatches", resource.getPath(), "-class", targetClass };

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 5, goals);

        int coveredGoals = computeCoveredGoalsFromResult(result);
        Assert.assertEquals("Non-optimal coverage: ", 5, coveredGoals);
    }

    @Test
    public void testInnerClassTargetLines() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = ClassWithInnerClass.class.getCanonicalName();
        URL resource = this.getClass().getResource("patch_population_with_inner_classes.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-criterion", "FIXLOCATION", "-targetPatches", resource.getPath(), "-class", targetClass };

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 3, goals);

        // MOSATestSuiteAdapter.getBestIndividuals:95 sets the suite fitness to 1.0 for some reason, even if all goals have been covered
        // TODO EvoRepair: investigate
        Assert.assertEquals("Non-optimal fitness: ", 1.0, best.getFitness(), 0.01);
    }
}
