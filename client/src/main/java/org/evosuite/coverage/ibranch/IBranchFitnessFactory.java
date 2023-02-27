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

package org.evosuite.coverage.ibranch;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
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
 * Create the IBranchTestFitness goals for the class under test.
 *
 * @author mattia, Gordon Fraser
 */
public class IBranchFitnessFactory extends AbstractFitnessFactory<IBranchTestFitness> {

    private static final Logger logger = LoggerFactory.getLogger(IBranchFitnessFactory.class);

    private final Map<String, Map<String, Set<OracleLocation>>> oracleLocations;

    public IBranchFitnessFactory() {
        if (Properties.EVOREPAIR_TEST_GENERATION) {
            oracleLocations = OracleLocationPool.getInstance().getOracleLocations();
        } else {
            oracleLocations = Collections.emptyMap();
        }
        if (oracleLocations.isEmpty()) {
            logger.warn("No oracle locations available for IBRANCH criterion. No goals will be produced.");
        }
    }

    /* (non-Javadoc)
     * @see org.evosuite.coverage.TestFitnessFactory#getCoverageGoals()
     */
    @Override
    public List<IBranchTestFitness> getCoverageGoals() {
        //TODO this creates duplicate goals. Momentary fixed using a Set.
        Set<IBranchTestFitness> goals = new HashSet<>();

        // retrieve set of branches
        BranchCoverageFactory branchFactory = new BranchCoverageFactory();

        // TODO EvoRepair: Limit branches to CUT only for know
        List<BranchCoverageTestFitness> branchGoals = branchFactory.getCoverageGoalsForAllKnownClasses();

        CallGraph callGraph = DependencyAnalysis.getCallGraph();


        // try to find all occurrences of this branch in the call tree
        for (BranchCoverageTestFitness branchGoal : branchGoals) {
            if (!shouldInclude(branchGoal)) continue;

            logger.info("Adding context branches for " + branchGoal.toString());
            for (CallContext context : callGraph.getAllContextsFromTargetClass(branchGoal.getClassName(),
                    branchGoal.getMethod(), true)) {
                //if is not possible to reach this branch from the target class, continue.
                if (context.isEmpty()) continue;
                goals.add(new IBranchTestFitness(branchGoal.getBranchGoal(), context));
            }
        }
        assert Properties.EVOREPAIR_TEST_GENERATION || (goals.size() >= branchFactory.getCoverageGoals().size());
        logger.info("Created " + goals.size() + " goals");

        return new ArrayList<>(goals);
    }

    private boolean shouldInclude(BranchCoverageTestFitness branchGoal) {
        // If we are not running evorepair, allow all ibranch goals
        if (!Properties.EVOREPAIR_TEST_GENERATION) {
            return true;
        }

        // We add goals for root branches of methods instrumented with the oracle
        // Branch must be null to be a root branch
        //if (branchGoal.getBranchGoal().getBranch() != null) {
        //    return true;
       //}

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

//	private boolean isGradient(BranchCoverageTestFitness branchGoal){
//		if (branchGoal.getBranchGoal().getBranch() == null)
//			return false;
//		int branchOpCode = branchGoal.getBranchGoal().getBranch().getInstruction().getASMNode()
//				.getOpcode();
//		switch (branchOpCode) {
//		// copmpare int with zero
//		case Opcodes.IFEQ:
//		case Opcodes.IFNE:
//		case Opcodes.IFLT:
//		case Opcodes.IFGE:
//		case Opcodes.IFGT:
//		case Opcodes.IFLE:
//			return false; 
//			// copmpare int with int
//		case Opcodes.IF_ICMPEQ:
//		case Opcodes.IF_ICMPNE:
//		case Opcodes.IF_ICMPLT:
//		case Opcodes.IF_ICMPGE:
//		case Opcodes.IF_ICMPGT:
//		case Opcodes.IF_ICMPLE:
//			return true; 
//			// copmpare reference with reference
//		case Opcodes.IF_ACMPEQ:
//		case Opcodes.IF_ACMPNE:
//			return false; 
//			// compare reference with null
//		case Opcodes.IFNULL:
//		case Opcodes.IFNONNULL:
//			return false;
//		default:
//			return false; 
//		}
//	}


//	//---------- 
//	List<String> l = new ArrayList<>();
//	for (IBranchTestFitness callGraphEntry : goals) {
//		l.add(callGraphEntry.toStringContext());
//	}
//	File f = new File("/Users/mattia/workspaces/evosuiteSheffield/evosuite/master/evosuite-report/ibranchgoals.txt");
//	f.delete();
//	try {
//		Files.write(f.toPath(), l, Charset.defaultCharset(), StandardOpenOption.CREATE);
//	} catch (IOException e) { 
//		e.printStackTrace();
//	}
//	//---------- 

