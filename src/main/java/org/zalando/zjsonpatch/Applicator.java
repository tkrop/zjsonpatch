package org.zalando.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.zalando.zjsonpatch.Constants.FROM;
import static org.zalando.zjsonpatch.Constants.OP;
import static org.zalando.zjsonpatch.Constants.PATH;
import static org.zalando.zjsonpatch.Constants.VALUE;
import static org.zalando.zjsonpatch.FeatureFlags.MISSING_VALUES_AS_NULLS;

public final class Applicator {

    /**
     * Set of feature flags for patch application.
     */
    private final Set<FeatureFlags> flags;

    public Applicator(Set<FeatureFlags> flags) {
        this.flags = flags;
    }

    private JsonNode resolve(JsonNode node, String attr) {
        JsonNode child = node.get(attr);
        if (child == null) {
            throw new JsonPatchException("invalid patch (missing field: " + attr + ")");
        }
        return child;
    }

    private JsonNode resolve(JsonNode node, String attr, JsonNode defaultValue) {
        JsonNode child = node.get(attr);
        return (child == null) ? defaultValue : child;
    }

    private List<String> path(JsonNode patch, String path) {
        return PathHelper.getPath(resolve(patch, path).asText());
    }

    private JsonNode value(JsonNode node) {
        if (!flags.contains(MISSING_VALUES_AS_NULLS)) {
            return resolve(node, VALUE);
        } else {
            return resolve(node, VALUE, NullNode.getInstance());
        }
    }

    private void patch(Processor processor, JsonNode patch) {
        OpType operation = OpType.fromRfcName(resolve(patch, OP).asText());
        switch (operation) {
            case ADD: {
                JsonNode value = value(patch);
                processor.add(path(patch, PATH), value);
                break;
            }

            case TEST: {
                JsonNode value = value(patch);
                processor.test(path(patch, PATH), value);
                break;
            }

            case REPLACE: {
                JsonNode value = value(patch);
                processor.replace(path(patch, PATH), value);
                break;
            }

            case REMOVE: {
                processor.remove(path(patch, PATH));
                break;
            }

            case MOVE: {
                processor.move(path(patch, FROM), path(patch, PATH));
                break;
            }

            case COPY: {
                processor.copy(path(patch, FROM), path(patch, PATH));
                break;
            }

            default:
                throw new JsonPatchException("invalid patch (unsupported operation: " +
                        resolve(patch, OP).asText() + ")");
        }
    }

    private List<JsonPatchException> apply(Processor processor, Iterator<JsonNode> patches) {
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
                    throw new JsonPatchException(message(except, patch, index), except);
                }
            } catch (JsonPatchException except) {
                excepts.add(except);
            }
        }
        return excepts;
    }

    public JsonNode apply(Processor processor, JsonNode patch) {
        if (!patch.isArray()) {
            throw new JsonPatchException("invalid patch (root not an array)");
        }
        rethrow(apply(processor, patch.iterator()));
        return processor.result();
    }

    private String message(Exception except, JsonNode patch, int index) {
        return "[" + resolve(patch, Constants.OP).asText() + "] "
                + except.getMessage() + "\n\t\t=> applying patch/" + index + ": " + patch;
    }

    private void rethrow(List<JsonPatchException> excepts) {
        switch (excepts.size()) {
            case 0:
                return;

            case 1:
                throw excepts.get(0);

            default:
                StringBuilder message = new StringBuilder("invalid patch");
                for (Exception suppressed : excepts) {
                    message.append("\n\t").append(suppressed.getMessage());
                }
                JsonPatchException except = new JsonPatchException(message.toString());
                /* Java 1.7 extension for (Exception suppressed : excepts) { except.addSuppressed(suppressed); } */
                throw except;
        }
    }
}
