package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * User: gopi.vishwakarma Date: 31/07/14
 */
public final class JsonPatch {
    private final Set<FeatureFlags> flags;

    private JsonPatch(Set<FeatureFlags> flags) {
        this.flags = flags;
    }

    private JsonNode value(JsonNode node) {
        if (!flags.contains(FeatureFlags.MISSING_VALUES_AS_NULLS)) {
            return JsonPatchHelper.getPatchAttr(node, Constants.VALUE);
        } else {
            return JsonPatchHelper.getPatchAttrWithDefault(node, Constants.VALUE, NullNode.getInstance());
        }
    }

    private void patch(JsonPatchProcessor processor, JsonNode patch) {
        Operation operation = Operation.fromRfcName(JsonPatchHelper.getPatchAttr(patch, Constants.OP).asText());
        List<String> path = JsonPathHelper.getPath(JsonPatchHelper.getPatchAttr(patch, Constants.PATH).asText());

        switch (operation) {
            case ADD: {
                JsonNode value = value(patch);
                processor.add(path, value);
                break;
            }

            case TEST: {
                JsonNode value = value(patch);
                processor.test(path, value);
                break;
            }

            case REPLACE: {
                JsonNode value = value(patch);
                processor.replace(path, value);
                break;
            }

            case REMOVE: {
                processor.remove(path);
                break;
            }

            case MOVE: {
                List<String> fromPath =
                        JsonPathHelper.getPath(JsonPatchHelper.getPatchAttr(patch, Constants.FROM).asText());
                processor.move(fromPath, path);
                break;
            }

            case COPY: {
                List<String> fromPath =
                        JsonPathHelper.getPath(JsonPatchHelper.getPatchAttr(patch, Constants.FROM).asText());
                processor.copy(fromPath, path);
                break;
            }
        }
    }

    private List<JsonPatchException> apply(JsonPatchProcessor processor, Iterator<JsonNode> patches) {
        List<JsonPatchException> excepts = new ArrayList<JsonPatchException>();
        for (int index = 0; patches.hasNext(); index++) {
            try {
                JsonNode patch = patches.next();
                if (!patch.isObject()) {
                    throw new JsonPatchException("invalid patch (patch not an object - index: " + index + ")");
                }
                try {
                    patch(processor, patch);
                } catch (JsonPatchException except) {
                    throw new JsonPatchException(JsonPatchHelper.getErrorMessage(except, patch, index), except);
                }
            } catch (JsonPatchException except) {
                excepts.add(except);
            }
        }
        return excepts;
    }

    private JsonNode apply(JsonPatchProcessor processor, JsonNode patch) {
        if (!patch.isArray()) {
            throw new JsonPatchException("invalid patch (root not an array)");
        }
        JsonPatchHelper.rethrowExceptions(apply(processor, patch.iterator()));
        return processor.result();
    }

    public static void validate(JsonNode patch, Set<FeatureFlags> flags) throws JsonPatchException {
        new JsonPatch(flags).apply(JsonPatchNoopProcessor.INSTANCE, patch);
    }

    public static void validate(JsonNode patch) throws JsonPatchException {
        validate(patch, FeatureFlags.defaults());
    }

    public static JsonNode apply(JsonNode patch, JsonNode source, Set<FeatureFlags> flags)
            throws JsonPatchException {
        JsonNode target = flags.contains(FeatureFlags.ENABLE_PATCH_IN_PLACE) ? source : source.deepCopy();
        return new JsonPatch(flags).apply(new JsonPatchApplyProcessor(target), patch);
    }

    public static JsonNode apply(JsonNode patch, JsonNode source) throws JsonPatchException {
        return apply(patch, source, FeatureFlags.defaults());
    }
}
