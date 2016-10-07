package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

class ApplyProcessor implements JsonPatchProcessor {

    private JsonNode target;

    ApplyProcessor(JsonNode target) {
        this.target = target;
    }

    public JsonNode result() {
        return target;
    }

    @Override
    public JsonNode add(List<String> path, JsonNode value) {
        JsonNode parent = getParent(path);
        if (parent == null) {
            throw new JsonPatchApplicationException(
                    "[add] no such path in source (path: " + JsonPathHelper.toString(path) + ")");
        }
        String field = path.get(path.size() - 1);
        if (field.isEmpty() && path.size() == 1) {
            target = value;
        } else if (!parent.isContainerNode()) {
            throw new JsonPatchApplicationException(
                    "[add] parent is not a container in source (path: " + JsonPathHelper.toString(path)
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
            throw new JsonPatchApplicationException(
                    "[test] no such path in source (path: " + JsonPathHelper.toString(path) + ")");
        } else if (!node.equals(value)) {
            throw new JsonPatchApplicationException(
                    "[test] value differs from expectations (path: " + JsonPathHelper.toString(path)
                            + " | value: " + value + " | node: " + node + ")");
        }
        return value;
    }

    @Override
    public JsonNode replace(List<String> path, JsonNode value) {
        JsonNode parent = getParent(path);
        if (parent == null) {
            throw new JsonPatchApplicationException(
                    "[replace] no such path in source (path: " + JsonPathHelper.toString(path) + ")");
        }
        String field = path.get(path.size() - 1);
        if (field.isEmpty() && path.size() == 1) {
            target = value;
        } else if (parent.isObject()) {
            ((ObjectNode) parent).set(field, value);
        } else if (parent.isArray()) {
            ((ArrayNode) parent).set(arrayIndex(field, parent.size() - 1, path), value);
        } else {
            throw new JsonPatchApplicationException(
                    "[replace] no such path in source (path: " + JsonPathHelper.toString(path) + ")");
        }
        return value;
    }

    @Override
    public JsonNode remove(List<String> path) {
        JsonNode parent = getParent(path);
        if (parent == null) {
            throw new JsonPatchApplicationException(
                    "[remove] no such path in source (path: " + JsonPathHelper.toString(path) + ")");
        }
        String field = path.get(path.size() - 1);
        if (parent.isObject()) {
            return ((ObjectNode) parent).remove(field);
        } else if (parent.isArray()) {
            return ((ArrayNode) parent).remove(arrayIndex(field, parent.size() - 1, path));
        }
        throw new JsonPatchApplicationException(
                "[remove] no such path in source (path: " + JsonPathHelper.toString(path) + ")");
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
            throw new JsonPatchApplicationException(
                    "index out of bounds (path: " + JsonPathHelper.toString(path) + " | index: " + index
                            + " | bounds: 0-" + max + ")");
        }
        return index;
    }
}
