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
 *
 * These tests comes from JS JSON-Patch libraries (
 * https://github.com/Starcounter-Jack/JSON-Patch/blob/master/test/spec/json-patch-tests/tests.json
 * https://github.com/cujojs/jiff/blob/master/test/json-patch-tests/tests.json)
 */
public class JsLibSamplesTest extends AbstractPatchTest {

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<TestCase> data() throws IOException {
        List<TestCase> tests = new ArrayList<TestCase>();
        for (Set<FeatureFlags> flags : new Set[]{
                EnumSet.of(FeatureFlags.LCS_VISIT_PATCH_GENERATOR),
                EnumSet.of(FeatureFlags.LCS_ITERATE_PATCH_GENERATOR),
                EnumSet.of(FeatureFlags.SIMPLE_COMPARE_PATCH_GENERATOR)
        }) {
            for (TestCase test : TestCase.load("js-libs-samples")) {
                tests.add(test.addFalgs(flags));
            }
        }
        return tests;
    }
}
