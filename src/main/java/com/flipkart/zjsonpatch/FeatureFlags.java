package com.flipkart.zjsonpatch;

import java.util.EnumSet;
import java.util.Set;

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
    ENABLE_PATCH_IN_PLACE,

    /**
     * Disable optimization of patch creation.
     */
    DISABLE_PATCH_OPTIMIZATION,

    /**
     * Enable fast patch generator.
     */
    ENABLE_FAST_PATCH_GENERATOR,

    /**
     * Enable optimized patch generator.
     */
    ENABLE_OPT_PATCH_GENERATOR,

    /**
     * Enable original patch generator.
     */
    ENABLE_ORIG_PATCH_GENERATOR,

    /**
     * Enable same patch generator.
     */
    ENABLE_SAME_PATCH_GENERATOR;

    public static Set<FeatureFlags> defaults() {
        return EnumSet.noneOf(FeatureFlags.class);
    }
}
