package com.examples.with.different.packagename.coverage;

public class ClassWithInnerClass {

    int x;
    public ClassWithInnerClass () {
        x = 0;
    }

    public void add5() {
        x += 5;
    }

    public class InnerClass {
        int y;
        public InnerClass() {
            y = 1;
        }

        public void sub5() {
            y -= 5;
        }
    }

    public static class StaticInnerClass {
        int z;
        public StaticInnerClass() {
            z = 1;
        }

        public void mul5() {
            z *= 5;
        }
    }
}
