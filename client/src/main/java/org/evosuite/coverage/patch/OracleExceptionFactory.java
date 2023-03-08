package org.evosuite.coverage.patch;

import org.evosuite.coverage.patch.communication.OracleLocationPool;
import org.evosuite.coverage.patch.communication.json.OracleLocation;
import org.evosuite.instrumentation.LinePool;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OracleExceptionFactory extends AbstractFitnessFactory<OracleExceptionTestFitness> {

    private static final Logger logger = LoggerFactory.getLogger(OracleExceptionFactory.class);

    // Set of hash codes of all oracle location (thrown custom exception) hash codes
    private static final Set<Integer> oracleLocationHashCodes = new LinkedHashSet<>();

    public static Set<Integer> getOracleLocationHashCodes() {
        return oracleLocationHashCodes;
    }

    @Override
    public List<OracleExceptionTestFitness> getCoverageGoals() {
        long start = System.currentTimeMillis();
        List<OracleExceptionTestFitness> goals  = new ArrayList<>();

        // TODO EvoRepair: Refactor, having this many nested for loops cannot be the way
        Map<String, Map<String, Set<OracleLocation>>> oracleLocations = OracleLocationPool.getInstance().getOracleLocations();
        for (String className : oracleLocations.keySet()) {
            for (String methodName : oracleLocations.get(className).keySet()) {
                for (OracleLocation loc : oracleLocations.get(className).get(methodName)) {
                    for (OracleExceptionTestFitness oracleLocationGoal : getCoverageGoals(className, loc.getCustomExceptionLines())) {
                        goals.add(oracleLocationGoal);
                        oracleLocationHashCodes.add(oracleLocationGoal.hashCode());
                    }
                }
            }
        }

        goalComputationTime = System.currentTimeMillis() - start;
        if (goals.isEmpty()) {
            logger.warn("No oracle exception goals were created, check if the specified target locations actually exist.");
        }
        return goals;

    }

    /**
     * Searches for the target lines within the class + inner anonymous classes and returns the corresponding list of goals
     * @param className class name to search target lines in (including inner and anonymous classes)
     * @param targetLines list of fix locations within specified class
     * @return list of goals constructed from the target class + fix locations
     */
    public List<OracleExceptionTestFitness> getCoverageGoals(String className, List<Integer> targetLines) {

        if (!LinePool.getKnownClasses().contains(className)) {
            logger.debug("Unable to find class in LinePool: " + className);
            return Collections.emptyList();
        }

        if (!isCUT(className)) {
            throw new IllegalArgumentException("Class containing oracle exception line is not part of CUT: " + className);
        }

        List<OracleExceptionTestFitness> goals = new ArrayList<>();

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
                            logger.info("Found oracle exception line {}:{} in method {}.", className, line, methodName);
                        } else {
                            logger.info("Found oracle exception line {}:{} in method {} of inner/anonymous class {}.", className, line, methodName, actualClassName);
                        }
                        goals.add(new OracleExceptionTestFitness(actualClassName, methodName, line));
                        break;
                    }
                }
            }
        }
        return goals;
    }
}
