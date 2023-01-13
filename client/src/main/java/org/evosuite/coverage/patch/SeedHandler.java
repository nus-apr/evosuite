package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evosuite.Properties;
import org.evosuite.coverage.patch.communication.json.FixLocation;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.coverage.patch.communication.json.SeedTest;
import org.evosuite.coverage.patch.communication.json.SeedTestPopulation;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SeedHandler {

    private static final Logger logger = LoggerFactory.getLogger(SeedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static SeedHandler instance = null;

    private List<Patch> patchPopulation = null;

    private List<TestChromosome> seedTestPopulation = null;





    public static SeedHandler getInstance() {
        if (instance == null) {
            instance = new SeedHandler();
        }
        return instance;
    }

    public void preload() {
        // This always needs to be provided
        loadPatchPopulation();

        // This does not necessarily need to be provided
        if (Properties.EVOREPAIR_SEED_POPULATION != null) {
            loadSeedTestPopulation();
        }
    }

    // -- SERIALIZATION
    public static void saveTestPopulation(TestSuiteChromosome testSuite) {
        String testDir = Properties.TEST_DIR;

        // First serialize population
        File populationDump = Paths.get(testDir, "dump").toFile();
        logger.info("Test population has been serialized to: {}.", populationDump.getPath());
        TestSuiteSerialization.saveTests(testSuite, populationDump);

        // Then write test names to file
        Path testNamePath = Paths.get(testDir, "test_names.txt");
        String testNamePrefix = Properties.TARGET_CLASS + Properties.JUNIT_SUFFIX + "#test";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testNamePath.toFile()))) {
            for (TestChromosome tc : testSuite.getTestChromosomes()) {
                writer.write(testNamePrefix + tc.getTestCase().getID());
                writer.newLine();
            }
            logger.info("Test names has been written to: {}.", testNamePath);
        } catch (IOException e) {
            logger.error("Error while serializing test population and test names.");
        }

    }


    // -- DESERIALIZATION
    public List<Patch> loadPatchPopulation() {
        if (patchPopulation != null) {
            return patchPopulation;
        }

        try {
            patchPopulation = objectMapper.readValue(new File(Properties.EVOREPAIR_TARGET_PATCHES),
                    new TypeReference<List<Patch>>() {
                    });

            // Add target lines
            for (Patch p: patchPopulation) {
                for (FixLocation f  : p.getFixLocations()) {
                    PatchLineCoverageFactory.addTargetLine(f.getClassname(), f.getTargetLines());
                }
            }

        } catch (IOException e) {
            logger.error("Error while loading target patches.");
            throw new RuntimeException(e);
        }
        logger.info("Read patch pool consisting of {} patches.", patchPopulation.size());

        return patchPopulation;
    }

    // TODO: We should ensure that the seed population has a specific size (since it won't grow).
    public List<TestChromosome> loadSeedTestPopulation() {
        if (seedTestPopulation != null) {
            return seedTestPopulation;
        }

        List<SeedTestPopulation> seedPopulations;
        try {
            seedPopulations = objectMapper.readValue(new File(Properties.EVOREPAIR_SEED_POPULATION),
                    new TypeReference<List<SeedTestPopulation>>() {});
        } catch (IOException e) {
            logger.error("Error while loading seed population.");
            throw new RuntimeException(e);
        }

        // The final seed population chosen from multiple serialized seed populations
        List<TestChromosome> result = new ArrayList<>();

        // The mapping between test-ids to the killed patch-ids
        Map<Integer, Set<String>> killMatrix = new LinkedHashMap<>();

        /**
         * For each seed population file, we filter out the chosen seeds and add them to the final population
         * TODO: If all seeds should be kept, seedsToKeep can be left empty
         */
        for (SeedTestPopulation seedPopulation : seedPopulations) {

            // The tests to filter out from the population
            Set<String> seedsToKeep = seedPopulation.getTests().stream()
                    .map(SeedTest::getName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // Mapping between the original testIds and killed patches (since testIds are not unique between populations)
            // FIXME: Using String maps and then changing to Integer maps for TestIds is confusing
            Map<String, Set<String>> originalSeedToKills = new LinkedHashMap<>();
            seedPopulation.getTests().forEach(seedTest -> originalSeedToKills.put(seedTest.getName(), seedTest.getKills()));

            List<TestChromosome> allSeeds = TestSuiteSerialization.loadTests(seedPopulation.getSerializedSuite());

            for (TestChromosome seed: allSeeds) {
                String testId = seedPopulation.getTestPrefix() + seed.getTestCase().getID(); // TODO: Optimize
                if (seedsToKeep.contains(testId)) {
                    TestChromosome tc = seed.clone(); // Necessary to create fresh and unique testIds
                    result.add(tc);
                    killMatrix.put(tc.getTestCase().getID(), originalSeedToKills.get(testId));
                }
            }
        }

        logger.info("Read kill matrix with {} entries.", killMatrix.keySet().size());
        logger.info("Read test population of size {}.", result.size());

        seedTestPopulation = result;
        PatchCoverageTestFitness.setKillMatrix(killMatrix);

        return seedTestPopulation;
    }
}
