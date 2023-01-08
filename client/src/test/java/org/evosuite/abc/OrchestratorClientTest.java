package org.evosuite.abc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.coverage.patch.OrchestratorClient;
import org.evosuite.coverage.patch.Patch;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OrchestratorClientTest {
    /**
     * If this is set to true, the Python server under 'evosuite/client/src/test/python/Server.py'
     * must be started before running any tests (must be run before starting each test).
     */
    private static final boolean USE_PYTHON_SERVER = false;
    private OrchestratorClient client;
    @Test
    public void testConnection() {
        try {
            // Start separate server thread
            if (!USE_PYTHON_SERVER) {
                ServerRunnable sr = new ServerRunnable(7777);
                Thread serverThread = new Thread(sr);
                serverThread.start();
            }

            // Let client connect to server and make some requests
            client = OrchestratorClient.getInstance();
            List<Integer> response = client.sendRequest("test", new TypeReference<List>() {});
            Assert.assertEquals(response, Arrays.asList(4,0,4));

        } catch (IOException e) {
            throw new RuntimeException("Unable to start server: " + e);
        } finally {
            client.close();
        }
    }

    @Test
    public void testCommands() {
        try {
            // Start separate server thread
            if (!USE_PYTHON_SERVER) {
                ServerRunnable sr = new ServerRunnable(7777);
                Thread serverThread = new Thread(sr);
                serverThread.start();
            }

            // Let client connect to server and make some requests
            client = OrchestratorClient.getInstance();

            // Request patch pool
            List<Patch> patchPool = client.sendRequest("getPatchPool", new TypeReference<List<Patch>>() {});
            for(int i = 0; i < 10; i++) {
                Assert.assertEquals(i, patchPool.get(i).getId());
            }

            // Update population
            Map<String, Object> populationRequestMap = new LinkedHashMap<>();
            populationRequestMap.put("cmd", "updateTestPopulation");
            Map<String, Object> populationData = new LinkedHashMap<>();
            populationData.put("generation", 24);
            populationData.put("tests", Arrays.asList("test0", "test1", "test2"));
            populationData.put("classname", "TestCasePopulation24");
            File file = File.createTempFile( "TestCasePopulation", null);
            file.deleteOnExit();
            populationData.put("filepath", file.getAbsolutePath());
            populationRequestMap.put("data", populationData);
            List<String> populationResponse = client.sendRequest(populationRequestMap, new TypeReference<List<String>>() {});
            Assert.assertEquals(populationResponse, Arrays.asList("24", "test0", "test1", "test2", "TestCasePopulation24"));

            // Get patch validation result
            Map<String, Object> patchValidationRequestMap = new LinkedHashMap<>();
            patchValidationRequestMap.put("cmd", "getPatchValidationResult");
            Map<String, Object> patchValidationData = new LinkedHashMap<>();
            patchValidationData.put("test", "test1");
            patchValidationData.put("patchId", "7");
            patchValidationRequestMap.put("data", patchValidationData);

            PatchValidationResult validationResult = client.sendRequest(patchValidationRequestMap, new TypeReference<PatchValidationResult>() {});
            Assert.assertEquals(validationResult.test, "test1");
            Assert.assertEquals(validationResult.patchId, 7);
            Assert.assertEquals(validationResult.result, true);

        } catch (IOException e) {
            throw new RuntimeException("Unable to start server: " + e);
        } finally {
            client.close();
        }
    }

    public static class PatchValidationResult {
        private String test;
        private int patchId;
        private boolean result;

        public PatchValidationResult() {}
        public PatchValidationResult(String test, int patchId, boolean result) {
            this.patchId = patchId;
            this.test = test;
            this.result = result;
        }

        public void setTest(String test) {
            this.test = test;
        }
        public void setPatchId(int patchId) {
            this.patchId = patchId;
        }
        public void setResult(boolean result) {
            this.result = result;
        }
    }
}
