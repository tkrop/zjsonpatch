package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * User: gopi.vishwakarma Date: 30/07/14
 */
class Diff {
    private final Operation op;
    private final List<Object> path;
    private final JsonNode value;
    private List<Object> toPath; // only to be used in move operation

    Diff(Operation op, List<Object> path, JsonNode value) {
        this.op = op;
        this.path = path;
        this.value = value;
    }

    Diff(Operation operation, List<Object> fromPath, JsonNode value, List<Object> toPath) {
        this.op = operation;
        this.path = fromPath;
        this.value = value;
        this.toPath = toPath;
    }

    public Operation getOp() {
        return op;
    }

    public List<Object> getPath() {
        return path;
    }

    public JsonNode getValue() {
        return value;
    }

    List<Object> getToPath() {
        return toPath;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append("{ \"op\": \"")
                .append(op.getName())
                .append("\", path: \"")
                .append(JsonPathHelper.getPathRep(path));
        if (value != null) {
            builder.append("\", \"value\": ").append(value.toString());
        }
        if (toPath != null) {
            builder.append(", \"toPath\": \"").append(JsonPathHelper.getPathRep(toPath)).append('"');
        }
        return builder.append(" }").toString();
    }
}
