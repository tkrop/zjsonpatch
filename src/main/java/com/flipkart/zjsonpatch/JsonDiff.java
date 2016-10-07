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

    public static JsonNode asJson(final JsonNode source, final JsonNode target) {
        return asJson(source, target, FeatureFlags.defaults());
    }

    public static JsonNode asJson(final JsonNode source, final JsonNode target, final Set<FeatureFlags> flags) {
        List<Diff> diffs = createJsonPatchGenerator(flags).create(source, target);

        if (!flags.contains(FeatureFlags.DISABLE_PATCH_OPTIMIZATION)) {
            new JsonPatchCompactHelper(flags).compact(diffs);
        }

        return getJsonNodes(diffs, flags);
    }

    private static JsonPatchGenerator createJsonPatchGenerator(final Set<FeatureFlags> flags) {
        if (flags.contains(FeatureFlags.ENABLE_OPT_PATCH_GENERATOR)) {
            return new JsonPatchOptGenerator(flags);
        } else if (flags.contains(FeatureFlags.ENABLE_FAST_PATCH_GENERATOR)) {
            return new JsonPatchFastGenerator(flags);
        } else if (flags.contains(FeatureFlags.ENABLE_ORIG_PATCH_GENERATOR)) {
            return new JsonPatchOrigGenerator(flags);
        } else if (flags.contains(FeatureFlags.ENABLE_SAME_PATCH_GENERATOR)) {
            return new JsonPatchSameGenerator(flags);
        }
        return new JsonPatchOptGenerator(flags);
    }

    private static ArrayNode getJsonNodes(List<Diff> diffs, Set<FeatureFlags> flags) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        final ArrayNode patch = factory.arrayNode();
        for (Diff diff : diffs) {
            patch.add(getJsonNode(factory.objectNode(), diff, flags));
        }
        return patch;
    }

    private static JsonNode getJsonNode(ObjectNode node, Diff diff, Set<FeatureFlags> flags) {
        node.put(Constants.OP, diff.getOp().getName());
        node.put(Constants.PATH, JsonPathHelper.getPathRep(diff.getPath()));
        if (Operation.MOVE.equals(diff.getOp())) {
            node.put(Constants.FROM, JsonPathHelper.getPathRep(diff.getPath()));
            node.put(Constants.PATH, JsonPathHelper.getPathRep(diff.getToPath()));
        } else if (!Operation.REMOVE.equals(diff.getOp())) {
            if (!diff.getValue().isNull() || !flags.contains(FeatureFlags.MISSING_VALUES_AS_NULLS)) {
                node.set(Constants.VALUE, diff.getValue());
            }
        }
        return node;
    }
}
