package org.zalando.zjsonpatch;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON Patch processor.
 */
abstract class Processor {
    /**
     * Singleton no operation JSON patch processor.
     */
    static final Processor.Noop NOOP = new Noop();

    /**
     * A processor for validation and testing.
     */
    private static class Noop extends Processor {
        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode add(List<String> path, JsonNode value) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode test(List<String> path, JsonNode value) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode replace(List<String> path, JsonNode value) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode remove(List<String> path) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode move(List<String> fromPath, List<String> toPath) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode copy(List<String> fromPath, List<String> toPath) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode result() {
            return null;
        }
    }

    protected static class Apply extends Processor {
        /**
         * Target JSON document.
         */
        private JsonNode target;

        Apply(JsonNode target) {
            this.target = target;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode result() {
            return target;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode add(List<String> path, JsonNode value) {
            JsonNode parent = getParent(path);
            if (parent == null) {
                throw new JsonPatchException(
                        "no such path in source (path: " + PathHelper.toString(path) + ")");
            }
            String field = path.get(path.size() - 1);
            if (field.isEmpty() && path.size() == 1) {
                target = value;
            } else if (!parent.isContainerNode()) {
                throw new JsonPatchException(
                        "parent is not a container in source (path: " + PathHelper.toString(path)
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

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode test(List<String> path, JsonNode value) {
            JsonNode node = getNode(target, path, 1);
            if (node == null) {
                throw new JsonPatchException(
                        "no such path in source (path: " + PathHelper.toString(path) + ")");
            } else if (!node.equals(value)) {
                throw new JsonPatchException(
                        "value differs from expectations (path: " + PathHelper.toString(path)
                                + " | value: " + value + " | node: " + node + ")");
            }
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode replace(List<String> path, JsonNode value) {
            JsonNode parent = getParent(path);
            if (parent == null) {
                throw new JsonPatchException(
                        "no such path in source (path: " + PathHelper.toString(path) + ")");
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
                        "no such path in source (path: " + PathHelper.toString(path) + ")");
            }
            return value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode remove(List<String> path) {
            JsonNode parent = getParent(path);
            if (parent == null) {
                throw new JsonPatchException(
                        "no such path in source (path: " + PathHelper.toString(path) + ")");
            }
            String field = path.get(path.size() - 1);
            if (parent.isObject()) {
                return ((ObjectNode) parent).remove(field);
            } else if (parent.isArray()) {
                return ((ArrayNode) parent).remove(arrayIndex(field, parent.size() - 1, path));
            }
            throw new JsonPatchException(
                    "no such path in source (path: " + PathHelper.toString(path) + ")");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JsonNode move(List<String> fromPath, List<String> toPath) {
            return add(toPath, remove(fromPath));
        }

        /**
         * {@inheritDoc}
         */
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
                        "index out of bounds (path: " + PathHelper.toString(path) + " | index: " + index
                                + " | bounds: 0-" + max + ")");
            }
            return index;
        }
    }

    /**
     * Apply add operation with given patch operation path and given and patch operation value.
     * 
     * @param path patch operation path.
     * @param value patch operation value.
     * 
     * @return added JSON node value.
     */
    public abstract JsonNode add(List<String> path, JsonNode value);

    /**
     * Apply test operation with given patch operation path and given and patch operation value.
     * 
     * @param path patch operation path.
     * @param value patch operation value.
     * 
     * @return tested JSON node value.
     */
    public abstract JsonNode test(List<String> path, JsonNode value);

    /**
     * Apply replace operation with given patch operation path and given and patch operation value.
     * 
     * @param path patch operation path.
     * @param value patch operation value.
     * 
     * @return replacement JSON node value.
     */
    public abstract JsonNode replace(List<String> path, JsonNode value);

    /**
     * Apply remove operation with given patch operation path and given.
     * 
     * @param path patch operation path.
     * 
     * @return removed JSON node value.
     */
    public abstract JsonNode remove(List<String> path);

    /**
     * Apply move operation with given patch operation from-path and given patch operation to-path.
     * 
     * @param fromPath patch operation from path.
     * @param toPath patch operation to path.
     * 
     * @return moved JSON node value.
     */
    public abstract JsonNode move(List<String> fromPath, List<String> toPath);

    /**
     * Apply copy operation with given patch operation from-path and given patch operation to-path.
     * 
     * @param fromPath patch operation from path.
     * @param toPath patch operation to path.
     * 
     * @return copied JSON node value.
     */
    public abstract JsonNode copy(List<String> fromPath, List<String> toPath);

    /**
     * Return target JSON node value.
     * 
     * @return target JSON node value.
     */
    public abstract JsonNode result();
}