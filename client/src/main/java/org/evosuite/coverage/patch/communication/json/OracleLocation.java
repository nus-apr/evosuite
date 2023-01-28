package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class OracleLocation {
    private String className;
    private String methodName;
    private int lineNumber;
    private List<Integer> customExceptionLines;
    private List<Integer> instrumentationFlagLines;

    public OracleLocation() {}
    public OracleLocation(String className, String methodName, int lineNumber,
                          List<Integer> customExceptionLines, List<Integer> instrumentationFlagLines) {
        this.className = className;
        this.methodName = methodName;
        this.customExceptionLines = customExceptionLines;
        this.instrumentationFlagLines = instrumentationFlagLines;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<Integer> getCustomExceptionLines() {
        return customExceptionLines;
    }

    public List<Integer> getInstrumentationFlagLines() {
        return getInstrumentationFlagLines();
    }
}
