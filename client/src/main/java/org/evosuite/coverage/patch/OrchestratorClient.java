package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class OrchestratorClient {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorClient.class);

    private final Socket socket;

    // Configure JsonFactory to not close streams after writing (otherwise Sockets will be closed too)
    private final ObjectMapper mapper;

    private static OrchestratorClient instance = null;

    /**
     * Creates a OrchestratorClient instance. Data is exchanged through a TCP connection with the orchestrator.
     * Communication format is in JSON:
     * {
     *     "cmd": cmdId,    // integer command id
     *     "data": {        // data related to command
     *         ...
     *     }
     * }
     */
    public OrchestratorClient() {
        try {
            socket = new Socket("localhost",7777); // TODO: Make port number configurable
            mapper = new ObjectMapper();
            mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
            mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
            logger.info("Connected to orchestrator through port 7777.");
        } catch (IOException e) {
            close();
            throw new RuntimeException("Error while establishing connection with orchestrator: " + e);
        }
    }

    public static OrchestratorClient getInstance() {
        if (instance == null) {
            instance = new OrchestratorClient();
        }
        return instance;
    }

    /**
     * Sends a simple cmd request (i.e., without any associated data) to the orchestrator.
     * Returns the JSON reply as a POJO of type T.
     * TODO: can potentially be optimized using JsonGenerator
     *
     * @param cmd the id of the request (e.g., 1 to receive the patch pool) TODO: document cmd ids, implement as enum
     * @param resultType the generic Java type of the expected reply
     * @return the JSON reply of the orchestrator as an instance of the type represented by resultType
     * @param <T> the type of the return object
     */
    public <T> T sendRequest (String cmd, TypeReference<T> resultType) {
        Map<String, Object> map = new HashMap<>();
        map.put("cmd", cmd);
        return sendRequest(map, resultType);
    }

    /**
     * Sends a cmd request with data (given as key-value pairs) to the orchestrator.
     * @param request the map of key-value pairs of the request (must contain cmd and data as keys)
     * @param resultType the generic Java type of the expected reply
     * @return the JSON reply of the orchestrator as an instance of the type represented by resultFormat
     * @param <T> the type of the return object
     */
    public <T> T sendRequest(Map<String, Object> request, TypeReference<T> resultType) {
        try {
            mapper.writeValue(socket.getOutputStream(), request); // send request to orchestrator over socket
            return getJSONReply(request.get("cmd").toString(), resultType);
        } catch (IOException e) {
            throw new RuntimeException("Error while sending request: " + request.toString() + "\n" + e);
        }
    }

    /**
     * Unmarshalls the JSON reply of the orchestrator to the given result type.
     * @param originalCmd the original cmd send to the orchestrator, will be checked against the cmd in the reply
     * @param resultType the generic Java type of the expected reply
     * @return the JSON reply of the orchestrator as an instance of the type represented by resultFormat
     * @param <T> the type of the return object
     */
    public <T> T getJSONReply(String originalCmd, TypeReference<T> resultType) {
        // Create a JsonParser instance
        try (JsonParser jsonParser = mapper.getFactory().createParser(socket.getInputStream())) {

            // Check the first token
            if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected content to be a object.");
            }

            // First property of JSON object should be "cmd"
            if (jsonParser.nextToken() != JsonToken.FIELD_NAME || !jsonParser.getCurrentName().equals("cmd")) {
                throw new IllegalStateException("Expected cmd field.");
            }

            // "cmd" property should hold an int value
            if (jsonParser.nextToken() != JsonToken.VALUE_STRING) {
                throw new IllegalArgumentException("Cmd value must be an integer.");
            }

            String cmd = jsonParser.getValueAsString();
            if (!cmd.equals(originalCmd)) {
                throw new IllegalArgumentException("Cmd does not match original request, expected: " + originalCmd);
            }

            // The next property should be the data corresponding to the command
            if (jsonParser.nextToken() != JsonToken.FIELD_NAME || !jsonParser.getCurrentName().equals("data")) {
                throw new IllegalStateException("Expected data after cmd.");
            }

            // Received data must be a JSON object or array (TODO: or should we enforce it to always be an object?)
            JsonToken dataType = jsonParser.nextToken();
            if (dataType != JsonToken.START_OBJECT && dataType != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected data content.");
            }

            // Unmarshall JSON to instanceof T
            T result = mapper.readValue(jsonParser, resultType);

            // process "end" of reply object
            if (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                throw new IllegalStateException("Expected end of data object.");
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error while reading data: " + e);
        }
    }

    public void close() {
        // clean up connections
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while closing streams/connections: " + e);
        }
    }
}
