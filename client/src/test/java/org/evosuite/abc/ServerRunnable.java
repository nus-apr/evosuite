package org.evosuite.abc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.evosuite.coverage.patch.Patch;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Example implementation of an Orchestrator-Server for testing purposes.
 * Sends dummy data based on the requests sent by the client.
 */

class ServerRunnable implements Runnable {
    private final ServerSocket serverSocket;
    private final ObjectMapper mapper;

    public ServerRunnable(int port) throws IOException {
        // listen on any free port
        serverSocket = new ServerSocket(port);
        mapper = new ObjectMapper();
        mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    public void run() {
        try {
            System.out.println("[Server] Waiting for connection.");
            Socket clientSocket = serverSocket.accept();
            System.out.println("[Server] Connected to client at local port: " + clientSocket.getLocalPort());

            requestLoop:
            while (true) {
                try {
                    Map<String, Object> requestMap = mapper.readValue(clientSocket.getInputStream(), Map.class);
                    String cmd = requestMap.get("cmd").toString();
                    switch(cmd) {
                        case "getPatchPool":
                            System.out.println("[Server] Sending patch pool.");
                            getPatchPool(clientSocket);
                            break;
                        case "updateTestPopulation":
                            System.out.println("[Server] Updating test population.");
                            updateTestPopulation(requestMap, clientSocket);
                            break;
                        case "getPatchValidationResult":
                            System.out.println("[Server] Sending patch validation result.");
                            getPatchValidationResult(requestMap, clientSocket);
                            break;
                        case "closeConnection":
                            break requestLoop;
                        default:
                            System.out.println("[Server] Unknown command: " + cmd + ". Sending back echo.");
                            // Echo back original request
                            if(!requestMap.containsKey("data")) {
                                requestMap.put("data", Arrays.asList(4,0,4));
                                mapper.writeValue(clientSocket.getOutputStream(), requestMap);
                            }
                            break;
                    }
                } catch (IOException e) {
                    if (e instanceof MismatchedInputException) {
                        System.out.println("[Server] Client closed connection.");
                        break;
                    }
                     else {
                        throw new RuntimeException(e);
                    }
                }
            }

            serverSocket.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getPatchPool(Socket clientSocket) throws IOException {
        // Must use LinkedHashMap to preserve order of cmd + data fields
        Map<String, Object> replyMap = new LinkedHashMap<>();
        replyMap.put("cmd", "getPatchPool");

        List<Patch> patchPool = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            patchPool.add(new Patch(i));
        }

        replyMap.put("data", patchPool);
        mapper.writeValue(clientSocket.getOutputStream(), replyMap);
    }

    private void updateTestPopulation(Map<String, Object> requestMap, Socket clientSocket) throws IOException {
        JsonNode rootNode = mapper.valueToTree(requestMap);
        String population = rootNode.at("/data/Generation").asText();
        List<String> testNames = mapper.treeToValue(rootNode.at("/data/Tests"), List.class);
        String className = rootNode.at("/data/Classname").asText();
        String filePath = rootNode.at("/data/Filepath").asText();

        Map<String, Object> replyMap = new LinkedHashMap<>();
        replyMap.put("cmd", "updateTestPopulation");

        // Simply return concatenated data to client
        List<String> replyData = new ArrayList<>();
        replyData.add(population);
        replyData.addAll(testNames);
        replyData.add(className);
        replyMap.put("data", replyData);
        mapper.writeValue(clientSocket.getOutputStream(), replyMap);
    }

    private void getPatchValidationResult(Map<String, Object> requestMap, Socket clientSocket) throws IOException{
        JsonNode rootNode = mapper.valueToTree(requestMap);
        String testName = rootNode.at("/data/test").asText();
        int patchID = rootNode.at("/data/patchId").asInt();
        boolean validationResult;
        if (testName.equals("test1") && patchID == 7) {
            validationResult = true;
        } else {
            validationResult = false;
        }

        // Add patch validation result to original request map and send back
        Map<String, Object> resultMap = (Map<String, Object>) requestMap.get("data");
        resultMap.put("result", validationResult);
        mapper.writeValue(clientSocket.getOutputStream(), requestMap);
    }
}
