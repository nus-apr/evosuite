package org.evosuite.patch;

import org.evosuite.ga.Chromosome;

public abstract class AbstractPatchChromosome<E extends AbstractPatchChromosome<E>>
    extends Chromosome<E> implements Patch {}
