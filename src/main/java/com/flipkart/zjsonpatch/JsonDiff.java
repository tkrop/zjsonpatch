package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Set;

/**
 * User: gopi.vishwakarma Date: 30/07/14
 */
public final class JsonDiff {

    private JsonDiff() {
    }

    public static JsonNode asJson(final JsonNode source, final JsonNode target) {
        return asJson(source, target, CompatibilityFlags.defaults());
    }

    public static JsonNode asJson(final JsonNode source, final JsonNode target, final Set<CompatibilityFlags> flags) {
        List<Diff> diffs = createJsonPatchGenerator(flags).create(source, target);

        if (!flags.contains(CompatibilityFlags.DISABLE_PATCH_OPTIMIZATION)) {
            new CompactHelper(flags).compact(diffs);
        }

        return getJsonNodes(diffs, flags);
    }

    private static JsonPatchGenerator createJsonPatchGenerator(final Set<CompatibilityFlags> flags) {
        if (flags.contains(CompatibilityFlags.ENABLE_FAST_PATCH_GENERATOR)) {
            return new JsonPatchFastGenerator(flags);
        } else if (flags.contains(CompatibilityFlags.ENABLE_OPT_PATCH_GENERATOR)) {
            return new JsonPatchOptGenerator(flags);
        } else if (flags.contains(CompatibilityFlags.ENABLE_ORIG_PATCH_GENERATOR)) {
            return new JsonPatchOrigGenerator(flags);
        }
        throw new JsonPatchApplicationException("no patch generator enabled");
    }

    private static ArrayNode getJsonNodes(List<Diff> diffs, Set<CompatibilityFlags> flags) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        final ArrayNode patch = factory.arrayNode();
        for (Diff diff : diffs) {
            patch.add(getJsonNode(factory.objectNode(), diff, flags));
        }
        return patch;
    }

    private static JsonNode getJsonNode(ObjectNode node, Diff diff, Set<CompatibilityFlags> flags) {
        node.put(Constants.OP, diff.getOp().getName());
        node.put(Constants.PATH, JsonPathHelper.getPathRep(diff.getPath()));
        if (Operation.MOVE.equals(diff.getOp())) {
            node.put(Constants.FROM, JsonPathHelper.getPathRep(diff.getPath())); // required {from} only in case of Move Operation
            node.put(Constants.PATH, JsonPathHelper.getPathRep(diff.getToPath())); // destination Path
        } else if (!Operation.REMOVE.equals(diff.getOp())) { // setting only for Non-Remove operation
            if (!diff.getValue().isNull() || !flags.contains(CompatibilityFlags.MISSING_VALUES_AS_NULLS)) {
                node.set(Constants.VALUE, diff.getValue());
            }
        }
        return node;
    }
}
