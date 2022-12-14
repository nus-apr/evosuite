package org.evosuite.coverage.patch;

import java.util.LinkedHashMap;
import java.util.Map;

public class PatchCoverageFactory {
    /**
     * Map class names to arrays of target lines
     */
    private static final Map<String, int[]> targetLineMap = new LinkedHashMap<>();

    public static void addTargetLine(String classname, int[] targetLines) {
        // TODO: Check if line actually exists (after LinePool as been populated)
        if (!targetLineMap.containsKey(classname)) {
            targetLineMap.put(classname, targetLines);
        }
        else {
            throw new IllegalArgumentException("Duplicate key for patched classes: " + classname);
        }
    }

    public static int[] getTargetLinesForClass(String classname) {
        return targetLineMap.get(classname);
    }
}
