package com.flipkart.zjsonpatch;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

final class JsonPatchSameGenerator extends JsonPatchGenerator {

    public JsonPatchSameGenerator(Set<CompatibilityFlags> flags) {
        super(flags);
    }

    @Override
    protected void compareArray(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target) {
        int ssize = source.size(), tsize = target.size(), pos = 0;
        while (pos < ssize && pos < tsize) {
            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
            generate(diffs, currPath, source.get(pos), target.get(pos));
            pos++;
        }
        while (pos < ssize) {
            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
            diffs.add(new Diff(Operation.REMOVE, currPath, source.get(pos)));
            pos++;
        }
        while (pos < tsize) {
            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
            diffs.add(new Diff(Operation.ADD, currPath, target.get(pos)));
            pos++;
        }
    }
}
