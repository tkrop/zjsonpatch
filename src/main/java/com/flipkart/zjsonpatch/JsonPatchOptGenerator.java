package com.flipkart.zjsonpatch;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;

import com.fasterxml.jackson.databind.JsonNode;

final class JsonPatchOptGenerator extends JsonPatchLcsGenerator {

    public JsonPatchOptGenerator(Set<FeatureFlags> flags) {
        super(flags);
    }

    @Override
    protected void compareArrayLcs(List<Diff> diffs, List<Object> path, List<JsonNode> source,
            List<JsonNode> target, int start) {
        List<JsonNode> lcs = ListUtils.longestCommonSubsequence(source, target);
        int lsize = lcs.size(), ssize = source.size(), tsize = target.size();
        int lindex = 0, sindex = 0, tindex = 0, pos = start;

        while (lindex < lsize) {
            JsonNode lnode = lcs.get(lindex);
            JsonNode snode = source.get(sindex);
            JsonNode tnode = target.get(tindex);

            boolean leqs = lnode.equals(snode);
            boolean leqt = lnode.equals(tnode);
            if (leqs && leqt) { // Both are same as lcs node, nothing to do here
                sindex++;
                tindex++;
                lindex++;
                pos++;
            } else if (leqs) { // src node is same as lcs, but not targetNode
                List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                diffs.add(new Diff(Operation.ADD, currPath, tnode));
                pos++;
                tindex++;
            } else if (leqt) { // targetNode node is same as lcs, but not src
                List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                diffs.add(new Diff(Operation.REMOVE, currPath, snode));
                sindex++;
            } else {
                List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                // both are unequal to lcs node
                generate(diffs, currPath, snode, tnode);
                sindex++;
                tindex++;
                pos++;
            }
        }

        while ((sindex < ssize) && (tindex < tsize)) {
            JsonNode snode = source.get(sindex);
            JsonNode tnode = target.get(tindex);
            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
            generate(diffs, currPath, snode, tnode);
            sindex++;
            tindex++;
            pos++;
        }
        pos = addRemaining(diffs, path, target, pos, tindex, tsize);
        removeRemaining(diffs, path, pos, sindex, ssize, source);
    }

    private Integer removeRemaining(List<Diff> diffs, List<Object> path, int pos, int sindex, int ssize,
            List<JsonNode> source) {
        while (sindex < ssize) {
            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
            diffs.add(new Diff(Operation.REMOVE, currPath, source.get(sindex)));
            sindex++;
        }
        return pos;
    }

    private Integer addRemaining(List<Diff> diffs, List<Object> path, List<JsonNode> target, int pos, int tindex,
            int tsize) {
        while (tindex < tsize) {
            JsonNode node = target.get(tindex);
            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
            diffs.add(new Diff(Operation.ADD, currPath, node.deepCopy()));
            pos++;
            tindex++;
        }
        return pos;
    }
}
