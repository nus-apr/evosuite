package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.BranchInsideCatchBlock;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

// Tests whether EvoSuite can provide guidance to branches within a catch-block
public class BranchesInsideCatchBlockSystemTest extends SystemTestBase {
    @Test
    public void test() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = BranchInsideCatchBlock.class.getCanonicalName();

        String[] command = new String[] {"-generateSuite", "-class", targetClass, "-criterion", "BRANCH",};
        Properties.ALGORITHM = Properties.Algorithm.NSGAII;
        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXTIME;
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("EvolvedTestSuite:\n" + best);

        int goals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size(); // assuming single fitness function
        Assert.assertEquals("Wrong number of goals: ", 8, goals);
        Assert.assertEquals("Non-optimal fitness: ", 0.0, best.getFitness(), 0.01);
    }
}
