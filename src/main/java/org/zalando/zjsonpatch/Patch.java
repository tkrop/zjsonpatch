package org.zalando.zjsonpatch;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Patch operation entry.
 */
class Patch {
    /**
     * Patch operation type.
     */
    public final OpType type;

    /**
     * Patch operation path.
     */
    public final List<Object> path;

    /**
     * Patch operation from path (only used by move operation).
     */
    public final List<Object> from;

    /**
     * Patch operation value.
     */
    public final JsonNode value;

    /**
     * Create patch operation entry with given patch operation type, patch operation path, and patch operation
     * value.
     * 
     * @param type patch operation type.
     * @param path patch operation path.
     * @param value patch operation value.
     */
    public Patch(OpType type, List<Object> path, JsonNode value) {
        this(type, path, null, value);
    }

    /**
     * Create patch operation entry with given patch operation type, patch operation to-path, patch operation
     * from-path, and patch operation value.
     * 
     * @param type patch operation type.
     * @param toPath patch operation to-path.
     * @param fromPath patch operation from-path.
     * @param value patch operation value.
     */
    public Patch(OpType type, List<Object> toPath, List<Object> fromPath, JsonNode value) {
        this.type = type;
        this.path = toPath;
        this.from = fromPath;
        this.value = value;
    }
}