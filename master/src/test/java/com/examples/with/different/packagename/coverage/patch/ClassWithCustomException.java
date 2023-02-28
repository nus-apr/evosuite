package com.examples.with.different.packagename.coverage.patch;

public class ClassWithCustomException {

    public int div(int x, int y) {
        if (Boolean.parseBoolean(System.getProperty("defects4j.instrumentation.enabled"))) {
            try {
                return div_original(x, y);
            } catch(ArithmeticException e) {
                throw new RuntimeException("[Defects4J_BugReport_Violation]");
            }
        } else {
            return div_original(x, y);
        }
    }

    public int div_original(int x, int y) {
        return (x+y) / (x+y+1);
    }
}
