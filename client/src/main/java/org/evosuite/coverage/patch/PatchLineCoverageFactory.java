package org.evosuite.coverage.patch;

import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.communication.json.FixLocation;
import org.evosuite.instrumentation.LinePool;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class PatchLineCoverageFactory extends AbstractFitnessFactory<LineCoverageTestFitness> {

    private static final Logger logger = LoggerFactory.getLogger(PatchLineCoverageFactory.class);

    @Override
    public List<LineCoverageTestFitness> getCoverageGoals() {
        long start = System.currentTimeMillis();

        PatchPool patchPool = PatchPool.getInstance();
        List<LineCoverageTestFitness> goals  = new ArrayList<>();
        for (String c : patchPool.getPatchedClasses()) {
            // Note: searchOuterClass is set to false because the set returned by the PatchPool contains no inner/anonymous classes
            goals.addAll(getCoverageGoals(c, new ArrayList<>(patchPool.getFixLocationsForClass(c, false))));
        }

        goalComputationTime = System.currentTimeMillis() - start;
        return goals;
    }

    public List<LineCoverageTestFitness> getCoverageGoals(List<FixLocation> fixLocations) {
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

        // Also search within inner and anonymouse classes
        for (String actualClassName : LinePool.getKnownClasses()) {
            if (!actualClassName.startsWith(className)) {
                continue;
            } else if (!actualClassName.equals(className) && actualClassName.charAt(actualClassName.indexOf(className) + className.length()) != '$') {
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
