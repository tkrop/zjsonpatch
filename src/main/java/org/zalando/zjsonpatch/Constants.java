package org.zalando.zjsonpatch;

/**
 * Field names of JSON Patch attributes.
 */
abstract class Constants {
    /**
     * Patch operation type field name.
     */
    public static String OP = "op";

    /**
     * Patch operation value field name.
     */
    public static String VALUE = "value";

    /**
     * Patch operation path field name.
     */
    public static String PATH = "path";

    /**
     * Patch operation from field name (used for move).
     */
    public static String FROM = "from";
}
