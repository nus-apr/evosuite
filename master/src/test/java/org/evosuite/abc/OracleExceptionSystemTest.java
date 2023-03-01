package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.patch.MethodWithOracle;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.patch.ContextLineTestFitness;
import org.evosuite.coverage.patch.OracleExceptionTestFitness;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class OracleExceptionSystemTest extends SystemTestBase {

    @Test
    public void testOracleException() {
        EvoSuite evosuite = new EvoSuite();
        String targetClass = MethodWithOracle.class.getCanonicalName();
        Properties.TARGET_CLASS = targetClass;
        URL targetPatches = this.getClass().getResource("methodWithOracle_targetPatches.json");
        URL oracleLocations = this.getClass().getResource("methodWithOracle_oracleLocations.json");

        String[] command = new String[] {"-evorepair", "testgen", "-generateMOSuite", "-criterion", "ORACLE:CONTEXTLINE",
                "-targetPatches", targetPatches.getPath(),
                "-oracleLocations", oracleLocations.getPath(), "-class", targetClass };

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        //System.out.println("EvolvedTestSuite:\n" + best);

        int oracleGoals = TestGenerationStrategy.getFitnessFactories().get(0).getCoverageGoals().size();
        int oracleContextGoals = TestGenerationStrategy.getFitnessFactories().get(1).getCoverageGoals().size();


        Assert.assertEquals("Wrong number of oracle goals: ", 1, oracleGoals);
        Assert.assertEquals("Wrong number of oracle context goals: ", 4, oracleContextGoals);

        int coveredOracleGoals = computeCoveredGoalsFromResult(result, OracleExceptionTestFitness.class);
        int coveredOracleContextGoals = computeCoveredGoalsFromResult(result, ContextLineTestFitness.class);

        Assert.assertEquals("Non-optimal coverage: ", 1, coveredOracleGoals);
        Assert.assertEquals("Non-optimal coverage: ", 4, coveredOracleContextGoals);

    }
}
