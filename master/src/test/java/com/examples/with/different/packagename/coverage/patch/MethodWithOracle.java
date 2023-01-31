package com.examples.with.different.packagename.coverage.patch;

public class MethodWithOracle {

    public void topLevelMethod(boolean a, int x) {
        if (a) {
            intermediateMethod(x);
        } else {
            intermediateMethod(0);
        }
    }

    public void anotherTopLevelMethod(int x) {
        if (x * 2 == 10) {
            instrumentedMethod("");
        }
    }

    public void intermediateMethod(int x) {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < x; i++) { // fix location
            input.append(i);
        }
        instrumentedMethod(input.toString());
    }

    public void instrumentedMethod(String input) {
        if (Boolean.parseBoolean(System.getProperty("defects4j.instrumentation.enabled"))) { // oracle flag location
            try {
                buggyMethod(input);
            } catch (StringIndexOutOfBoundsException e) {
                if (input.isEmpty()) {
                    throw new RuntimeException("[Defects4J_BugReport_Violation]"); // custom exception location
                }
            }
        } else {
            buggyMethod(input);
        }
    }

    private int buggyMethod(String input) {
        return input.charAt(0); // May return NPE or StringIndexOutOfBoundsException for "" // fix location
    }
}
