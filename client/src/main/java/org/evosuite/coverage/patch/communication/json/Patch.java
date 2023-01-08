package org.evosuite.coverage.patch;

// TODO: Merge with TargetLinesSpec
public class Patch {
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
}
