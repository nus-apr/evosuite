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
     * Map class names to arrays of target lines
     */
    private static final Map<String, Set<Integer>> targetLineMap = new LinkedHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(PatchLineCoverageFactory.class);


    public static void addTargetLine(String classname, List<Integer> targetLines) {
        if (!targetLineMap.containsKey(classname)) {
            targetLineMap.put(classname, new LinkedHashSet<>(targetLines));
        }
        else {
            throw new IllegalArgumentException("Duplicate key for patched classes: " + classname);
        }
    }

    public static Set<Integer> getTargetLinesForClass(String classname) {
        return targetLineMap.get(classname);
    }

    public static Map<String, Set<Integer>> getTargetLineMap() {
        return targetLineMap;
    }

    @Override
    public List<LineCoverageTestFitness> getCoverageGoals() {
        List<LineCoverageTestFitness> goals = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (String className : targetLineMap.keySet()) {
            // Check that target line is actually part of the CUT
            if (!LinePool.getKnownClasses().contains(className)) {
                throw new IllegalArgumentException("Unable to find class in LinePool: " + className);
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
                throw new IllegalArgumentException("Unable to find class in LinePool: " + className);
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
