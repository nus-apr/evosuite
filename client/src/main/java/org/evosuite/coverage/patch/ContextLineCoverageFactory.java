package org.evosuite.coverage.patch;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
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

public class ContextLineCoverageFactory extends AbstractFitnessFactory<ContextLineCoverageTestFitness> {
    private static final Logger logger = LoggerFactory.getLogger(ContextLineCoverageFactory.class);

    // Mapping between fix location hash codes to hash codes of context goals
    // Note: We use hash codes to allow multiple computations of the goals not mess with the results
    private static final Map<Integer, Set<Integer>> fixLocationContextMap = new LinkedHashMap<>();

    // Mapping between oracle location hash codes to hash codes of context goals
    private static final Map<Integer, Set<Integer>> oracleLocationContextMap = new LinkedHashMap<>();

    public static Map<Integer, Set<Integer>> getFixLocationContextMap() {
        return fixLocationContextMap;
    }

    public static Map<Integer, Set<Integer>> getOracleLocationContextMap() {
        return oracleLocationContextMap;
    }

    @Override
    public List<ContextLineCoverageTestFitness> getCoverageGoals() {
        // TODO EvoRepair: Can there be duplicate goals as in CBranchFitnessFactory?
        List<ContextLineCoverageTestFitness> goals = new ArrayList<>();

        Map<String, Map<String, Set<OracleLocation>>> oracleLocations = OracleLocationPool.getInstance().getOracleLocations();
        if (oracleLocations.isEmpty()) {
            logger.warn("No oracle locations available for CLINE criterion. No goals will be produced.");
        }

        CallGraph callGraph = DependencyAnalysis.getCallGraph();

        for (LineCoverageTestFitness lineGoal : new PatchLineCoverageFactory().getCoverageGoals()) {
            for (BranchCoverageTestFitness dependencyGoal: lineGoal.getControlDependencyGoals()) {

                Set<CallContext> callContexts = callGraph.getAllContextsFromTargetClass(dependencyGoal.getClassName(),
                        dependencyGoal.getMethod(), Properties.EVOREPAIR_SUB_CBRANCHES);

                for (CallContext context : callContexts) {
                    
                }

                branchGoals.add(dependencyGoal);
                branchGoalToTargetLocationMap.put(dependencyGoal, lineGoal.hashCode());
            }
        }
    }
}
