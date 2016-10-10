package org.zalando.zjsonpatch;

import org.junit.runners.Parameterized;
import org.zalando.zjsonpatch.FeatureFlags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.zalando.zjsonpatch.FeatureFlags.LCS_ITERATE_PATCH_GENERATOR;
import static org.zalando.zjsonpatch.FeatureFlags.LCS_VISIT_PATCH_GENERATOR;
import static org.zalando.zjsonpatch.FeatureFlags.SIMPLE_COMPARE_PATCH_GENERATOR;

/**
 * JSON Patch test.
 */
public class JsonPatchSamplesTest extends AbstractPatchTest {

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<TestCase> data() throws IOException {
        List<TestCase> tests = new ArrayList<TestCase>();
        for (Set<FeatureFlags> flags : new Set[]{
                null, EnumSet.noneOf(FeatureFlags.class),
                EnumSet.of(LCS_VISIT_PATCH_GENERATOR),
                EnumSet.of(LCS_ITERATE_PATCH_GENERATOR),
                EnumSet.of(SIMPLE_COMPARE_PATCH_GENERATOR)
        }) {
            for (TestCase test : TestCase.load("patch-samples")) {
                tests.add(test.addFalgs(flags));
            }
        }
        return tests;
    }
}
