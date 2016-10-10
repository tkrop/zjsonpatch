package org.zalando.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.functors.DefaultEquator;
import org.apache.commons.collections4.sequence.CommandVisitor;
import org.apache.commons.collections4.sequence.SequencesComparator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.databind.node.JsonNodeType.ARRAY;
import static com.fasterxml.jackson.databind.node.JsonNodeType.OBJECT;
import static org.zalando.zjsonpatch.OpType.ADD;
import static org.zalando.zjsonpatch.OpType.REMOVE;
import static org.zalando.zjsonpatch.OpType.REPLACE;

/**
 * Abstract patch generator.
 */
abstract class Generator {
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
        protected static final class Visit extends Generator.Lcs {
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
                Visit.Visitor visitor = new Visitor(this, patches, path, start);
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
                private OpType type = null;
                private int pos;

                public Visitor(Generator.Lcs gen, List<Patch> patches, List<Object> path, int start) {
                    this.gen = gen;
                    this.patches = patches;
                    this.deque = new ArrayDeque<Patch>();
                    this.path = path;
                    this.pos = start;
                }

                private void queue(OpType type, List<Object> path, JsonNode node) {
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
                    List<Object> path = PathHelper.getPathExt(this.path, pos);
                    // This code seems to be not reachable because of LCS algorithm.
                    // if (type != REMOVE) {
                    queue(ADD, path, node);
                    // } else {
                    // Patch patch = deque.remove();
                    // generate(path, patch.value, node);
                    // }
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
                    if (type != ADD) {
                        List<Object> path = PathHelper.getPathExt(this.path, pos);
                        queue(REMOVE, path, node);
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

        protected static final class Iterate extends Generator.Lcs {

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
                        List<Object> currPath = PathHelper.getPathExt(path, pos);
                        patches.add(new Patch(ADD, currPath, tnode));
                        pos++;
                        tindex++;
                    } else if (leqt) { // targetNode node is same as lcs, but not src
                        List<Object> currPath = PathHelper.getPathExt(path, pos);
                        patches.add(new Patch(REMOVE, currPath, snode));
                        sindex++;
                    } else {
                        List<Object> currPath = PathHelper.getPathExt(path, pos);
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
                    List<Object> currPath = PathHelper.getPathExt(path, pos);
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
                    List<Object> currPath = PathHelper.getPathExt(path, pos);
                    patches.add(new Patch(REMOVE, currPath, source.get(sindex)));
                    sindex++;
                }
                return pos;
            }

            private Integer addRemaining(List<Patch> patches, List<Object> path, List<JsonNode> target, int pos,
                    int tindex,
                    int tsize) {
                while (tindex < tsize) {
                    JsonNode node = target.get(tindex);
                    List<Object> currPath = PathHelper.getPathExt(path, pos);
                    patches.add(new Patch(ADD, currPath, node.deepCopy()));
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
                List<Object> currPath = PathHelper.getPathExt(path, pos);
                generate(patches, currPath, source.get(pos), target.get(pos));
                pos++;
            }
            while (pos < tsize) {
                List<Object> currPath = PathHelper.getPathExt(path, pos);
                patches.add(new Patch(ADD, currPath, target.get(pos)));
                pos++;
            }
            List<Object> currPath = PathHelper.getPathExt(path, pos);
            while (pos < ssize) {
                patches.add(new Patch(REMOVE, currPath, source.get(pos)));
                pos++;
            }
        }
    }

    public Generator(Set<FeatureFlags> flags) {
        this.flags = flags;
    }

    public List<Patch> generate(JsonNode source, JsonNode target) {
        return generate(new ArrayList<Patch>(), new ArrayList<Object>(), source, target);
    }

    protected List<Patch> generate(List<Patch> patches, List<Object> path, JsonNode source, JsonNode target) {
        return (!source.equals(target)) ? generateAll(patches, path, source, target) : patches;
    }

    protected List<Patch> generateAll(List<Patch> patches, List<Object> path, JsonNode source, JsonNode target) {
        JsonNodeType sourceType = source.getNodeType();
        JsonNodeType targetType = target.getNodeType();

        if (sourceType == OBJECT && targetType == OBJECT) {
            compareObject(patches, path, source, target);
        } else if (sourceType == ARRAY && targetType == ARRAY) {
            compareArray(patches, path, source, target);
        } else {
            patches.add(new Patch(REPLACE, path, target));
        }
        return patches;
    }

    protected void compareObject(List<Patch> patches, List<Object> path, JsonNode source, JsonNode target) {
        Iterator<String> keysFromSrc = source.fieldNames();
        while (keysFromSrc.hasNext()) {
            String key = keysFromSrc.next();
            if (!target.has(key)) { // remove case
                List<Object> currPath = PathHelper.getPathExt(path, key);
                patches.add(new Patch(REMOVE, currPath, source.get(key)));
                continue;
            }
            List<Object> currPath = PathHelper.getPathExt(path, key);
            generate(patches, currPath, source.get(key), target.get(key));
        }
        Iterator<String> keysFromTarget = target.fieldNames();
        while (keysFromTarget.hasNext()) {
            String key = keysFromTarget.next();
            if (!source.has(key)) { // add case
                List<Object> currPath = PathHelper.getPathExt(path, key);
                patches.add(new Patch(ADD, currPath, target.get(key)));
            }
        }
    }

    protected abstract void compareArray(List<Patch> patches, List<Object> path, JsonNode source, JsonNode target);
}