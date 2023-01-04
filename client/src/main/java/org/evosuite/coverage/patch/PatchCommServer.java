package org.evosuite.coverage.patch;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;


public class PatchCommServer {
    private static final Logger logger = LoggerFactory.getLogger(PatchCommServer.class);

    private final ServerSocket serverSocket;
    private final Socket clientSocket;
    private final OutputStreamWriter out;
    private final BufferedReader in;

    // Configure JsonFactory to not close streams after writing (otherwise Sockets will be closed too)
    private final JsonFactory jsonFactory = new JsonFactory().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    private final ObjectMapper mapper = new ObjectMapper(jsonFactory);

    private static PatchCommServer instance = null;

    /**
     * Creates a PatchCommServer instance. Data is exchanged through a TCP connection with the orchestrator.
     * Communication format is in JSON:
     * {
     *     "cmd": cmdId,    // integer command id
     *     "data": {        // data related to command
     *         ...
     *     }
     * }
     */
    public PatchCommServer() {
        try {
            serverSocket = new ServerSocket(7777); // TODO: Make port number configurable
            logger.info("Waiting for orchestrator to establish connection.");
            clientSocket = serverSocket.accept();
            out = new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            logger.info("Connected to orchestrator through port 7777.");
        } catch (IOException e) {
            clear();
            throw new RuntimeException("Error while establishing connection with orchestrator: " + e.getMessage());
        }
    }

    public static PatchCommServer getInstance() {
        if (instance == null) {
            instance = new PatchCommServer();
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
     * @return the JSON reply of the orchestrator as an instance of T
     * @param <T> the type of the return object
     */
    public <T> T sendRequest (int cmd, TypeReference<T> resultType) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cmd", cmd);
        return sendRequest(map, resultType);
    }

    /**
     * Sends a cmd request with data (given as key-value pairs) to the orchestrator.
     * @param request the map of key-value pairs of the request (must contain cmd and data as keys)
     * @param resultType the generic Java type of the expected reply
     * @return the JSON reply of the orchestrator as an instance of T
     * @param <T> the type of the return object
     */
    public <T> T sendRequest(Map<String, Object> request, TypeReference<T> resultType) {
        try {
            mapper.writeValue(out, request); // send request to orchestrator over socket
            return getJSONReply((int) request.get("cmd"), resultType);
        } catch (Exception e) {
            throw new RuntimeException("Error while sending request: " + request.toString() + e.getMessage());
        }
    }

    /**
     * Unmarshalls the JSON reply of the orchestrator to the given result type.
     * @param requestedCmd the original cmd send to the orchestrator, will be checked against the cmd in the reply
     * @param resultType the generic Java type of the expected reply
     * @return the JSON reply of the orchestrator as an instance of T
     * @param <T> the type of the return object
     */
    public <T> T getJSONReply(int requestedCmd, TypeReference<T> resultType) {
        // Create a JsonParser instance
        try (JsonParser jsonParser = mapper.getFactory().createParser(in)) {

            // Check the first token
            if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected content to be a object.");
            }

            // First property of object should be "cmd"
            if (jsonParser.nextToken() != JsonToken.FIELD_NAME || !jsonParser.getCurrentName().equals("cmd")) {
                throw new IllegalStateException("Expected cmd field.");
            }

            // "cmd" property should hold an int value
            if (jsonParser.nextToken() != JsonToken.VALUE_NUMBER_INT) {
                throw new IllegalArgumentException("Cmd value must be an integer.");
            }

            int cmd = jsonParser.getIntValue();
            logger.info("Read cmd: " + cmd);

            // Cmd in reply should match cmd from original request
            if (cmd != requestedCmd) {
                throw new IllegalArgumentException("Cmd does not match original request, expected: " + requestedCmd);
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

            // End of reply object
            if (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                throw new IllegalStateException("Expected end of data object.");
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error while reading data: " + e.getMessage());
        }
    }

    public void clear() {
        // clean up connections
        try {
            if (serverSocket != null) serverSocket.close();
            if (clientSocket != null) clientSocket.close();
            if (in != null) in.close();
            if (out != null) out.close();

        } catch (IOException e) {
            throw new RuntimeException("Error while closing streams/connections: " + e.getMessage());
        }
    }
}


