package org.evosuite.coverage.patch.communication.json;

import java.io.Serializable;

// TODO: Probably does not need to implement Serializable interface if not actually serialized
public class JsonFilePath implements Serializable {
    private static final long serialVersionUID = 8224763132037572291L;

    private String path;

    public JsonFilePath() {}

    public JsonFilePath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
    public void setPath(String path) {
        this.path = path;
    }
}
