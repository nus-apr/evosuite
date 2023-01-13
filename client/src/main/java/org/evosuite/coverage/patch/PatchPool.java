package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.Properties;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/*
 * TODO: When asking the orchestrator for patch mutation score, send test data as file location rather than source code.
 */

public class PatchPool {
    private static final Logger logger = LoggerFactory.getLogger(PatchPool.class);

    private final Set<Patch> patches = new LinkedHashSet<>();
    private static PatchPool instance = null;

    public PatchPool() {
        try {
            initPatchPool();
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
    public void initPatchPool() throws IOException {
        if (!patches.isEmpty()) {
            logger.warn("Patch pool not empty.");
        }

        List<Patch> result;

        if (Properties.EVOREPAIR_TARGET_PATCHES != null) {
            result = SeedHandler.getInstance().loadPatchPopulation();
        } else {
            // Request patch pool from orchestrator through comm server
            logger.warn("No target patches provided through commandline, attempting to retrieve from orchestrator.");
            result = OrchestratorClient.getInstance().sendRequest("getPatchPool", new TypeReference<List<Patch>>() {});
            // Add received patches to the patch pool
            logger.info("Received patch pool from orchestrator of size: " + result.size());
        }

        patches.addAll(result);
    }

    public Set<Patch> getPatchPool() {
        return patches;
    }
}
