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
 */
public class Rfc6902SamplesTest extends AbstractPatchTest {

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<TestCase> data() throws IOException {
        List<TestCase> tests = new ArrayList<TestCase>();
        for (Set<CompatibilityFlags> flags : new Set[]{
                EnumSet.of(CompatibilityFlags.ENABLE_ORIG_PATCH_GENERATOR),
                EnumSet.of(CompatibilityFlags.ENABLE_OPT_PATCH_GENERATOR),
                EnumSet.of(CompatibilityFlags.ENABLE_FAST_PATCH_GENERATOR),
                EnumSet.of(CompatibilityFlags.ENABLE_SAME_PATCH_GENERATOR)
        }) {
            for (TestCase test : TestCase.load("rfc6902-samples")) {
                tests.add(test.addFalgs(flags));
            }
        }
        return tests;
    }
}
