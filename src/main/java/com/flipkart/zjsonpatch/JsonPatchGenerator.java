package com.flipkart.zjsonpatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

abstract class JsonPatchGenerator {

    protected final Set<CompatibilityFlags> flags;

    public JsonPatchGenerator(Set<CompatibilityFlags> flags) {
        this.flags = flags;
    }

    public List<Diff> create(JsonNode source, JsonNode target) {
        return generate(new ArrayList<Diff>(), new ArrayList<Object>(), source, target);
    }

    private List<JsonNode> newArrayList(JsonNode node) {
        List<JsonNode> list = new ArrayList<JsonNode>();
        for (JsonNode elem : node) {
            list.add(elem);
        }
        return list;
    }

    protected List<Diff> generate(final List<Diff> diffs, final List<Object> path, JsonNode source,
            JsonNode target) {
        if (!source.equals(target)) {
            JsonNodeType sourceType = source.getNodeType();
            JsonNodeType targetType = target.getNodeType();

            if (sourceType == JsonNodeType.OBJECT && targetType == JsonNodeType.OBJECT) {
                compareObjects(diffs, path, source, target);
            } else if (sourceType == JsonNodeType.ARRAY && targetType == JsonNodeType.ARRAY) {
                compareArray(diffs, path, source, target);
            } else {
                diffs.add(new Diff(Operation.REPLACE, path, target));
            }
        }
        return diffs;
    }

    private void compareObjects(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target) {
        Iterator<String> keysFromSrc = source.fieldNames();
        while (keysFromSrc.hasNext()) {
            String key = keysFromSrc.next();
            if (!target.has(key)) { // remove case
                List<Object> currPath = JsonPathHelper.getPathExt(path, key);
                diffs.add(new Diff(Operation.REMOVE, currPath, source.get(key)));
                continue;
            }
            List<Object> currPath = JsonPathHelper.getPathExt(path, key);
            generate(diffs, currPath, source.get(key), target.get(key));
        }
        Iterator<String> keysFromTarget = target.fieldNames();
        while (keysFromTarget.hasNext()) {
            String key = keysFromTarget.next();
            if (!source.has(key)) { // add case
                List<Object> currPath = JsonPathHelper.getPathExt(path, key);
                diffs.add(new Diff(Operation.ADD, currPath, target.get(key)));
            }
        }
    }

    private void compareArray(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target) {
        compareArray(diffs, path, newArrayList(source), newArrayList(target));
    }

    private void compareArray(List<Diff> diffs, List<Object> path, List<JsonNode> source,
            List<JsonNode> target) {
        int start = 0, send = source.size(), tend = target.size();
        while ((start < send) && (start < tend)) {
            if (!compareArrayEquals(source, start, target, start)) {
                break;
            }
            start++;
        }
        while ((start < send) && (start < tend)) {
            if (!compareArrayEquals(source, --send, target, --tend)) {
                send++;
                tend++;
                break;
            }
        }
        compareArrayLcs(diffs, path, source.subList(start, send), target.subList(start, tend), start);
    }

    private boolean compareArrayEquals(List<JsonNode> source, int sindex, List<JsonNode> target, int tindex) {
        return source.get(sindex).equals(target.get(tindex));
    }

    protected abstract void compareArrayLcs(List<Diff> diffs, List<Object> path, List<JsonNode> source,
            List<JsonNode> target, int start);
}
