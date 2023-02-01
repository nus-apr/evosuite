package org.evosuite.abc;

import com.examples.with.different.packagename.cbranch.CBranchSimpleExample;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class CBranchSystemTest extends SystemTestBase {

    @Test
    public void test() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = CBranchSimpleExample.class.getCanonicalName();
        URL resource = this.getClass().getResource("patch_population_with_inner_classes.json");

        //String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-criterion", "BRANCH:CBRANCH", "-targetPatches", resource.getPath(), "-class", targetClass };
        String[] command = new String[] {"-generateSuite", "-criterion", "BRANCH:CBRANCH", "-targetPatches", resource.getPath(), "-class", targetClass };
        Properties.ALGORITHM = Properties.Algorithm.MONOTONIC_GA;
        Properties.BRANCH_EVAL = true;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 7, goals);

        // MOSATestSuiteAdapter.getBestIndividuals:95 sets the suite fitness to 1.0 for some reason, even if all goals have been covered
        // TODO EvoRepair: investigate
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);
    }
}
