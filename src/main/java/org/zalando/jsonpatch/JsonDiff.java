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

/**
 * Entry point to create a JSON Patch from given source and target JSON documents. The {@link FeatureFlags} can be used
 * to control the behavior of the patch generation. The default setting uses the standard longest common sequence patch
 * generator together with general patch optimization. For special cases their are faster algorithm, that can be enabled
 * to speed up patch generation.
 */
public final class JsonDiff {
    /**
     * Default set of feature flags for JSON Patch generation.
     */
    private static final EnumSet<FeatureFlags> DEFAULT = EnumSet.of(FeatureFlags.PATCH_OPTIMIZATION);

    /**
     * Create patch generator using given set of feature flags.
     * 
     * @param flags set of feature flags.
     * 
     * @return patch generator.
     */
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

    /**
     * Create JSON Patch document from given source JSON document and given target JSON document using the default set
     * of feature flags.
     * 
     * @param source source JSON document.
     * @param target target JSON document.
     * 
     * @return JSON Patch document.
     */
    public static JsonNode asJson(final JsonNode source, final JsonNode target) {
        return asJson(source, target, DEFAULT);
    }

    /**
     * Create JSON Patch document from given source JSON document and given target JSON document using given set of
     * feature flags.
     * 
     * @param source source JSON document.
     * @param target target JSON document.
     * @param flags set of feature flags.
     * 
     * @return JSON Patch document.
     */
    public static JsonNode asJson(final JsonNode source, final JsonNode target, final Set<FeatureFlags> flags) {
        List<Patch> patches = create(flags).create(source, target);
        if (flags.contains(FeatureFlags.PATCH_OPTIMIZATION)) {
            new Compactor(flags).compact(patches);
        }
        return convert(patches, flags);
    }

    /**
     * Convert given list of patches into JSON Patch document using given feature set of flags.
     * 
     * @param patches list of patch operation entries.
     * @param flags set of feature flags.
     * 
     * @return JSON Patch document.
     */
    private static JsonNode convert(List<Patch> patches, Set<FeatureFlags> flags) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        final ArrayNode node = factory.arrayNode();
        for (Patch patch : patches) {
            convert(node, patch, flags);
        }
        return node;
    }

    /**
     * Convert single patch operation entry using given set of feature flags into a JSON Patch fragment added to the
     * given root node.
     * 
     * @param root JSON Patch document root node.
     * @param patch patch operation entry.
     * @param flags set of feature flags.
     */
    private static void convert(ArrayNode root, Patch patch, Set<FeatureFlags> flags) {
        ObjectNode node = root.addObject();
        node.put(Constants.OP, patch.type.getName());
        node.put(Constants.PATH, JsonPathHelper.getPathRep(patch.path));
        
        if (JsonPatchOpType.MOVE.equals(patch.type)) {
            node.put(Constants.FROM, JsonPathHelper.getPathRep(patch.path));
            node.put(Constants.PATH, JsonPathHelper.getPathRep(patch.from));
        } else if (!JsonPatchOpType.REMOVE.equals(patch.type)) {
            if (!patch.value.isNull() || !flags.contains(FeatureFlags.MISSING_VALUES_AS_NULLS)) {
                node.set(Constants.VALUE, patch.value);
            }
        }
    }

    /**
     * Patch operation entry.
     */
    private static class Patch {
        /**
         * Patch operation type.
         */
        public final JsonPatchOpType type;

        /**
         * Patch operation path.
         */
        public final List<Object> path;

        /**
         * Patch operation from path (only used by move operation).
         */
        public final List<Object> from;

        /**
         * Patch operation value.
         */
        public final JsonNode value;

        /**
         * Create patch operation entry with given patch operation type, patch operation path, and patch operation
         * value.
         * 
         * @param type patch operation type.
         * @param path patch operation path.
         * @param value patch operation value.
         */
        public Patch(JsonPatchOpType type, List<Object> path, JsonNode value) {
            this(type, path, null, value);
        }

        /**
         * Create patch operation entry with given patch operation type, patch operation to-path, patch operation
         * from-path, and patch operation value.
         * 
         * @param type patch operation type.
         * @param toPath patch operation to-path.
         * @param fromPath patch operation from-path.
         * @param value patch operation value.
         */
        public Patch(JsonPatchOpType type, List<Object> toPath, List<Object> fromPath, JsonNode value) {
            this.type = type;
            this.path = toPath;
            this.from = fromPath;
            this.value = value;
        }
    }

    /**
     * Abstract patch generator.
     */
    protected static abstract class Generator {
        /**
         * Set of feature flags for patch generation.
         */
        protected final Set<FeatureFlags> flags;

        /**
         * Longest common sequence patch generator.
         */
        protected static abstract class Lcs extends Generator {

            /**
             * Visitor based longest common sequence patch generator.
             */
            protected static final class Visit extends Lcs {
                /**
                 * Create visitor based common sequence patch generator using given set of feature flags.
                 * 
                 * @param flags set of feature flags.
                 */
                public Visit(Set<FeatureFlags> flags) {
                    super(flags);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                protected void compareArrayLcs(List<Patch> patches, List<Object> path, List<JsonNode> source,
                        List<JsonNode> target, int start) {
                    SequencesComparator<JsonNode> comparator =
                            new SequencesComparator<JsonNode>(source, target, DefaultEquator.defaultEquator());
                    Visitor visitor = new Visitor(this, patches, path, start);
                    comparator.getScript().visit(visitor);
                    visitor.visitEndCommand();
                }

                /**
                 * Longest common sequence script visitor.
                 */
                private static final class Visitor implements CommandVisitor<JsonNode> {

                    private final Generator.Lcs gen;
                    private final Deque<Patch> deque;
                    private final List<Patch> patches;
                    private final List<Object> path;
                    private JsonPatchOpType type = null;
                    private int pos;

                    public Visitor(Generator.Lcs gen, List<Patch> patches, List<Object> path, int start) {
                        this.gen = gen;
                        this.patches = patches;
                        this.deque = new ArrayDeque<Patch>();
                        this.path = path;
                        this.pos = start;
                    }

                    private void queue(JsonPatchOpType type, List<Object> path, JsonNode node) {
                        deque.add(new Patch(type, path, node));
                        this.type = type;
                    }

                    private void generate(List<Object> path, JsonNode source, JsonNode target) {
                        gen.generateAll(patches, path, source, target);
                        if (deque.isEmpty()) {
                            type = null;
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void visitInsertCommand(JsonNode node) {
                        List<Object> path = JsonPathHelper.getPathExt(this.path, pos);
                        if (type != JsonPatchOpType.REMOVE) {
                            queue(JsonPatchOpType.ADD, path, node);
                        } /* This code seems to be not reachable because of LCS algorithm. else { Diff patch =
                           * deque.remove(); generate(path, patch.getValue(), node); } */
                        pos++;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void visitKeepCommand(JsonNode node) {
                        clear();
                        pos++;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void visitDeleteCommand(JsonNode node) {
                        if (type != JsonPatchOpType.ADD) {
                            List<Object> path = JsonPathHelper.getPathExt(this.path, pos);
                            queue(JsonPatchOpType.REMOVE, path, node);
                        } else {
                            Patch patch = deque.remove();
                            generate(patch.path, node, patch.value);
                        }
                    }

                    public void visitEndCommand() {
                        clear();
                    }

                    private void clear() {
                        patches.addAll(deque);
                        deque.clear();
                        type = null;
                    }
                }
            }

            protected static final class Iterate extends Lcs {

                public Iterate(Set<FeatureFlags> flags) {
                    super(flags);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                protected void compareArrayLcs(List<Patch> patches, List<Object> path,
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
                            patches.add(new Patch(JsonPatchOpType.ADD, currPath, tnode));
                            pos++;
                            tindex++;
                        } else if (leqt) { // targetNode node is same as lcs, but not src
                            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                            patches.add(new Patch(JsonPatchOpType.REMOVE, currPath, snode));
                            sindex++;
                        } else {
                            List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                            // both are unequal to lcs node
                            generate(patches, currPath, snode, tnode);
                            sindex++;
                            tindex++;
                            pos++;
                        }
                    }

                    while ((sindex < ssize) && (tindex < tsize)) {
                        JsonNode snode = source.get(sindex);
                        JsonNode tnode = target.get(tindex);
                        List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                        generate(patches, currPath, snode, tnode);
                        sindex++;
                        tindex++;
                        pos++;
                    }
                    pos = addRemaining(patches, path, target, pos, tindex, tsize);
                    removeRemaining(patches, path, pos, sindex, ssize, source);
                }

                private Integer removeRemaining(List<Patch> patches, List<Object> path, int pos, int sindex, int ssize,
                        List<JsonNode> source) {
                    while (sindex < ssize) {
                        List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                        patches.add(new Patch(JsonPatchOpType.REMOVE, currPath, source.get(sindex)));
                        sindex++;
                    }
                    return pos;
                }

                private Integer addRemaining(List<Patch> patches, List<Object> path, List<JsonNode> target, int pos,
                        int tindex,
                        int tsize) {
                    while (tindex < tsize) {
                        JsonNode node = target.get(tindex);
                        List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                        patches.add(new Patch(JsonPatchOpType.ADD, currPath, node.deepCopy()));
                        pos++;
                        tindex++;
                    }
                    return pos;
                }
            }

            public Lcs(Set<FeatureFlags> flags) {
                super(flags);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void compareArray(List<Patch> patches, List<Object> path, JsonNode source, JsonNode target) {
                compareArray(patches, path, newArrayList(source), newArrayList(target));
            }

            private void compareArray(List<Patch> patches, List<Object> path, List<JsonNode> source,
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
                compareArrayLcs(patches, path, source.subList(start, send), target.subList(start, tend), start);
            }

            private boolean compareArrayEquals(List<JsonNode> source, int sindex, List<JsonNode> target, int tindex) {
                return source.get(sindex).equals(target.get(tindex));
            }

            protected abstract void compareArrayLcs(List<Patch> patches, List<Object> path, List<JsonNode> source,
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
            protected void compareArray(List<Patch> patches, List<Object> path, JsonNode source, JsonNode target) {
                int ssize = source.size(), tsize = target.size(), pos = 0;
                while (pos < ssize && pos < tsize) {
                    List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                    generate(patches, currPath, source.get(pos), target.get(pos));
                    pos++;
                }
                while (pos < ssize) {
                    List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                    patches.add(new Patch(JsonPatchOpType.REMOVE, currPath, source.get(pos)));
                    pos++;
                }
                while (pos < tsize) {
                    List<Object> currPath = JsonPathHelper.getPathExt(path, pos);
                    patches.add(new Patch(JsonPatchOpType.ADD, currPath, target.get(pos)));
                    pos++;
                }
            }
        }

        public Generator(Set<FeatureFlags> flags) {
            this.flags = flags;
        }

        public List<Patch> create(JsonNode source, JsonNode target) {
            return generate(new ArrayList<Patch>(), new ArrayList<Object>(), source, target);
        }

        protected List<Patch> generate(final List<Patch> patches, final List<Object> path, JsonNode source,
                JsonNode target) {
            return (!source.equals(target)) ? generateAll(patches, path, source, target) : patches;
        }

        protected List<Patch> generateAll(final List<Patch> patches, final List<Object> path, JsonNode source,
                JsonNode target) {
            JsonNodeType sourceType = source.getNodeType();
            JsonNodeType targetType = target.getNodeType();

            if (sourceType == JsonNodeType.OBJECT && targetType == JsonNodeType.OBJECT) {
                compareObject(patches, path, source, target);
            } else if (sourceType == JsonNodeType.ARRAY && targetType == JsonNodeType.ARRAY) {
                compareArray(patches, path, source, target);
            } else {
                patches.add(new Patch(JsonPatchOpType.REPLACE, path, target));
            }
            return patches;
        }

        protected void compareObject(List<Patch> patches, List<Object> path, JsonNode source, JsonNode target) {
            Iterator<String> keysFromSrc = source.fieldNames();
            while (keysFromSrc.hasNext()) {
                String key = keysFromSrc.next();
                if (!target.has(key)) { // remove case
                    List<Object> currPath = JsonPathHelper.getPathExt(path, key);
                    patches.add(new Patch(JsonPatchOpType.REMOVE, currPath, source.get(key)));
                    continue;
                }
                List<Object> currPath = JsonPathHelper.getPathExt(path, key);
                generate(patches, currPath, source.get(key), target.get(key));
            }
            Iterator<String> keysFromTarget = target.fieldNames();
            while (keysFromTarget.hasNext()) {
                String key = keysFromTarget.next();
                if (!source.has(key)) { // add case
                    List<Object> currPath = JsonPathHelper.getPathExt(path, key);
                    patches.add(new Patch(JsonPatchOpType.ADD, currPath, target.get(key)));
                }
            }
        }

        protected abstract void compareArray(List<Patch> patches, List<Object> path, JsonNode source, JsonNode target);
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

        public void compact(final List<Patch> patches) {
            for (int i = 0; i < patches.size(); i++) {
                Patch patch1 = patches.get(i);

                // if not remove OR add, move to next patch
                if (!(JsonPatchOpType.REMOVE.equals(patch1.type) ||
                        JsonPatchOpType.ADD.equals(patch1.type))) {
                    continue;
                }

                for (int j = i + 1; j < patches.size(); j++) {
                    Patch patch2 = patches.get(j);
                    if (!patch1.value.equals(patch2.value)) {
                        continue;
                    }

                    Patch moveDiff = null;
                    if (JsonPatchOpType.REMOVE.equals(patch1.type) &&
                            JsonPatchOpType.ADD.equals(patch2.type)) {
                        computeRelativePath(patch2.path, i + 1, j - 1, patches);
                        moveDiff = new Patch(JsonPatchOpType.MOVE, patch1.path, patch2.path, patch2.value);

                    } else if (JsonPatchOpType.ADD.equals(patch1.type) &&
                            JsonPatchOpType.REMOVE.equals(patch2.type)) {
                        computeRelativePath(patch2.path, i, j - 1, patches); // patch1's add should also be considered
                        moveDiff = new Patch(JsonPatchOpType.MOVE, patch2.path, patch1.path, patch1.value);
                    }
                    if (moveDiff != null) {
                        patches.remove(j);
                        patches.set(i, moveDiff);
                        break;
                    }
                }
            }
        }

        // Note : only to be used for arrays
        // Finds the longest common Ancestor ending at Array
        private void computeRelativePath(List<Object> path, int startIdx, int endIdx, List<Patch> patches) {
            List<Integer> counters = new ArrayList<Integer>();

            resetCounters(counters, path.size());

            for (int i = startIdx; i <= endIdx; i++) {
                Patch patch = patches.get(i);
                // Adjust relative path according to #Add and #Remove
                if (JsonPatchOpType.ADD.equals(patch.type) || JsonPatchOpType.REMOVE.equals(patch.type)) {
                    updatePath(path, patch, counters);
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

        private void updatePath(List<Object> path, Patch pseudo, List<Integer> counters) {
            // find longest common prefix of both the paths
            if (pseudo.path.size() <= path.size()) {
                int idx = -1;
                for (int i = 0; i < pseudo.path.size() - 1; i++) {
                    if (pseudo.path.get(i).equals(path.get(i))) {
                        idx = i;
                    } else {
                        break;
                    }
                }
                if (idx == pseudo.path.size() - 2) {
                    if (pseudo.path.get(pseudo.path.size() - 1) instanceof Integer) {
                        updateCounters(pseudo, pseudo.path.size() - 1, counters);
                    }
                }
            }
        }

        private void updateCounters(Patch pseudo, int idx, List<Integer> counters) {
            if (JsonPatchOpType.ADD.equals(pseudo.type)) {
                counters.set(idx, counters.get(idx) - 1);
            } else {
                if (JsonPatchOpType.REMOVE.equals(pseudo.type)) {
                    counters.set(idx, counters.get(idx) + 1);
                }
            }
        }
    }
}
