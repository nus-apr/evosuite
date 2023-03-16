package org.evosuite.ga.metaheuristics.mosa;

import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.archive.Archive;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DynaMOSAPatch extends DynaMOSA {

    private static final long serialVersionUID = -141604475997878217L;

    private static final Logger logger = LoggerFactory.getLogger(DynaMOSAPatch.class);


    /**
     * Constructor based on the abstract class {@link AbstractMOSA}.
     *
     * @param factory
     */
    public DynaMOSAPatch(ChromosomeFactory<TestChromosome> factory) {
        super(factory);
        if (Properties.ARCHIVE_TYPE != Properties.ArchiveType.MULTI_CRITERIA_COVERAGE) {
            logger.error("DynaMOSA-Patch only supports multi criteria coverage archives, current archive type: {}", Properties.ARCHIVE_TYPE);
            throw new IllegalStateException("DynaMOSAPatch requires a multi criteria coverage archive.");
        }
    }


    @Override
    protected void evolve() {
        super.evolve();

        // Attempt to produce additional solutions by crossover of partial solutions with line solutions
        List<TestChromosome> targetLineSolutions = Archive.getMultiCriteriaArchive().getFixLocationSolutions();

        // Need at least one target line solution to crossover partial solutions with
        if (targetLineSolutions.isEmpty()) {
            return;
        }
        
        for (TestChromosome parent1 : Archive.getMultiCriteriaArchive().getPartialSolutions()) {
            TestChromosome parent2 = targetLineSolutions.get(Randomness.nextInt(targetLineSolutions.size()));
            
            TestChromosome offspring1 = parent1.clone();
            TestChromosome offspring2 = parent2.clone();
            try {
                this.crossoverFunction.crossOver(offspring1, offspring2);
            } catch (ConstructionFailedException e) {
                logger.debug("CrossOver failed.");
                continue;
            }

            this.removeUnusedVariables(offspring1);
            this.removeUnusedVariables(offspring2);

            // Note: We don't perform mutation as it might affect the coverage of the original goal
            // this.mutate(offspring1, parent1);
            if (offspring1.isChanged()) {
                this.clearCachedResults(offspring1);
                offspring1.updateAge(this.currentIteration);
                this.calculateFitness(offspring1); // if the offspring now covers both goals, it will be saved here
            }

            if (offspring2.isChanged()) {
                this.clearCachedResults(offspring2);
                offspring2.updateAge(this.currentIteration);
                this.calculateFitness(offspring2); // if the offspring now covers both goals, it will be saved here
            }

        }

    }
}
