package org.evosuite.patch;

import org.evosuite.Properties;
import org.evosuite.ga.metaheuristics.NSGAII;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NSGAIIWithInit extends NSGAII<PatchChromosome> {

    private static final Logger logger = LoggerFactory.getLogger(NSGAII.class);

    public NSGAIIWithInit(PatchChromosomeFactory factory) {
        super(factory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializePopulation() {
        logger.info("executing initializePopulation function");

        notifySearchStarted();
        currentIteration = 0;

        population.addAll(((PatchChromosomeFactory) chromosomeFactory).getSeedPopulation(Properties.POPULATION));
        this.generateInitialPopulation(Properties.POPULATION);

        this.notifyIteration();
    }


}
