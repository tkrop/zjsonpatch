package org.zalando.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Flags for enabling and disabling features.
 */
public enum FeatureFlags {

    /**
     * Enable general patch optimization feature after creating a patch via {@link JsonDiff#create(JsonNode, JsonNode)}
     * or {@link JsonDiff#create(JsonNode, JsonNode, Set)}.
     */
    PATCH_OPTIMIZATION,

    /**
     * Enable representation of nulls as missing values and vice versa, when creating or applying patches via
     * {@link JsonDiff#create(JsonNode, JsonNode)}, {@link JsonDiff#create(JsonNode, JsonNode, Set)},
     * {@link JsonPatch#apply(JsonNode, JsonNode)}, or {@link JsonPatch#apply(JsonNode, JsonNode, Set))}.
     */
    MISSING_VALUES_AS_NULLS,

    /**
     * Enable longest common sequence (LCS) visitor patch generator.
     */
    LCS_VISIT_PATCH_GENERATOR,

    /**
     * Enable longest common sequence (LCS) iterator patch generator.
     */
    LCS_ITERATE_PATCH_GENERATOR,

    /**
     * Enable simple array compare patch generator.
     */
    SIMPLE_COMPARE_PATCH_GENERATOR;
}
