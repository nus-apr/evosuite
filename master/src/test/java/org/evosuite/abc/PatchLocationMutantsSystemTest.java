package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.MethodReturnsPrimitive;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.coverage.mutation.Mutation;
import org.evosuite.coverage.mutation.MutationPool;
import org.evosuite.coverage.patch.PatchPool;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.List;

public class PatchLocationMutantsSystemTest extends SystemTestBase {

    @Test
    public void testPatchMutationFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateSuite", "-criterion", "STRONGMUTATION", "-targetPatches", resource.getPath(), "-class", targetClass};
        Properties.ALGORITHM = Properties.Algorithm.MONOTONIC_GA;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        Assert.assertTrue(checkMutationLocations(MutationPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getMutants()));

        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .map(TestFitnessFactory::getCoverageGoals)
                .mapToInt(List::size).sum();
        Assert.assertEquals("Wrong number of goals: ", 25, goals);
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);

    }

    @Test
    public void testMOSAPatchMutationFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-criterion", "STRONGMUTATION", "-targetPatches", resource.getPath(), "-class", targetClass };

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        Assert.assertTrue(checkMutationLocations(MutationPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getMutants()));

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function

        Assert.assertEquals("Wrong number of goals: ", 25, goals);

        // MOSATestSuiteAdapter.getBestIndividuals:95 sets the suite fitness to 1.0 for some reason, even if all goals have been covered
        // TODO EvoRepair: investigate
        Assert.assertEquals("Non-optimal fitness: ", 1.0, best.getFitness(), 0.01);
    }

    // Ensures that mutants have only been applied to patch fix locations
    private boolean checkMutationLocations(List<Mutation> mutations) {
        return mutations.stream()
                .allMatch(m -> PatchPool.getInstance().getFixLocationsForClass(m.getClassName(), true).contains(m.getLineNumber()));
    }
}
