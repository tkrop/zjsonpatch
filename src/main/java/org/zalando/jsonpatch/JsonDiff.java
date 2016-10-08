package org.zalando.jsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.functors.DefaultEquator;
import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;

public final class JsonDiff {

    private static final EnumSet<FeatureFlags> DEFAULT = EnumSet.of(FeatureFlags.PATCH_OPTIMIZATION);

    private static Generator create(final Set<FeatureFlags> flags) {
        if (flags.contains(FeatureFlags.LCS_ITERATE_PATCH_GENERATOR)) {
            return new Generator.Lcs.Iterate(flags);
        } else if (flags.contains(FeatureFlags.LCS_VISIT_PATCH_GENERATOR)) {
            return new Generator.Lcs.Visit(flags);
        } else if (flags.contains(FeatureFlags.SIMPLE_COMPARE_PATCH_GENERATOR)) {
            return new Generator.Compare(flags);
        }
        return new Generator.Lcs.Iterate(flags);
    }

    public static JsonNode asJson(final JsonNode source, final JsonNode target) {
        return asJson(source, target, DEFAULT);
    }

    public static JsonNode asJson(final JsonNode source, final JsonNode target, final Set<FeatureFlags> flags) {
        List<Diff> diffs = create(flags).create(source, target);
        if (flags.contains(FeatureFlags.PATCH_OPTIMIZATION)) {
            new Compactor(flags).compact(diffs);
        }
        return convert(diffs, flags);
    }

    private static ArrayNode convert(List<Diff> diffs, Set<FeatureFlags> flags) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        final ArrayNode patch = factory.arrayNode();
        for (Diff diff : diffs) {
            patch.add(convert(factory.objectNode(), diff, flags));
        }
        return patch;
    }

    private static JsonNode convert(ObjectNode node, Diff diff, Set<FeatureFlags> flags) {
        node.put(Constants.OP, diff.getOp().getName());
        node.put(Constants.PATH, JsonPathHelper.getPathRep(diff.getPath()));
        if (JsonPatchOp.MOVE.equals(diff.getOp())) {
            node.put(Constants.FROM, JsonPathHelper.getPathRep(diff.getPath()));
            node.put(Constants.PATH, JsonPathHelper.getPathRep(diff.getToPath()));
        } else if (!JsonPatchOp.REMOVE.equals(diff.getOp())) {
            if (!diff.getValue().isNull() || !flags.contains(FeatureFlags.MISSING_VALUES_AS_NULLS)) {
                node.set(Constants.VALUE, diff.getValue());
            }
        }
        return node;
    }

    private static class Diff {
        public final JsonPatchOp op;
        public final List<Object> path;
        public final List<Object> from; // only to be used in move operation
        public final JsonNode value;

        public Diff(JsonPatchOp op, List<Object> path, JsonNode value) {
            this(op, null, path, value);
        }

        public Diff(JsonPatchOp op, List<Object> toPath, List<Object> fromPath, JsonNode value) {
            this.op = op;
            this.path = fromPath;
            this.from = toPath;
            this.value = value;
        }

        public JsonPatchOp getOp() {
            return op;
        }

        public List<Object> getPath() {
            return path;
        }

        public JsonNode getValue() {
            return value;
        }

        List<Object> getToPath() {
            return from;
        }
    }

    protected static abstract class Generator {

        protected final Set<FeatureFlags> flags;

        protected static abstract class Lcs extends Generator {

            protected static final class Visit extends Lcs {

                public Visit(Set<FeatureFlags> flags) {
                    super(flags);
                }

                @Override
                protected void compareArrayLcs(List<Diff> diffs, List<Object> path, List<JsonNode> source,
                        List<JsonNode> target, int start) {
                    SequencesComparator<JsonNode> comparator =
                            new SequencesComparator<JsonNode>(source, target, DefaultEquator.defaultEquator());
                    Visitor visitor = new Visitor(this, diffs, path, start);
                    comparator.getScript().visit(visitor);
                    visitor.visitEndCommand();
                }

                private static final class Visitor implements CommandVisitor<JsonNode> {

                    private final Lcs gen;
                    private final Deque<Diff> deque;
                    private final List<Diff> diffs;
                    private final List<Object> path;
                    private JsonPatchOp op = null;
                    private int pos;

                    public Visitor(Lcs gen, List<Diff> diffs, List<Object> path, int start) {
                        this.gen = gen;
                        this.diffs = diffs;
                        this.deque = new ArrayDeque<Diff>();
                        this.path = path;
                        this.pos = start;
                    }

                    private void queue(JsonPatchOp op, List<Object> path, JsonNode node) {
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
                        if (op != JsonPatchOp.REMOVE) {
                            queue(JsonPatchOp.ADD, path, node);
                        } /* This code seems to be not reachable because of LCS algorithm.
                          else { 
                            Diff diff = deque.remove();
                            generate(path, diff.getValue(), node);
                        } */
                        pos++;
                    }

                    @Override
                    public void visitKeepCommand(JsonNode node) {
                        clear();
                        pos++;
                    }

                    @Override
                    public void visitDeleteCommand(JsonNode node) {
                        if (op != JsonPatchOp.ADD) {
                            List<Object> path = JsonPathHelper.getPathExt(this.path, pos);
                            queue(JsonPatchOp.REMOVE, path, node);
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

            protected static final class Iterate extends Lcs {

                public Iterate(Set<FeatureFlags> flags) {
                    super(flags);
                }

                @Override
                protected void compareArrayLcs(List<Diff> diffs, List<Object> path,
                        List<JsonNode> source, List<JsonNode> target, int start) {
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
                            diffs.add(new Diff(JsonPatchOp.ADD, currPath, tnode));
                            pos++;
                            tindex++;
                        } else if (leqt) { // targetNode node is same as lcs, but not src
                            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                            diffs.add(new Diff(JsonPatchOp.REMOVE, currPath, snode));
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
                        diffs.add(new Diff(JsonPatchOp.REMOVE, currPath, source.get(sindex)));
                        sindex++;
                    }
                    return pos;
                }

                private Integer addRemaining(List<Diff> diffs, List<Object> path, List<JsonNode> target, int pos,
                        int tindex,
                        int tsize) {
                    while (tindex < tsize) {
                        JsonNode node = target.get(tindex);
                        List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                        diffs.add(new Diff(JsonPatchOp.ADD, currPath, node.deepCopy()));
                        pos++;
                        tindex++;
                    }
                    return pos;
                }
            }

            public Lcs(Set<FeatureFlags> flags) {
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

        protected static final class Compare extends Generator {

            public Compare(Set<FeatureFlags> flags) {
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
                    diffs.add(new Diff(JsonPatchOp.REMOVE, currPath, source.get(pos)));
                    pos++;
                }
                while (pos < tsize) {
                    List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                    diffs.add(new Diff(JsonPatchOp.ADD, currPath, target.get(pos)));
                    pos++;
                }
            }
        }

        public Generator(Set<FeatureFlags> flags) {
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
                diffs.add(new Diff(JsonPatchOp.REPLACE, path, target));
            }
            return diffs;
        }

        protected void compareObject(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target) {
            Iterator<String> keysFromSrc = source.fieldNames();
            while (keysFromSrc.hasNext()) {
                String key = keysFromSrc.next();
                if (!target.has(key)) { // remove case
                    List<Object> currPath = JsonPathHelper.getPathExt(path, key);
                    diffs.add(new Diff(JsonPatchOp.REMOVE, currPath, source.get(key)));
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
                    diffs.add(new Diff(JsonPatchOp.ADD, currPath, target.get(key)));
                }
            }
        }

        protected abstract void compareArray(List<Diff> diffs, List<Object> path, JsonNode source, JsonNode target);
    }

    /**
     * Merging remove and add to a single move operation.
     */
    protected static final class Compactor {

        @SuppressWarnings("unused")
        private final Set<FeatureFlags> flags;

        public Compactor(Set<FeatureFlags> flags) {
            this.flags = flags;
        }

        public void compact(final List<Diff> diffs) {
            for (int i = 0; i < diffs.size(); i++) {
                Diff diff1 = diffs.get(i);

                // if not remove OR add, move to next diff
                if (!(JsonPatchOp.REMOVE.equals(diff1.getOp()) ||
                        JsonPatchOp.ADD.equals(diff1.getOp()))) {
                    continue;
                }

                for (int j = i + 1; j < diffs.size(); j++) {
                    Diff diff2 = diffs.get(j);
                    if (!diff1.getValue().equals(diff2.getValue())) {
                        continue;
                    }

                    Diff moveDiff = null;
                    if (JsonPatchOp.REMOVE.equals(diff1.getOp()) &&
                            JsonPatchOp.ADD.equals(diff2.getOp())) {
                        computeRelativePath(diff2.getPath(), i + 1, j - 1, diffs);
                        moveDiff = new Diff(JsonPatchOp.MOVE, diff2.getPath(), diff1.getPath(), diff2.getValue());

                    } else if (JsonPatchOp.ADD.equals(diff1.getOp()) &&
                            JsonPatchOp.REMOVE.equals(diff2.getOp())) {
                        computeRelativePath(diff2.getPath(), i, j - 1, diffs); // diff1's add should also be considered
                        moveDiff = new Diff(JsonPatchOp.MOVE, diff1.getPath(), diff2.getPath(), diff1.getValue());
                    }
                    if (moveDiff != null) {
                        diffs.remove(j);
                        diffs.set(i, moveDiff);
                        break;
                    }
                }
            }
        }

        // Note : only to be used for arrays
        // Finds the longest common Ancestor ending at Array
        private void computeRelativePath(List<Object> path, int startIdx, int endIdx, List<Diff> diffs) {
            List<Integer> counters = new ArrayList<Integer>();

            resetCounters(counters, path.size());

            for (int i = startIdx; i <= endIdx; i++) {
                Diff diff = diffs.get(i);
                // Adjust relative path according to #Add and #Remove
                if (JsonPatchOp.ADD.equals(diff.getOp()) || JsonPatchOp.REMOVE.equals(diff.getOp())) {
                    updatePath(path, diff, counters);
                }
            }
            updatePathWithCounters(counters, path);
        }

        private void resetCounters(List<Integer> counters, int size) {
            for (int i = 0; i < size; i++) {
                counters.add(0);
            }
        }

        private void updatePathWithCounters(List<Integer> counters, List<Object> path) {
            for (int i = 0; i < counters.size(); i++) {
                int value = counters.get(i);
                if (value != 0) {
                    Integer currValue = Integer.parseInt(path.get(i).toString());
                    path.set(i, String.valueOf(currValue + value));
                }
            }
        }

        private void updatePath(List<Object> path, Diff pseudo, List<Integer> counters) {
            // find longest common prefix of both the paths
            if (pseudo.getPath().size() <= path.size()) {
                int idx = -1;
                for (int i = 0; i < pseudo.getPath().size() - 1; i++) {
                    if (pseudo.getPath().get(i).equals(path.get(i))) {
                        idx = i;
                    } else {
                        break;
                    }
                }
                if (idx == pseudo.getPath().size() - 2) {
                    if (pseudo.getPath().get(pseudo.getPath().size() - 1) instanceof Integer) {
                        updateCounters(pseudo, pseudo.getPath().size() - 1, counters);
                    }
                }
            }
        }

        private void updateCounters(Diff pseudo, int idx, List<Integer> counters) {
            if (JsonPatchOp.ADD.equals(pseudo.getOp())) {
                counters.set(idx, counters.get(idx) - 1);
            } else {
                if (JsonPatchOp.REMOVE.equals(pseudo.getOp())) {
                    counters.set(idx, counters.get(idx) + 1);
                }
            }
        }
    }
}
