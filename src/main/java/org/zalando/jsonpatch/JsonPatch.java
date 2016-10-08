package org.zalando.jsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class JsonPatch {
    private static final EnumSet<FeatureFlags> DEFAULT = EnumSet.of(FeatureFlags.PATCH_OPTIMIZATION);
    private final Set<FeatureFlags> flags;

    private static interface Processor {
        static final Noop NOOP = new Noop();

        /**
         * A processor for validation and testing.
         */
        public class Noop implements Processor {
            @Override
            public JsonNode add(List<String> path, JsonNode value) {
                return null;
            }

            @Override
            public JsonNode test(List<String> path, JsonNode value) {
                return null;
            }

            @Override
            public JsonNode replace(List<String> path, JsonNode value) {
                return null;
            }

            @Override
            public JsonNode remove(List<String> path) {
                return null;
            }

            @Override
            public JsonNode move(List<String> fromPath, List<String> toPath) {
                return null;
            }

            @Override
            public JsonNode copy(List<String> fromPath, List<String> toPath) {
                return null;
            }

            @Override
            public JsonNode result() {
                return null;
            }
        }

        class Apply implements Processor {

            private JsonNode target;

            Apply(JsonNode target) {
                this.target = target;
            }

            @Override
            public JsonNode result() {
                return target;
            }

            @Override
            public JsonNode add(List<String> path, JsonNode value) {
                JsonNode parent = getParent(path);
                if (parent == null) {
                    throw new JsonPatchException(
                            "no such path in source (path: " + JsonPathHelper.toString(path) + ")");
                }
                String field = path.get(path.size() - 1);
                if (field.isEmpty() && path.size() == 1) {
                    target = value;
                } else if (!parent.isContainerNode()) {
                    throw new JsonPatchException(
                            "parent is not a container in source (path: " + JsonPathHelper.toString(path)
                                    + " | node: " + parent + ")");
                } else if (parent.isArray()) {
                    addToArray(path, value, parent);
                } else {
                    addToObject(path, parent, value);
                }
                return value;
            }

            private void addToArray(List<String> path, JsonNode value, JsonNode parent) {
                final ArrayNode target = (ArrayNode) parent;
                String idxStr = path.get(path.size() - 1);

                if ("-".equals(idxStr)) {
                    // see http://tools.ietf.org/html/rfc6902#section-4.1
                    target.add(value);
                } else {
                    int idx = arrayIndex(idxStr, target.size(), path);
                    target.insert(idx, value);
                }
            }

            private void addToObject(List<String> path, JsonNode node, JsonNode value) {
                final ObjectNode target = (ObjectNode) node;
                String key = path.get(path.size() - 1);
                target.set(key, value);
            }

            @Override
            public JsonNode test(List<String> path, JsonNode value) {
                JsonNode node = getNode(target, path, 1);
                if (node == null) {
                    throw new JsonPatchException(
                            "no such path in source (path: " + JsonPathHelper.toString(path) + ")");
                } else if (!node.equals(value)) {
                    throw new JsonPatchException(
                            "value differs from expectations (path: " + JsonPathHelper.toString(path)
                                    + " | value: " + value + " | node: " + node + ")");
                }
                return value;
            }

            @Override
            public JsonNode replace(List<String> path, JsonNode value) {
                JsonNode parent = getParent(path);
                if (parent == null) {
                    throw new JsonPatchException(
                            "no such path in source (path: " + JsonPathHelper.toString(path) + ")");
                }
                String field = path.get(path.size() - 1);
                if (field.isEmpty() && path.size() == 1) {
                    target = value;
                } else if (parent.isObject()) {
                    ((ObjectNode) parent).set(field, value);
                } else if (parent.isArray()) {
                    ((ArrayNode) parent).set(arrayIndex(field, parent.size() - 1, path), value);
                } else {
                    throw new JsonPatchException(
                            "no such path in source (path: " + JsonPathHelper.toString(path) + ")");
                }
                return value;
            }

            @Override
            public JsonNode remove(List<String> path) {
                JsonNode parent = getParent(path);
                if (parent == null) {
                    throw new JsonPatchException(
                            "no such path in source (path: " + JsonPathHelper.toString(path) + ")");
                }
                String field = path.get(path.size() - 1);
                if (parent.isObject()) {
                    return ((ObjectNode) parent).remove(field);
                } else if (parent.isArray()) {
                    return ((ArrayNode) parent).remove(arrayIndex(field, parent.size() - 1, path));
                }
                throw new JsonPatchException(
                        "no such path in source (path: " + JsonPathHelper.toString(path) + ")");
            }

            @Override
            public JsonNode move(List<String> fromPath, List<String> toPath) {
                return add(toPath, remove(fromPath));
            }

            @Override
            public JsonNode copy(List<String> fromPath, List<String> toPath) {
                return add(toPath, getNode(target, fromPath, 1));
            }

            private JsonNode getParent(List<String> path) {
                return getNode(target, path.subList(0, path.size() - 1), 1);
            }

            private JsonNode getNode(JsonNode node, List<String> path, int index) {
                if (index >= path.size()) {
                    return node;
                }
                String key = path.get(index);
                if (node.isArray()) {
                    int keyInt = Integer.parseInt(key);
                    JsonNode element = node.get(keyInt);
                    if (element == null)
                        return null;
                    else
                        return getNode(node.get(keyInt), path, ++index);
                } else if (node.isObject()) {
                    if (node.has(key)) {
                        return getNode(node.get(key), path, ++index);
                    }
                    return null;
                } else {
                    return node;
                }
            }

            private int arrayIndex(String string, int max, List<String> path) {
                int index = Integer.parseInt(string);
                if (index < 0 || index > max) {
                    throw new JsonPatchException(
                            "index out of bounds (path: " + JsonPathHelper.toString(path) + " | index: " + index
                                    + " | bounds: 0-" + max + ")");
                }
                return index;
            }
        }

        public abstract JsonNode add(List<String> path, JsonNode value);

        public abstract JsonNode test(List<String> path, JsonNode value);

        public abstract JsonNode replace(List<String> path, JsonNode value);

        public abstract JsonNode remove(List<String> path);

        public abstract JsonNode move(List<String> fromPath, List<String> toPath);

        public abstract JsonNode copy(List<String> fromPath, List<String> toPath);

        public abstract JsonNode result();
    }

    /**
     * Static methods used for patch access and failure handling.
     */
    private static final class Helper {

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
                    /* Java 1.7 extension for (Exception suppressed : excepts) { except.addSuppressed(suppressed); } */
                    throw except;
            }
        }
    }

    private JsonPatch(Set<FeatureFlags> flags) {
        this.flags = flags;
    }

    private JsonNode value(JsonNode node) {
        if (!flags.contains(FeatureFlags.MISSING_VALUES_AS_NULLS)) {
            return Helper.getPatchAttr(node, Constants.VALUE);
        } else {
            return Helper.getPatchAttrWithDefault(node, Constants.VALUE, NullNode.getInstance());
        }
    }

    private void patch(Processor processor, JsonNode patch) {
        JsonPatchOp operation = JsonPatchOp.fromRfcName(Helper.getPatchAttr(patch, Constants.OP).asText());
        List<String> path = JsonPathHelper.getPath(Helper.getPatchAttr(patch, Constants.PATH).asText());

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
                        JsonPathHelper.getPath(Helper.getPatchAttr(patch, Constants.FROM).asText());
                processor.move(fromPath, path);
                break;
            }

            case COPY: {
                List<String> fromPath =
                        JsonPathHelper.getPath(Helper.getPatchAttr(patch, Constants.FROM).asText());
                processor.copy(fromPath, path);
                break;
            }
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
                    throw new JsonPatchException(Helper.getErrorMessage(except, patch, index), except);
                }
            } catch (JsonPatchException except) {
                excepts.add(except);
            }
        }
        return excepts;
    }

    private JsonNode apply(Processor processor, JsonNode patch) {
        if (!patch.isArray()) {
            throw new JsonPatchException("invalid patch (root not an array)");
        }
        Helper.rethrowExceptions(apply(processor, patch.iterator()));
        return processor.result();
    }

    public static void validate(JsonNode patch, Set<FeatureFlags> flags) throws JsonPatchException {
        new JsonPatch(flags).apply(Processor.NOOP, patch);
    }

    public static void validate(JsonNode patch) throws JsonPatchException {
        validate(patch, DEFAULT);
    }

    public static JsonNode apply(JsonNode patch, JsonNode source, Set<FeatureFlags> flags)
            throws JsonPatchException {
        JsonNode target = flags.contains(FeatureFlags.PATCH_IN_PLACE) ? source : source.deepCopy();
        return new JsonPatch(flags).apply(new Processor.Apply(target), patch);
    }

    public static JsonNode apply(JsonNode patch, JsonNode source) throws JsonPatchException {
        return apply(patch, source, DEFAULT);
    }
}
