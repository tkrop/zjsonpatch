package org.zalando.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.zalando.zjsonpatch.FeatureFlags;
import org.zalando.zjsonpatch.JsonPatch;

import java.util.Set;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Abstract JSON Diff test.
 */
@RunWith(Parameterized.class)
public abstract class AbstractDiffTest {

    @Parameter
    public TestCase p;

    private static final class Actual {
        public JsonNode patch;
        public JsonNode target;
    }

    private Actual operation(JsonNode source, JsonNode target) {
        Actual actual = new Actual();
        Set<FeatureFlags> flags = p.getFlags();
        if (flags == null || flags.isEmpty()) {
            actual.patch = JsonPatch.create(source, target);
            actual.target = JsonPatch.apply(actual.patch, source.deepCopy());
            return actual;
        }
        actual.patch = JsonPatch.create(source, target, flags);
        actual.target = JsonPatch.apply(actual.patch, source.deepCopy(), flags);
        return actual;
    }

    @Test
    public void test() throws Exception {
        Assume.assumeTrue("is disabled", p.isEnabled());
        if (p.isOperation()) {
            testOpertaion();
        } else {
            testError();
        }
    }

    private void testOpertaion() throws Exception {
        JsonNode node = p.getNode();

        JsonNode source = node.get("source");
        JsonNode target = node.get("target");
        JsonNode patch = node.get("patch");
        String message = node.has("message") ? node.get("message").toString() : "no message";

        Actual actual = operation(source, target);

        assertThat(message, actual.patch, equalTo(patch));
        assertThat(message, actual.target, equalTo(target));
    }

    private void testError() {
        JsonNode node = p.getNode();

        JsonNode first = node.get("source");
        JsonNode patch = node.get("patch");
        String error = node.has("error") && !node.get("error").isNull() ? node.get("error").asText() : null;

        try {
            JsonPatch.apply(patch, first);

            fail("Failure expected: " + ((error != null) ? error : node.get("message")));
        } catch (Exception ex) {
            assertThat(ex.toString(), containsString(error));
        }
    }
}
