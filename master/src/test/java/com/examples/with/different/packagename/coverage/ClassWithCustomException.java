package com.examples.with.different.packagename.coverage;

public class ClassWithCustomException {

    public char test(String s) {
        try {
            return handleString(s);
        } catch (StringIndexOutOfBoundsException e) {
            if (s.contains("&")) {
                throw new RuntimeException("[Defects4J_BugReport_Violation]");
            } else {
                throw e;
            }
        }
    }

    // Return the first char following '&', which will throw an exception if it is the only char
    private char handleString(String s) {
        if (s.startsWith("&")) {
            return s.charAt(s.indexOf("&") + 1);
        } else {
            throw new IllegalArgumentException("Input does not contain char &"); // Expected exception
        }
    }

}
