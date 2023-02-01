package com.examples.with.different.packagename.cbranch;

public class CBranchSimpleExample {

    public void A(int x) {
        if (x > 0) {
            System.out.println("x > 0");
            B(x);
        } else {
            System.out.println("x <= 0");
            C(x);
        }
    }

    private void B(int x) {
        if (x > 10) {
            System.out.println("x > 10");
            C(x);
        } else {
            System.out.println("10 >= x");
        }
    }

    private void C(int x) {
        if (x > 20) {
            System.out.println("x > 20");
        } else {
            System.out.println("x <= 20");
        }
    }
}
