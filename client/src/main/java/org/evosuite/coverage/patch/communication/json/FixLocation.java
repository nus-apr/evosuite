package org.evosuite.coverage.patch.communication.json;

import java.util.List;

public class FixLocation {
    String classname;
    List<Integer> targetLines;

    public FixLocation(){}
    public FixLocation(String classname, List<Integer> targetLines) {
        this.classname = classname;
        this.targetLines = targetLines;
    }

    public String getClassname() {
        return this.classname;
    }
    public void setClassname(String classname) {
        this.classname = classname;
    }

    public List<Integer> getTargetLines() {
        return this.targetLines;
    }

    public void setTargetLines(List<Integer> targetLines) {
        this.targetLines = targetLines;
    }
}
