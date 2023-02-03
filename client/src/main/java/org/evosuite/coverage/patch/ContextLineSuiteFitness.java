package org.evosuite.coverage.patch;

import org.evosuite.Properties;
import org.evosuite.coverage.cbranch.CBranchTestFitness;
import org.evosuite.ga.archive.Archive;
import org.evosuite.setup.CallContext;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

import java.util.*;

public class ContextLineSuiteFitness extends TestSuiteFitnessFunction {

    private final List<ContextLineTestFitness> lineGoals;

    private final Map<Integer, Map<CallContext, Set<ContextLineTestFitness>>> contextGoalsMap;

    private final Map<Integer, Set<ContextLineTestFitness>> privateMethodsGoalsMap;

    private final Map<String, Map<CallContext, ContextLineTestFitness>> methodsMap;

    private final Map<String, ContextLineTestFitness> privateMethodsMethodsMap;

    private final Set<ContextLineTestFitness> toRemoveGoals = new LinkedHashSet<>();
    private final Set<ContextLineTestFitness> removedGoals = new LinkedHashSet<>();

    public ContextLineSuiteFitness() {
        contextGoalsMap = new LinkedHashMap<>();
        privateMethodsGoalsMap = new LinkedHashMap<>();
        methodsMap = new LinkedHashMap<>();
        privateMethodsMethodsMap = new LinkedHashMap<>();

        ContextLineFactory factory = new ContextLineFactory();
        lineGoals = factory.getCoverageGoals();
        for (ContextLineTestFitness goal : lineGoals) {
            if (Properties.TEST_ARCHIVE)
                Archive.getArchiveInstance().addTarget(goal);

            if (goal.getBranchGoal() != null && goal.getBranchGoal().getBranch() != null) {
                int branchId = goal.getBranchGoal().getBranch().getActualBranchId();

                // if private method do not consider context
                if (goal.getContext().isEmpty()) {
                    Set<ContextLineTestFitness> tempInSet = privateMethodsGoalsMap.get(branchId);
                    if (tempInSet == null) {
                        privateMethodsGoalsMap.put(branchId, tempInSet = new LinkedHashSet<>());
                    }
                    tempInSet.add(goal);
                } else {
                    // if public method consider context
                    Map<CallContext, Set<ContextLineTestFitness>> innermap = contextGoalsMap
                            .get(branchId);
                    if (innermap == null) {
                        contextGoalsMap.put(branchId, innermap = new LinkedHashMap<>());
                    }
                    Set<ContextLineTestFitness> tempInSet = innermap.get(goal.getContext());
                    if (tempInSet == null) {
                        innermap.put(goal.getContext(), tempInSet = new LinkedHashSet<>());
                    }
                    tempInSet.add(goal);
                }
            } else {
                String methodName = goal.getTargetClass() + "." + goal.getTargetMethod();
                // if private method do not consider context
                if (goal.getContext().isEmpty()) {
                    privateMethodsMethodsMap.put(methodName, goal);
                } else {
                    // if public method consider context
                    Map<CallContext, ContextLineTestFitness> innermap = methodsMap.get(methodName);
                    if (innermap == null) {
                        methodsMap.put(methodName, innermap = new LinkedHashMap<>());
                    }
                    innermap.put(goal.getContext(), goal);
                }
            }
            logger.info("Context goal: " + goal);
        }
    }

    private ContextLineTestFitness getContextGoal(String classAndMethodName, CallContext context) {
        if (privateMethodsMethodsMap.containsKey(classAndMethodName)) {
            return privateMethodsMethodsMap.get(classAndMethodName);
        } else if (methodsMap.get(classAndMethodName) == null
                || methodsMap.get(classAndMethodName).get(context) == null)
            return null;
        else
            return methodsMap.get(classAndMethodName).get(context);
    }

    private ContextLineTestFitness getContextGoal(Integer branchId, CallContext context, boolean value) {
        if (privateMethodsGoalsMap.containsKey(branchId)) {
            for (ContextLineTestFitness clf : privateMethodsGoalsMap.get(branchId)) {
                if (clf.getValue() == value) {
                    return clf;
                }
            }
        } else if (contextGoalsMap.get(branchId) == null
                || contextGoalsMap.get(branchId).get(context) == null)
            return null;
        else
            for (ContextLineTestFitness clf : contextGoalsMap.get(branchId).get(context)) {
                if (clf.getValue() == value) {
                    return clf;
                }
            }
        return null;
    }

    @Override
    public double getFitness(TestSuiteChromosome suite) {
        double fitness = 0.0; // branchFitness.getFitness(suite);

        List<ExecutionResult> results = runTestSuite(suite);
        Map<ContextLineTestFitness, Double> distanceMap = new LinkedHashMap<>();

        Map<Integer, Integer> callCounter = new LinkedHashMap<>();
        Map<Integer, Integer> branchCounter = new LinkedHashMap<>();

        for (ExecutionResult result : results) {
            if (result.hasTimeout() || result.hasTestException()) {
                continue;
            }

            // Determine minimum branch distance for each branch in each context
            assert (result.getTrace().getTrueDistancesContext().keySet().size() == result
                    .getTrace().getFalseDistancesContext().keySet().size());

            TestChromosome test = new TestChromosome();
            test.setTestCase(result.test);
            test.setLastExecutionResult(result);
            test.setChanged(false);

            for (Integer branchId : result.getTrace().getTrueDistancesContext().keySet()) {
                Map<CallContext, Double> trueMap = result.getTrace().getTrueDistancesContext()
                        .get(branchId);
                Map<CallContext, Double> falseMap = result.getTrace().getFalseDistancesContext()
                        .get(branchId);

                for (CallContext context : trueMap.keySet()) {
                    ContextLineTestFitness goalT = getContextGoal(branchId, context, true);
                    if (goalT == null)
                        continue;

                    double distanceT = trueMap.get(context);
                    if (Double.compare(distanceT, 0.0) == 0) {
                        if (result.getTrace().getCoveredLines().contains(goalT.getLine())) {
                            distanceT = 0.0;
                        } else {
                            distanceT = 1.0;
                        }
                    } else {
                        distanceT = 1.0 + normalize(distanceT);
                    }

                    if (distanceMap.get(goalT) == null || distanceMap.get(goalT) > distanceT) {
                        distanceMap.put(goalT, distanceT);
                    }
                    if (Double.compare(distanceT, 0.0) == 0) {
                        if (removedGoals.contains(goalT))
                            continue;
                        test.getTestCase().addCoveredGoal(goalT);
                        toRemoveGoals.add(goalT);
                    }
                    if (Properties.TEST_ARCHIVE) {
                        Archive.getArchiveInstance().updateArchive(goalT, test, distanceT);
                    }
                }

                for (CallContext context : falseMap.keySet()) {
                    ContextLineTestFitness goalF = getContextGoal(branchId, context, false);
                    if (goalF == null)
                        continue;

                    double distanceF = falseMap.get(context);
                    if (Double.compare(distanceF, 0.0) == 0) {
                        if (result.getTrace().getCoveredLines().contains(goalF.getLine())) {
                            distanceF = 0.0;
                        } else {
                            distanceF = 0.5;
                        }
                    } else {
                        distanceF = 1.0 + normalize(distanceF);
                    }


                    if (distanceMap.get(goalF) == null || distanceMap.get(goalF) > distanceF) {
                        distanceMap.put(goalF, distanceF);
                    }
                    if (Double.compare(distanceF, 0.0) == 0) {
                        if (removedGoals.contains(goalF))
                            continue;
                        test.getTestCase().addCoveredGoal(goalF);
                        toRemoveGoals.add(goalF);
                    }
                    if (Properties.TEST_ARCHIVE) {
                        Archive.getArchiveInstance().updateArchive(goalF, test, distanceF);
                    }
                }
            }

            for (Map.Entry<Integer, Map<CallContext, Integer>> entry : result.getTrace()
                    .getPredicateContextExecutionCount().entrySet()) {
                for (Map.Entry<CallContext, Integer> value : entry.getValue().entrySet()) {
                    int count = value.getValue();

                    ContextLineTestFitness goalT = getContextGoal(entry.getKey(), value.getKey(), true);
                    if (goalT != null) {
                        if (branchCounter.get(goalT.getGenericContextBranchIdentifier()) == null
                                || branchCounter.get(goalT.getGenericContextBranchIdentifier()) < count) {
                            branchCounter.put(goalT.getGenericContextBranchIdentifier(), count);
                        }
                    } else {
                        ContextLineTestFitness goalF = getContextGoal(entry.getKey(), value.getKey(),
                                false);
                        if (goalF != null) {
                            if (branchCounter.get(goalF.getGenericContextBranchIdentifier()) == null
                                    || branchCounter.get(goalF.getGenericContextBranchIdentifier()) < count) {
                                branchCounter.put(goalF.getGenericContextBranchIdentifier(), count);
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Map<CallContext, Integer>> entry : result.getTrace()
                    .getMethodContextCount().entrySet()) {
                for (Map.Entry<CallContext, Integer> value : entry.getValue().entrySet()) {
                    ContextLineTestFitness goal = getContextGoal(entry.getKey(), value.getKey());
                    if (goal == null)
                        continue;
                    int count = value.getValue();
                    if (callCounter.get(goal.hashCode()) == null
                            || callCounter.get(goal.hashCode()) < count) {
                        callCounter.put(goal.hashCode(), count);
                    }
                    if (count > 0) {
                        if (removedGoals.contains(goal))
                            continue;
                        test.getTestCase().addCoveredGoal(goal);
                        toRemoveGoals.add(goal);
                    }
                    if (Properties.TEST_ARCHIVE) {
                        Archive.getArchiveInstance().updateArchive(goal, test, count == 0 ? 1.0 : 0.0);
                    }
                }
            }
        }

        int numCoveredGoals = removedGoals.size();
        for (ContextLineTestFitness goal : lineGoals) {
            if (removedGoals.contains(goal))
                continue;
            Double distance = distanceMap.get(goal);
            if (distance == null)
                distance = 1.0;

            if (goal.getBranch() == null) {
                Integer count = callCounter.get(goal.hashCode());
                if (count == null || count == 0) {
                    fitness += 1;
                } else {
                    numCoveredGoals++;
                }
            } else {
                Integer count = branchCounter.get(goal.getGenericContextBranchIdentifier());
                if (count == null || count == 0)
                    fitness += 1;
                else if (count == 1)
                    fitness += 0.5;
                else {
                    if (Double.compare(distance, 0.0) == 0) {
                        numCoveredGoals++;
                    }
                    fitness += distance;
                }
            }
        }

        if (!lineGoals.isEmpty()) {
            suite.setCoverage(this, (double) numCoveredGoals / (double) lineGoals.size());
        } else {
            suite.setCoverage(this, 1);
        }
        suite.setNumOfCoveredGoals(this, numCoveredGoals);
        suite.setNumOfNotCoveredGoals(this, lineGoals.size() - numCoveredGoals);
        updateIndividual(suite, fitness);

        return fitness;
    }

    @Override
    public boolean updateCoveredGoals() {
        if (!Properties.TEST_ARCHIVE) {
            return false;
        }

        this.removedGoals.addAll(this.toRemoveGoals);

        this.toRemoveGoals.clear();
        logger.info("Current state of archive: " + Archive.getArchiveInstance().toString());

        return true;
    }
}
