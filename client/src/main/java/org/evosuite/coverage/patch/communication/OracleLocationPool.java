package org.evosuite.coverage.patch.communication;

import org.evosuite.coverage.patch.SeedHandler;
import org.evosuite.coverage.patch.communication.json.OracleLocation;
import org.evosuite.instrumentation.LinePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OracleLocationPool {

    private static final Logger logger = LoggerFactory.getLogger(OracleLocationPool.class);

    // Mapping from classNames to methodNames to list of oracle locations
    // TODO EvoRepair: Set should not be necessary, as method name + descriptor should be unique
    private final Map<String, Map<String, Set<OracleLocation>>> oracleLocationMap;

    private static OracleLocationPool instance = null;

    public OracleLocationPool() {
        Map<String, Map<String, Set<OracleLocation>>> tempMap = SeedHandler.getInstance().getOracleLocationsFromFile();
        if (LinePool.getKnownClasses().isEmpty()) {
            logger.warn("LinePool has not been initialized yet, no oracle location goals will be produced!!!");
            oracleLocationMap = Collections.emptyMap();
            return;
        }

        oracleLocationMap = new LinkedHashMap<>();
        // Locations from file are based on class + method + line number, find actual locations using the LinePool
        for (String className : tempMap.keySet()) {
            // Find matching class
            for (String knownClass : LinePool.getKnownClasses()) {
                if (!knownClass.startsWith(className)) {
                    continue;
                } else if (!knownClass.equals(className) && !knownClass.startsWith(className + '$')) {
                    logger.warn("{} has a matching prefix of {} but is not an inner/anonymous class.", knownClass, className);
                    continue;
                }

                // Find matching method
                for (String methodName : tempMap.get(className).keySet()) {
                    for (String knownMethod : LinePool.getKnownMethodsFor(knownClass)) {
                        if (!knownMethod.startsWith(methodName + '(')) { // '(' denotes start of method descriptor
                            continue;
                        }

                        // We have found a matching method name, check if it contains the correct line number
                        // Note: We cannot check against instrumentationLines because it also contains lines w/o bytecode instructions (e.g., brackets)
                        Set<Integer> methodLines = LinePool.getLines(knownClass, knownMethod);
                        for (OracleLocation loc : tempMap.get(className).get(methodName)) {
                            if (methodLines.containsAll(loc.getInstrumentationFlagLines())
                                    && methodLines.containsAll(loc.getCustomExceptionLines())) {
                                logger.info("Matched instrumented method {}.{}:{} with method {}.{}.", className, methodName, loc.getLineNumber(), knownClass, knownMethod);
                                if (!oracleLocationMap.containsKey(knownClass)) {
                                    oracleLocationMap.put(knownClass, new LinkedHashMap<>());
                                }

                                if  (!oracleLocationMap.get(knownClass).containsKey(knownMethod)) {
                                    oracleLocationMap.get(knownClass).put(knownMethod, new LinkedHashSet<>());
                                } else {
                                    logger.warn("Found duplicated entry for method {}.", knownMethod);
                                }

                                oracleLocationMap.get(knownClass).get(knownMethod).add(loc);
                            }
                        }
                    }
                }

            }
        }

    }

    public static OracleLocationPool getInstance() {
        if (instance == null) {
            instance = new OracleLocationPool();
        }
        return instance;
    }

    public Map<String, Map<String, Set<OracleLocation>>> getOracleLocations() {
        return oracleLocationMap;
    }

    public Set<String> getInstrumentedMethodsForClass(String className) {
        if (oracleLocationMap.containsKey(className)) {
            return oracleLocationMap.get(className).keySet();
        } else {
            return Collections.emptySet();
        }
    }

    // Returns the lines that check if the instrumentation has been enabled
    // TODO EvoRepair: Using line numbers for everything assumes that we have 1 statement per line, is this reasonable?
    public Set<Integer> getInstrumentationFlagLinesForMethod(String className, String methodName) {
        if (!oracleLocationMap.containsKey(className) || !oracleLocationMap.get(className).containsKey(methodName)) {
            return Collections.emptySet();
        }

        Set<Integer> lines = new LinkedHashSet<>();
        for (OracleLocation loc : oracleLocationMap.get(className).get(methodName)) {
            lines.addAll(loc.getInstrumentationFlagLines());
        }

        return lines;
    }
}
