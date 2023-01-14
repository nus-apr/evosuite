package org.evosuite.abc;

import com.fasterxml.jackson.core.type.TypeReference;
import org.evosuite.coverage.patch.communication.OrchestratorClient;
import org.evosuite.coverage.patch.communication.json.Patch;
import org.evosuite.coverage.patch.communication.json.SinglePatchValidationResult;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class OrchestratorClientTest {
    /**
     * If this is set to true, the Python server under 'evosuite/client/src/test/python/Server.py'
     * must be started before running any tests (must be run before starting each test).
     * TODO: Make python server listen for subsequent connections.
     */
    private static final boolean USE_PYTHON_SERVER = false;
    private OrchestratorClient client;
    @Ignore
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

    @Ignore
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
                Assert.assertEquals(String.valueOf(i), patchPool.get(i).getIndex());
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
            patchValidationData.put("testId", "test1");
            patchValidationData.put("patchId", "7");
            patchValidationRequestMap.put("data", patchValidationData);

            SinglePatchValidationResult validationResult = client.sendRequest(patchValidationRequestMap, new TypeReference<SinglePatchValidationResult>() {});
            Assert.assertEquals(validationResult.getTestId(), "test1");
            Assert.assertEquals(validationResult.getPatchId(), 7);
            Assert.assertEquals(validationResult.getResult(), true);

        } catch (IOException e) {
            throw new RuntimeException("Unable to start server: " + e);
        } finally {
            client.close();
        }
    }
}
