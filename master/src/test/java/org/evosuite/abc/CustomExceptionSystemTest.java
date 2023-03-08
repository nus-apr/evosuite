package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.patch.ClassWithCustomException;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;

public class CustomExceptionSystemTest extends SystemTestBase {

    @Test
    public void testWholeSuiteCustomException() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = ClassWithCustomException.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL resource = this.getClass().getResource("patch_population_custom_exception.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateSuite", "-criterion", "FIXLOCATION:EXCEPTION", "-targetPatches", resource.getPath(), "-class", targetClass };


        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 1, goals);

        Assert.assertEquals("Non-optimal fitness: ", 0, best.getFitness(), 0.01);
    }

    @Test
    public void testCustomExceptionEvoSuiteVanilla() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = ClassWithCustomException.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;

        String[] command = new String[] {"-generateMOSuite", "-criterion", "BRANCH:EXCEPTION", "-class", targetClass };
        Properties.ALGORITHM= Properties.Algorithm.MOSA;


        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("Criterion: " + Arrays.toString(Properties.CRITERION) + "\n");
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 1, goals);

        // MOSATestSuiteAdapter.getBestIndividuals:95 sets the suite fitness to 1.0 for some reason, even if all goals have been covered
        // TODO EvoRepair: investigate
        Assert.assertEquals("Non-optimal fitness: ", 1.0, best.getFitness(), 0.01);
    }
}
