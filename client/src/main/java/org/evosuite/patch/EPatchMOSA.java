package org.evosuite.patch;

import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EPatchMOSA extends GenericMOSA<EPatchChromosome> {
    /**
     * Constructor.
     *
     * @param factory a {@link ChromosomeFactory} object
     */
    public EPatchMOSA(EPatchChromosomeFactory factory) {
        super(factory);
    }

    @Override
    protected List<EPatchChromosome> getSeeds() {
        List<EPatchChromosome> seeds = ((EPatchChromosomeFactory) chromosomeFactory).getSeeds();
        Collections.shuffle(seeds);
        return seeds;
    }

    @Override
    protected Set<FitnessFunction<EPatchChromosome>> getCoveredGoals() {
        return (Set<FitnessFunction<EPatchChromosome>>) (Object) SingleTestArchive.getInstance().getCoveredTargets();
    }

    @Override
    protected int getNumberOfCoveredGoals() {
        return SingleTestArchive.getInstance().getNumberOfCoveredTargets();
    }

    @Override
    protected void addUncoveredGoal(FitnessFunction<EPatchChromosome> goal) {
        SingleTestArchive.getInstance().addTarget((SingleTestPatchFitness<?>) goal);
    }

    @Override
    protected Set<FitnessFunction<EPatchChromosome>> getUncoveredGoals() {
        return (Set<FitnessFunction<EPatchChromosome>>) (Object) SingleTestArchive.getInstance().getLeastCoveredTargets();
    }

    @Override
    protected int getNumberOfUncoveredGoals() {
        return getUncoveredGoals().size();
    }

    @Override
    protected int getTotalNumberOfGoals() {
        return SingleTestArchive.getInstance().getNumberOfTargets();
    }

    @Override
    protected List<EPatchChromosome> getSolutions() {
        return (List<EPatchChromosome>) (Object) SingleTestArchive.getInstance().getSolutions();
    }
}
