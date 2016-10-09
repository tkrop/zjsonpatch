package org.zalando.jsonpatch;

/**
 * JSON Patch operation type.
 */
enum OpType {
    /**
     * Unknown JSON Patch operation type (default/fallback).
     */
    UNKNOWN,

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

    public static OpType fromRfcName(String name) throws JsonPatchException {
        for (OpType type : OpType.values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return UNKNOWN;
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
