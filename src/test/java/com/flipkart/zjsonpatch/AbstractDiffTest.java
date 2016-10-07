package com.flipkart.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Set;

/**
 * Abstract JSON Diff test.
 */
@RunWith(Parameterized.class)
public abstract class AbstractDiffTest {

    @Parameter
    public TestCase p;
    private static final class Actual {
        public JsonNode patch;
        public JsonNode second;
    }

    private Actual operation(JsonNode first, JsonNode second) {
        Actual actual = new Actual();
        Set<CompatibilityFlags> flags = p.getFlags();
        if (flags == null) {
            actual.patch = JsonDiff.asJson(first, second);
            actual.second = JsonPatch.apply(actual.patch, first);
            return actual;
        }
        actual.patch = JsonDiff.asJson(first, second, flags);
        actual.second = JsonPatch.apply(actual.patch, first, flags);
        return actual;
    }

    @Test
    public void test() throws Exception {
        if (p.isOperation()) {
            testOpertaion();
        } else {
            testError();
        }
    }

    private void testOpertaion() throws Exception {
        JsonNode node = p.getNode();

        JsonNode first = node.get("first");
        JsonNode second = node.get("second");
        JsonNode patch = node.get("patch");
        String message = node.has("message") ? node.get("message").toString() : "no message";

        Actual actual = operation(first, second);

        assertThat(message, actual.patch, equalTo(patch));
        assertThat(message, actual.second, equalTo(second));
    }

    private void testError() {
        JsonNode node = p.getNode();

        JsonNode first = node.get("first");
        JsonNode patch = node.get("patch");
        String error = node.has("error") && !node.get("error").isNull() ? node.get("error").asText() : null;

        try {
            JsonPatch.apply(patch, first);

            fail("Failure expected: " + ((error != null) ? error : node.get("message")));
        } catch (Exception ex) {
            assertThat(ex.toString(), equalTo(error));
        }
    }
}