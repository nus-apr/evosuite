package com.examples.with.different.packagename.coverage;

public class ComplexConstraints {
    public int testInt(int x, int y) {
        if (x > 1 && y > 1) {
            if (x * y == 100) {
                return 0;
            }
        }
        return 1;
    }
}
