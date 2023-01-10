package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.Properties;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.junit.naming.methods.IDTestNameGenerationStrategy;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

/*
 * TODO: When asking the orchestrator for patch mutation score, send test data as file location rather than source code.
 */

public class PatchPool {
    private static final Logger logger = LoggerFactory.getLogger(PatchPool.class);

    private final Set<Patch> patches = new LinkedHashSet<>();
    private static PatchPool instance = null;

    public PatchPool() {
        try {
            getPatchPoolFromOrchestrator();
        } catch (IOException e) {
            throw new RuntimeException("Unable to obtain initial patch pool from orchestrator: " + e);
        }
    }

    public static PatchPool getInstance() {
        if (instance == null) {
            instance = new PatchPool();
        }
        return instance;
    }

    /**
     * Requests the patch pool from the orchestrator.
     * @throws IOException
     */
    public void getPatchPoolFromOrchestrator() throws IOException {
        if (!patches.isEmpty()) {
            logger.warn("Patch pool not empty.");
        }

        // Request patch pool from orchestrator through comm server
        List<Patch> result = OrchestratorClient.getInstance().sendRequest("getPatchPool", new TypeReference<List<Patch>>() {});

        // Add received patches to the patch pool
        logger.info("Received patch pool from orchestrator of size: " + result.size());
        patches.addAll(result);
    }

    public Set<Patch> getPatchPool() {
        return patches;
    }
}
