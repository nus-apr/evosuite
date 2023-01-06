package org.evosuite.patch;

import org.evosuite.ga.metaheuristics.NSGAII;
import org.evosuite.ga.operators.selection.BinaryTournamentSelectionCrowdedComparison;
import org.evosuite.ga.populationlimit.PopulationLimit;
import org.evosuite.ga.stoppingconditions.MaxGenerationStoppingCondition;

import us.msu.cse.repair.Interpreter;

import java.util.HashMap;
import java.util.List;

public class RepairMain {
    public static void main(String[] args) throws Exception {
        NSGAII<PatchChromosome> repairAlg = new NSGAII<>(new PatchChromosomeFactory());

        repairAlg.setCrossOverFunction(new PatchCrossOver());
        repairAlg.setSelectionFunction(new BinaryTournamentSelectionCrowdedComparison<>());

        WeightedFailureRatePatchFitness weightedFailureRatePatchFitness = new WeightedFailureRatePatchFitness();

        repairAlg.addFitnessFunction(weightedFailureRatePatchFitness);
        repairAlg.addFitnessFunction(new SizePatchFitness());

        HashMap<String, String> parameterStrs = Interpreter.getParameterStrings(args);

        String populationSizeS = parameterStrs.get("populationSize");
        final int populationSize = populationSizeS != null ? Integer.parseInt(populationSizeS) : 40;

        String maxGenerationsS = parameterStrs.get("maxGenerations");
        final int maxGenerations = maxGenerationsS != null ? Integer.parseInt(maxGenerationsS) : 50;
        MaxGenerationStoppingCondition<PatchChromosome> condition = new MaxGenerationStoppingCondition<>();
        condition.setMaxIterations(maxGenerations);

        repairAlg.setPopulationLimit(new PopulationLimit<PatchChromosome>() {
            @Override
            public boolean isPopulationFull(List<PatchChromosome> population) {
                return population.size() >= populationSize;
            }
        });

        repairAlg.addStoppingCondition(condition);

        repairAlg.generateSolution();

        List<PatchChromosome> bestIndividuals = repairAlg.getBestIndividuals();

        for (PatchChromosome chromosome : bestIndividuals) {
            if (chromosome.getFitnessValues().get(weightedFailureRatePatchFitness) == 0) {
                // save patch
            }
        }
    }
}
