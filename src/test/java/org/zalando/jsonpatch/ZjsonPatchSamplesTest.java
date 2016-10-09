package org.zalando.jsonpatch;

import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.zalando.jsonpatch.FeatureFlags.LCS_ITERATE_PATCH_GENERATOR;
import static org.zalando.jsonpatch.FeatureFlags.LCS_VISIT_PATCH_GENERATOR;
import static org.zalando.jsonpatch.FeatureFlags.PATCH_OPTIMIZATION;
import static org.zalando.jsonpatch.FeatureFlags.SIMPLE_COMPARE_PATCH_GENERATOR;

/**
 * JSON Patch test.
 */
public class ZjsonPatchSamplesTest extends AbstractPatchTest {

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<TestCase> data() throws IOException {
        List<TestCase> tests = new ArrayList<TestCase>();
        for (Set<FeatureFlags> flags : new Set[] {
                EnumSet.of(PATCH_OPTIMIZATION, LCS_VISIT_PATCH_GENERATOR),
                EnumSet.of(PATCH_OPTIMIZATION, LCS_ITERATE_PATCH_GENERATOR),
                EnumSet.of(PATCH_OPTIMIZATION, SIMPLE_COMPARE_PATCH_GENERATOR)
        }) {
            for (TestCase test : TestCase.load("zjsonpatch-samples")) {
                tests.add(test.addFalgs(flags));
            }
        }
        return tests;
    }
}
