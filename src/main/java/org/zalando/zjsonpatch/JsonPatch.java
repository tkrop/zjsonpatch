package org.zalando.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.zalando.zjsonpatch.Constants.FROM;
import static org.zalando.zjsonpatch.Constants.OP;
import static org.zalando.zjsonpatch.Constants.PATH;
import static org.zalando.zjsonpatch.Constants.VALUE;
import static org.zalando.zjsonpatch.FeatureFlags.LCS_ITERATE_PATCH_GENERATOR;
import static org.zalando.zjsonpatch.FeatureFlags.LCS_VISIT_PATCH_GENERATOR;
import static org.zalando.zjsonpatch.FeatureFlags.MISSING_VALUES_AS_NULLS;
import static org.zalando.zjsonpatch.FeatureFlags.PATCH_OPTIMIZATION;
import static org.zalando.zjsonpatch.FeatureFlags.SIMPLE_COMPARE_PATCH_GENERATOR;
import static org.zalando.zjsonpatch.OpType.MOVE;
import static org.zalando.zjsonpatch.OpType.REMOVE;
import static org.zalando.zjsonpatch.Processor.NOOP;

/**
 * Entry point to create and apply a JSON Patches from given source and target JSON nodes. The {@link FeatureFlags} can
 * be used to control the behavior of the patch generation. The default setting uses the standard longest common
 * sequence patch generator together with general patch optimization. For special cases their are faster algorithm, that
 * can be enabled to speed up patch generation.
 */
public abstract class JsonPatch {

    /**
     * Default set of feature flags for JSON Patch creation and application.
     */
    private static final EnumSet<FeatureFlags> DEFAULT = EnumSet.of(PATCH_OPTIMIZATION);

    /**
     * Create patch generator using given set of feature flags.
     * 
     * @param flags set of feature flags.
     * 
     * @return patch generator.
     */
    private static Generator create(final Set<FeatureFlags> flags) {
        if (flags.contains(LCS_ITERATE_PATCH_GENERATOR)) {
            return new Generator.Lcs.Iterate(flags);
        } else if (flags.contains(LCS_VISIT_PATCH_GENERATOR)) {
            return new Generator.Lcs.Visit(flags);
        } else if (flags.contains(SIMPLE_COMPARE_PATCH_GENERATOR)) {
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
    public static JsonNode create(final JsonNode source, final JsonNode target) {
        return create(source, target, DEFAULT);
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
    public static JsonNode create(final JsonNode source, final JsonNode target, final Set<FeatureFlags> flags) {
        List<Patch> patches = create(flags).generate(source, target);
        if (flags.contains(PATCH_OPTIMIZATION)) {
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
        node.put(OP, patch.type.getName());
        node.put(PATH, PathHelper.getPathRep(patch.path));

        if (MOVE.equals(patch.type)) {
            node.put(FROM, PathHelper.getPathRep(patch.path));
            node.put(PATH, PathHelper.getPathRep(patch.from));
        } else if (!REMOVE.equals(patch.type)) {
            if (!patch.value.isNull() || !flags.contains(MISSING_VALUES_AS_NULLS)) {
                node.set(VALUE, patch.value);
            }
        }
    }

    public static void validate(JsonNode patch, Set<FeatureFlags> flags) throws JsonPatchException {
        new Applicator(flags).apply(NOOP, patch);
    }

    public static void validate(JsonNode patch) throws JsonPatchException {
        validate(patch, DEFAULT);
    }

    public static JsonNode apply(JsonNode patch, JsonNode target, Set<FeatureFlags> flags)
            throws JsonPatchException {
        return new Applicator(flags).apply(new Processor.Apply(target), patch);
    }

    public static JsonNode apply(JsonNode patch, JsonNode target) throws JsonPatchException {
        return apply(patch, target, DEFAULT);
    }
}
