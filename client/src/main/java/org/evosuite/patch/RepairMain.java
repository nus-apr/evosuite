package org.evosuite.patch;

import org.evosuite.Properties;
import org.evosuite.ga.metaheuristics.NSGAII;
import org.evosuite.ga.operators.selection.BinaryTournamentSelectionCrowdedComparison;
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

        double initRatioOfPerfect = 0;
        double initRatioOfFame = 0;

        String initRatioOfPerfectS = parameterStrs.get("initRatioOfPerfect");
        if (initRatioOfPerfectS != null)
            initRatioOfPerfect = Double.parseDouble(initRatioOfPerfectS);

        String initRatioOfFameS = parameterStrs.get("initRatioOfFame");
        if (initRatioOfFameS != null)
            initRatioOfFame = Double.parseDouble(initRatioOfFameS);

        NSGAII<PatchChromosome> repairAlg = new NSGAIIWithInit(
                new PatchChromosomeFactory(new ArjaProblem(parameters), initRatioOfPerfect, initRatioOfFame));

        Properties.CROSSOVER_RATE = 1;
        Properties.MUTATION_RATE = 1;
        repairAlg.setCrossOverFunction(new PatchCrossOver());
        repairAlg.setSelectionFunction(new BinaryTournamentSelectionCrowdedComparison<>(false));

        WeightedFailureRatePatchFitness weightedFailureRatePatchFitness = new WeightedFailureRatePatchFitness();

        repairAlg.addFitnessFunction(weightedFailureRatePatchFitness);
        repairAlg.addFitnessFunction(new SizePatchFitness());


        String populationSizeS = parameterStrs.get("populationSize");
        Properties.POPULATION = populationSizeS != null ? Integer.parseInt(populationSizeS) : 40;

        String maxGenerationsS = parameterStrs.get("maxGenerations");
        final int maxGenerations = maxGenerationsS != null ? Integer.parseInt(maxGenerationsS) : 50;
        MaxGenerationStoppingCondition<PatchChromosome> condition = new MaxGenerationStoppingCondition<>();
        condition.setMaxIterations(maxGenerations);

        repairAlg.addStoppingCondition(condition);

        repairAlg.generateSolution();
    }
}
