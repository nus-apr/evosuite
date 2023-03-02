package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.patch.ArithmeticOracleException;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.mutation.StrongPatchMutationTestFitness;
import org.evosuite.coverage.patch.OracleExceptionTestFitness;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class StrongPatchMutationSystemTest extends SystemTestBase {
    @Test
    public void testStrongPatchMutation() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = ArithmeticOracleException.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL targetPatches = this.getClass().getResource("ArithmeticOracleException_targetPatches.json");
        URL oracleLocations = this.getClass().getResource("ArithmeticOracleException_oracleLocations.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-Dalgorithm=MOSA", "-class", targetClass,
                "-criterion", "STRONGMUTATION:ORACLE", "-targetPatches", targetPatches.getPath(), "-oracleLocations", oracleLocations.getPath()};

        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXTIME;
        Properties.SEARCH_BUDGET = 30;

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int patchMutationGoals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size();
        Assert.assertEquals("Wrong number of patch mutation goals: ", 19, patchMutationGoals);

        int oracleExceptionGoals = TestGenerationStrategy.getFitnessFactories().get(1).getCoverageGoals().size();
        Assert.assertEquals("Wrong number of oracle exception goals: ", 2, oracleExceptionGoals);

        // Note: Recall that we have to compute coverage this way because the TestGenerationContext gets resetted (which also clears the archive)
        int coveredPatchMutationGoals = computeCoveredGoalsFromResult(result, StrongPatchMutationTestFitness.class);
        int coveredOracleExceptionGoals = computeCoveredGoalsFromResult(result, OracleExceptionTestFitness.class);

        // All mutants can be killed except for those that replace the division operand (except by %)
        Assert.assertEquals("Non-optimal number of covered patch mutation goals: ", coveredPatchMutationGoals, 16);

        // ArithmeticException can't be triggered because it is only thrown by integer division!
        Assert.assertEquals("Non-optimal number of covered oracle exception goals: ", coveredOracleExceptionGoals, 1);
    }
}
