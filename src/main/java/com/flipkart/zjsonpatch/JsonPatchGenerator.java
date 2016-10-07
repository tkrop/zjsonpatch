package com.flipkart.zjsonpatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

abstract class JsonPatchGenerator {

    protected final Set<FeatureFlags> flags;

    public JsonPatchGenerator(Set<FeatureFlags> flags) {
        this.flags = flags;
    }

    public List<Diff> create(JsonNode source, JsonNode target) {
        return generate(new ArrayList<Diff>(), new ArrayList<Object>(), source, target);
    }

    protected List<Diff> generate(final List<Diff> diffs, final List<Object> path, JsonNode source,
            JsonNode target) {
        return (!source.equals(target)) ? generateAll(diffs, path, source, target) : diffs;
    }

    protected List<Diff> generateAll(final List<Diff> diffs, final List<Object> path, JsonNode source,
            JsonNode target) {
        JsonNodeType sourceType = source.getNodeType();
        JsonNodeType targetType = target.getNodeType();

        if (sourceType == JsonNodeType.OBJECT && targetType == JsonNodeType.OBJECT) {
            compareObject(diffs, path, source, target);
        } else if (sourceType == JsonNodeType.ARRAY && targetType == JsonNodeType.ARRAY) {
            compareArray(diffs, path, source, target);
        } else {
            diffs.add(new Diff(Operation.REPLACE, path, target));
        }
        return diffs;
    }

    protected void compareObject(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target) {
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

    protected abstract void compareArray(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target);
}
