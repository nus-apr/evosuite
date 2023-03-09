package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.patch.ArithmeticOracleException;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.mutation.StrongMutationTestFitness;
import org.evosuite.coverage.mutation.StrongPatchMutationTestFitness;
import org.evosuite.coverage.patch.OracleExceptionTestFitness;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

public class StrongPatchMutationSystemTest extends SystemTestBase {
    private EvoSuite evosuite;
    private String targetClass = ArithmeticOracleException.class.getCanonicalName();
    private URL targetPatches = this.getClass().getResource("ArithmeticOracleException_targetPatches.json");
    private URL oracleLocations = this.getClass().getResource("ArithmeticOracleException_oracleLocations.json");
    private String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-Dalgorithm=DYNAMOSA", "-class", targetClass,
            "-criterion", "STRONGMUTATION:ORACLE:FIXLOCATION", "-targetPatches", targetPatches.getPath(), "-oracleLocations", oracleLocations.getPath()};

    @Before
    public void setup() {
        evosuite = new EvoSuite();
    }


    @Test
    public void testStrongMutation() {
        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXGENERATIONS;
        Properties.SEARCH_BUDGET = 40;

        Object result = evosuite.parseCommandLine(command);

        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        System.out.println("Age: " + ga.getAge());
        //System.out.println("EvolvedTestSuite:\n" + best);

        int strongMutationGoals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size();
        Assert.assertEquals("Wrong number of patch mutation goals: ", 38, strongMutationGoals);

        int oracleExceptionGoals = TestGenerationStrategy.getFitnessFactories().get(1).getCoverageGoals().size();
        Assert.assertEquals("Wrong number of oracle exception goals: ", 2, oracleExceptionGoals);

        // Note: Recall that we have to compute coverage this way because the TestGenerationContext gets resetted (which also clears the archive)
        int coveredStrongMutationGoals = computeCoveredGoalsFromResult(result, StrongMutationTestFitness.class);
        int coveredOracleExceptionGoals = computeCoveredGoalsFromResult(result, OracleExceptionTestFitness.class);

        // All strong mutants can be killed
        Assert.assertEquals("Non-optimal number of covered mutation goals: ", coveredStrongMutationGoals, 31);

        // ArithmeticException can't be triggered because it is only thrown by integer division!
        Assert.assertEquals("Non-optimal number of covered oracle exception goals: ", coveredOracleExceptionGoals, 1);
    }

    @Test
    public void testStrongPatchMutationOnly() {
        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXGENERATIONS;
        Properties.SEARCH_BUDGET = 10;
        Properties.EVOREPAIR_STRONG_MUTATION_GOALS = false;

        Object result = evosuite.parseCommandLine(command);

        //GeneticAlgorithm<?> ga = getGAFromResult(result);
        //TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int patchMutationGoals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size();
        Assert.assertEquals("Wrong number of patch mutation goals: ", 19, patchMutationGoals);

        int oracleExceptionGoals = TestGenerationStrategy.getFitnessFactories().get(1).getCoverageGoals().size();
        Assert.assertEquals("Wrong number of oracle exception goals: ", 2, oracleExceptionGoals);

        // Note: Recall that we have to compute coverage this way because the TestGenerationContext gets resetted (which also clears the archive)
        int coveredStrongPatchMutationGoals = computeCoveredGoalsFromResult(result, StrongPatchMutationTestFitness.class);
        int coveredOracleExceptionGoals = computeCoveredGoalsFromResult(result, OracleExceptionTestFitness.class);

        // All strong patch mutants can be killed except for those that replace the division operand or change constants
        Assert.assertEquals("Non-optimal number of covered strong patch mutation goals: ", coveredStrongPatchMutationGoals, 12);

        // ArithmeticException can't be triggered because it is only thrown by integer division!
        Assert.assertEquals("Non-optimal number of covered oracle exception goals: ", coveredOracleExceptionGoals, 1);
    }

    @Test
    public void testStrongMutationOnly() {
        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXGENERATIONS;
        Properties.SEARCH_BUDGET = 30;
        Properties.EVOREPAIR_STRONG_PATCH_MUTATION_GOALS = false;

        Object result = evosuite.parseCommandLine(command);
        //GeneticAlgorithm<?> ga = getGAFromResult(result);
        //TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int strongMutationGoals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size();
        Assert.assertEquals("Wrong number of patch mutation goals: ", 19, strongMutationGoals);

        int oracleExceptionGoals = TestGenerationStrategy.getFitnessFactories().get(1).getCoverageGoals().size();
        Assert.assertEquals("Wrong number of oracle exception goals: ", 2, oracleExceptionGoals);

        // Note: Recall that we have to compute coverage this way because the TestGenerationContext gets resetted (which also clears the archive)
        int coveredStrongMutationGoals = computeCoveredGoalsFromResult(result, StrongMutationTestFitness.class);
        int coveredOracleExceptionGoals = computeCoveredGoalsFromResult(result, OracleExceptionTestFitness.class);

        // All strong mutants can be killed
        Assert.assertEquals("Non-optimal number of covered mutation goals: ", coveredStrongMutationGoals, 19);

        // ArithmeticException can't be triggered because it is only thrown by integer division!
        Assert.assertEquals("Non-optimal number of covered oracle exception goals: ", coveredOracleExceptionGoals, 1);
    }
}
