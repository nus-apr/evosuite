package org.evosuite.coverage.patch.communication.json;

import java.io.Serializable;
import java.util.Objects;

// TODO: Merge with TargetLinesSpec
public class Patch implements Serializable, Comparable<Patch> {
    private static final long serialVersionUID = -6295242867507437030L;
    private int id;

    public Patch() {}

    public Patch(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Patch patch = (Patch) o;

        return id == patch.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public int compareTo(Patch other) {
        return Integer.compare(id, other.getId());
    }
}
