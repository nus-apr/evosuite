package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            throw new RuntimeException("Error while receiving patch pool: " + e.getMessage());
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

        // Request patch pool from orchestrator through comm server
        List<Patch> result = OrchestratorClient.getInstance().sendRequest("sendPatchPool", new TypeReference<List<Patch>>() {});

        // Add received patches to the patch pool
        logger.info("Received patch pool from orchestrator of size: " + result.size());
        patches.addAll(result);
    }

    public Set<Patch> getPatchPool() {
        return patches;
    }

    /**
     * (Unused, kept for reference only)
     * Type representation of the patch pool to receive from the orchestrator.
     */
    public static class PatchPoolType {
        private List<Patch> patches;

        public PatchPoolType() {}
        public PatchPoolType(List<Patch> patches) {
            this.patches = patches;
        }

        public void setPatches(List<Patch> patches) {
            this.patches = patches;
        }
    }
}
