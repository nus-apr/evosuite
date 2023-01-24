package com.examples.with.different.packagename.coverage;

public class ClassWithCustomException {

    public int testInt(int x) {
        if  (x > 1024) {
            throw new RuntimeException("[Defects4J_BugReport_Violation]");
        } else {
            return x;
        }
    }

    /*
    public char test(String s) {
        return s.charAt(0);
    }


    public int other(String s) {
        if (s.length() > 1) {
            if (s.endsWith("&")) {
                throw new RuntimeException("[Defects4J_BugReport_Violation]");
            }
            return 0;
        }
        return 1;
    }

     */
}
