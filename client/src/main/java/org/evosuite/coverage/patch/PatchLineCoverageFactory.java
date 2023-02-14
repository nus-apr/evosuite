package org.evosuite.coverage.patch;

import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.communication.OracleLocationPool;
import org.evosuite.coverage.patch.communication.json.OracleLocation;
import org.evosuite.coverage.patch.communication.json.TargetLocation;
import org.evosuite.instrumentation.LinePool;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class PatchLineCoverageFactory extends AbstractFitnessFactory<LineCoverageTestFitness> {

    private static final Logger logger = LoggerFactory.getLogger(PatchLineCoverageFactory.class);

    // Set of hash codes of all fix location goals
    private static final Set<Integer> fixLocationHashCodes = new LinkedHashSet<>();

    // Set of hash codes of all oracle location (thrown custom exception) hash codes
    private static final Set<Integer> oracleLocationHashCodes = new LinkedHashSet<>();

    private static final Set<Integer> fixLocations = new LinkedHashSet<>();

    @Override
    public List<LineCoverageTestFitness> getCoverageGoals() {
        long start = System.currentTimeMillis();

        PatchPool patchPool = PatchPool.getInstance();
        List<LineCoverageTestFitness> goals  = new ArrayList<>();
        for (String c : patchPool.getPatchedClasses()) {
            // Note: searchOuterClass is set to false because the set returned by the PatchPool contains no inner/anonymous classes
            for (LineCoverageTestFitness fixLocationGoal : getCoverageGoals(c, new ArrayList<>(patchPool.getFixLocationsForClass(c, false)))) {
                goals.add(fixLocationGoal);
                fixLocations.add(fixLocationGoal.getLine());
                fixLocationHashCodes.add(fixLocationGoal.hashCode());
            }
        }

        // Add goals for custom exceptions thrown by instrumented methods
        // TODO EvoRepair: Refactor, having this many nested for loops cannot be the way
        Map<String, Map<String, Set<OracleLocation>>> oracleLocations = OracleLocationPool.getInstance().getOracleLocations();
        for (String className : oracleLocations.keySet()) {
            for (String methodName : oracleLocations.get(className).keySet()) {
                for (OracleLocation loc : oracleLocations.get(className).get(methodName)) {
                    for (LineCoverageTestFitness oracleLocationGoal : getCoverageGoals(className, loc.getCustomExceptionLines())) {
                        goals.add(oracleLocationGoal);
                        oracleLocationHashCodes.add(oracleLocationGoal.hashCode());
                    }
                }
            }
        }
        goalComputationTime = System.currentTimeMillis() - start;
        if (goals.isEmpty()) {
            logger.warn("No PatchLineGoals were created, check if the specified target locations actually exist.");
        }
        return goals;
    }

    public static Set<Integer> getFixLocationHashCodes() {
        return fixLocationHashCodes;
    }

    public static Set<Integer> getOracleLocationHashCodes() {
        return oracleLocationHashCodes;
    }

    public static Set<Integer> getFixLocations() {
        return fixLocations;
    }

    public List<LineCoverageTestFitness> getCoverageGoals(List<TargetLocation> fixLocations) {
        return fixLocations.stream()
                .map(fl -> getCoverageGoals(fl.getClassname(), fl.getTargetLines()))
                .flatMap(List::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Searches for the target lines within the class + inner anonymous classes and returns the corresponding list of goals
     * @param className class name to search target lines in (including inner and anonymous classes)
     * @param targetLines list of fix locations within specified class
     * @return list of goals constructed from the target class + fix locations
     */
    public List<LineCoverageTestFitness> getCoverageGoals(String className, List<Integer> targetLines) {

        if (!LinePool.getKnownClasses().contains(className)) {
            logger.debug("Unable to find class in LinePool: " + className);
            return Collections.emptyList();
        }

        if (!isCUT(className)) {
            throw new IllegalArgumentException("Class containing target line is not part of CUT: " + className);
        }

        List<LineCoverageTestFitness> goals = new ArrayList<>();

        // Also search within inner and anonymous classes
        for (String actualClassName : LinePool.getKnownClasses()) {
            if (!actualClassName.startsWith(className)) {
                continue;
            } else if (!actualClassName.equals(className) && !actualClassName.startsWith(className + '$')) {
                logger.warn("{} has a matching prefix of {} but is not an inner/anonymous class.", actualClassName, className);
                continue;
            }

            for (int line : targetLines) {
                for (String methodName : LinePool.getKnownMethodsFor(actualClassName)) {
                    if (LinePool.getLines(actualClassName, methodName).contains(line)) {
                        if (actualClassName.equals(className)) {
                            logger.info("Found target line {}:{} in method {}.", className, line, methodName);
                        } else {
                            logger.info("Found target line {}:{} in method {} of inner/anonymous class {}.", className, line, methodName, actualClassName);
                        }
                        goals.add(new LineCoverageTestFitness(actualClassName, methodName, line));
                        break;
                    }
                }
            }
        }
        return goals;
    }
}
