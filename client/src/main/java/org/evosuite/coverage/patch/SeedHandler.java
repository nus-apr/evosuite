package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evosuite.Properties;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.communication.json.*;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteSerialization;
import org.evosuite.utils.DebuggingObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SeedHandler {

    private static final Logger logger = LoggerFactory.getLogger(SeedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static SeedHandler instance = null;

    private List<TestChromosome> seedTestPopulation = null;

    private final Set<TestChromosome> seedTests = new LinkedHashSet<>();

    public static SeedHandler getInstance() {
        if (instance == null) {
            instance = new SeedHandler();
        }
        return instance;
    }

    // -- SERIALIZATION

    /**
     * Serialize the final test suite to disk
     * @param testSuite the actual test suite to serialize (does not contain uncompilable tests or prefix tests)
     */
    public void saveTestPopulation(TestSuiteChromosome testSuite) {
        String outputDir = Properties.CLIENT_ON_THREAD ? Properties.BASE_DIR + File.separator + Properties.TEST_DIR : Properties.TEST_DIR;

        // First serialize population
        File populationDump = Paths.get(outputDir, "dump").toFile();
        logger.info("Test population has been serialized to: {}.", populationDump.getPath());
        TestSuiteSerialization.saveTests(testSuite, populationDump);

        // Then write test names to file
        Path testNamePath = Paths.get(outputDir, "test_names.txt");
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

    public void saveTargetLineSolutions(File outputFile) {
        if (Properties.ARCHIVE_TYPE != Properties.ArchiveType.MULTI_CRITERIA_COVERAGE) {
            logger.warn("No target line solutions to serialize because the archive type is not MULTI_CRITERIA_COVERAGE: {}", Properties.ARCHIVE_TYPE);
            return;
        }

        Map<LineCoverageTestFitness, Map<Set<Integer>, TestChromosome>> fixLocationSolutions = Archive.getMultiCriteriaArchive().getFixLocationSolutionMap();

        // Check if dir exists, if not create
        File parent = outputFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        try (ObjectOutputStream out = new DebuggingObjectOutputStream(new FileOutputStream(outputFile))) {
            out.writeObject(fixLocationSolutions);
            out.flush();
            logger.info("{} fix location solutions have been serialized to: {}.", fixLocationSolutions.keySet().size(), outputFile.getPath());
        } catch (IOException e) {
            logger.error("Failed to open/handle " + outputFile.getAbsolutePath() + " for writing: " + e.getMessage());
        }
    }


    // -- DESERIALIZATION
    public List<Patch> loadPatchPopulation() {
        try {
            List<Patch> patchPopulation = objectMapper.readValue(new File(Properties.EVOREPAIR_TARGET_PATCHES),
                    new TypeReference<List<Patch>>() {
                    });
            logger.info("Read patch pool consisting of {} patches.", patchPopulation.size());
            return patchPopulation;

        } catch (IOException e) {
            logger.error("Error while loading target patches.");
            throw new RuntimeException(e);
        }
    }

    public Map<String, Map<String, Set<OracleLocation>>> getOracleLocationsFromFile() {

        if (Properties.EVOREPAIR_ORACLE_LOCATIONS == null) {
            return Collections.emptyMap();
        }

        try {
             Map<String, Map<String, Set<OracleLocation>>> oracleLocationMap = new LinkedHashMap<>();
            List<OracleLocation> oracleLocations = objectMapper.readValue(new File(Properties.EVOREPAIR_ORACLE_LOCATIONS),
                    new TypeReference<List<OracleLocation>>() {
                    });
            logger.info("Specified {} oracle locations.", oracleLocations.size());

            for (OracleLocation loc : oracleLocations) {
                String className = loc.getClassName();
                String methodName = loc.getMethodName();
                if (!oracleLocationMap.containsKey(className)) {
                    oracleLocationMap.put(className, new LinkedHashMap<>());
                }

                if  (!oracleLocationMap.get(className).containsKey(methodName)) {
                    oracleLocationMap.get(className).put(methodName, new LinkedHashSet<>());
                }

                oracleLocationMap.get(className).get(methodName).add(loc);
            }

            return oracleLocationMap;

        } catch (IOException e) {
            logger.error("Error while loading oracle locations.");
            throw new RuntimeException(e);
        }
    }

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

        if (seedPopulations.isEmpty()) {
            return result;
        }

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
                    .map(s -> s.substring(0, s.indexOf("#")))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // NOTE: Uncommented for now as this is not used
            // Mapping between the original testIds and killed patches (since testIds are not unique between populations)
            // FIXME: Using String maps and then changing to Integer maps for TestIds is confusing
            //Map<String, Set<String>> originalSeedToKills = new LinkedHashMap<>();
            //seedPopulation.getTests().forEach(seedTest -> originalSeedToKills.put(seedTest.getName().substring(0, seedTest.getName().indexOf("#")), seedTest.getKills()));

            List<TestChromosome> allSeeds = TestSuiteSerialization.loadTests(seedPopulation.getSerializedSuite());

            for (TestChromosome seed: allSeeds) {
                String testId = "test" + seed.getTestCase().getID(); // FIXME: Optimize, do we need the testPrefix?
                if (seedsToKeep.contains(testId)) {
                    TestChromosome tc = seed.clone(); // Necessary to create fresh and unique testIds
                    result.add(tc);
                    //killMatrix.put(tc.getTestCase().getID(), originalSeedToKills.get(testId));
                }
            }
        }

        //logger.info("Read kill matrix with {} entries.", killMatrix.keySet().size());
        logger.info("Read test population of size {}.", result.size());

        seedTestPopulation = result;
        //PatchCoverageTestFitness.setKillMatrix(killMatrix);
        seedTests.addAll(seedTestPopulation);
        return seedTestPopulation;
    }

    public Set<TestChromosome> getSeedTests() {
        return seedTests;
    }

    public List<TestChromosome> loadTargetLineSolutions() {
        logger.info("Loading target line solutions map from file: {}", Properties.EVOREPAIR_TARGET_LINE_SOLUTIONS);
        File inputFile = new File(Properties.EVOREPAIR_TARGET_LINE_SOLUTIONS);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(inputFile))) {
            try {
                Object obj = in.readObject();
                if (obj == null) {
                    logger.error("Unable to read target line solutions map from file: {}", Properties.EVOREPAIR_TARGET_LINE_SOLUTIONS);
                    return Collections.emptyList();
                }
                if (obj instanceof Map) {
                    List<TestChromosome> seedSolutions = new ArrayList<>();
                    // TODO EvoRepair: Only add solutions for current target lines
                    for (Map<Set<Integer>, TestChromosome> traceMap : ((Map<LineCoverageTestFitness, Map<Set<Integer>, TestChromosome>>) obj).values()) {
                        seedSolutions.addAll(traceMap.values());
                    }
                    return seedSolutions;
                } else {
                    logger.error("Deserialized object is not a map: {}", obj.getClass());
                    return Collections.emptyList();
                }
            } catch (EOFException e) {
                //fine
            } catch (Exception e) {
                logger.error("Problems when reading a serialized test from " + inputFile.getAbsolutePath() + " : " + e.getMessage());
            }
        } catch (FileNotFoundException e) {
            logger.error("Cannot load target line solutions because file does not exist: " + inputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to open/handle " + inputFile.getAbsolutePath() + " for reading: " + e.getMessage());
        }
        return Collections.emptyList();
    }
}
