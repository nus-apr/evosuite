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

public class FixLocationMutantsSystemTest extends SystemTestBase {

    @Test
    public void testFixLocationMutationFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateSuite", "-criterion", "STRONGMUTATION", "-targetPatches", resource.getPath(), "-class", targetClass};

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        Assert.assertTrue(checkMutationLocations(MutationPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getMutants()));

        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .map(TestFitnessFactory::getCoverageGoals)
                .mapToInt(List::size).sum();
        Assert.assertEquals("Wrong number of goals: ", 47, goals);

        // No full coverage because there are mutants that can't be killed (3/47, see notes in test below)
        Assert.assertTrue("Non-optimal coverage: " + best.getCoverage(), best.getCoverage() > 0.93);

    }

    @Test
    public void testMOSAFixLocationMutationFitness() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL resource = this.getClass().getResource("patch_population.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-Dalgorithm=MOSA", "-criterion", "STRONGMUTATION", "-targetPatches", resource.getPath(), "-class", targetClass };

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        Assert.assertTrue(checkMutationLocations(MutationPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).getMutants()));

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 47, goals);

        // Note: Recall that we have to compute coverage this way because the TestGenerationContext gets resetted (which also clears the archive)
        int coveredGoals = computeCoveredGoalsFromResult(result);
        /* Note: No full coverage because some mutants cannot be killed, in particular:
         *  1) MethodReturnsPrimitive.testChar(II)C:43 - InsertUnaryOp IINC 1 x (output/branching diff is unsat)
         *  2) MethodReturnsPrimitive.testChar(II)C:43 - InsertUnaryOp IINC -1 y (output/branching diff is unsat)
         *  3) MethodReturnsPrimitive.testChar(II)C:43 - ReplaceComparisonOperator <= -> < (equivalent mutant)
         *  For 3), infection distance is computed as d=abs(x-y). Since x != y (previous condition), d > 0 for all x and y (i.e., state cannot be infected).
         */
        Assert.assertEquals("Non-optimal number of covered goals: ", coveredGoals, 44);
    }

    // Ensures that mutants have only been applied to patch fix locations
    private boolean checkMutationLocations(List<Mutation> mutations) {
        return mutations.stream()
                .allMatch(m -> PatchPool.getInstance().getFixLocationsForClass(m.getClassName(), true).contains(m.getLineNumber()));
    }
}
