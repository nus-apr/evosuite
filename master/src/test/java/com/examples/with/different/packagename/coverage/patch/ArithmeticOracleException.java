package com.examples.with.different.packagename.coverage.patch;

public class ArithmeticOracleException {

    public double div(int x, int y) {
        if (Boolean.parseBoolean(System.getProperty("defects4j.instrumentation.enabled"))) { // oracle flag location
            try {
                double result =  div_original(x, y);
                if (Double.isInfinite(result) || Double.isNaN(result)) {
                    throw new RuntimeException("[Defects4J_BugReport_Violation]"); // custom exception location
                }
                return result;
            } catch (ArithmeticException e) { // Can apparently only be triggered by integer division (double/float division will return infinity/NaN) instead
                throw new RuntimeException("[Defects4J_BugReport_Violation]"); // custom exception location
            }
        } else {
            return div_original(x, y);
        }
    }

    public double div_original(int x, int y) {
        if (x == 0 && y == 0) {
            return 0;
        }
        return ((double) 1) / (x + y); // Note: Double division yields infinity for x==y, rather than ArithmeticException
    }
}
