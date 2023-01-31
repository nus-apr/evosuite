package org.evosuite.coverage.patch.communication.json;

import java.io.Serializable;
import java.util.List;

public class Patch implements Serializable, Comparable<Patch> {
    private static final long serialVersionUID = 1583968801710072964L;
    private String index;
    private List<TargetLocation> fixLocations;

    public Patch() {}

    public Patch(String index, List<TargetLocation> fixLocations) {
        this.index = index;
        this.fixLocations = fixLocations;
    }

    public String getIndex() {
        return this.index;
    }

    public List<TargetLocation> getFixLocations() {
        return this.fixLocations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Patch that = (Patch) o;

        if (!index.equals(that.index)) return false;
        return fixLocations.equals(that.fixLocations);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + index.hashCode();
        result = 31 * result + fixLocations.hashCode();
        return result;
    }

    @Override
    public int compareTo(Patch other) {
        return index.compareTo(other.getIndex());
    }
}
