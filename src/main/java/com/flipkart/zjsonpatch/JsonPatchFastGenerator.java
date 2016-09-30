package com.flipkart.zjsonpatch;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.functors.DefaultEquator;
import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;

import com.fasterxml.jackson.databind.JsonNode;

final class JsonPatchFastGenerator extends JsonPatchGenerator {

    public JsonPatchFastGenerator(Set<CompatibilityFlags> flags) {
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

        private final JsonPatchGenerator gen;
        private final Deque<Diff> sources;
        private final Deque<Diff> targets;
        private final List<Diff> diffs;
        private final List<Object> path;
        private int pos;

        public LcsDiffVisitor(JsonPatchGenerator gen, List<Diff> diffs, List<Object> path, int start) {
            this.gen = gen;
            this.targets = new ArrayDeque<Diff>();
            this.sources = new ArrayDeque<Diff>();
            this.diffs = diffs;
            this.path = path;
            this.pos = start;
        }

        @Override
        public void visitInsertCommand(JsonNode node) {
            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
            targets.offer(new Diff(Operation.ADD, currPath, node));
            pos++;
        }

        @Override
        public void visitKeepCommand(JsonNode node) {
            this.visitEndCommand();
            pos++;
        }

        @Override
        public void visitDeleteCommand(JsonNode node) {
            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
            sources.offer(new Diff(Operation.REMOVE, currPath, node));
        }

        public void visitEndCommand() {
            while (!sources.isEmpty() && !targets.isEmpty()) {
                Diff target = targets.poll(), source = sources.poll();
                gen.generate(diffs, target.getPath(), source.getValue(), target.getValue());
            }
            diffs.addAll(targets);
            diffs.addAll(sources);
            sources.clear();
        }
    }
}
