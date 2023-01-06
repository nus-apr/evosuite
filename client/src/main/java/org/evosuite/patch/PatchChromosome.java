package org.evosuite.patch;

//public final class TestChromosome extends AbstractTestChromosome<TestChromosome> {

//public abstract class ExecutableChromosome<E extends ExecutableChromosome<E>> extends Chromosome<E> {

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.localsearch.LocalSearchObjective;


//public abstract class ExecutableChromosome<E extends ExecutableChromosome<E>> extends Chromosome<E> {

public final class PatchChromosome extends Chromosome<PatchChromosome> {
    /**
     * {@inheritDoc}
     * <p>
     * Create a deep copy of the chromosome
     */
    @Override
    public PatchChromosome clone() {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @param obj
     */
    @Override
    public boolean equals(Object obj) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Secondary Objectives are specific to chromosome types
     *
     * @param o a {@link Chromosome} object.
     * @return a int.
     */
    @Override
    public int compareSecondaryObjective(PatchChromosome o) {
        return 0;
    }

    /**
     * Apply mutation
     */
    @Override
    public void mutate() {

    }

    /**
     * Single point cross over
     *
     * @param other     a {@link Chromosome} object.
     * @param position1 a int.
     * @param position2 a int.
     * @throws ConstructionFailedException if any.
     */
    @Override
    public void crossOver(PatchChromosome other, int position1, int position2) throws ConstructionFailedException {

    }

    /**
     * Apply the local search
     *
     * @param objective a {@link LocalSearchObjective}
     *                  object.
     */
    @Override
    public boolean localSearch(LocalSearchObjective<PatchChromosome> objective) {
        return false;
    }

    /**
     * Return length of individual
     *
     * @return a int.
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * <p>
     * Returns the runtime type of the implementor (a.k.a. "self-type"). This method must only be
     * implemented in concrete, non-abstract subclasses by returning a reference to {@code this},
     * and nothing else. Returning a reference to any other runtime type other than {@code this}
     * breaks the contract.
     * </p>
     * <p>
     * In other words, every concrete subclass {@code Foo} that implements the interface {@code
     * SelfTyped} must implement this method as follows:
     * <pre>{@code
     * public class Foo implements SelfTyped<Foo> {
     *     @Override
     *     public Foo self() {
     *         return this;
     *     }
     * }
     * }</pre>
     * </p>
     *
     * @return a reference to the self-type
     */
    @Override
    public PatchChromosome self() {
        return null;
    }
}
