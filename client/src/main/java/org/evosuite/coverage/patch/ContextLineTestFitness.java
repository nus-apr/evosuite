package org.evosuite.coverage.patch;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.cbranch.CBranchTestFitness;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.ga.archive.Archive;
import org.evosuite.setup.CallContext;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ContextLineTestFitness extends TestFitnessFunction {

    private static final long serialVersionUID = 7193796202600984135L;

    // Assign unique IDs to each CallContext to make output stats more readable
    private final static Map<CallContext, Integer> contextToId = new LinkedHashMap<>();

    private final int contextId;

    /**
     * Target line
     */
    private final String className;

    private final String methodName;

    private final Integer line;

    protected final LineCoverageTestFitness lineGoal;

    // Control dependency context goal
    protected final CBranchTestFitness contextFitnessGoal;

    public ContextLineTestFitness(LineCoverageTestFitness lineGoal, CBranchTestFitness contextFitnessGoal) {
        this.className = lineGoal.getClassName();
        this.methodName = lineGoal.getMethod();
        this.line = lineGoal.getLine();
        this.lineGoal = lineGoal;
        this.contextFitnessGoal = Objects.requireNonNull(contextFitnessGoal, "controlDependencyContext cannot be null");

        boolean valid = lineGoal.getControlDependencyGoals().stream()
                .map(BranchCoverageTestFitness::getBranchGoal)
                .anyMatch(g -> g.equals(contextFitnessGoal.getBranchGoal()));

        if (!valid) {
            throw new IllegalStateException("Branch " + contextFitnessGoal.getBranchGoal() + " is not a control dependency of " + this);
        }

        CallContext context = contextFitnessGoal.getContext();
        if (!ContextLineTestFitness.contextToId.containsKey(context)) {
            int newId = ContextLineTestFitness.contextToId.size();
            ContextLineTestFitness.contextToId.put(context, newId);
        }
        contextId = ContextLineTestFitness.contextToId.get(context);
    }

    public int getContextId() {
        return contextId;
    }

    /**
     * <p>
     * getClassName
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getClassName() {
        return className;
    }

    /**
     * <p>
     * getMethod
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMethod() {
        return methodName;
    }

    /**
     * <p>
     * getLine
     * </p>
     *
     * @return a {@link java.lang.Integer} object.
     */
    public Integer getLine() {
        return line;
    }

    public LineCoverageTestFitness getLineGoal() {
        return  lineGoal;
    }

    public BranchCoverageGoal getBranchGoal() {
        return contextFitnessGoal.getBranchGoal();
    }

    public Branch getBranch() {
        return contextFitnessGoal.getBranch();
    }

    public CallContext getContext() {
        return contextFitnessGoal.getContext();
    }

    public boolean getValue() {
        return contextFitnessGoal.getValue();
    }

    public int getGenericContextBranchIdentifier() {
        return contextFitnessGoal.getGenericContextBranchIdentifier();
    }



    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        double fitness = 1.0;
        // Deactivate coverage archive while measuring fitness, since branchcoverage fitness
        // evaluating will attempt to claim coverage for it in the archive
        boolean archive = Properties.TEST_ARCHIVE;
        Properties.TEST_ARCHIVE = false;

        // Compute distance to satisfying control dependency context
        double contextFitness = contextFitnessGoal.getFitness(individual, result);
        if (contextFitness == 0.0) {
            // Although the context goal has been covered, it is not part of the
            // optimisation
            individual.getTestCase().removeCoveredGoal(contextFitnessGoal);

            if (result.getTrace().getCoveredLines().contains(this.line)) {
                fitness = 0.0;
            } else {
                // If the control dependency was covered, then likely
                // an exception happened before the line was reached
                fitness = 1.0;
            }
        } else {
            // What if a test covers the line but not the context?
            fitness = 1.0 + normalize(contextFitness);
        }

        Properties.TEST_ARCHIVE = archive;
        updateIndividual(individual, fitness);

        if (fitness == 0.0) {
            individual.getTestCase().addCoveredGoal(this);
        }

        if (Properties.TEST_ARCHIVE) {
            Archive.getArchiveInstance().updateArchive(this, individual, fitness);
        }

        return fitness;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        if (!methodName.isEmpty()) {
            sb.append(".");
            sb.append(methodName);
        }
        sb.append(": Line");
        sb.append(line);
        sb.append(" in context: ");
        sb.append(contextFitnessGoal);
        return sb.toString();
    }

    @Override
    public String toSimpleString() {
        return className + ": Line " + line + " in " + "Context-" + contextId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContextLineTestFitness that = (ContextLineTestFitness) o;

        if (!className.equals(that.className)) return false;
        if (!methodName.equals(that.methodName)) return false;
        if (!line.equals(that.line)) return false;
        return contextFitnessGoal.equals(that.contextFitnessGoal);
    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + line.hashCode();
        result = 31 * result + contextFitnessGoal.hashCode();
        return result;
    }

    @Override
    public int compareTo(TestFitnessFunction other) {
        if (other == null) return 1;
        if (other instanceof ContextLineTestFitness) {
            ContextLineTestFitness otherLineFitness = (ContextLineTestFitness) other;
            if (className.compareTo(otherLineFitness.getClassName()) != 0)
                return className.compareTo(otherLineFitness.getClassName());
            else if (methodName.compareTo(otherLineFitness.getMethod()) != 0)
                return methodName.compareTo(otherLineFitness.getMethod());
            else if (line.compareTo(otherLineFitness.getLine()) != 0)
                return line.compareTo(otherLineFitness.getLine());
            else
                return contextFitnessGoal.compareTo(contextFitnessGoal);
        }
        return compareClassName(other);
    }

    /* (non-Javadoc)
     * @see org.evosuite.testcase.TestFitnessFunction#getTargetClass()
     */
    @Override
    public String getTargetClass() {
        return getClassName();
    }

    /* (non-Javadoc)
     * @see org.evosuite.testcase.TestFitnessFunction#getTargetMethod()
     */
    @Override
    public String getTargetMethod() {
        return getMethod();
    }
}
