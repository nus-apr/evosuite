package org.evosuite.patch;

import org.evosuite.Properties;
import org.evosuite.ga.operators.selection.TournamentSelectionRankAndCrowdingDistanceComparator;
import org.evosuite.ga.stoppingconditions.MaxTimeStoppingCondition;
import org.evosuite.patch.NonDomNSGAIIWithInit;
import org.evosuite.ga.operators.selection.BinaryTournamentSelectionCrowdedComparison;
import org.evosuite.ga.stoppingconditions.MaxGenerationStoppingCondition;

import us.msu.cse.repair.Interpreter;
import us.msu.cse.repair.ec.problems.ArjaEProblem;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ERepairMosaMain {
    public static void main(String[] args) throws Exception {
        HashMap<String, String> parameterStrs = Interpreter.getParameterStrings(args);
        HashMap<String, Object> parameters = Interpreter.getBasicParameterSetting(parameterStrs);

        parameters.put("ingredientScreenerName", "Direct2");
        parameters.put("testExecutorName", "ExternalTestExecutor2");
        parameters.put("manipulationNames", new String[] { "Replace", "InsertBefore", "Delete" });

        parameters.putIfAbsent("maxNumberOfModificationPoints", 60);

        String repSimS = parameterStrs.get("repSim");
        if (repSimS != null) {
            double repSim = Double.parseDouble(repSimS);
            parameters.put("repSim", repSim);
        }

        String insRelS = parameterStrs.get("insRel");
        if (insRelS != null) {
            double insRel = Double.parseDouble(insRelS);
            parameters.put("insRel", insRel);
        }

        double initRatioOfPerfect = 0;
        double initRatioOfFame = 0;

        String initRatioOfPerfectS = parameterStrs.get("initRatioOfPerfect");
        if (initRatioOfPerfectS != null)
            initRatioOfPerfect = Double.parseDouble(initRatioOfPerfectS);

        String initRatioOfFameS = parameterStrs.get("initRatioOfFame");
        if (initRatioOfFameS != null)
            initRatioOfFame = Double.parseDouble(initRatioOfFameS);

        double mutationProbability = 1;
        ArjaEProblem problem = new ArjaEProblem(parameters);
        EPatchMOSA repairAlg = new EPatchMOSA(
                new EPatchChromosomeFactory(problem, initRatioOfPerfect, initRatioOfFame, mutationProbability));

        Properties.CROSSOVER_RATE = 1;
        Properties.MUTATION_RATE = 1;
        repairAlg.setCrossOverFunction(new EPatchCrossOver());
        repairAlg.setSelectionFunction(new TournamentSelectionRankAndCrowdingDistanceComparator<>(false));

//        WeightedFailureRatePatchFitness<EPatchChromosome> weightedFailureRatePatchFitness =
//                new WeightedFailureRatePatchFitness<>();
//
//        repairAlg.addFitnessFunction(weightedFailureRatePatchFitness);
//        repairAlg.addFitnessFunction(new SizePatchFitness<>());
        double weight = problem.getWeight();
        for (String test: problem.getPositiveTests()) {
            repairAlg.addFitnessFunction(new SingleTestPatchFitness<>(test, weight));
        }
        for (String test: problem.getNegativeTests()) {
            repairAlg.addFitnessFunction(new SingleTestPatchFitness<>(test, 1));
        }

        String populationSizeS = parameterStrs.get("populationSize");
        Properties.POPULATION = populationSizeS != null ? Integer.parseInt(populationSizeS) : 40;

        String maxGenerationsS = parameterStrs.get("maxGenerations");
        final int maxGenerations = maxGenerationsS != null ? Integer.parseInt(maxGenerationsS) : 50;
        MaxGenerationStoppingCondition<EPatchChromosome> maxGenCondition = new MaxGenerationStoppingCondition<>();
        maxGenCondition.setMaxIterations(maxGenerations);

        String maxTimeS = parameterStrs.get("maxTime");
        int maxTime = maxTimeS != null ? Integer.parseInt(maxTimeS) * 60 : 60 * 60;
        MaxTimeStoppingCondition<EPatchChromosome> maxTimeCondition = new MaxTimeStoppingCondition<>();
        maxTimeCondition.setLimit(maxTime);

        repairAlg.addStoppingCondition(maxGenCondition);
        repairAlg.addStoppingCondition(maxTimeCondition);

//        Properties.EVOREPAIR_SEED_POPULATION = true;

        repairAlg.generateSolution();
    }
}
