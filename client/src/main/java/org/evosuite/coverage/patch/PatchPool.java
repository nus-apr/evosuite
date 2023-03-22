package org.evosuite.coverage.patch;

import org.evosuite.Properties;
import org.evosuite.coverage.patch.communication.json.TargetLocation;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.ga.FitnessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class PatchPool {
    private static final Logger logger = LoggerFactory.getLogger(PatchPool.class);

    /**
     * Set of patches (id + fix locations)
     */
    private final Set<Patch> patches = new LinkedHashSet<>();

    /**
     * Map from class names to fix locations to counts
     */
    private final Map<String, Map<Integer, Integer>> fixLocationMap = new LinkedHashMap<>();

    /**
     * Singleton instance
     */
    private static PatchPool instance = null;


    public static PatchPool getInstance() {
        if (instance == null) {
            instance = new PatchPool();
        }
        return instance;
    }

    public PatchPool() {
        if (Properties.EVOREPAIR_TARGET_PATCHES == null) {
            throw new Error("Property EVOREPAIR_TARGET_PATCHES is not set.");
        }

        for (Patch p : SeedHandler.getInstance().loadPatchPopulation()) {
            patches.add(p);

            for (TargetLocation fl : p.getFixLocations()) {

                String classname = fl.getClassname();
                if(!fixLocationMap.containsKey(classname)) {
                    fixLocationMap.put(classname, new LinkedHashMap<>());
                }

                // For new lines, set 1 as the value, otherwise increment count
                Map<Integer, Integer> countMap = fixLocationMap.get(classname);
                fl.getTargetLines().forEach(tl -> countMap.merge(tl, 1, Integer::sum));
            }
        }
    }

    /**
     * Returns the patch pool
     * @return set of patches
     */
    public Set<Patch> getPatches() {
        return patches;
    }

    /**
     * Returns the patched classes (as specified by the -targetPatches file)
     * @return set of classes that were patched
     */
    public Set<String> getPatchedClasses() {
        return fixLocationMap.keySet();
    }

    /**
     * Checks if any patch has fix locations in the class or containing class (for anonymous and inner classes)
     * Note: Assumes that the fix locations are valid (we can't be sure of that before the LinePool is initialized)
     * @param className name of the class
     * @return true, if the class or containing class is present in the fix location map, i.e., at least one patch
     * has fixed a line in that class; false otherwise
     */
    public boolean isPatchedOrInnerClass(String className) {
        for (String patchedClass : fixLocationMap.keySet()) {
            if (className.startsWith(patchedClass)) {
                if (patchedClass.equals(className) || className.startsWith(patchedClass + '$')) {
                    return true;
                } else {
                    logger.warn("Found class {} in fixLocationMap with matching prefix, but it is not a containing class of {}.", patchedClass, className);
                }
            }
        }
        return false;
    }

    /**
     * Returns the fix locations for a class
     * @param className The name of the class to look for
     * @param searchOuterClass Whether to also include target lines (originally) specified for the outer class
     * @return The set of fixed source lines by the patch
     */
    public Set<Integer> getFixLocationsForClass(String className, boolean searchOuterClass) {
        if (!searchOuterClass) {
            if (fixLocationMap.containsKey(className)) {
                return fixLocationMap.get(className).keySet();
            } else {
                logger.warn("No fix locations found for class {}.", className);
                return Collections.emptySet();
            }
        } else {
            Set<Integer> allFixLocations = new LinkedHashSet<>();
            for (String patchedClass : fixLocationMap.keySet()) {
                if (className.startsWith(patchedClass)) {
                    if (patchedClass.equals(className) || className.startsWith(patchedClass + '$')) {
                        allFixLocations.addAll(fixLocationMap.get(patchedClass).keySet());
                    } else {
                        logger.warn("Found class {} in fixLocationMap with matching prefix, but it is not a containing class of {}.", patchedClass, className);
                    }
                }
            }
            return allFixLocations;
        }
    }

    public double getFixLocationWeight(String className, Integer fixLocation) {
        return getFixLocationWeight(className, fixLocation, true);
    }

    /**
     * Returns the weight of a fix location based on the number of patches that target it
     * @param className containing class of the fix location
     * @param fixLocation fix location line number
     * @param searchOuterClass whether to also search the target line in the outer class; set to true if
     *                          className may be an anonymous or inner class
     * @return the ratio of patches that have applied a change on the given fix location
     */
    public double getFixLocationWeight(String className, Integer fixLocation, boolean searchOuterClass) {
        if (!getFixLocationsForClass(className, searchOuterClass).contains(fixLocation)) {
            logger.error("Target line {}:{} not present in fixLocationMap.", className, fixLocation);
            throw new IllegalArgumentException("Invalid key for fixLocationMap");
        }

        // If we don't consider the outer class, we can directly get the value from the map
        if (!searchOuterClass) {
            int patchCount = fixLocationMap.get(className).get(fixLocation); // Number of patches fixing this location
            return FitnessFunction.normalize(patchCount);
        }

        // Find the fix location in the map by searching within possible containing classes
        for (String patchedClass : fixLocationMap.keySet()) {
            if (className.startsWith(patchedClass) && fixLocationMap.get(patchedClass).containsKey(fixLocation)) {
                if (patchedClass.equals(className) || className.startsWith(patchedClass + '$')) {
                    int patchCount = fixLocationMap.get(patchedClass).get(fixLocation); // Number of patches fixing this location
                    return FitnessFunction.normalize(patchCount);
                } else {
                    logger.warn("Found class {} in fixLocationMap with matching prefix, but it is not a containing class of {}.", patchedClass, className);
                }
            }
        }
        logger.error("Unable to find target line {}:{} in fixLocationMap.", className, fixLocation);
        return 0;
    }
}
