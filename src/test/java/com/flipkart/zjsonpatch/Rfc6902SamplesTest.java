package com.flipkart.zjsonpatch;

import java.io.IOException;
import java.util.Collection;

import org.junit.runners.Parameterized;

/**
 * @author ctranxuan (streamdata.io).
 */
public class Rfc6902SamplesTest extends AbstractPatchTest {

    @Parameterized.Parameters
    public static Collection<TestCase> data() throws IOException {
        return TestCase.load("rfc6902-samples");
    }
}
