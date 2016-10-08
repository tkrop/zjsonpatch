package org.zalando.jsonpatch;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Static methods used for patch access and failure handling.
 */
final class JsonPatchHelper {

    public static JsonNode getPatchAttr(JsonNode node, String attr) {
        JsonNode child = node.get(attr);
        if (child == null) {
            throw new JsonPatchException("invalid patch (missing field: " + attr + ")");
        }
        return child;
    }

    public static JsonNode getPatchAttrWithDefault(JsonNode node, String attr, JsonNode defaultValue) {
        JsonNode child = node.get(attr);
        return (child == null) ? defaultValue : child;
    }

    public static String getErrorMessage(Exception except, JsonNode patch, int index) {
        return "[" + getPatchAttr(patch, Constants.OP).asText() + "] "
                + except.getMessage() + "\n\t\t => applying patch/" + index + ": " + patch;
    }

    public static void rethrowExceptions(List<JsonPatchException> excepts) {
        switch (excepts.size()) {
            case 0:
                return;
            
            case 1:
                throw excepts.get(0);
    
            default:
                StringBuilder message = new StringBuilder("invalid patch\n");
                for (Exception suppressed : excepts) {
                    message.append('\t').append(suppressed.getMessage());
                }
                JsonPatchException except = new JsonPatchException(message.toString());
                /* Java 1.7 extension
                for (Exception suppressed : excepts) {
                    except.addSuppressed(suppressed);
                } */
                throw except;
        }
    }
}
