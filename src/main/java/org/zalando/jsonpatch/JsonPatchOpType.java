package org.zalando.jsonpatch;

/**
 * JSON Patch operation type.
 */
enum JsonPatchOpType {

    /**
     * Add JSON Patch operation type.
     */
    ADD,

    /**
     * ReplaceE JSON Patch operation type.
     */
    REPLACE,

    /**
     * Remove JSON Patch operation type.
     */
    REMOVE,

    /**
     * Move JSON Patch operation type.
     */
    MOVE,

    /**
     * Copy JSON Patch operation type.
     */
    COPY,

    /**
     * Test JSON Patch operation type.
     */
    TEST;

    /**
     * RFC name of JSON Patch operation type.
     */
    private final String name = this.name().toLowerCase().intern();

    public static JsonPatchOpType fromRfcName(String name) throws JsonPatchException {
        for (JsonPatchOpType type : JsonPatchOpType.values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new JsonPatchException("invalid patch (unsupported operation: " + name + ")");
    }

    /**
     * Return RFC name of JSON Patch operation type.
     * 
     * @return RFC name of JSON Patch operation type.
     */
    public String getName() {
        return this.name;
    }
}
