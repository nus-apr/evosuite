package org.evosuite.coverage.patch;

import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.communication.json.FixLocation;
import org.evosuite.instrumentation.LinePool;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PatchLineCoverageFactory extends AbstractFitnessFactory<LineCoverageTestFitness> {
    /**
     * Map class names to arrays of target lines and counts TODO: merge both maps
     */
    private static final Map<String, Set<Integer>> targetLineMap = new LinkedHashMap<>();
    private static final Map<String, Map<Integer, Integer>> targetLineCountMap = new LinkedHashMap<>();

    private static int numPatches = 0;

    private static final Logger logger = LoggerFactory.getLogger(PatchLineCoverageFactory.class);

    public static void setNumPatches(int numPatches) {
        PatchLineCoverageFactory.numPatches = numPatches;
    }

    public static void addTargetLine(String classname, List<Integer> targetLines) {
        if (!targetLineMap.containsKey(classname)) {
            targetLineMap.put(classname, new LinkedHashSet<>(targetLines));

            // Add counts
            assert(!targetLineCountMap.containsKey(classname));
            Map<Integer, Integer> targetLineCounts = new LinkedHashMap<>();
            targetLines.forEach(targetLine -> targetLineCounts.put(targetLine, 1));
            targetLineCountMap.put(classname, targetLineCounts);
        }
        else {
            targetLineMap.get(classname).addAll(targetLines);

            // Add counts
            assert(targetLineCountMap.containsKey(classname));
            Map<Integer, Integer> targetLineCounts = targetLineCountMap.get(classname);
            targetLines.forEach(targetLine -> targetLineCounts.merge(targetLine, 1, Integer::sum));

        }
    }

    public static Set<Integer> getTargetLinesForClass(String classname) {
        return targetLineMap.get(classname);
    }

    public static Map<String, Set<Integer>> getTargetLineMap() {
        return targetLineMap;
    }

    /**
     * Returns the weight of a target line based on the number of patches that target it.
     * @param className containing class of the target line
     * @param targetLine target line number
     * @return the ratio of patches that have applied a change on the given target line
     */
    public static double getTargetLineWeight(String className, Integer targetLine) {
        if (!targetLineCountMap.containsKey(className) || !targetLineCountMap.get(className).containsKey(targetLine)) {
            logger.error("Target line {}:{} not present in targetLineCountMap.", className, targetLine);
            throw new IllegalArgumentException("Invalid key for targetLintCountMap");
        }

        return ((double) targetLineCountMap.get(className).get(targetLine)) / numPatches;
    }

    @Override
    public List<LineCoverageTestFitness> getCoverageGoals() {
        List<LineCoverageTestFitness> goals = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (String className : targetLineMap.keySet()) {
            // Check that target line is actually part of the CUT
            if (!LinePool.getKnownClasses().contains(className)) {
                 logger.debug("Unable to find class in LinePool: " + className);
                 targetLineCountMap.remove(className); // TODO: Remove individual target lines as well?
                 continue;
            }

            if (!isCUT(className)) {
                throw new IllegalArgumentException("Class containing target line is not part of CUT: " + className);
            }

            // TODO: Should we specify the containing method by args?

            Set<Integer> targetLines = targetLineMap.get(className);
            for (String methodName : LinePool.getKnownMethodsFor(className)) {
                Set<Integer> methodLines = LinePool.getLines(className, methodName);

                for (int line : targetLines) {
                    if (methodLines.contains(line)) {
                        logger.info("Found target line " + className + ":" + line + " in  method" + methodName);
                        goals.add(new LineCoverageTestFitness(className, methodName, line));
                    }
                }
            }
        }
        goalComputationTime = System.currentTimeMillis() - start;
        return goals;    }

    public List<TestFitnessFunction> getCoverageGoals(List<FixLocation> fixLocations) {
        List<TestFitnessFunction> goals = new ArrayList<>();

        for (FixLocation fl : fixLocations) {
            String className = fl.getClassname();
            List<Integer> targetLines = fl.getTargetLines();

            if (!LinePool.getKnownClasses().contains(className)) {
                logger.debug("Unable to find class in LinePool: " + className);
                continue;
            }

            if (!isCUT(className)) {
                throw new IllegalArgumentException("Class containing target line is not part of CUT: " + className);
            }

            for (String methodName : LinePool.getKnownMethodsFor(className)) {
                Set<Integer> methodLines = LinePool.getLines(className, methodName);

                for (int line : targetLines) {
                    if (methodLines.contains(line)) {
                        logger.info("Found target line " + className + ":" + line + " in  method" + methodName);
                        goals.add(new LineCoverageTestFitness(className, methodName, line));
                    }
                }
            }
        }

        return goals;
    }
}
