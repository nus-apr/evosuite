package org.evosuite.coverage.patch.communication.json;

import org.evosuite.Properties;
import org.evosuite.coverage.FitnessFunctions;
import org.evosuite.coverage.mutation.StrongPatchMutationTestFitness;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class TestCaseStats {
    private String testID;

    private int numStatements;

    private boolean isFailing;

    private boolean isUnstable;

    private int numCoveredGoals;

    private boolean coversFixLocation;

    private boolean triggersOracleException;
    private Map<Class<?>, Map<String, Double>> fitnessValues;

    public TestCaseStats() {}

    public TestCaseStats(TestChromosome tc) {
        this.testID = "test" + tc.getTestCase().getID();
        this.numStatements = tc.size();
        this.isFailing = tc.getTestCase().isFailing();
        this.isUnstable = tc.getTestCase().isUnstable();
        this.numCoveredGoals = tc.getTestCase().getCoveredGoals().size();
        this.coversFixLocation = tc.coversFixLocation();
        this.triggersOracleException = tc.hasOracleException();

        Set<Class<?>> fitnessClasses = Arrays.stream(Properties.CRITERION)
                .map(FitnessFunctions::getTestFitnessFunctionClass)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // FIXME: Temporary workaround since StrongPatchMutation does not use its own factory
        fitnessClasses.add(StrongPatchMutationTestFitness.class);

        this.fitnessValues = new LinkedHashMap<>();
        for (Entry<FitnessFunction<TestChromosome>, Double> entry : tc.getFitnessValues().entrySet()) {
            Class<?> fitnessClass = entry.getKey().getClass();
            if (!fitnessClasses.contains(fitnessClass)) continue;
            if (!this.fitnessValues.containsKey(fitnessClass)) {
                this.fitnessValues.put(fitnessClass, new LinkedHashMap<>());
            }
            this.fitnessValues.get(fitnessClass).put(entry.getKey().toSimpleString(), entry.getValue());
        }


    }

    public String getTestID() {
        return testID;
    }

    public void setTestID(String testID) {
        this.testID = testID;
    }

    public int getNumStatements() {
        return numStatements;
    }

    public void setNumStatements(int numStatements) {
        this.numStatements = numStatements;
    }

    public boolean isFailing() {
        return isFailing;
    }

    public void setFailing(boolean failing) {
        isFailing = failing;
    }

    public boolean isUnstable() {
        return isUnstable;
    }

    public void setUnstable(boolean unstable) {
        isUnstable = unstable;
    }

    public int getNumCoveredGoals() {
        return numCoveredGoals;
    }

    public void setNumCoveredGoals(int numCoveredGoals) {
        this.numCoveredGoals = numCoveredGoals;
    }

    public boolean isCoversFixLocation() {
        return coversFixLocation;
    }

    public void setCoversFixLocation(boolean coversFixLocation) {
        this.coversFixLocation = coversFixLocation;
    }

    public boolean isTriggersOracleException() {
        return triggersOracleException;
    }

    public void setTriggersOracleException(boolean triggersOracleException) {
        this.triggersOracleException = triggersOracleException;
    }

    public Map<Class<?>, Map<String, Double>> getFitnessValues() {
        return fitnessValues;
    }

    public void setFitnessValues(Map<Class<?>, Map<String, Double>> fitnessValues) {
        this.fitnessValues = fitnessValues;
    }
}
