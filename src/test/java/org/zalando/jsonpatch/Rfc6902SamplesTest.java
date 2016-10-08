package org.zalando.jsonpatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.runners.Parameterized;
import org.zalando.jsonpatch.FeatureFlags;

/**
 * @author ctranxuan (streamdata.io).
 */
public class Rfc6902SamplesTest extends AbstractPatchTest {

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<TestCase> data() throws IOException {
        List<TestCase> tests = new ArrayList<TestCase>();
        for (Set<FeatureFlags> flags : new Set[]{
                EnumSet.of(FeatureFlags.LCS_VISIT_PATCH_GENERATOR),
                EnumSet.of(FeatureFlags.LCS_ITERATE_PATCH_GENERATOR),
                EnumSet.of(FeatureFlags.SIMPLE_COMPARE_PATCH_GENERATOR)
        }) {
            for (TestCase test : TestCase.load("rfc6902-samples")) {
                tests.add(test.addFalgs(flags));
            }
        }
        return tests;
    }
}
