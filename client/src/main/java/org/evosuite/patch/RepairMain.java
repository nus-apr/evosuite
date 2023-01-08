package org.evosuite.patch;

import org.evosuite.ga.metaheuristics.NSGAII;
import org.evosuite.ga.operators.selection.BinaryTournamentSelectionCrowdedComparison;
import org.evosuite.ga.populationlimit.PopulationLimit;
import org.evosuite.ga.stoppingconditions.MaxGenerationStoppingCondition;

import us.msu.cse.repair.Interpreter;
import us.msu.cse.repair.ec.problems.ArjaProblem;

import java.util.HashMap;
import java.util.List;

public class RepairMain {
    public static void main(String[] args) throws Exception {
        HashMap<String, String> parameterStrs = Interpreter.getParameterStrings(args);

        HashMap<String, Object> parameters = Interpreter.getBasicParameterSetting(parameterStrs);
        String ingredientScreenerNameS = parameterStrs.get("ingredientScreenerName");
        if (ingredientScreenerNameS != null)
            parameters.put("ingredientScreenerName", ingredientScreenerNameS);

        NSGAII<PatchChromosome> repairAlg = new NSGAII<>(new PatchChromosomeFactory(new ArjaProblem(parameters)));

        repairAlg.setCrossOverFunction(new PatchCrossOver());
        repairAlg.setSelectionFunction(new BinaryTournamentSelectionCrowdedComparison<>());

        WeightedFailureRatePatchFitness weightedFailureRatePatchFitness = new WeightedFailureRatePatchFitness();

        repairAlg.addFitnessFunction(weightedFailureRatePatchFitness);
        repairAlg.addFitnessFunction(new SizePatchFitness());


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
    }
}
