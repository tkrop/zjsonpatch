package com.flipkart.zjsonpatch;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.functors.DefaultEquator;
import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;

import com.fasterxml.jackson.databind.JsonNode;

final class JsonPatchFastGenerator extends JsonPatchLcsGenerator {

    public JsonPatchFastGenerator(Set<FeatureFlags> flags) {
        super(flags);
    }

    @Override
    protected void compareArrayLcs(List<Diff> diffs, List<Object> path, List<JsonNode> source,
            List<JsonNode> target, int start) {
        SequencesComparator<JsonNode> comparator =
                new SequencesComparator<JsonNode>(source, target, DefaultEquator.defaultEquator());
        LcsDiffVisitor visitor = new LcsDiffVisitor(this, diffs, path, start);
        comparator.getScript().visit(visitor);
        visitor.visitEndCommand();
    }


    private static final class LcsDiffVisitor implements CommandVisitor<JsonNode> {

        private final JsonPatchLcsGenerator gen;
        private final Deque<Diff> deque;
        private final List<Diff> diffs;
        private final List<Object> path;
        private Operation op = null;
        private int pos;

        public LcsDiffVisitor(JsonPatchLcsGenerator gen, List<Diff> diffs, List<Object> path, int start) {
            this.gen = gen;
            this.diffs = diffs;
            this.deque = new ArrayDeque<Diff>();
            this.path = path;
            this.pos = start;
        }

        private void queue(Operation op, List<Object> path, JsonNode node) {
            deque.add(new Diff(op, path, node));
            this.op = op;
        }

        private void generate(List<Object> path, JsonNode source, JsonNode target) {
            gen.generateAll(diffs, path, source, target);
            if (deque.isEmpty()) {
                op = null;
            }
        }

        @Override
        public void visitInsertCommand(JsonNode node) {
            List<Object> path = JsonPathHelper.getPathExt(this.path, pos);
            if (op != Operation.REMOVE) {
                queue(Operation.ADD, path, node);
            } else { // This code seems to be not reachable because of LCS algorithm.
                Diff diff = deque.remove();
                generate(path, diff.getValue(), node);
            }
            pos++;
        }

        @Override
        public void visitKeepCommand(JsonNode node) {
            clear();
            pos++;
        }

        @Override
        public void visitDeleteCommand(JsonNode node) {
            if (op != Operation.ADD) {
                List<Object> path = JsonPathHelper.getPathExt(this.path, pos);
                queue(Operation.REMOVE, path, node);
            } else {
                Diff diff = deque.remove();
                generate(diff.getPath(), node, diff.getValue());
            }
        }

        public void visitEndCommand() {
            clear();
        }

        private void clear() {
            diffs.addAll(deque);
            deque.clear();
            op = null;
        }
    }
}
