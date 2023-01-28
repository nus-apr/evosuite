package com.examples.with.different.packagename.coverage;

public class MethodWithOracleAnnotation {

    public void topLevelMethod(boolean a, int x) {
        if (a) {
            intermediateMethod(x);
        } else {
            intermediateMethod(0);
        }
    }

    public void intermediateMethod(int x) {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < x; i++) {
            input.append(i);
        }
        instrumentedMethod(input.toString());
    }

    public void instrumentedMethod(String input) {
        if (Boolean.parseBoolean(System.getProperty("defects4j.instrumentation.enabled"))) {
            try {
                buggyMethod(input);
            } catch (StringIndexOutOfBoundsException e) {
                if (input.isEmpty()) {
                    throw new RuntimeException("[Defects4J_BugReport_Violation]");
                }
            }
        } else {
            buggyMethod(input);

        }
    }

    public int buggyMethod(String input) {
        return input.charAt(0); // May return NPE or StringIndexOutOfBoundsException for ""
    }
}
