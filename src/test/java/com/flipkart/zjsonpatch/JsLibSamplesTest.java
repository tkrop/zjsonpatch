package com.flipkart.zjsonpatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.runners.Parameterized;

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
                EnumSet.of(FeatureFlags.ENABLE_ORIG_PATCH_GENERATOR),
                EnumSet.of(FeatureFlags.ENABLE_OPT_PATCH_GENERATOR),
                EnumSet.of(FeatureFlags.ENABLE_FAST_PATCH_GENERATOR),
                EnumSet.of(FeatureFlags.ENABLE_SAME_PATCH_GENERATOR)
        }) {
            for (TestCase test : TestCase.load("js-libs-samples")) {
                tests.add(test.addFalgs(flags));
            }
        }
        return tests;
    }
}
