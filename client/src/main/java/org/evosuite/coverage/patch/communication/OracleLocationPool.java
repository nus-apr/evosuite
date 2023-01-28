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
    // Note: Within a class, there can be multiple annotated methods with the same name (but with different signatures)
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
                } else if (!knownClass.equals(className) && knownClass.charAt(knownClass.indexOf(className) + className.length()) != '$') {
                    logger.warn("{} has a matching prefix of {} but is not an inner/anonymous class.", knownClass, className);
                    continue;
                }

                // Find matching method
                for (String methodName : tempMap.get(className).keySet()) {
                    for (String knownMethod : LinePool.getKnownMethodsFor(knownClass)) {
                        if (!knownMethod.startsWith(methodName)) { // knownMethod is methodName + descriptor
                            continue;
                        }

                        // We have found a matching methodname, check if it contains the correct line number
                        for (OracleLocation loc : tempMap.get(className).get(methodName)) {
                            // TODO EvoRepair: The method signature does not have its own instruction/line number
                            // FIXME: assumes that the first line of code is not written on the same line, which cannot be guaranteed
                            //        we can probably use the customExceptionLineNumber instead
                            int lineNumber = loc.getLineNumber() + 1;
                            int customExceptionLineNumber = loc.getCustomExceptionLines().get(0);
                            if (LinePool.getLines(knownClass, knownMethod).contains(lineNumber)
                            || LinePool.getLines(knownClass, knownMethod).contains(customExceptionLineNumber)) {
                                logger.info("Matched instrumented method {}.{}:{} to method {}.{}.", className, methodName, lineNumber, knownClass, knownMethod);
                                if (!oracleLocationMap.containsKey(knownClass)) {
                                    oracleLocationMap.put(knownClass, new LinkedHashMap<>());
                                }

                                if  (!oracleLocationMap.get(knownClass).containsKey(knownMethod)) {
                                    oracleLocationMap.get(knownClass).put(knownMethod, new LinkedHashSet<>());
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

    // Returns a key with a matching prefix from the map or null, if no such key exists
    /*
    public String getClassPrefixKeyOrNull(String className) {
        for (String prefixKey : oracleLocationMap.keySet()) {
            if (className.startsWith(prefixKey)) {
                if (prefixKey.equals(className) || className.charAt(className.indexOf(prefixKey) + prefixKey.length()) == '$') {
                    return prefixKey;
                } else {
                    logger.warn("Found class {} in oracleLocationMap with matching prefix, but it is not a containing class of {}.", prefixKey, className);
                }
            }
        }
        return null;
    }
     */

    public Set<String> getInstrumentedMethodsForClass(String className) {
        if (oracleLocationMap.containsKey(className)) {
            return oracleLocationMap.get(className).keySet();
        } else {
            return Collections.emptySet();
        }
    }
}
