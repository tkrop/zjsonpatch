package org.zalando.zjsonpatch;

import com.fasterxml.jackson.databind.JsonNode;
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

@RunWith(Parameterized.class)
public abstract class AbstractPatchTest {

    @Parameter
    public TestCase p;

    private JsonNode operation(JsonNode source, JsonNode target, JsonNode patch) {
        Set<FeatureFlags> flags = p.getFlags();
        if (flags == null || flags.isEmpty()) {
            if (patch == null && target != null) {
                patch = JsonPatch.create(source, target);
            }
            JsonPatch.validate(patch);
            return JsonPatch.apply(patch, source.deepCopy());
        }
        if (patch == null && target != null) {
            patch = JsonPatch.create(source, target, flags);
        }
        JsonPatch.validate(patch, flags);
        return JsonPatch.apply(patch, source.deepCopy(), flags);
    }

    @Test
    public void test() throws Exception {
        if (p.isEnabled()) {
            if (p.isOperation()) {
                testOpertaion();
            } else {
                testError();
            }
        }
    }

    private void testOpertaion() throws Exception {
        JsonNode node = p.getNode();

        JsonNode source = node.get("source");
        JsonNode target = node.get("target");
        JsonNode patch = node.get("patch");
        String message = node.has("message") ? node.get("message").toString() : "";

        final JsonNode actualSecond = operation(source, target, patch);

        assertThat(message, actualSecond, equalTo(target));
    }

    private void testError() {
        JsonNode node = p.getNode();

        JsonNode source = node.get("source");
        JsonNode target = node.get("target");
        JsonNode patch = node.get("patch");
        String error = node.has("error") && !node.get("error").isNull() ? node.get("error").asText() : null;

        try {
            operation(source, target, patch);
            fail("Failure expected: " + ((error != null) ? error : node.get("message")));
        } catch (Exception ex) {
            if (error != null) {
                assertThat(ex.toString(), containsString(error));
            }
        }
    }
}
