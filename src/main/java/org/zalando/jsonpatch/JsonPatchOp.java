package org.zalando.jsonpatch;

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
enum JsonPatchOp {
    ADD,
    REPLACE,
    REMOVE,
    MOVE,
    COPY,
    TEST;

    private final String name = this.name().toLowerCase().intern();

    public static JsonPatchOp fromRfcName(String name) throws JsonPatchException {
        for (JsonPatchOp  op : JsonPatchOp.values()) {
            if (op.name.equalsIgnoreCase(name)) {
                return op;
            }
        }
        throw new JsonPatchException("invalid patch (unsupported operation: " + name +")");
    }

    public String getName() {
        return this.name;
    }
}
