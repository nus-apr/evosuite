package org.evosuite.coverage.patch;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.cbranch.CBranchTestFitness;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.communication.OracleLocationPool;
import org.evosuite.coverage.patch.communication.json.OracleLocation;
import org.evosuite.setup.CallContext;
import org.evosuite.setup.DependencyAnalysis;
import org.evosuite.setup.callgraph.CallGraph;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ContextLineFactory extends AbstractFitnessFactory<ContextLineTestFitness> {
    private static final Logger logger = LoggerFactory.getLogger(ContextLineFactory.class);

    @Override
    public List<ContextLineTestFitness> getCoverageGoals() {
        // TODO EvoRepair: Can there be duplicate goals as in CBranchFitnessFactory?
        List<ContextLineTestFitness> goals = new ArrayList<>();

        Map<String, Map<String, Set<OracleLocation>>> oracleLocations = OracleLocationPool.getInstance().getOracleLocations();
        if (oracleLocations.isEmpty()) {
            logger.warn("No oracle locations available for CONTEXTLINE criterion. No goals will be produced.");
        }
        logger.warn("CONTEXTLINE goals are currently not produced for oracle locations - need to fix context handling for catch-blocks.");

        CallGraph callGraph = DependencyAnalysis.getCallGraph();

        for (LineCoverageTestFitness lineGoal : new FixLocationCoverageFactory().getCoverageGoals()) {
            for (BranchCoverageTestFitness dependencyGoal: lineGoal.getControlDependencyGoals()) {

                Set<CallContext> callContexts = callGraph.getAllContextsFromTargetClass(dependencyGoal.getClassName(),
                        dependencyGoal.getMethod(), Properties.EVOREPAIR_USE_SUB_CONTEXTS);

                for (CallContext context : callContexts) {
                    CBranchTestFitness contextGoal = new CBranchTestFitness(dependencyGoal.getBranchGoal(), context);
                    ContextLineTestFitness goal = new ContextLineTestFitness(lineGoal, contextGoal);
                    goals.add(goal);
                }
            }
        }
        logger.info("Created " + goals.size() + " goals");
        return goals;
    }
}
