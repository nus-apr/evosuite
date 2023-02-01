package org.evosuite.coverage.patch.communication.json;

import java.io.Serializable;
import java.util.List;

public class TargetLocation implements Serializable {
    private static final long serialVersionUID = -1926875323300165541L;
    private String classname;
    private List<Integer> targetLines;

    public TargetLocation(){}
    public TargetLocation(String classname, List<Integer> targetLines) {
        this.classname = classname;
        this.targetLines = targetLines;
    }

    public String getClassname() {
        return this.classname;
    }
    //public void setClassname(String classname) {
    //    this.classname = classname;
    //}

    public List<Integer> getTargetLines() {
        return this.targetLines;
    }

    //public void setTargetLines(List<Integer> targetLines) {
    //    this.targetLines = targetLines;
    //}
}
