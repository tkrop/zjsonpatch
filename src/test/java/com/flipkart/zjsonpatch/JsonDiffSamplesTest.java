package com.flipkart.zjsonpatch;

import java.io.IOException;
import java.util.Collection;

import org.junit.runners.Parameterized;

/**
 * JSON Diff sample test.
 */
public class JsonDiffSamplesTest extends AbstractDiffTest {

    @Parameterized.Parameters(name="{index}: {0}")
    public static Collection<TestCase> data() throws IOException {
        return TestCase.load("diff-samples");
    }
}
