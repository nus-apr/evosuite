package org.evosuite.patch;

import org.evosuite.Properties;
import org.evosuite.ga.metaheuristics.NSGAII;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonDomNSGAIIWithInit extends NSGAII<EPatchChromosome> {
    private static final Logger logger = LoggerFactory.getLogger(NonDomNSGAIIWithInit.class);

    public NonDomNSGAIIWithInit(EPatchChromosomeFactory factory) {
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

        population.addAll(((EPatchChromosomeFactory) chromosomeFactory).getSeedPopulation(Properties.POPULATION));
        this.generateInitialPopulation(Properties.POPULATION);

        this.notifyIteration();
    }
}
