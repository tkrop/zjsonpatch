package com.flipkart.zjsonpatch;

import java.io.IOException;
import java.util.Collection;

import org.junit.runners.Parameterized;

/**
 * JSON Patch test.
 */
public class JsonPatchSamplesTest extends AbstractPatchTest {

    public JsonPatchSamplesTest() {
        super(false);
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<TestCase> data() throws IOException {
        return TestCase.load("patch-samples");
    }
}
