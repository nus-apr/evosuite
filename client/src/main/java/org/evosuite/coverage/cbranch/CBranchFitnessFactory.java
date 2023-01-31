/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */

package org.evosuite.coverage.cbranch;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.line.LineCoverageTestFitness;
import org.evosuite.coverage.patch.PatchLineCoverageFactory;
import org.evosuite.coverage.patch.communication.OracleLocationPool;
import org.evosuite.coverage.patch.communication.json.OracleLocation;
import org.evosuite.setup.CallContext;
import org.evosuite.setup.DependencyAnalysis;
import org.evosuite.setup.callgraph.CallGraph;
import org.evosuite.testsuite.AbstractFitnessFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Gordon Fraser, mattia
 */
public class CBranchFitnessFactory extends AbstractFitnessFactory<CBranchTestFitness> {

    private static final Logger logger = LoggerFactory.getLogger(CBranchFitnessFactory.class);

    /* (non-Javadoc)
     * @see org.evosuite.coverage.TestFitnessFactory#getCoverageGoals()
     */
    @Override
    public List<CBranchTestFitness> getCoverageGoals() {
        //TODO this creates duplicate goals. Momentary fixed using a Set, but it should be optimised
        Set<CBranchTestFitness> goals = new HashSet<>();

        Map<String, Map<String, Set<OracleLocation>>> oracleLocations;
        if (Properties.EVOREPAIR_USE_FIX_LOCATION_GOALS) {
            oracleLocations = OracleLocationPool.getInstance().getOracleLocations();
        } else {
            oracleLocations = Collections.emptyMap();
        }
        if (oracleLocations.isEmpty()) {
            logger.warn("No oracle locations available for CBRANCH criterion. No goals will be produced.");
        }

        // retrieve set of branches
        BranchCoverageFactory branchFactory = new BranchCoverageFactory();
        List<BranchCoverageTestFitness> branchGoals = branchFactory.getCoverageGoals();

        // First, filter out any uninteresting branch goals
        branchGoals.removeIf(b -> !shouldInclude(b, oracleLocations));

        // Then, add control dependencies of target lines (fix locations and custom exceptions) as branch goals
        if (Properties.EVOREPAIR_USE_FIX_LOCATION_GOALS) {
            for (LineCoverageTestFitness lineGoal : new PatchLineCoverageFactory().getCoverageGoals()) {
                branchGoals.addAll(lineGoal.getControlDependencyGoals());
            }
        }

        CallGraph callGraph = DependencyAnalysis.getCallGraph();

        // try to find all occurrences of this branch in the call tree
        for (BranchCoverageTestFitness branchGoal : branchGoals) {
            logger.info("Adding context branches for " + branchGoal.toString());

            Set<CallContext> callContexts;
            if (Properties.EVOREPAIR_USE_FIX_LOCATION_GOALS) {
                callContexts = callGraph.getAllContextsFromTargetClass(branchGoal.getClassName(),
                        branchGoal.getMethod(), true);
            } else {
                callContexts = callGraph.getMethodEntryPoint(branchGoal.getClassName(),
                        branchGoal.getMethod());
            }

            for (CallContext context : callContexts) {
                goals.add(new CBranchTestFitness(branchGoal.getBranchGoal(), context));
            }
        }

        logger.info("Created " + goals.size() + " goals");
        return new ArrayList<>(goals);
    }

    private boolean shouldInclude(BranchCoverageTestFitness branchGoal, Map<String, Map<String, Set<OracleLocation>>> oracleLocations) {
        // If we are not running evorepair, allow all ibranch goals
        if (!Properties.EVOREPAIR_USE_FIX_LOCATION_GOALS) {
            return true;
        }

        // Any instrumented methods in this class?
        String className = branchGoal.getClassName();
        if (!oracleLocations.containsKey(className)) {
            return false;
        }

        // Any instrumented methods with this name + descriptor?
        String methodName = branchGoal.getMethod();
        return oracleLocations.get(className).containsKey(methodName); // exclude if not contained
    }
}

