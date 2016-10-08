package org.zalando.jsonpatch;

/**
 * Flags for enabling and disabling features.
 */
public enum FeatureFlags {
    /**
     * Represent nulls as missing values and vice versa.
     */
    MISSING_VALUES_AS_NULLS,

    /**
     * Allows to patch the JSON Object instead of applying the patched on a deep copy.
     */
    PATCH_IN_PLACE,

    /**
     * Enable optimization of patch creation.
     */
    PATCH_OPTIMIZATION,

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
