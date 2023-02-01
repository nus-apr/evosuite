package com.examples.with.different.packagename.coverage.patch;

public class BranchInsideCatchBlock {

    public int test(String s, int x, int y) {
        try {
            return buggy(s);
        } catch (StringIndexOutOfBoundsException e) {
            if (x > 1 && y > 1) {
                if (x * y == 100) {
                    return 0;
                }
            }
            return 1;
        }
    }

    public char buggy(String s) {
        return s.charAt(0);
    }
}
