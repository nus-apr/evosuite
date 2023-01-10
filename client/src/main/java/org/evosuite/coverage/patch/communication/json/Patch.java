package org.evosuite.coverage.patch.communication.json;

import java.io.Serializable;
import java.util.Objects;

// TODO: Merge with TargetLinesSpec
public class Patch implements Serializable, Comparable<Patch> {
    private static final long serialVersionUID = -6295242867507437030L;
    private String index;

    public Patch() {}

    public Patch(String index) {
        this.index = index;
    }

    public String getIndex() {
        return this.index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Patch patch = (Patch) o;

        return index.equals(patch.getIndex());
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }

    @Override
    public int compareTo(Patch other) {
        return index.compareTo(other.getIndex());
    }
}
