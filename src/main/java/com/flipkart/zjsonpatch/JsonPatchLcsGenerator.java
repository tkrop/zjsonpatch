package com.flipkart.zjsonpatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

abstract class JsonPatchLcsGenerator extends JsonPatchGenerator {

    public JsonPatchLcsGenerator(Set<CompatibilityFlags> flags) {
        super(flags);
    }

    @Override
    protected void compareArray(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target) {
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

    private List<JsonNode> newArrayList(JsonNode node) {
        List<JsonNode> list = new ArrayList<JsonNode>();
        for (JsonNode elem : node) {
            list.add(elem);
        }
        return list;
    }
}
