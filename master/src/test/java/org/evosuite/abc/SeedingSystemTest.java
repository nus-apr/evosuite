package org.evosuite.abc;

import com.examples.with.different.packagename.coverage.patch.ComplexConstraints;
import com.examples.with.different.packagename.coverage.MethodReturnsPrimitive;
import org.evosuite.EvoSuite;
import org.evosuite.Properties;
import org.evosuite.SystemTestBase;
import org.evosuite.coverage.patch.PatchCoverageTestFitness;
import org.evosuite.coverage.patch.SeedHandler;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.strategy.TestGenerationStrategy;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteSerialization;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.evosuite.Properties.Criterion.*;

public class SeedingSystemTest extends SystemTestBase {

    private EvoSuite evosuite;
    String targetClass = ComplexConstraints.class.getCanonicalName();
    String[] command = new String[] {"-generateMOSuite", "-class", targetClass};


    @Before
    public void setup() {
        evosuite = new EvoSuite();
        Properties.ASSERTIONS = false;
        Properties.ALGORITHM = Properties.Algorithm.MOSA;
        Properties.CRITERION = new Properties.Criterion[]{
                BRANCH, LINE
        };
        Properties.SEARCH_BUDGET = 20;
        Properties.STOPPING_CONDITION = Properties.StoppingCondition.MAXTIME;
    }

    // TODO: The number of generations depends on the performance of the current implementation
    //       Also, this test should write out the seeds-file json for the other tests.
    @Test
    public void testWriteSeeds() throws IOException {
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);

        // EvoSuite is deterministic, should stop at exactly 18 generations
        Assert.assertEquals(19, ga.getAge());
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();

        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                        .mapToInt(f -> f.getCoverageGoals().size()).sum();
        Assert.assertEquals("Wrong number of goals: ", 12, goals);
        Assert.assertEquals("Non-optimal coverage: ", 12, best.getCoveredGoals().size(), 0.01);

        File populationFile = new File("src/test/resources/org/evosuite/abc/population.tmp");
        TestSuiteSerialization.saveTests(best, populationFile);
        System.out.println("-----------");
        System.out.println(best);
        System.out.println("-----------");

        Path testNamePath = Paths.get("src/test/resources/org/evosuite/abc/population_names.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testNamePath.toFile()))) {
            for (TestChromosome tc : best.getTestChromosomes()) {
                writer.write("test" + tc.getTestCase().getID());
                writer.newLine();
            }
        }
    }

    // Note: If this test fails (wrong number of generations), make sure that the names of the seed tests
    //  in the seeds_all.json file match the names in population_names.txt produced by the previous test
    @Test
    public void testLoadSeeds() {
        Properties.EVOREPAIR_SEED_POPULATION = "src/test/resources/org/evosuite/abc/seeds_all.json";
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);

        // Since we use a full-coverage suite as seed, evolution should stop right after population initizalization
        Assert.assertEquals(0, ga.getAge());
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .mapToInt(f -> f.getCoverageGoals().size()).sum();
        Assert.assertEquals("Wrong number of goals: ", 12, goals);
        Assert.assertEquals("Non-optimal coverage: ", 12, best.getCoveredGoals().size(), 0.01);
        System.out.println(best);
    }

    // Note: Similar issue as for the above test: test names may change
    @Test
    public void testLoadSeedsFromJSON() {
        Properties.EVOREPAIR_SEED_POPULATION = "src/test/resources/org/evosuite/abc/seeds_subset.json";
        List<TestChromosome> population = SeedHandler.getInstance().loadSeedTestPopulation();
        Assert.assertEquals(population.size(), 2);

        Map<Integer, Set<String>> expectedKillmatrix = new LinkedHashMap<>();

        // Recall that the tests have been assigned new ids after deserialization
        expectedKillmatrix.put(2, new LinkedHashSet<>(Arrays.asList("patch1", "patch2", "patch3")));
        expectedKillmatrix.put(5, new LinkedHashSet<>(Arrays.asList("patch1")));

        Map<Integer, Set<String>> actualKillMatrix = PatchCoverageTestFitness.getKillMatrix();
        Assert.assertEquals(expectedKillmatrix,  actualKillMatrix);
    }

    @Test
    public void testLoadSeedsFromEmptyJSON() {
        Properties.EVOREPAIR_SEED_POPULATION = "src/test/resources/org/evosuite/abc/seeds_empty.json";
        List<TestChromosome> population = SeedHandler.getInstance().loadSeedTestPopulation();
        Assert.assertEquals(population.size(), 0);
        Assert.assertTrue(PatchCoverageTestFitness.getKillMatrix().isEmpty());
    }

    @Test
    public void testLoadGoalsFromJSON() {
        targetClass = MethodReturnsPrimitive.class.getCanonicalName();
        String patchFile = "src/test/resources/org/evosuite/abc/patch_population.json";
        command = new String[] {"-generateMOSuite", "-evorepair", "testgen", "-Dalgorithm=MOSA", "-criterion", "FIXLOCATION:PATCH", "-targetPatches", patchFile, "-class", targetClass};
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);
        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();


        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .mapToInt(f -> f.getCoverageGoals().size()).sum();
        Assert.assertEquals("Wrong number of goals: ", 8, goals); // 3 patches  + 5 target lines
        Assert.assertEquals("Non-optimal coverage: ", 5, best.getCoveredGoals().size());
    }

    @Test
    public void testSaveTargetLineSolutions() {
        Properties.CRITERION = new Properties.Criterion[]{LINE};
        Properties.ARCHIVE_TYPE = Properties.ArchiveType.MULTI_CRITERIA_COVERAGE;
        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);

        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .mapToInt(f -> f.getCoverageGoals().size()).sum();
        Assert.assertEquals("Wrong number of goals: ", 5, goals);
        Assert.assertEquals("Non-optimal coverage: ", 5, best.getCoveredGoals().size());

        File outputFile = new File("src/test/resources/org/evosuite/abc/targetline_solutions.tmp");
        SeedHandler.getInstance().saveTargetLineSolutions(outputFile);
    }

    @Test
    public void testLoadTargetLineSolutions() {
        Properties.CRITERION = new Properties.Criterion[]{LINE};
        Properties.ARCHIVE_TYPE = Properties.ArchiveType.MULTI_CRITERIA_COVERAGE;
        Properties.EVOREPAIR_TARGET_LINE_SOLUTIONS = "src/test/resources/org/evosuite/abc/targetline_solutions.tmp";

        Object result = evosuite.parseCommandLine(command);
        GeneticAlgorithm<?> ga = getGAFromResult(result);

        // Seed solutions should already cover all targets
        Assert.assertEquals(0, ga.getAge());


        TestSuiteChromosome best = (TestSuiteChromosome) ga.getBestIndividual();
        int goals = TestGenerationStrategy.getFitnessFactories().stream()
                .mapToInt(f -> f.getCoverageGoals().size()).sum();
        Assert.assertEquals("Wrong number of goals: ", 5, goals);
        Assert.assertEquals("Non-optimal coverage: ", 5, best.getCoveredGoals().size());
    }
}
